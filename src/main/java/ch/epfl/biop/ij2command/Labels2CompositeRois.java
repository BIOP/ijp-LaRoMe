/*-
 * #%L
 * LaRoMe - BIOP - EPFL
 * %%
 * Copyright (C) 2019 - 2022 ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package ch.epfl.biop.ij2command;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p>
 * A newer version that makes use of the same approach as Particle Analyzer
 * https://github.com/imagej/ImageJ/blob/c85560833a24dde2576494ab1d0e6c772e6c66ac/ij/plugin/filter/ParticleAnalyzer.java#L906
 * We also tried to make it parallel. This seems to cut the time by about 4x
 * It also allows for arbitrary roi shapes, which was not possible in the original
 * </p>
 */
@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Image Analysis>ROIs>Label image to composite ROIs")
public class Labels2CompositeRois implements Command {

    @Parameter
    ImagePlus imp;

    @Parameter
    RoiManager rm;

    private static Logger log = LoggerFactory.getLogger(Labels2CompositeRois.class);

    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {

        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }

    @Override
    public void run() {

        // Log time taken
        long start = System.currentTimeMillis();

        // Help format resulting time with 2 decimal places
        DecimalFormat df = new DecimalFormat("#.##");

        rm.reset();

        if (imp.getNDimensions() > 3) {
            log.error(imp.getTitle() + " is a hyperstack (multi c , z or t), please prepare a stack (single c, either z-stack or t-stack) from it.");
            return;
        }

        if (imp.isRGB()) {
            log.warn(imp.getTitle() + " is an RGB image. Labels might be inconsistent. Check the results to ensure they are what you expect.");
        }

        // Pick up the ROIs from all ImageProcessors as a flat map. Avoid adding it in parallel to the ROI Manager, so we do it later
        List<Roi> allRois = IntStream.range(0, imp.getImageStackSize()).parallel()
                .mapToObj(i -> L2Rp(imp.getStack().getProcessor(i + 1), i + 1))
                .flatMap(r -> r.stream()).collect(Collectors.toList());


        log.info("Adding " + allRois.size() + " Rois to the ROI Manager");
        for (Roi r : allRois) {
            // This should avoid GUI updates, but we need to cast null to ImagePlus xD
            // Otherwise, it becomes an ambiguous call as it clashes with another add() method.
            rm.add((ImagePlus) null, r, -1);
        }

        long end = System.currentTimeMillis();
        log.info("Label extraction took " + df.format((end - start) / 1000) + " s");
    }

    /**
     * Converts Labels to ROIs by iteratively thresholding the label image
     *
     * @param ip       the processor to extract the data from
     * @param position the position of that processor in the image stack
     * @return a list of ImageJ Rois
     */
    public static List<Roi> L2Rp(ImageProcessor ip, int position) {
        log.info("Getting Labels for Slice " + position);

        // Keep it simple, convert everything to a double to avoid dealing with
        // different pixel types
        // NOTE: that RGB data needs to be dealt with differently, as converting to float just averages the RGB channels
        float[] pixels;
        if (ip instanceof ColorProcessor) {
            ip = getRGBPixels((ColorProcessor) ip);
        } else {
            ip = ip.convertToFloatProcessor();
        }
        pixels = (float[]) ip.getPixels();

        double max = ip.getMax();

        // Find unique values for the labels
        // Pre-initialize with the max label value, so the worse thing that can happen is that
        // The map is a bit too big.
        Set<Float> labels = new HashSet<>((int) 1000);

        //make hash to find uniques
        for (int j = 0; j < pixels.length; j++) labels.add(pixels[j]);

        // Remove label 0
        labels.remove(new Float(0));

        // Threshold the image with each min max corresponding to the label ID
        ImageProcessor finalIp = ip;
        List<Roi> rois = labels.parallelStream().map(lab -> {
            ImageProcessor ip2 = (ImageProcessor) finalIp.clone(); // deep copy, so big memory footprint
            ip2.setThreshold(lab, lab, ImageProcessor.NO_LUT_UPDATE);
            Roi roi = new ThresholdToSelection().convert(ip2);
            roi.setPosition(position);

            // Name the Roi with the position in the stack followed by the label ID
            String roiName = String.format("%04d", position) + " - ID " + String.format("%04d", lab.intValue());
            roi.setName(roiName);
            return roi;
        }).collect(Collectors.toList());

        return rois;
    }

    /**
     * Get RGB pixel values as floats, without using tofloatProcessor, which averages
     * the 3 channels, which is not what we want.
     * @param ip the ColorProcessor to convert
     * @return a floatProcessor
     */
    protected static FloatProcessor getRGBPixels(ColorProcessor ip) {
        int[] pixels = (int[]) ip.getPixels();
        float[] fp = new float[pixels.length];

        for (int i = 0; i < pixels.length; i++) {
            // add 2^24 so that the minimum value is 0
            fp[i] = (float) pixels[i] + (float) Math.pow(2, 24);
        }
        return new FloatProcessor(ip.getWidth(), ip.getHeight(), fp);
    }
}

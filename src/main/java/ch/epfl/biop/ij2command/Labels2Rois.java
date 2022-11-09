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

import ij.ImagePlus ;
import ij.IJ;
import ij.gui.Wand ;
import ij.gui.PolygonRoi ;
import ij.gui.Roi ;
import ij.plugin.Duplicator;
import ij.plugin.frame.RoiManager ;
import ij.process.ImageProcessor;

import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.stream.IntStream;

/**
 * <p>
 * The code here is making ROIs out of a labels image.
 * Our first implementation was rather slow , until our gifted Oli thought about
 * stealing Pete Bankhead great idea of using the magic wand to select each ROIs.
 *
 * </p>
 */

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Image Analysis>ROIs>Label image to ROIs")
public class Labels2Rois implements Command {

    @Parameter
    ImagePlus imp ;

    @Parameter
    RoiManager rm ;

    ArrayList<Roi> rois;

    @Override
    public void run() {

        ImagePlus copy = imp.duplicate();
        // reset RoiManager
        rm.reset();
        rois = new ArrayList<Roi>();

        // get the imp dimension (width, height, nChannels, nSlices, nFrames)
        int[] dimensions = copy.getDimensions();

        int nChannels   = dimensions[2];
        int nSlices     = dimensions[3];
        int nFrames     = dimensions[4];

        if ( ((nChannels>1)&&(nSlices>1)) || ((nChannels>1)&&(nFrames>1)) || ((nSlices>1)&&(nFrames>1))){
            System.err.println(""+copy.getTitle()+" is a hyperstack (multi c , z or t), please prepare a stack (single c, either z-stack or t-stack) from it.");
            return;
        } else {
            //System.out.println(""+imp.getTitle()+" is a stack of size"+copy_imp.getImageStackSize() );
            for (int i = 0; i < copy.getImageStackSize(); i++) {
                ImageProcessor ip = copy.getImageStack().getProcessor(i + 1);
                L2R(ip, i + 1);
            }
        }

        for (Roi r : rois)
         {
            // This should avoid GUI updates, but we need to cast null to ImagePlus xD
            // Otherwise, it becomes an ambiguous call as it clashes with another add() method.
            rm.add((ImagePlus) null, r, -1);
        }
    }

    private void L2R(ImageProcessor ip, int position){
        Wand wand = new Wand( ip );

        // create range list
        int width = ip.getWidth();
        int height = ip.getHeight();

        int[] pixel_width = new int[ width ];
        int[] pixel_height = new int[ height ];

        IntStream.range(0,width-1).forEach(val -> pixel_width[val] = val);
        IntStream.range(0,height-1).forEach(val -> pixel_height[val] = val);

        /*
         * Will iterate through pixels, when getPixel > 0 ,
         * then use the magic wand to create a roi
         * finally set value to 0 and add to the roiManager
         */

        // will "erase" found ROI by setting them to 0
        ip.setColor(0);

        for ( int y_coord : pixel_height) {
            for (int x_coord : pixel_width) {
                float val = ip.getf(x_coord, y_coord);
                if (val > 0.0) {
                    // use the magic wand at this coordinate
                    wand.autoOutline(x_coord, y_coord, val, val);
                    // if there is a region , then it has npoints
                    if (wand.npoints > 0) {
                        // get the Polygon, fill with 0 and add to the manager
                        Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.FREEROI);
                        roi.setPosition(position);
                        // Name the Roi with the position in the stack followed by the label ID
                        String roiName = String.format("%04d", position) + " - ID " + String.format("%04d", new Float(val).intValue());
                        roi.setName(roiName);
                        // ip.fill should use roi, otherwise make a rectangle that erases surrounding pixels
                        rois.add(roi);
                        ip.fill(roi);
                    }
                }
            }
        }
    }


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
        Boolean test_with_single_image = false ;

        if (test_with_single_image){ // test on a single image, the famous blobs
            ImagePlus imp = IJ.openImage("http://imagej.nih.gov/ij/images/blobs.gif");
            imp.show();
            IJ.setAutoThreshold(imp, "Default");
            IJ.run(imp, "Analyze Particles...", "  show=[Count Masks]");
        } else { // or test on a stack
            ImagePlus stk_imp = IJ.openImage("http://wsr.imagej.net/images/confocal-series.zip");
            ImagePlus c1_imp = new Duplicator().run(stk_imp, 1, 1, 1, stk_imp.getNSlices(), 1, 1);
            //ImagePlus c1_imp = new Duplicator().run(stk_imp, 1, 1, 1, 1, 1, 1);
            IJ.run(c1_imp, "Median...", "radius=2 stack");
            c1_imp.show();
            IJ.setAutoThreshold(c1_imp, "Default dark");
            IJ.run(c1_imp, "Convert to Mask", "method=Default background=Dark calculate black");
            IJ.run(c1_imp,"Analyze Particles...", "  show=[Count Masks] stack");
        }
        // will run on the current image
        ij.command().run(Labels2Rois.class, true);
    }
}



package ch.epfl.biop.ij2command;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * <p>
 * The code here is making ROIs out of a labels image.
 * Our first implementation was rather slow , until our gifted Oli thought about
 * stealing Pete Bankhead great idea of using the magic wand to select each ROIs.
 *
 * </p>
 */

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Image Analysis>ROIs>ROIs to Label image")
public class Rois2Labels implements Command {

    @Parameter
    ImagePlus imp ;

    @Parameter
    RoiManager rm ;

    @Override
    public void run() {

        // reset RoiManager
        RoiManager rm = RoiManager.getRoiManager();

        // duplicate the processor for the task
        ImageProcessor ip = imp.getProcessor().duplicate();
        ip.setValue(0.0);
        ip.fill(); // reset image

        // go throught the ROIs an assigned a value
        Roi[] rois = rm.getRoisAsArray()  ;
        for (int i = 0; i < rois.length; i++) {
            ip.setValue(i+1);
            ip.fill( rois[i] );
        }

        ImagePlus imp2 = new ImagePlus("LabelImage" , ip);
        imp2.show();

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
        ImagePlus imp = IJ.openImage("http://imagej.nih.gov/ij/images/blobs.gif");

        IJ.setAutoThreshold(imp, "Default");

        IJ.run(imp, "Analyze Particles...", "  show=[Count Masks]");
        IJ.run(imp, "Label image to ROIs", "");

        ij.command().run(Rois2Labels.class, true);
    }
}



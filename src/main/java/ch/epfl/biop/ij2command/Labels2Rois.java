package ch.epfl.biop.ij2command;

import ij.ImagePlus ;
import ij.IJ;
import ij.gui.Wand ;
import ij.gui.PolygonRoi ;
import ij.gui.Roi ;
import ij.plugin.frame.RoiManager ;
import ij.process.ImageProcessor;

import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

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

    @Override
    public void run() {

        // reset RoiManager
        RoiManager rm = RoiManager.getRoiManager();
        rm.reset();

        Wand wand = new Wand( imp.getProcessor() );

        // get the imp dimension, a create range list
        int[] dimensions = imp.getDimensions()    ;

        int width = dimensions[0];
        int height = dimensions[1];

        int[] pixel_width = new int[ width ];
        int[] pixel_height = new int[ height ];

        IntStream.range(0,width-1).forEach(val -> pixel_width[val] = val);
        IntStream.range(0,height-1).forEach(val -> pixel_height[val] = val);

        /*
         * Will iterate through pixels, when getPixel > 0 ,
         * then use the magic wand to create a roi
         * finally set value to 0 and add to the roiManager
         */

        // duplicate the processor for the task
        ImageProcessor ip = imp.getProcessor().duplicate();
        // will "erase" found ROI by setting them to 0
        ip.setColor(0);

        for ( int y_coord : pixel_height) {
            for ( int x_coord : pixel_width) {
                if ( ip.getPixel( x_coord, y_coord ) > 0.0 ){
                    // use the magic wand at this coordinate
                    wand.autoOutline( x_coord, y_coord );
                    // if there is a region , then it has npoints
                    if ( wand.npoints > 0 ) {
                        // get the Polygon, fill with 0 and add to the manager
                        Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.TRACED_ROI);
                        ip.setRoi( roi );
                        ip.fill();
                        rm.addRoi( roi );
                    }
                }
            }
        }

            rm.runCommand( imp , "Show All" );

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

        ij.command().run(Labels2Rois.class, true);
    }
}



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

    @Override
    public void run() {

        ImagePlus copy_imp = imp.duplicate();
        // reset RoiManager
        rm.reset();
        // get the imp dimension (width, height, nChannels, nSlices, nFrames)
        int[] dimensions = copy_imp.getDimensions() ;

        int nChannels   = dimensions[2];
        int nSlices     = dimensions[3];
        int nFrames     = dimensions[4];

        if ( ((nChannels>1)&&(nSlices>1)) || ((nChannels>1)&&(nFrames>1)) || ((nSlices>1)&&(nFrames>1))){
            System.out.println(""+imp.getTitle()+" is a hyperstack (multi c , z or t), please prepare a stack (single c, either z-stack or t-stack) from it.");
            return;
        } else if ((nChannels>1)||(nSlices>1)||(nFrames>1)){
            System.out.println(""+imp.getTitle()+" is a stack of size"+copy_imp.getImageStackSize() );
            for (int i = 0; i < copy_imp.getImageStackSize(); i++) {
                // changes to imp.getStack().getProcessor(i)
                copy_imp.setPosition(i+1);
                L2R( copy_imp );
                //
                // Versus, but actually lost Stack pos!
                //ImageProcessor ip = imp.getStack().getProcessor(i+1);// should not be messed by GUI
                //L2Rprocessor( ip ); // change to processor
            }
        } else {
            System.out.println(""+imp.getTitle()+" is a single image");
            //ImageProcessor ip = imp.getProcessor();
            //L2Rprocessor( ip );
            L2R( copy_imp );
        }
        copy_imp.show();
    }

    private void L2R(ImagePlus imp){

        ImageProcessor ip = imp.getProcessor();
        Wand wand = new Wand( ip );

        // create range list
        int width = imp.getWidth();
        int height = imp.getHeight();

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
                        roi.setPosition( imp.getSlice() );
                        rm.addRoi( roi );
                    }
                }
            }
        }
        rm.runCommand( imp , "Show All" );
        return;
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
            //ImagePlus c1_imp = new Duplicator().run(stk_imp, 1, 1, 1, stk_imp.getNSlices(), 1, 1);
            ImagePlus c1_imp = new Duplicator().run(stk_imp, 1, 1, 1, 1, 1, 1);
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



package ch.epfl.biop.ij2command;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.Duplicator;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.process.ImageStatistics;

import static javax.xml.bind.DatatypeConverter.parseFloat;

/**
 *
 * <p>
 * The code here is making ROIs out of a labels image.
 * Our first implementation was rather slow , until our gifted Oli thought about
 * stealing Pete Bankhead great idea of using the magic wand to select each ROIs.
 *
 * </p>
 */

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Image Analysis>ROIs>ROIs to Measurement Image")
public class Rois2MeasurementMap implements Command {

    @Parameter
    ImagePlus imp ;

    @Parameter
    RoiManager rm ;

    @Parameter ( label="Measure", choices = {"Area", "Angle", "AngleVert","AR", "Circ.", "Major","Minor","Mean","Median","Mode","Min","Max", "Perim.","Track"})
    String column_name;

    @Override
    public void run() {

        ImagePlus results_imp = R2M( imp );
        results_imp.show();

    }


    private ImagePlus R2M(ImagePlus imp ){

        // duplicate the imp for the task
        ImagePlus imp2 = imp.duplicate();
        // check if it's a stack
        int stackN = imp2.getImageStackSize();
        boolean isStack = false ;
        if (stackN > 1 ) isStack = true ;
        // reset imp2
        for (int i = 0; i < stackN ; i++) {
            imp2.getStack().getProcessor(i + 1).setValue(0.0);
            imp2.getStack().getProcessor(i + 1).fill();
        }
        // convert to 32-bit (because measurements can be float , or negative)
        IJ.run(imp2, "32-bit", "");
        // initiate the filling value
        double filling_value = 0.0;

        Roi[] rois = rm.getRoisAsArray()  ;
        for (int i = 0; i < rois.length; i++) {
            //set the position in the stack if necessary
            if ( isStack )  imp2.setPosition( rois[i].getPosition() );
            // and set the ROI
            imp2.setRoi( rois[i]);
            // so we can get Stats
            ImageStatistics ip_stats = imp2.getProcessor().getStatistics() ;
            System.out.println( ip_stats);
            // from user choice
            switch (column_name) {
                case "Area" :
                    filling_value = ip_stats.area;
                    break;
                case "Angle" :
                    filling_value = ip_stats.angle;
                    break;
                // the Angle measure is based on horizontal,
                // the AngleVert measure is based on vertical (substracting 90 )
                case "AngleVert" :
                    filling_value = ip_stats.angle - 90 ;
                    break;
                case "AR" :
                    filling_value = ip_stats.major / ip_stats.minor;
                    break;
                case "Circ." :   // 4 x  pi x area / perimeter^2
                    filling_value = 4*Math.PI * ip_stats.area / Math.pow(rois[i].getLength() , 2 );
                    break;
                case "Major":
                    filling_value = ip_stats.major;
                    break;
                case "Minor" :
                    filling_value = ip_stats.minor;
                    break;
                case "Mean" :
                    filling_value = ip_stats.mean;
                    break;
                case "Median" :
                    filling_value = ip_stats.median;
                    break;
                case "Mode" :
                    filling_value = ip_stats.mode;
                    break;
                case "Min" :
                    filling_value = ip_stats.min;
                    break;
                case "Max" :
                    filling_value = ip_stats.max;
                    break;
                case "Perim.":
                    filling_value = rois[i].getLength();
                    break;
                case "Track":
                    // roi_name follows the model Track-0001:Frame-0001 ...
                    // which corresponds to an output from a custom script making use of TrackMate, to link rois
                    // value will be the number of the track, hijacking Rois2Measurements to make a  Rois2Labels
                    String roi_name = rois[i].getName();
                    if (roi_name.matches("Track-.*")) {
                        String[] splitted_roi_name = roi_name.split(":");
                        filling_value = parseFloat( splitted_roi_name[0].split("-")[1] );
                    }else{ // orphan ROIs won't be filled
                        filling_value = 0.0;
                    }
                    break;
            }

            imp2.getProcessor().setValue( filling_value );
            imp2.getProcessor().fill( rois[i] );

        }
        imp2.setTitle(column_name +"_Image");
        return imp2;
        //ImagePlus imp2 = new ImagePlus(column_name +"_Image" , ip_result );
        //imp2.show();
        //IJ.resetMinAndMax(imp2);

        /*
        if (rtw != null){
            existingTable = true ;
            rtw.rename("Results");
        }
        */
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
        RoiManager rm = RoiManager.getRoiManager();

        Boolean test_with_single_image = true ;
        ImagePlus imp = new ImagePlus();
        if (test_with_single_image){ // test on a single image, the famous blobs
             imp = IJ.openImage("http://imagej.nih.gov/ij/images/blobs.gif");
            imp.show();
            IJ.setAutoThreshold(imp, "Default");
            IJ.run(imp, "Analyze Particles...", "  show=[Count Masks]");

        } else { // or test on a stack
            ImagePlus stk_imp = IJ.openImage("http://wsr.imagej.net/images/confocal-series.zip");
            imp = new Duplicator().run(stk_imp, 1, 1, 1, stk_imp.getNSlices(), 1, 1);
            //ImagePlus c1_imp = new Duplicator().run(stk_imp, 1, 1, 1, 1, 1, 1);
            IJ.run(imp, "Median...", "radius=2 stack");
            imp.show();
            IJ.setAutoThreshold(imp, "Default dark");
            IJ.run(imp, "Convert to Mask", "method=Default background=Dark calculate black");
            IJ.run(imp,"Analyze Particles...", "  show=[Count Masks] stack");
        }

        IJ.run(imp, "Label image to ROIs", "");
        ij.command().run(Rois2MeasurementMap.class, true, "column_name","Area");
    }
}



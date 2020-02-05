package ch.epfl.biop.ij2command;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.process.ImageStatistics;

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

    @Parameter ( label="Measure", choices = {"Area", "Angle", "AngleVert","AR", "Circ.", "Major","Minor","Mean","Median","Mode","Min","Max", "Perim."})
    String column_name;

    @Override
    public void run() {

        // reset RoiManager
        RoiManager rm = RoiManager.getRoiManager();

        ImageProcessor ip_ori = imp.getProcessor().duplicate();
        // duplicate the processor for the task
        // as a 32-bit (because measurements can be float , or negative)
        ImageProcessor ip_result = imp.getProcessor().duplicate().convertToFloat();
        ip_result.setValue(0.0);
        ip_result.fill(); // reset image

        Roi[] rois = rm.getRoisAsArray()  ;
        for (int i = 0; i < rois.length; i++) {

            ip_ori.setRoi( rois[i]);
            ImageStatistics ip_ori_stats = ip_ori.getStatistics() ;
            double filling_value = 0.0;

            switch (column_name) {
                case "Area" :   filling_value = ip_ori_stats.area;
                                break;
                case "Angle" :  filling_value = ip_ori_stats.angle;
                                break;
                // the Angle measure is based on horizontal,
                // the AngleVert measure is based on vertical (substracting 90 )
                case "AngleVert" :  filling_value = ip_ori_stats.angle - 90 ;
                                break;
                case "AR" :     filling_value = ip_ori_stats.major / ip_ori_stats.minor;
                                break;
                case "Circ." :   // 4 x  pi x area / perimeter^2
                                filling_value = 4*Math.PI * ip_ori_stats.area / Math.pow(rois[i].getLength() , 2 );
                                break;
                case "Major":   filling_value = ip_ori_stats.major;
                                break;
                case "Minor" :  filling_value = ip_ori_stats.minor;
                                break;
                case "Mean" :   filling_value = ip_ori_stats.mean;
                                break;
                case "Median" : filling_value = ip_ori_stats.median;
                                break;
                case "Mode" :   filling_value = ip_ori_stats.mode;
                                break;
                case "Min" :    filling_value = ip_ori_stats.min;
                                break;
                case "Max" :    filling_value = ip_ori_stats.max;
                                break;
                case "Perim.":   filling_value = rois[i].getLength();
                    break;
            }

            ip_result.setValue( filling_value );
            ip_result.fill( rois[i] );

        }


        ImagePlus imp2 = new ImagePlus(column_name +"_Image" , ip_result );
        imp2.show();
        IJ.resetMinAndMax(imp2);

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
        ImagePlus imp = IJ.openImage("http://imagej.nih.gov/ij/images/blobs.gif");

        IJ.setAutoThreshold(imp, "Default");

        IJ.run(imp, "Analyze Particles...", "  show=[Count Masks]");
        IJ.run(imp, "Label image to ROIs", "");

        RoiManager rm = RoiManager.getRoiManager();

        ij.command().run(Rois2MeasurementMap.class, true, "column_name","Area");
    }
}



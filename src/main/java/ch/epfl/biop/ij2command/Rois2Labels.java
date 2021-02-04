package ch.epfl.biop.ij2command;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
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

        // get the imp dimension (width, height, nChannels, nSlices, nFrames)
        int[] dimensions = imp.getDimensions()    ;

        int nChannels   = dimensions[2];
        int nSlices     = dimensions[3];
        int nFrames     = dimensions[4];

        ImagePlus label_imp;

        if ( ((nChannels>1)&&(nSlices>1)) || ((nChannels>1)&&(nFrames>1)) || ((nSlices>1)&&(nFrames>1))){
            System.err.println(""+imp.getTitle()+" is a hyperstack (multi c , z or t), please prepare a stack (single c, either z-stack or t-stack) from it.");
            return;
        }
        label_imp = R2L( imp );
        label_imp.show();
    }

    private ImagePlus R2L(ImagePlus imp ){

        // duplicate the imp for the task and get the rois
        ImagePlus label_imp = imp.duplicate();
        Roi[] rois = rm.getRoisAsArray();

        int stackN = label_imp.getImageStackSize();
        boolean isStack = false ;
        if (stackN > 1 ) isStack = true ;

        //TODO : discuss if this a good idea or force 16 or 32-bit ?
        if (rois.length < 255) IJ.run(label_imp, "8-bit", "");
        else if ((rois.length > 255)&&(rois.length < 65535) ) IJ.run(label_imp, "16-bit", "");
        else IJ.run(label_imp, "32-bit", "");

        // reset label imp
        for (int i = 0; i < stackN ; i++) {
            label_imp.getStack().getProcessor(i + 1).setValue(0.0);
            label_imp.getStack().getProcessor(i + 1).fill();
        }

        // go throught the ROIs and assigned a value
        //int pos=0;
        for (int i = 0; i < rois.length; i++) {

           // if (isStack) pos = rois[i].getPosition();

           // label_imp.getStack().getProcessor( pos+1 ).setValue(i+1);
          //  label_imp.getStack().getProcessor( pos +1).fill( rois[i] );

            // if imp is a stack, set position based on roi position
            if (isStack) label_imp.setPosition( rois[i].getPosition());
            label_imp.getProcessor().setValue(i+1);
            label_imp.getProcessor().fill( rois[i] );
        }

        // output
        label_imp.setTitle( "ROIs2Label_"+imp.getTitle() );
        return label_imp;
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

        Boolean test_with_single_image = true ;
        if (test_with_single_image){ // test on a single image, the famous blobs
            ImagePlus imp = IJ.openImage("http://imagej.nih.gov/ij/images/blobs.gif");
            imp.show();
            IJ.setAutoThreshold(imp, "Default");
            IJ.run(imp, "Analyze Particles...", "  show=Nothing add");
        } else { // or test on a stack
            ImagePlus stk_imp = IJ.openImage("http://wsr.imagej.net/images/confocal-series.zip");
            ImagePlus c1_imp = new Duplicator().run(stk_imp, 1, 1, 1, stk_imp.getNSlices(), 1, 1);
            //IJ.run(c1_imp, "Median...", "radius=2 stack");
            c1_imp.show();
            IJ.setAutoThreshold(c1_imp, "Default dark");
            IJ.run(c1_imp, "Analyze Particles...", "  show=Nothing add stack");
        }
        // will run on the current image
        ij.command().run(Rois2Labels.class, true);
    }
}



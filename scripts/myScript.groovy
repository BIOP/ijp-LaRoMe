#@CommandService command
#@RoiManager rm

rm.reset();
IJ.run("Close All", "");

// Open blobs and detect ROIs
imp = IJ.openImage("http://imagej.nih.gov/ij/images/blobs.gif");
imp.show()
IJ.setAutoThreshold(imp, "Default");
IJ.run(imp, "Analyze Particles...", "add");

// from ROIs make a Label image
label_imp = command.run( Rois2Labels , false , 'imp' , imp , 'rm', rm).get().getOutput("label_imp")
label_imp.show()

// reset the ROImanager
rm.reset();

// from the label image make ROIs
command.run( Labels2Rois , false , 'imp' , label_imp , 'rm', rm).get()

// from the ROIs get the Area measurement map
area_imp = command.run( Rois2MeasurementMap , false , 'imp' , label_imp , 'rm', rm, 'column_name', 'Area').get().getOutput("results_imp")
area_imp.show()

import ij.*
import ch.epfl.biop.ij2command.*
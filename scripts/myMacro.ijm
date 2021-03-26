
run("Close All", "");

// Open blobs and detect ROIs
open("http://imagej.nih.gov/ij/images/blobs.gif");
setAutoThreshold("Default");
run("Analyze Particles...", "add");

// from ROIs make a Label image
run("ROIs to Label image", "")

// reset the ROImanager
roiManager("reset");

// from the label image make ROIs
run("Label image to ROIs")

// from the ROIs get the Area measurement map
run("ROIs to Measurement Image" , "column_name=Area pattern=[]");
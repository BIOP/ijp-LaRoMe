[![](https://github.com/BIOP/ijp-LaRoMe/actions/workflows/build-main.yml/badge.svg)](https://github.com/BIOP/ijp-LaRoMe/actions/workflows/build-main.yml)

# LaRoMe

## What a strange name ! 
LaRoME = Label + Region Of Interest + Measure

**Label image** (aka Count Masks): An image in which pixels of an object have all the same value. Each object has a unique value.
 
**Measurement image**: An image in which pixels of an object have all the same value, corresponding to a measurement (Area, Angle, Mean...) 

## Scripting

You can find a Macro language and a Groovy examples in the scripts folder.

## Installation

You can install LaRoMe via the PTBIOP update site.

Help>Update..., Manage Update Sites, select PTBIOP in the list (cf image below), Apply changes, Restart Fiji.

<img src="https://github.com/BIOP/ijp-LaRoMe/raw/master/images/image.png" title="update site" width="75%" align="center">

## ImageJ plugin 

Plugins > BIOP > Image Analysis > ROIs >

## run("Label image to ROIs")

From a **Label Image**(§), generate ROIs and add them to the ImageJ ROI Manager.

<img src="https://github.com/BIOP/ijp-LaRoMe/raw/master/images/Label_image_to_ROIs.png" title="Label_image_to_ROIs" width="75%" align="center">

Macro Language, you can use ```run("Label image to ROIs");``` as in the example code below : 
```
if (nImages>0) run("Label image to ROIs");
```
### Known Limitations and Advantages
> **Note**
> In the case of RGB label images (instead of 8, 16 or 32 bit images), this plugin *can* yield accurate labels so long as touching labels do not share the same RGB color. This is in contrast to `run("Label image to composite ROIs")` that will systematically put separate labels together as the same ROI if they have the same RGB color, no matter wether they are touching or not. 
> Your best best is to avoid RGB images as label images :)

> **Warning**
> [Following a post made on the ImageSC forum](https://forum.image.sc/t/label-image-to-roimanager/32940/17?u=oburri), "Label image to ROIs" **cannot handle composite ROIs*** (ROIs with holes or disconnected ROIs).
> In case you expect complex ROIs of this type, you should use `run("Label image to composite ROIs")`

## run("Label image to composite ROIs")

<img src="https://github.com/BIOP/ijp-LaRoMe/raw/master/images/Label_image_to_composite_ROIs.png" title="Label_image_to_composite_ROIs" width="75%" align="center"/>

Uses the same method as Analyse Particles with the "Composite ROIs" option checked. This method was added in November 2022 as a response to [a post made on the ImageSC forum](https://forum.image.sc/t/label-image-to-roimanager/32940/17?u=oburri)


> **Warning**
> In the case of RGB label images (instead of 8, 16 or 32 bit images), this plugin will potentially merge separate labels (that are physically apart, but share the same RGB color) into a single ROI.
> Your best best is to avoid RGB images as label images if you can :)


## run("ROIs to Label image", "")

From an **Image** and some **ROIs**, generates a **Label Image**.
<img src="https://github.com/BIOP/ijp-LaRoMe/raw/master/images/ROIs_to_Label_image.png" title="ROIs to Label Image" width="75%" align="center">

Macro Language, you can use ```run("ROIs to Label image");``` as in the example code below :  
```
if ((nImages>0)&&( roiManager("count") > 0)) run("ROIs to Label image");
else if (roiManager("count") == 0) print("No existing ROIs to make Label Image");
else if (nImages==0) print("No open image to use as template for dimensions");
```

## run("ROIs to Measurement Image", "column_name=Area");

From an **Image**, some **ROIs** and a selected **Measurement** generates a **Measurement Image**.
<img src="https://github.com/BIOP/ijp-LaRoMe/raw/master/images/ROIs_to_Measurement_Image.png" title="ROIsto Measurement Image.png" width="75%" align="center">

Measurement list encompass (change column_name ) : 
"Area", "Angle", "AngleVert","AR", "Circ.", "Major","Minor","Mean","Median","Mode","Min","Max", "Perim."

**Angle** measure is based on horizontal, the **AngleVert** measure is based on vertical (substracting 90 )
 
**AR**, Aspect Ratio = Major / Minor

**Circ.**, Aspect Ratio = 4 * PI * Area / Perim.^2


Macro Language, you can use ```run("ROIs to Measurement Image";``` as in the example code below: 

``` run("ROIs to Measurement Image" , "column_name=Area pattern=[]"); ```


With the lastest version (0.2) you can now use the ROI name with the option Pattern to define label (application for tracking or 3D-objects).
For example , using [ijs-script](https://github.com/BIOP/ijs-TrackMate/blob/master/ijs-Run-TrackMate-Using-RoiManager-or-ResultsTable.groovy) 
you can use ``Track-(\d*):.*`` to generate a label image per track.

<img src="https://github.com/BIOP/ijp-LaRoMe/raw/master/images/ROIs_to_Measurement_Image_Pattern.png" title="pattern" width="75%" align="center">

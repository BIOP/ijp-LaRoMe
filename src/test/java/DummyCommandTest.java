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

import ch.epfl.biop.ij2command.Labels2Rois;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import org.junit.Assert;
import org.junit.Test;
import org.scijava.command.CommandModule;
import java.util.concurrent.Future;

public class DummyCommandTest {

    @Test
    public void run() throws Exception {
        // Arrange
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ImagePlus imp = IJ.openImage("http://imagej.nih.gov/ij/images/blobs.gif");

        IJ.setAutoThreshold(imp, "Default");

        IJ.run(imp, "Analyze Particles...", "  show=[Count Masks]");
        // Act
        Future<CommandModule> m = ij.command().run(Labels2Rois.class, true);
    }
}

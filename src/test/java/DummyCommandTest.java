
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
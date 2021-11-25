package md.leonis.shingler.gui.crawler.moby;

import org.openimaj.image.ImageUtilities;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;

public class ImagesValidator {

    public static String dir = "C:\\Users\\user\\Documents\\";

    public static void main(String[] args) {

        isBrokenImage(dir + "test.jpg");
        isBrokenImage(dir + "test.png");
        isBrokenImage(dir + "test.gif");
        isBrokenImage(dir + "test.png.jpg");
        isBrokenImage(dir + "test.gif.jpg");
        isBrokenImage(dir + "broken-test.jpg");
        isBrokenImage(dir + "broken-test.png");
        isBrokenImage(dir + "broken-test.gif");
        isBrokenImage(dir + "broken-test.png.jpg");
        isBrokenImage(dir + "broken-test.gif.jpg");
        System.out.println("gata");
    }

    public static boolean isBrokenImage(String path) {
        try {
            ImageUtilities.readMBF(new File(path));
            return false;
        } catch (Exception e) {
            System.out.println(path + ": " + e.getMessage());
            return true;
        }
    }

    public static boolean isBrokenImage(Path path) {
        try {
            ImageUtilities.readMBF(path.toFile());
            return false;
        } catch (Exception e) {
            System.out.println(path + ": " + e.getMessage());
            return true;
        }
    }

    public static boolean isBrokenImage(String path, byte[] bytes) {
        try {
            ImageUtilities.readMBF(new ByteArrayInputStream(bytes));
            return false;
        } catch (Exception e) {
            System.out.println(path + ": " + e.getMessage());
            return true;
        }
    }
}

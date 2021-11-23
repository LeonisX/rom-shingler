package md.leonis.shingler.gui.crawler.moby;

import org.openimaj.image.ImageUtilities;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;

public class ImagesTest {

    public static String dir = "C:\\Users\\user\\Documents\\";

    public static void main(String[] args) throws FileNotFoundException {

        isValidImage(dir + "test.jpg");
        isValidImage(dir + "test.png");
        isValidImage(dir + "test.gif");
        isValidImage(dir + "test.png.jpg");
        isValidImage(dir + "test.gif.jpg");
        isValidImage(dir + "broken-test.jpg");
        isValidImage(dir + "broken-test.png");
        isValidImage(dir + "broken-test.gif");
        isValidImage(dir + "broken-test.png.jpg");
        isValidImage(dir + "broken-test.gif.jpg");
        System.out.println("gata");
    }

    public static boolean isValidImage(String path) {
        try {
            ImageUtilities.readMBF(new File(path));
            return true;
        } catch (Exception e) {
            System.out.println(path + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean isValidImage(Path path) {
        try {
            ImageUtilities.readMBF(path.toFile());
            return true;
        } catch (Exception e) {
            System.out.println(path + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean isValidImage(String path, byte[] bytes) {
        try {
            ImageUtilities.readMBF(new ByteArrayInputStream(bytes));
            return true;
        } catch (Exception e) {
            System.out.println(path + ": " + e.getMessage());
            return false;
        }
    }
}

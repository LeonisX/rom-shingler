package md.leonis.shingler.utils;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

    private static final int MAX_LENGTH = 64;

    public static String force63(String fileName) {

        String name = fileName;

        // [t1][a1][T-Port] -> [t1a1T-Port]
        // From tail consistently delete these substrings:
        String[] toDelete = new String[]{" The ", " the ", "][", ",", ";", "/", "\\", "~", "'", "\"", "`", "!", "."};

        fileName = stripExtension(name);
        String ext = getFileExtension(name);
        int maxLength = MAX_LENGTH - ext.length();

        for (String substr : toDelete) {
            fileName = deleteSubstr(substr, fileName, 0);
        }

        if (fileName.endsWith(" The")) {
            fileName = fileName.substring(0, fileName.indexOf(" The"));
        }

        name = fileName.replace("  ", " ").replace(" - ", "-").trim().replace(" ", "_");

        // If file name length <= 64 - return
        if (name.length() <= maxLength) {
            return name + ext;
        }

        // Delete all spaces, except two first, next symbol to upper case
        name = deleteSpaces(name);

        if (name.length() <= maxLength) {
            return name + ext;
        }

        // Replace " -" by " "
        name = name.replace(" -", " ");
        name = name.replace("- ", "-");

        if (name.length() <= maxLength) {
            return name + ext;
        }

        // Delete all ")", replace all "("
        name = deleteSubstr(")", name, 0);

        if (name.length() <= maxLength) {
            return name + ext;
        }
        name = name.replace("(", "-");

        // Get first 36 characters + "~" + tail.
        return name.substring(0, 36) + "~" + name.substring(name.length() - 27) + ext;
    }

    public static String removeSpecialChars(String fileName) {
        for (char c : "'[!]\"<>;/\\`~@#$%^&*()".toCharArray()) {
            fileName = fileName.replace("" + c, "");
        }
        for (char c : "=+".toCharArray()) {
            fileName = fileName.replace("" + c, "-");
        }
        return fileName;
    }

    private static String deleteSpaces(String name) {
        if (name.length() <= MAX_LENGTH || name.split(" ", -1).length - 1 <= 2) {
            return name;
        }
        int index = name.lastIndexOf(" ");
        if (index == -1) {
            return name;
        }
        return deleteSpaces(name.substring(0, index) + name.substring(index + 1));
    }

    public static String deleteSubstr(String substr, String name, int maxLength) {
        if (name.length() <= maxLength) {
            return name;
        }
        int index = name.lastIndexOf(substr);
        if (index == -1) {
            return name;
        }
        return deleteSubstr(substr, name.substring(0, index) + name.substring(index + substr.length()), maxLength);
    }

    public static List<String> toChunks(String string) {
        List<String> chunks = new ArrayList<>();

        int openSB = 0;
        int openBR = 0;

        StringBuilder chunk = new StringBuilder();

        char[] chars = string.toCharArray();
        for (char c : chars) {
            switch (c) {
                case '[':
                    if (openSB == 0 && openBR == 0 && chunk.length() > 0) {
                        chunks.add(chunk.toString().trim());
                        chunk = new StringBuilder();
                    }
                    openSB++;
                    break;
                case ']':
                    if (openSB == 0 && openBR == 0 && chunk.length() > 0) {
                        chunks.add(chunk.append(']').toString().trim());
                        chunk = new StringBuilder();
                    }
                    openSB--;
                    break;
                case '(':
                    if (openSB == 0 && openBR == 0 && chunk.length() > 0) {
                        chunks.add(chunk.toString().trim());
                        chunk = new StringBuilder();
                    }
                    openBR++;
                    break;
                case ')':
                    if (openSB == 0 && openBR == 0 && chunk.length() > 0) {
                        chunks.add(chunk.append(')').toString().trim());
                        chunk = new StringBuilder();
                    }
                    openBR--;
                    break;
            }
            chunk.append(c);
        }
        if (chunk.length() > 0) {
            chunks.add(chunk.toString());
        }
        return chunks;
    }

    private static String stripExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return fileName;
        }
        return fileName.substring(0, lastIndexOf);
    }

    private static String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return fileName.substring(lastIndexOf);
    }

    public static String replaceExt(String fileName, String ext) {
        return stripExtension(fileName) + "." + ext;
    }
}

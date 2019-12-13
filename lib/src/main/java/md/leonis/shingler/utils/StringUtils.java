package md.leonis.shingler.utils;

import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static md.leonis.shingler.model.ConfigHolder.platform;
import static md.leonis.shingler.model.ConfigHolder.platformsByCpu;

public class StringUtils {

    private static final int MAX_LENGTH = 64;

    public static String normalize(String fileName) {
        String result = StringUtils.removeSpecialChars(fileName.replace("_", " ")); // remove special symbols
        return StringUtils.force63(result).replace(" ", "_");
    }

    static String force63(String fileName) {

        String name = fileName;

        name = fileName.replace(" The ", " ").replace(" The ", " ").replace(" the ", " ").replace(" The ", " ");

        // [t1][a1][T-Port] -> [t1a1T-Port]
        // From tail consistently delete these substrings:
        String[] toDelete = new String[]{"][", ",", ";", "/", "\\", "~", "'", "\"", "`", "!", "."};

        fileName = stripExtension(name);
        String ext = getFileExtension(name);
        int maxLength = MAX_LENGTH - ext.length();

        for (String substr : toDelete) {
            fileName = deleteSubstr(substr, fileName, 0);
        }

        if (fileName.endsWith(" The")) {
            fileName = fileName.substring(0, fileName.indexOf(" The"));
        }

        name = fileName.replace("  ", " ").replace(" - ", "-").replace(" -", "-").trim();

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

        // Get first 21 characters + "~" + tail.
        return name.substring(0, 21) + "~" + name.substring(name.length() - 42) + ext;
    }

    private static String removeSpecialChars(String fileName) {
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

    public static String stripExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        for (String ext : platformsByCpu.get(platform).getExts()) {
            int lastIndexOf = fileName.lastIndexOf(ext);
            if (lastIndexOf != -1) {
                return fileName.substring(0, lastIndexOf);
            }
        }
        return fileName;
    }

    public static String getFileExtension(String fileName) {
        for (String ext : platformsByCpu.get(platform).getExts()) {
            int lastIndexOf = fileName.lastIndexOf(ext);
            if (lastIndexOf != -1) {
                return fileName.substring(lastIndexOf);
            }
        }
        return "";
    }

    public static String replaceExt(String fileName, String ext) {
        return addExt(stripExtension(fileName), ext);
    }

    public static String addExt(String fileName, String ext) {
        return fileName + (fileName.endsWith("." + ext) ? "" : "." + ext);
    }


    public static String cpu(String cpu) {
        cpu = cpu.replace("&rsquo;", "'");
        cpu = StringEscapeUtils.unescapeHtml4(cpu);

        String separators = " _+";
        String restricted = "'\"().,&!?$@#%^*=/\\[];:'\"|<>{}";

        for (char c : separators.toCharArray()) {
            cpu = cpu.replace("" + c, "-");
        }

        for (char c : restricted.toCharArray()) {
            cpu = cpu.replace("" + c, "");
        }

        cpu = translit(cpu);
        cpu = cpu.toLowerCase();
        cpu = cpu.replace("--", "-");
        cpu = cpu.replace("--", "-");

        List<String> tails = Arrays.asList("-unl", "-iv", "-v", "-vi", "-vii", "-");
        List<String> tailsReplacement = Arrays.asList("", "-4", "-5", "-6", "-7", "");

        for (int i = 0; i < tails.size(); i++) {
            String s = tails.get(i);
            cpu = replaceFromTail(s, cpu) + tailsReplacement.get(i);
        }

        cpu = cpu.replace("-iii", "-3");
        cpu = cpu.replace("-ii", "-2");

        return cpu;
    }

    public static String replaceFromTail(String substr, String string) {
        if (string.endsWith(substr)) {
            return string.substring(0, string.length() - substr.length());
        } else {
            return string;
        }
    }

    public static String translit(String string) {
        /*Transliterator toLatinTrans = Transliterator.getInstance("Russian-Latin/BGN");
        String result = toLatinTrans.transliterate(resursing);*/

        List<String> rus = Arrays.asList("а", "б", "в", "г", "д", "е", "з", "и", "к", "л", "м", "н", "о", "п", "р",
                "с", "т", "у", "ф", "ц", "ы", "й", "ё", "ж", "х", "ч", "ш", "щ", "э", "ю", "я", "ъ", "ь", "%20%20", "%20", " ");

        List<String> lat = Arrays.asList("a", "b", "v", "g", "d", "e", "z", "i", "k", "l", "m", "n", "o", "p", "r",
                "s", "t", "u", "f", "c", "y", "ij", "yo", "zh", "h", "ch", "sh", "shch", "je", "yu", "ya", "", "", "-", "-", "-");

        for (int i = 0; i < rus.size(); i++) {
            string = string.replace(rus.get(i), lat.get(i));
        }

        for (int i = 0; i < rus.size(); i++) {
            string = string.replace(org.apache.commons.lang3.StringUtils.capitalize(rus.get(i)), org.apache.commons.lang3.StringUtils.capitalize(lat.get(i)));
        }

        string = string.replaceAll("/[^a-zA-Z0-9_\\-]+/si", "");
        return string;
    }

}

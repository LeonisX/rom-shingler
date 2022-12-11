package md.leonis.shingler.utils;

import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static md.leonis.shingler.model.ConfigHolder.platform;
import static md.leonis.shingler.model.ConfigHolder.platformsByCpu;

public class StringUtils {

    private static final int MAX_LENGTH = 64;

    public static String normalize(String fileName, String ext) {
        return normalize(fileName + "." + ext);
    }

    public static String normalize(String fileName) {
        fileName = fileName.replace(" (SG-1000)", "");
        fileName = fileName.replace(" (SC-3000)", "");
        fileName = fileName.replace(" (SF-7000)", "");
        fileName = fileName.replace(" (MV)", "");
        fileName = fileName.replace(" (Unreleased)", "");
        fileName = fileName.replace(" (Prototype)", "");
        fileName = fileName.replace(" (Sample)", "");
        fileName = fileName.replace(" (Beta)", "");
        fileName = fileName.replace(" (Unl)", "");
        fileName = fileName.replace(" (Wxn)", "");
        String result = StringUtils.removeSpecialChars(fileName.replace("_", " ")); // remove special symbols
        return StringUtils.force63(result).replace(" ", "_");
    }

    /// SNES Columns (Unl) problem

    public static String normalize7z(String fileName, String ext) {
        return normalize7z(fileName + "." + ext);
    }

    public static String normalize7z(String fileName) {
        fileName = removeSpecialStatus(fileName);
        String result = StringUtils.removeSpecialChars(fileName.replace("_", " ")); // remove special symbols
        return StringUtils.force63(result).replace(" ", "_");
    }

    static String force63(String fileName) {

        String name = removeThe(fileName);

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
        name = deleteSpaces(name, maxLength);

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
        return name.substring(0, 21) + "-" + name.substring(name.length() - maxLength + 22) + ext;
    }

    //TODO [S] [C]
    public static String removeSpecialStatus(String s) {
        s = s.replace(" (SG-1000)", "");
        s = s.replace(" (SC-3000)", "");
        s = s.replace(" (SF-7000)", "");
        s = s.replace(" (MV)", "");
        s = s.replace(" (Unreleased)", "");
        s = s.replace(" (Prototype)", "");
        s = s.replace(" (Sample)", "");
        s = s.replace(" (Beta)", "");
        //s = fileName.replace(" (Unl)", "");
        return s.replace(" (Wxn)", "");
    }

    public static String removeThe(String s) {
        s = s.replace(" The ", " ").replace(" The ", " ").replace(" the ", " ").replace(" The ", " ");
        s = removeFront(s, "The ");
        return removeTail(s, ", The");
    }

    public static String removeFront(String s, String substr) {
        if (s.startsWith(substr)) {
            return s.substring(substr.length());
        } else {
            return s;
        }
    }

    public static String removeTail(String s, String substr) {
        if (s.endsWith(substr)) {
            return s.substring(0, s.length() - substr.length());
        } else {
            return s;
        }
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

    public static String replaceColon(String s) {
        return s.replace(":", " - ").replace("  ", " ").replace("  ", " ");
    }

    private static String deleteSpaces(String name, int maxLength) {
        if (name.length() <= maxLength || name.split(" ", -1).length - 1 <= 2) {
            return name;
        }
        int index = name.lastIndexOf(" ");
        if (index == -1) {
            return name;
        }
        return deleteSpaces(name.substring(0, index) + name.substring(index + 1), maxLength);
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
            if (lastIndexOf != -1 && lastIndexOf + ext.length() == fileName.length()) {
                return fileName.substring(0, lastIndexOf);
            }
        }
        return fileName;
    }

    public static String stripArchiveExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        for (String ext : Arrays.asList(".7z", ".zip")) {
            int lastIndexOf = fileName.lastIndexOf(ext);
            if (lastIndexOf != -1 && lastIndexOf + ext.length() == fileName.length()) {
                return fileName.substring(0, lastIndexOf);
            }
        }
        return fileName;
    }

    public static String getFileExtension(String fileName) {
        for (String ext : platformsByCpu.get(platform).getExts()) {
            int lastIndexOf = fileName.lastIndexOf(ext);
            if (lastIndexOf != -1 && lastIndexOf + ext.length() == fileName.length()) {
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

    public static String escapeChars(String s) { //todo
        //s = s.replace("&", "&amp;");
        s = s.replace("'", "&rsquo;");
        s = s.replace("`", "&rsquo;");
        return StringEscapeUtils.escapeHtml4(s);
    }

    public static String unescapeChars(String s) {
        s = s.replace("&rsquo;", "'");
        //s = s.replace("&amp;", "&");

        return StringEscapeUtils.unescapeHtml4(s);
    }

    public static String cpu(String cpu) {
        cpu = unescapeChars(cpu);

        String separators = " _+~";
        String restricted = "'\"().,&!?$@#%^*=/\\[];:|<>{}";

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

        List<String> tails = Arrays.asList("-unl", "-ii", "-iii", "-iv", "-v", "-vi", "-vii", "-");
        List<String> tailsReplacement = Arrays.asList("", "-2", "-3", "-4", "-5", "-6", "-7", "");

        for (int i = 0; i < tails.size(); i++) {
            String s = tails.get(i);
            cpu = replaceFromTail(s, tailsReplacement.get(i), cpu);
        }

        cpu = cpu.replace("-iii-", "-3-");
        cpu = cpu.replace("-ii-", "-2-");

        return cpu;
    }

    public static String replaceFromTail(String substr, String replace, String string) {
        if (string.endsWith(substr)) {
            return string.substring(0, string.length() - substr.length()) + replace;
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

    //TODO https://perishablepress.com/stop-using-unsafe-characters-in-urls/
    //TODO https://stackoverflow.com/questions/1547899/which-characters-make-a-url-invalid
    private static final String[] ESCAPE_URI_CHARS = {" ", "[", "]", "&", "\\", "<", ">", "{", "}", "?", "=", "\"", ",", "#", "|"};
    private static final String[] UNESCAPE_URI_CHARS = {"%20", "%5B", "%5D", "%26", "%5C", "%3C", "%3E", "%7B", "%7D", "%3F", "%3D", "%22", "%2C", "%23", "%7C"};

    public static String escapeUriChars(String str) {
        //System.out.println("Before escape: " + str);
        if (str.equals("#")) {
            return "";
        }

        String href = str.trim();

        for (int i = 0; i < ESCAPE_URI_CHARS.length; i++) {
            href = href.replace(ESCAPE_URI_CHARS[i], UNESCAPE_URI_CHARS[i]);
        }

        StringBuilder res = new StringBuilder();

        boolean isFirst = false;

        for (int i = 0; i < href.length(); i++) {
            char chr = href.charAt(i);
            if (chr == ':') {
                if (!isFirst) {
                    isFirst = true;
                    res.append(chr);
                } else {
                    res.append("%3A");
                }
            } else {
                res.append(chr);
            }
        }

        href = res.toString().split("#")[0];
        return href;
    }

    public static String unescapeUriChars(String str) {
        //System.out.println("Before unescape: " + str);

        String href = str.trim();

        for (int i = 0; i < ESCAPE_URI_CHARS.length; i++) {
            href = href.replace(UNESCAPE_URI_CHARS[i], ESCAPE_URI_CHARS[i]);
        }

        href = href.replace("%3A", ":");
        href = href.replace("%2F", "/");

        return href;
    }

    public static String escapePathChars(String path) {
        return path.replace("://", "@").replace("?", "@").replace(":", "@");
    }

    // From commons

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isBlank(String str) {
        int strLen;
        if (str != null && (strLen = str.length()) != 0) {
            for(int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
}

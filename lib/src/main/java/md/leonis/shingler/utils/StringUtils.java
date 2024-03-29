package md.leonis.shingler.utils;

import md.leonis.shingler.model.dto.TiviStructure;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

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
        String ext = getFileExtension(fileName);
        fileName = stripExtension(fileName);
        fileName = StringUtils.removeSpecialChars(fileName.replace("_", " ")); // remove special symbols
        return StringUtils.force63(fileName + "." + ext).replace(" ", "_");
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
        for (char c : "'[!]\",<>;/\\`~@#$%^&*().:?".toCharArray()) {
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

    // https://wiki.no-intro.org/index.php?title=Naming_Convention
    // If the first word is a common article then it will be moved to the end of the main title and separated with a comma.
    // This includes non English common articles too.

    // In cases where the title begins with "The" or "A", it should be moved to the end of the title, and preceded by a comma.
    // This same rule applies if the title is not in English, e.g. "De" for Dutch, "Die" for German, and "Le/La/Les" for French etc. //TODO

    // Subtitles and pretitles are always separated from the main title by a hyphen " - ".
    // Titles that use a different separation style (ex. colon or "~ Subtitle ~") will be converted to a hyphen style.
    //If the first word of a subtitle is a common article it will NOT be moved to the end.
    public static String formatTitle(String title) {
        //A, The
        if (title.startsWith("A ")) {
            if (title.contains(":")) {
                title = title.substring(2).replace(":", ", A:");
            } else if (title.contains("-")) {
                title = title.substring(2).replace(" -", ", A -");
            } else {
                title = title.substring(2) + ", A";
            }
        }
        if (title.startsWith("The ")) {
            if (title.contains(":")) {
                title = title.substring(4).replace(":", ", The:");
            } else if (title.contains("-")) {
                title = title.substring(4).replace(" -", ", The -");
            } else {
                title = title.substring(4) + ", The";
            }
        }

        return title;
    }

    //TODO refactor
    public static String normalizeImageName(TiviStructure tiviGame, Set<String> images, int index, String imageOld) {
        String sid = tiviGame.getSid();
        String region = (sid.equals("pd") || sid.equals("hak")) ? "" : "_" + tiviGame.getRegion().replace(";", "");
        String image = StringUtils.cleanString(unescapeChars(tiviGame.getName() + region).replace(" ", "_"));
        image = image.substring(0, Math.min(62, image.length()));
        return renameImage(tiviGame, images, index, unescapeChars(imageOld), image);
    }

    private static String renameImage(TiviStructure tiviGame, Set<String> images, int index, String imageOld, String image) {
        String ext = FilenameUtils.getExtension(imageOld); // jpg
        String imageNew = String.format("%s_%s.%s", image, index, ext);
        if (images.contains(imageNew)) {
            String author = tiviGame.getPublisher().isEmpty() ? tiviGame.getDeveloper() : tiviGame.getPublisher();
            imageNew = String.format("%s_%s_%s.%s", image, StringUtils.cleanString(unescapeChars(author)).replace(" ", "_"), index, ext);
            if (images.contains(imageNew)) {
                throw new RuntimeException("Can't find new image title: " + imageNew);
            }
        }
        return imageNew.replace(" ", "_");
    }

    static List<String> tails = Arrays.asList(" unl", " ii", " iii", " iv", " v", " vi", " vii", " viii", " ix");
    static List<String> tailsReplacement = Arrays.asList("", " 2", " 3", " 4", " 5", " 6", " 7", " 8", " 9");
    static List<String> mids = Arrays.asList("ii", "iii", "iv");
    static List<String> midsReplacement = Arrays.asList("2", "3", "4");

    // ЧПУ для названий страниц игр
    public static String cpu(String text) {
        text = unescapeChars(text);
        return Arrays.stream(generateCpu(text).split(" ")).filter(s -> !s.equals("a") && !s.equals("the")).collect(Collectors.joining("-"));
    }

    // ЧПУ, слова разделены пробелами
    public static String generateCpu(String text) {
        text = cleanString(text).toLowerCase();

        for (int i = 0; i < tails.size(); i++) {
            text = replaceFromTail(" " + tails.get(i), " " + tailsReplacement.get(i), text);
        }

        for (int i = 0; i < mids.size(); i++) {
            text = replaceFromTail(" " + mids.get(i) + " ", " " + midsReplacement.get(i) + " ", text);
        }

        return text;
    }

    public static String cleanString(String string) {
        for (char c : " -_+~".toCharArray()) {
            string = string.replace("" + c, " ");
        }
        return Normalizer.normalize(StringUtils.toTranslit(string), Normalizer.Form.NFD) // repair aáeéiíoóöőuúüű AÁEÉIÍOÓÖŐUÚÜŰ
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "") // repair aáeéiíoóöőuúüű AÁEÉIÍOÓÖŐUÚÜŰ
                .replaceAll("[^\\p{Alnum}]+", " ") // all, except [a-zA-Z0-9] convert to a single " "
                .trim().replace("  ", " ");
    }

    public static String replaceFromTail(String substr, String replace, String string) {
        if (string.toLowerCase().endsWith(substr.toLowerCase())) {
            return string.substring(0, string.length() - substr.length()) + replace;
        } else {
            return string;
        }
    }

    public static String sid(String title) {
        if (title.startsWith("Замечание")) {
            return "1";
        } else if (StringUtils.isNotBlank(title)) {
            if (title.endsWith("(Hack)") || title.endsWith(" Hack)") || title.contains("(Hack ")) {
                return "hak";
            } else if (title.contains("(PD)")) {
                return "pd";
            } else if ("abcdefghijklmnopqrstuvwxyz".contains(title.substring(0, 1).toLowerCase())) {
                return title.substring(0, 1).toLowerCase();
            } else {
                // todo russian
                return "num";
            }
        } else {
            throw new RuntimeException("Blank game title!");
        }
    }

    // Транслитерация по правилам международных телеграмм
    private static final List<Character> RUS = Arrays.asList(
            'а', 'б', 'в', 'г', 'д', 'е', 'ё', 'ж', 'з', 'и', 'й',
            'к', 'л', 'м', 'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф',
            'х', 'ц', 'ч', 'ш', 'щ', 'ъ', 'ы', 'ь', 'э', 'ю', 'я');

    private static final List<String> LAT = Arrays.asList(
            "a", "b", "v", "g", "d", "e", "e", "j", "z", "i", "i",
            "k", "l", "m", "n", "o", "p", "r", "s", "t", "u", "f",
            "h", "c", "ch", "sh", "sc", "", "y", "", "e", "iu", "ia");

    private static final Map<Integer, String> TRANSLIT_MAP = new HashMap<>();

    static {
        for (int i = 0; i < RUS.size(); i++) {
            TRANSLIT_MAP.put((int) RUS.get(i), LAT.get(i));
            TRANSLIT_MAP.put((int) RUS.get(i).toString().toUpperCase().charAt(0), LAT.get(i).toUpperCase());
        }
    }

    /**
     * Переводит русский текст в транслит. В результирующей строке
     * каждая русская буква будет заменена на соответствующую английскую.
     * Не русские символы останутся прежними.
     *
     * @param text исходный текст с русскими символами
     * @return результат
     */
    public static String toTranslit(String text) {
        return text.chars().mapToObj(c -> {
            String replace = TRANSLIT_MAP.get(c);
            return (replace == null) ? Character.valueOf((char) c).toString() : replace;
        }).collect(Collectors.joining());
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


    //TODO plural function https://www.irlc.msu.ru/irlc_projects/speak-russian/time_new/rus/grammar/

    //todo return to tivi admin
    private static List<String> jmRule = Arrays.asList("сь", "бь", "дь", "рь");
    private static List<String> jiRule = Arrays.asList("га", "жа", "ка", "ха", "ча", "ша", "ща");

    public static String plural(String word, int count) {
        if (jmRule.stream().anyMatch(word::endsWith)) {
            return pluraljm(word, count);
        } else if (jiRule.stream().anyMatch(word::endsWith)) {
            return pluralj(word, count);
        } else if (word.endsWith("а")) {
            return pluralj2(word, count);
        } else {
            return pluralm(word, count);
        }
    }

    //                                       1   2-4  6...11,...
    private static final String[] RULE_J = {"a", "и", ""}; // книга -> книги
    private static final String[] RULE_J2 = {"a", "ы", ""}; // игра -> игры
    private static final String[] RULE_JM = {"ь", "и", "ей"};
    private static final String[] RULE_M = {"", "а", "ов"};

    private static String pluralj(String word, int count) { // книга -> книги
        return plural(word.substring(0, word.length() - 1), RULE_J, count);
    }

    private static String pluralj2(String word, int count) { // игра -> игры
        return plural(word.substring(0, word.length() - 1), RULE_J2, count);
    }

    private static String pluraljm(String word, int count) { // запись
        return plural(word.substring(0, word.length() - 1), RULE_JM, count);
    }

    private static String pluralm(String word, int count) { // журнал
        return plural(word, RULE_M, count);
    }

    private static String plural(String word, String[] rule, int count) {
        if (count >= 11 & count <= 19) {
            return word + rule[2];
        }
        switch (count % 10) {
            case 1:
                return word + rule[0];
            case 2:
            case 3:
            case 4:
                return word + rule[1];
            default:
                return word + rule[2];
        }
    }

    // Просто множественное число

    public static String pluralWords(String word) {
        return Arrays.stream(word.split(" ")).map(StringUtils::plural).collect(Collectors.joining(" "));
    }

    public static String plural(String word) {
        if (word.endsWith("сь") || word.endsWith("бь") || word.endsWith("дь") || word.endsWith("рь") || word.endsWith("а") || word.endsWith("я")) {
            return word.substring(0, word.length() - 1) + "и"; // запись, книга, документация
        } else if (word.endsWith("о")) {
            return word.substring(0, word.length() - 1) + "а"; // окно
        } else if (word.endsWith("ый")) {
            return word.substring(0, word.length() - 1) + "е"; // сервисный
        } else {
            return word + "ы"; // журнал
        }
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
            for (int i = 0; i < strLen; ++i) {
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

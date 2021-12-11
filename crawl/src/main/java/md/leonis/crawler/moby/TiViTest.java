package md.leonis.crawler.moby;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.NoArgsConstructor;
import md.leonis.crawler.moby.config.ConfigHolder;
import md.leonis.crawler.moby.utils.WebUtils;
import org.apache.commons.text.similarity.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TiViTest {

    public static void main(String[] args) throws IOException {

        show("5200", "5200");
        show("5200", "a5200");
        show("5200", "a-5200");
        show("5200", "atari-5200");
        show("5200", "atari-520");
        show("5200", "0025");
        show("5200", "tro-lo-lo");

        /*ConfigHolder.loadProtectedProperties();
        System.out.println(readTables());

        System.out.println(readTable("sms"));*/
    }

    private static void show(String left, String right) {
        System.out.println(String.format("%s vs %s", left, right));
        System.out.println("LevenshteinDistance: " + new LevenshteinDistance().apply(left, right));
        System.out.println("calculateDamLevDistance: " + calculateDamLevDistance(left, right));
        System.out.println("getDamerauLevenshteinDistance: " + getDamerauLevenshteinDistance(left, right));
        System.out.println();
    }


    // Damerau Levenshtein Distance
    // Вроде как лучше реагирует на опечатки в словах

    // https://www.demo2s.com/java/java-string-determines-the-damerau-levenshtein-distance-edit-distance.html
    public static int calculateDamLevDistance(String l1, String l2) {
        int[][] d = new int[l1.length() + 1][l2.length() + 1];
        int i, j, cost;
        char[] str1 = l1.toCharArray();
        char[] str2 = l2.toCharArray();

        for (i = 0; i <= str1.length; i++) {
            d[i][0] = i;
        }
        for (j = 0; j <= str2.length; j++) {
            d[0][j] = j;
        }
        for (i = 1; i <= str1.length; i++) {
            for (j = 1; j <= str2.length; j++) {

                if (str1[i - 1] == str2[j - 1])
                    cost = 0;
                else
                    cost = 1;

                d[i][j] = Math.min(d[i - 1][j] + 1, // Deletion
                        Math.min(d[i][j - 1] + 1, // Insertion
                                d[i - 1][j - 1] + cost)); // Substitution

                if ((i > 1) && (j > 1) && (str1[i - 1] == str2[j - 2]) && (str1[i - 2] == str2[j - 1])) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + cost); //transposition
                }
            }
        }
        int _editDistance = d[str1.length][str2.length];
        return _editDistance;
    }

    // http://www.java2s.com/example/java/java.lang/get-damerau-levenshtein-distance.html
    private static final int getDamerauLevenshteinDistance(String s,
                                                           String t) {//from ww  w .  ja  va  2s.  c om
        if (s == null || t == null) {
            throw new IllegalArgumentException("Strings must not be null");
        }
        if (s.equals(t))
            return 0;
        /*
           The difference between this impl. and the previous is that, rather
           than creating and retaining a matrix of size s.length()+1 by t.length()+1,
           we maintain two single-dimensional arrays of length s.length()+1.  The first, d,
           is the 'current working' distance array that maintains the newest distance cost
           counts as we iterate through the characters of String s.  Each time we increment
           the index of String t we are comparing, d is copied to p, the second int[].  Doing so
           allows us to retain the previous cost counts as required by the algorithm (taking
           the minimum of the cost count to the left, up one, and diagonally up and to the left
           of the current cost count being calculated).  (Note that the arrays aren't really
           copied anymore, just switched...this is clearly much better than cloning an array
           or doing a System.arraycopy() each time  through the outer loop.)

           Effectively, the difference between the two implementations is this one does not
           cause an out of memory condition when calculating the LD over two very large strings.
         */

        int n = s.length(); // length of s
        int m = t.length(); // length of t

        if (n == 0)
            return m;
        if (m == 0)
            return n;

        if (n > m) {
            // swap the input strings to consume less memory
            String tmp = s;
            s = t;
            t = tmp;
            n = m;
            m = t.length();
        }

        int p[] = new int[n + 1]; //'previous' cost array, horizontally

        int p_p[] = new int[n + 1]; //n-2 cost array for transpositions

        int d[] = new int[n + 1]; // cost array, horizontally
        int _d[]; // placeholder to assist in swapping p and d

        // indexes into strings s and t
        int i; // iterates through s
        for (i = 0; i <= n; i++)
            p[i] = i;

        for (int j = 1; j <= m; j++) { // iterates through t
            char t_j = t.charAt(j - 1); // jth character of t
            d[0] = j;

            for (i = 1; i <= n; i++) {
                int cost = s.charAt(i - 1) == t_j ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1]
                        + cost);

                //damerau extension
                if (i > 1 && j > 1 && s.charAt(i - 1) == t.charAt(j - 2)
                        && s.charAt(i - 2) == t_j)
                    d[i] = Math.min(d[i], p_p[i - 2] + cost); // transposition
            }

            // copy current distance counts to 'previous row' distance counts
            _d = p_p;
            p_p = p;
            p = d;
            d = _d;
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n];
    }

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    public static List<String> readTables() {
        List<TableName> tables = new ArrayList<>();
        String requestURL = ConfigHolder.apiPath + "shingler-api.php?to=tables";
        try {
            String jsonString = WebUtils.readFromUrl(requestURL);
            JavaType type = MAPPER.getTypeFactory().constructCollectionType(List.class, TableName.class);
            tables = MAPPER.readValue(jsonString, type);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error in readTables");
        }
        return tables.stream().map(t -> t.tableName.replace("base_", "")).collect(Collectors.toList());
    }

    public static List<TiviStructure> readTable(String platform) {
        List<TiviStructure> tables = new ArrayList<>();

        int offset = 0;
        int count = 2000;

        try {
            while (true) {
                String requestURL = String.format("%sshingler-api.php?to=games&platform=%s&offset=%s&count=%s", ConfigHolder.apiPath, platform, offset, count);
                String jsonString = WebUtils.readFromUrl(requestURL);
                JavaType type = MAPPER.getTypeFactory().constructCollectionType(List.class, TiviStructure.class);
                List<TiviStructure> result = MAPPER.readValue(jsonString, type);
                tables.addAll(result);
                if (result.size() < count) {
                    break;
                }
                offset += count;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error in readTable");
        }
        return tables;
    }

    @Data
    @NoArgsConstructor
    public static class TableName implements Cloneable {

        @JsonProperty("TABLE_NAME")
        private String tableName;
    }

    @Data
    @NoArgsConstructor
    @JsonPropertyOrder({"n", "sys", "created", "modified", "sid", "cpu", "name", "descript", "keywords", "region", "publisher", "developer",
            "god", "god1", "ngamers", "type", "genre", "image1", "image2", "image3", "image4", "image5", "image6", "image7", "image8",
            "image9", "image10", "image11", "image12", "image13", "image14", "game", "downloaded", "music", "music_downloaded", "rom",
            "playable", "played", "text1", "text2", "analog", "drname", "cros", "serie", "rating", "userrating", "totalrating", "viewes", "comments"})
    public static class TiviStructure implements Cloneable {

        @JsonAlias("n")
        private int id;
        private String sys;
        private long created;
        private long modified;
        private String sid;
        private String cpu;
        private String name;

        private String descript;
        private String keywords;
        private String region;
        private String publisher;
        private String developer;
        private String god;
        private String god1;
        private int ngamers;
        private String type;
        private String genre;

        private String image1;
        private String image2;
        private String image3;
        private String image4;
        private String image5;
        private String image6;
        private String image7;
        private String image8;
        private String image9;
        private String image10;
        private String image11;
        private String image12;
        private String image13;
        private String image14;

        private String game;
        private int downloaded;

        private String music;
        private int music_downloaded;

        private String rom;
        private String playable; //TODO enum('yes','no')
        private int played;

        private String text1;
        private String text2;
        private String analog;
        private String drname;
        private String cros;
        private String serie;

        private int rating;
        private int userrating;
        private int totalrating;
        private int viewes;
        private int comments;

        public String getGame() {
            return game == null ? "" : game.trim();
        }

        public String getRom() {
            return rom == null ? "" : rom.trim();
        }


        public List<String> getImages() {
            List<String> images = new ArrayList<>();
            if (image1 != null && !image1.isEmpty()) {
                images.add(image1);
            }
            if (image2 != null && !image2.isEmpty()) {
                images.add(image2);
            }
            if (image3 != null && !image3.isEmpty()) {
                images.add(image3);
            }
            if (image4 != null && !image4.isEmpty()) {
                images.add(image4);
            }
            if (image5 != null && !image5.isEmpty()) {
                images.add(image5);
            }
            if (image6 != null && !image6.isEmpty()) {
                images.add(image6);
            }
            if (image7 != null && !image7.isEmpty()) {
                images.add(image7);
            }
            if (image8 != null && !image8.isEmpty()) {
                images.add(image8);
            }
            if (image9 != null && !image9.isEmpty()) {
                images.add(image9);
            }
            if (image10 != null && !image10.isEmpty()) {
                images.add(image10);
            }
            if (image11 != null && !image11.isEmpty()) {
                images.add(image11);
            }
            if (image12 != null && !image12.isEmpty()) {
                images.add(image12);
            }
            if (image13 != null && !image13.isEmpty()) {
                images.add(image13);
            }
            if (image14 != null && !image14.isEmpty()) {
                images.add(image14);
            }

            return images;
        }
    }
}

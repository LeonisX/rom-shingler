package md.leonis.shingler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GID {

    public static GID EMPTY = new GID("", 0, null, null, null, null, null, null, null);

    private String title;
    private long size;
    private Long crc32;
    private byte[] md5;
    private byte[] sha1;
    //TODO delete if not need
    private Long crc32wh;
    private byte[] md5wh;
    private byte[] sha1wh;
    private String family; //TODO family ID

    /*public GID(String title, long size, Long crc32, byte[] md5, byte[] sha1, Long crc32wh, byte[] md5wh, byte[] sha1wh, String family) {
        this.title = title;
        this.size = size;
        this.crc32 = crc32;
        this.md5 = md5;
        this.sha1 = sha1;
        this.crc32wh = crc32wh;
        this.md5wh = md5wh;
        this.sha1wh = sha1wh;
        this.family = family;
    }*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GID gid = (GID) o;
        return Arrays.equals(sha1wh, gid.sha1wh);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(sha1wh);
    }
}

package md.leonis.shingler.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.CRC32;

public class BinaryUtils {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static long crc32(byte[] bytes) {

        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    public static byte[] md5(byte[] bytes) {

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] sha1(byte[] bytes) {

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return md.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Measured
    public static long[] unionArrays(long[] arr1, long[] arr2) {

        long[] merge = new long[arr1.length + arr2.length];
        int i = 0, j = 0, k = 0;
        while (i < arr1.length && j < arr2.length) {
            if (arr1[i] < arr2[j]) {
                merge[k++] = arr1[i++];
            } else {
                merge[k++] = arr2[j++];
            }
        }
        while (i < arr1.length) {
            merge[k++] = arr1[i++];
        }
        while (j < arr2.length) {
            merge[k++] = arr2[j++];
        }
        return removeDuplicates(Arrays.copyOfRange(merge, 0, k));
    }

    @Measured
    public static long[] intersectArrays(long[] arr1, long[] arr2) {

        long[] intersect = new long[Math.max(arr1.length, arr2.length)];
        int i = 0, j = 0, k = 0;
        while (i < arr1.length && j < arr2.length) {
            if (arr1[i] < arr2[j]) {
                i++;
            } else if (arr2[j] < arr1[i]) {
                j++;
            } else {
                intersect[k++] = arr1[i++];
            }
        }
        return Arrays.copyOfRange(intersect, 0, k);
    }

    /*@Measured
    static long[] intersectArrays0(long[] arr1, long[] arr2) {

        long[] c = new long[Math.max(arr1.length, arr2.length)];
        int k = 0;
        for (long n : arr2) {
            if (ArrayUtils.contains(arr1, n)) {
                c[k++] = n;
            }
        }

        return Arrays.copyOfRange(c, 0, k);
    }*/

    @Measured
    public static long[] filterArrays(long[] a, int index) {

        long[] c = new long[a.length];
        int k = 0;
        for (long n : a) {
            if (n % index == 0) {
                c[k++] = n;
            }
        }
        return Arrays.copyOfRange(c, 0, k);
    }

    @Measured
    static long[] removeDuplicates(long[] arr) { // Only for sorted arrays

        if (arr.length < 2) {
            return arr;
        }
        int j = 0;
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i] != arr[i + 1]) {
                arr[j++] = arr[i];
            }
        }
        arr[j++] = arr[arr.length - 1];
        return Arrays.copyOfRange(arr, 0, j);
    }
}

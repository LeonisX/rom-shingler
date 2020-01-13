package md.leonis.shingler.utils;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import static md.leonis.shingler.utils.BinaryUtils.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BinaryUtilsTest {

    @Test
    void unionArrays() {
        int[] a = {1,2,3,4};
        int[] b = {2,3,4,5};

        int[] result = BinaryUtils.unionArrays(a,b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{1,2,3,4,5}, result);
    }

    @Test
    void unionArrays2() {
        int[] a = {1,2,3,4};
        int[] b = {};

        int[] result = BinaryUtils.unionArrays(a,b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{1,2,3,4}, result);
    }

    @Test
    void unionArrays3() {
        int[] b = {1,2,3,4};
        int[] a = {};

        int[] result = BinaryUtils.unionArrays(a,b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{1,2,3,4}, result);
    }

    @Test
    void unionArrays4() {
        int[] a = {1,2,3,4};
        int[] b = {5,6,7};

        int[] result = BinaryUtils.unionArrays(a,b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{1,2,3,4,5,6,7}, result);
    }

    @Test
    void mergeArrays() {
        int[] a = {1, 2, 3, 4};
        int[] b = {2, 3, 4, 5};

        int[] result = BinaryUtils.unionArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void mergeArrays2() {
        int[] a = {1, 2, 6};
        int[] b = {2, 3, 4, 5};

        int[] result = BinaryUtils.unionArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6}, result);
    }

    @Test
    void mergeArrays3() {
        int[] a = {1, 2, 2};
        int[] b = {2, 3, 4, 5};

        int[] result = BinaryUtils.unionArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void mergeArrays4() {
        int[] a = {1, 2, 3};
        int[] b = {4, 5};

        int[] result = BinaryUtils.unionArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void mergeArrays5() {
        int[] a = {6, 7, 8};
        int[] b = {2, 3};

        int[] result = BinaryUtils.unionArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{2, 3, 6, 7, 8}, result);
    }

    @Test
    void testIntersectArrays() {
        int[] a = {1, 2, 3, 4};
        int[] b = {2, 3, 4, 5};

        int[] result = intersectArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{2, 3, 4}, result);
    }

    @Test
    void testIntersectArrays2() {
        int[] a = {1, 2, 3, 4};
        int[] b = {5, 6, 7};

        int[] result = intersectArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{}, result);
    }

    @Test
    void testIntersectArrays3() {
        int[] a = {1, 2, 3, 7, 8, 9};
        int[] b = {1, 7};

        int[] result = intersectArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{1, 7}, result);
    }

    @Test
    void testFilterArrays() {
        int[] a = {1, 2, 3, 4, 5};

        int[] result = filterArrays(a, 1);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void testFilterArrays2() {
        int[] a = {1, 2, 3, 4, 5};

        int[] result = filterArrays(a, 2);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{2, 4}, result);
    }

    @Test
    void testFilterArrays3() {
        int[] a = {1, 2, 3, 4, 5};

        int[] result = filterArrays(a, 3);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{3}, result);
    }

    @Test
    void testRemoveDuplicates() {
        int[] a = {1, 2, 3, 4, 4, 5};

        int[] result = removeDuplicates(a);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void testRemoveDuplicates2() {
        int[] a = {1, 1, 1, 1, 2, 2, 3, 4, 4, 4, 5};

        int[] result = removeDuplicates(a);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void testCrc32() {
        assertEquals(-662_733_300, crc32("test".getBytes(StandardCharsets.UTF_8))); // 3632233996L
    }

    @Test
    void testMd5() {
        assertEquals("098F6BCD4621D373CADE4E832627B4F6", bytesToHex(md5("test".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void testSha1() {
        assertEquals("A94A8FE5CCB19BA61C4C0873D391E987982FBBD3", bytesToHex(sha1("test".getBytes(StandardCharsets.UTF_8))));
    }
}

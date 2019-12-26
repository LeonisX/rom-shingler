package md.leonis.shingler.utils;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveUtilsTest {

    @Test
    void extract() {

        ArchiveUtils.extract(Paths.get("D:\\Downloads\\merged-roms\\gg-final\\roms\\gg\\5_in_1_Funpak.zip"), Paths.get("D:\\Downloads\\merged-roms\\gg-final2\\roms\\gg\\5_in_1_Funpak.zip"));
    }
}
package md.leonis.shingler.utils;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static md.leonis.shingler.model.ConfigHolder.*;
import static md.leonis.shingler.model.ConfigHolder.romsCollection;
import static org.slf4j.LoggerFactory.getLogger;

public class ArchiveUtils {

    private static final Logger LOGGER = getLogger(ArchiveUtils.class);

    public static void compress7z(String name, List<String> members, int i) {

        LOGGER.info("Compressing: {} [{}]|{}", name, members.size(), (i + 1) * 100.0 / families.size());

        String archiveName = name.endsWith(".7z") ? name : name + ".7z";
        archiveName = outputDir.resolve(platform).resolve(archiveName).toAbsolutePath().toString();

        List<String> args = new ArrayList<>(Arrays.asList(
                // 7z a -mx9 -m0=LZMA -md1536m -mfb273 -ms8g -mmt=off <archive_name> [<file_names>...]
                System.getenv("ProgramFiles").concat("\\7-Zip\\7z"), "a", "-mx9", "-m0=LZMA", "-md1536m", "-mfb273", "-ms8g", "-mmt=off", '"' + archiveName + '"')

                // 7z a -mx9 -m0=LZMA -md1536m -mfb273 -ms8g <archive_name> [<file_names>...]
                //System.getenv("ProgramFiles").concat("\\7-Zip\\7z"), "a", "-mx9", "-m0=LZMA", "-md1536m", "-mfb273", "-ms8g", '"' + archiveName + '"')

                // 7z a -mx9 -mmt=off <archive_name> [<file_names>...]
                //System.getenv("ProgramFiles").concat("\\7-Zip\\7z"), "a", "-mx9", "-mmt=off", '"' + archiveName + '"')

                // 7z a -mx9 -mmt2 <archive_name> [<file_names>...]
                //System.getenv("ProgramFiles").concat("\\7-Zip\\7z"), "a", "-mx9", "-mmt2", '"' + archiveName + '"')

                // 7z a -mx9 -mmt4 <archive_name> [<file_names>...]
                //System.getenv("ProgramFiles").concat("\\7-Zip\\7z"), "a", "-mx9", "-mmt4", '"' + archiveName + '"')

                // 7z a -mx9 <archive_name> [<file_names>...]
                //System.getenv("ProgramFiles").concat("\\7-Zip\\7z"), "a", "-mx9", '"' + archiveName + '"')
        );
        doCompress(args, members);
    }

    public static void compressZip(String name, List<String> members, int i) {

        LOGGER.info("Compressing: {} [{}]|{}", name, members.size(), (i + 1) * 100.0 / families.size());

        String archiveName = name.endsWith(".zip") ? name : name + ".zip";
        archiveName = outputDir.resolve(platform).resolve(archiveName).toAbsolutePath().toString();

        List<String> args = new ArrayList<>(Arrays.asList(
                // 7z a -tzip -mx9 -mm=LZMA -md1536m -mfb273 -mmt=off <archive_name> [<file_names>...]
                System.getenv("ProgramFiles").concat("\\7-Zip\\7z"), "a", "-tzip", "-mx9", "-mm=LZMA", "-md1536m", "-mfb273", "-mmt=off", '"' + archiveName + '"')

                // 7z a -tzip -mx9 -mfb258 -mmt=off <archive_name> [<file_names>...]
                // 7z a -tzip -mx9 -mm=Deflate64 -mfb257 -mmt=off
        );
        doCompress(args, members);
    }

    private static void doCompress(List<String> args, List<String> members) {

        ProcessBuilder processBuilder = new ProcessBuilder();
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        try {
            if (isWindows) {
                if (members.size() > 50) {
                    File tmp = File.createTempFile("shg", "ar");
                    IOUtils.saveToFile(tmp.toPath(), members.stream().map(n -> romsCollection.getRomsPath().resolve(n).toString()).collect(Collectors.toList()));
                    args.add("@" + tmp.getAbsolutePath());
                } else {
                    args.addAll(members.stream().map(n -> '"' + romsCollection.getRomsPath().resolve(n).toString() + '"').collect(Collectors.toList()));
                }

                processBuilder.command(args);
            } else {
                //TODO finish, test on Linux
                processBuilder.command("7z", "a", "-mx9", "-m0=LZMA", "-md1536m", "-mfb273", "-ms8g", "-mmt=off", "archive.7z", "...");
            }

            Process proc = processBuilder.start();
            BufferedReader errBR = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            BufferedReader outBR = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            int code = proc.waitFor();

            if (code > 0) {
                LOGGER.warn("Exit code: {}", code);
                LOGGER.warn("Out message: {}", errBR.lines().collect(Collectors.joining("\n")));
                LOGGER.warn("Error message: {}", outBR.lines().collect(Collectors.joining("\n")));
                System.out.println(args);
            }

        } catch (Exception ex) {
            LOGGER.error("Compression error", ex);
        }
    }
}

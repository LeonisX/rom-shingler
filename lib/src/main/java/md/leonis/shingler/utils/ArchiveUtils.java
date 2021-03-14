package md.leonis.shingler.utils;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
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

        if (families != null) {
            LOGGER.info("Compressing: {} [{}]|{}", name, members.size(), (i + 1) * 100.0 / families.size());
        }

        String archiveName = name.endsWith(".7z") ? name : name + ".7z";
        archiveName = getOutputPath().resolve(platform).resolve(archiveName).toAbsolutePath().toString();

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

        if (families != null) {
            LOGGER.info("Compressing: {} [{}]|{}", name, members.size(), (i + 1) * 100.0 / families.size());
        }

        String archiveName = name.endsWith(".zip") ? name : name + ".zip";
        archiveName = getOutputPath().resolve(platform).resolve(archiveName).toAbsolutePath().toString();

        List<String> args = new ArrayList<>(Arrays.asList(
                // 7z a -tzip -mx9 -mfb258 -mmt=off <archive_name> [<file_names>...]
                System.getenv("ProgramFiles").concat("\\7-Zip\\7z"), "a", "-tzip", "-mx9", "-mfb258", "-mmt=off", '"' + archiveName + '"')
                // 7z a -tzip -mx9 -mm=LZMA -md1536m -mfb273 -mmt=off <archive_name> [<file_names>...]
                //System.getenv("ProgramFiles").concat("\\7-Zip\\7z"), "a", "-tzip", "-mx9", "-mm=LZMA", "-md1536m", "-mfb273", "-mmt=off", '"' + archiveName + '"')

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


    public static void compressZip(String archiveName, List<String> members) {

        List<String> args = new ArrayList<>(Arrays.asList(
                // 7z a -tzip -mx9 -mfb258 -mmt=off <archive_name> [<file_names>...]
                System.getenv("ProgramFiles").concat("\\7-Zip\\7z"), "a", "-tzip", "-mx9", "-mfb258", "-mmt=off", '"' + archiveName + '"')
                // 7z a -tzip -mx9 -mm=LZMA -md1536m -mfb273 -mmt=off <archive_name> [<file_names>...]
                //System.getenv("ProgramFiles").concat("\\7-Zip\\7z"), "a", "-tzip", "-mx9", "-mm=LZMA", "-md1536m", "-mfb273", "-mmt=off", '"' + archiveName + '"')

                // 7z a -tzip -mx9 -mfb258 -mmt=off <archive_name> [<file_names>...]
                // 7z a -tzip -mx9 -mm=Deflate64 -mfb257 -mmt=off
        );

        ProcessBuilder processBuilder = new ProcessBuilder();
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        try {
            if (isWindows) {
                if (members.size() > 50) {
                    File tmp = File.createTempFile("shg", "ar");
                    IOUtils.saveToFile(tmp.toPath(), members);
                    args.add("@" + tmp.getAbsolutePath());
                } else {
                    args.addAll(members.stream().map(n -> '"' + n + '"').collect(Collectors.toList()));
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

    public static void extract(Path path, Path outDir) {
        // 7z e archive.zip -oc:\soft

        List<String> args = new ArrayList<>(Arrays.asList(
                // 7z a -tzip -mx9 -mfb258 -mmt=off <archive_name> [<file_names>...]
                System.getenv("ProgramFiles").concat("\\7-Zip\\7z"), "e", '"' + path.toAbsolutePath().toString() + '"', "-o\"" + outDir.toAbsolutePath().toString() + '"')
                // 7z a -tzip -mx9 -mm=LZMA -md1536m -mfb273 -mmt=off <archive_name> [<file_names>...]
                //System.getenv("ProgramFiles").concat("\\7-Zip\\7z"), "a", "-tzip", "-mx9", "-mm=LZMA", "-md1536m", "-mfb273", "-mmt=off", '"' + archiveName + '"')

                // 7z a -tzip -mx9 -mfb258 -mmt=off <archive_name> [<file_names>...]
                // 7z a -tzip -mx9 -mm=Deflate64 -mfb257 -mmt=off
        );

        ProcessBuilder processBuilder = new ProcessBuilder();
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        try {
            if (isWindows) {
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

    public static List<String> list(Path path) {

        List<String> args = Arrays.asList(System.getenv("ProgramFiles").concat("\\7-Zip\\7z"), "l", "-slt", '"' + path.toAbsolutePath().toString() + '"');

        ProcessBuilder processBuilder = new ProcessBuilder();
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        try {
            if (isWindows) {
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
                return null;
            } else {
                return outBR.lines().collect(Collectors.toList());
            }

        } catch (Exception ex) {
            LOGGER.error("List archive error", ex);
        }
        return null;
    }
/*
D:\Downloads>"C:\Program Files\7-Zip\7z.exe" l -slt snes9x-1.60-win32-x64.zip

7-Zip 19.00 (x64) : Copyright (c) 1999-2018 Igor Pavlov : 2019-02-21

Scanning the drive for archives:
1 file, 3854525 bytes (3765 KiB)

Listing archive: snes9x-1.60-win32-x64.zip

--
Path = snes9x-1.60-win32-x64.zip
Type = zip
Physical Size = 3854525

----------
Path = changes.txt
Folder = -
Size = 170167
Packed Size = 55683
Modified = 2019-04-23 21:04:45
Created = 2019-04-23 21:12:48
Accessed = 2019-04-23 21:12:48
Attributes = A
Encrypted = -
Comment =
CRC = C2271472
Method = Deflate
Characteristics = NTFS
Host OS = FAT
Version = 20
Volume Index = 0
Offset = 0

Path = cheats.bml
Folder = -
Size = 2034204
Packed Size = 417399
Modified = 2019-04-23 21:04:45
Created = 2019-04-23 21:12:48
Accessed = 2019-04-23 21:12:48
Attributes = A
Encrypted = -
Comment =
CRC = B86651F9
Method = Deflate
Characteristics = NTFS
Host OS = FAT
Version = 20
Volume Index = 0
Offset = 55724
*/
}

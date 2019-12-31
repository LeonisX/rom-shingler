package md.leonis.shingler.gui;

import md.leonis.shingler.ListFilesa;
import md.leonis.shingler.model.ConfigHolder;
import md.leonis.shingler.model.Platform;
import md.leonis.shingler.model.nes.INesHeader;
import md.leonis.shingler.model.nes.UnifHeader;
import md.leonis.shingler.utils.IOUtils;
import md.leonis.shingler.utils.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static md.leonis.shingler.model.ConfigHolder.*;

public class NesGroup {

    private static final File root = new File("D:\\Downloads\\games");

    public static void main(String[] args) {

        ConfigHolder.platform = "nes";
        ConfigHolder.platforms.put("Nintendo NES", new Platform("Nintendo NES", "nes", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)", "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*", Arrays.asList(".nes", ".unf", ".unif", ".fds", ".bin", ".nsf", ".nez", ".7z", ".zip")));
        platforms.values().forEach(p -> platformsByCpu.put(p.getCpu(), p));

        List<File> files = IOUtils.listFiles(root);

        Map<String, Set<String>> map = new HashMap<>();

        for (File file : files) {

            if (file.getName().endsWith("nes")) {
                INesHeader header = new INesHeader(file);
                //System.out.println(header.getMapper() + " : " + file);
                put(map, String.format("%03d", header.getMapper()), file);
            } else if (file.getName().endsWith("unf") || file.getName().endsWith("unif")) {
                UnifHeader header = new UnifHeader(file);
                put(map, header.getMapper(), file);
                //System.out.println(header.getMapper() + " : " + header.getName() + " : " + file);
            } else {
                System.out.println("Unknown extension: " + file);
            }
        }

        map.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    System.out.println(e.getKey() + ": ");
                    e.getValue().stream().sorted().forEach(s -> System.out.println("  " + s));
                });
    }

    private static void put(Map<String, Set<String>> map, String mapper, File file) {

        String name = StringUtils.stripExtension(file.getName());

        Platform plat = platformsByCpu.get(platform);
        if (!plat.isHack(name) && !plat.isPD(name)) {

            if (mapper == null) {
                mapper = "";
            }

            try {
                IOUtils.createDirectories(root.toPath().resolve(mapper));
                Files.copy(file.toPath(), root.toPath().resolve(mapper).resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String cleanedName = ListFilesa.getCleanName(name);

            Set<String> set = map.get(mapper);
            if (set == null) {
                map.put(mapper, new HashSet<>(Arrays.asList(cleanedName)));
            } else {
                set.add(cleanedName);
            }
        }
    }
}

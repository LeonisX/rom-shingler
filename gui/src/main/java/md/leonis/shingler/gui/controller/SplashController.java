package md.leonis.shingler.gui.controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.util.Duration;
import md.leonis.shingler.gui.view.FxmlView;
import md.leonis.shingler.gui.view.StageManager;
import md.leonis.shingler.model.Platform;
import md.leonis.shingler.utils.FileUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static md.leonis.shingler.model.ConfigHolder.*;

@Controller
public class SplashController {

    private final StageManager stageManager;

    @Lazy
    public SplashController(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    @FXML
    private void initialize() throws IOException {

        //TODO service, read from disk
        platforms.put("Nintendo NES", new Platform("Nintendo NES", "nes", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".nes", ".unf", ".unif", ".fds", ".bin", ".prg", ".nsf", ".nez", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.singletonList(0),
                8));
        platforms.put("Nintendo Famicom", new Platform("Nintendo Famicom", "famicom", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".nes", ".unf", ".unif", ".fds", ".bin", ".prg", ".nsf", ".nez", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.singletonList(0),
                8));
        platforms.put("Virtual Boy", new Platform("Virtual Boy", "vboy", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".vb", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.singletonList(0),
                8));
        platforms.put("Sega SG-1000", new Platform("Sega SG-1000", "sg1000", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".sg", ".sc", ".sf7", ".mv", ".sms", ".bin", ".7z", ".zip"),
                Arrays.asList("(MV)", "(SC-3000)", "(SF-7000)"),
                Collections.singletonList(0),
                8));
        platforms.put("Sega Master System", new Platform("Sega Master System", "sms", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".sg", ".sc", ".sf7", ".mv", ".sms", ".bin", ".7z", ".zip"),
                Collections.emptyList(),
                Collections.singletonList(0),
                8));
        platforms.put("Sega Game Gear", new Platform("Sega Game Gear", "gg", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".gg", ".sms", ".bin", ".ic1", ".u1", ".7z", ".zip"),
                Collections.emptyList(),
                Collections.singletonList(0),
                8));
        platforms.put("Sega MegaDrive", new Platform("Sega MegaDrive", "megadrive", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".smd", ".md", ".mdx", ".bin", ".32x", ".1", ".gen", ".rom", ".ic1", ".u1", ".7z", ".zip"),
                Collections.emptyList(),
                Arrays.asList(1, 2),
                8));
        platforms.put("Sega CD", new Platform("Sega CD", "segacd", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".cue", ".iso", ".bin", ".7z", ".zip"),
                Collections.emptyList(),
                Arrays.asList(1, 2),
                8));
        platforms.put("Super Nintendo", new Platform("Super Nintendo", "snes", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".smc", ".fig", ".058", ".078", ".sfc", ".swc", ".048", ".1", ".rom", ".st", ".bs", "bin", ".zzz", ".7z", ".zip"),
                Collections.singletonList("(BS)"),
                Arrays.asList(1, 2, 4),
                8));
        platforms.put("Game Boy", new Platform("Game Boy", "gb", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".gb", ".sgb", ".gbc", ".cgb", ".boy", ".7z", ".zip"),
                Arrays.asList("[C]", "[S]"),
                Collections.singletonList(0),
                4));
        platforms.put("Neo Geo Pocket", new Platform("Neo Geo Pocket", "ngp", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".ngp",".ngc", ".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.singletonList(0),
                4));

        platforms.put("Atari 5200", new Platform("Atari 5200", "5200", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".a52",".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.singletonList(0),
                4));
        platforms.put("Atari 7800", new Platform("Atari 7800", "7800", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".a78",".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.singletonList(0),
                4));
        platforms.put("Atari Lynx", new Platform("Atari Lynx", "lynx", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".lnx", ".com", ".o", ".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.singletonList(0),
                4));
        platforms.put("TurboGrafx-16", new Platform("TurboGrafx-16", "tg16", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".pce", ".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Arrays.asList(1, 2),
                4));
        platforms.put("TurboGrafx-CD", new Platform("TurboGrafx-CD", "tg16cd", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".pce", ".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.singletonList(0),
                4));
        platforms.put("3DO Interactive Multiplayer", new Platform("3DO Interactive Multiplayer", "3do", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".pce", ".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.singletonList(0),
                4));
        platforms.put("Watara Supervision", new Platform("Watara Supervision", "sv", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".sv", ".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.singletonList(0),
                1));
        platforms.put("Tiger Game.com", new Platform("Tiger Game.com", "gamecom", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".tgc", ".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.singletonList(1),
                4));
        platforms.put("APF Imagination Machine", new Platform("APF Imagination Machine", "apf", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".apd", ".apt", ".apw", ".asm", ".bas", ".cas", ".cpf", ".rom", ".s19", ".wav", ".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Arrays.asList(1, 2),
                4));
        platforms.put("Bally Astrocade", new Platform("Bally Astrocade", "astrocade", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".bml", ".flac", ".prg", ".rom", ".wav", ".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.emptyList(),
                1));
        platforms.put("Fairchild Channel F", new Platform("Fairchild Channel F", "channelf", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".chf", ".rom", ".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.emptyList(),
                1));
        platforms.put("RCA Studio II", new Platform("RCA Studio II", "studio2", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".asm", ".st2", ".rom", ".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.emptyList(),
                1));
        platforms.put("Interton VC4000", new Platform("Interton VC4000", "vc4000", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".rom", ".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.emptyList(),
                1));
        platforms.put("Intellivision", new Platform("Intellivision", "intv", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".cfg", ".int", ".itv", ".rom", ".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.emptyList(),
                1));
        platforms.put("ColecoVision", new Platform("ColecoVision", "coleco", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".col", ".rom", ".bin", ".7z", ".zip"),
                Collections.singletonList("(Unl)"),
                Collections.emptyList(),
                1));


        platforms.values().forEach(p -> platformsByCpu.put(p.getCpu(), p));

        FileUtils.createDirectories(collectionsDir);
        platforms.values().forEach(p -> FileUtils.createDirectories(collectionsDir.resolve(p.getCpu())));

        loadProtectedProperties();

        //TODO switch on
        //PauseTransition delay = new PauseTransition(Duration.seconds(1));
        PauseTransition delay = new PauseTransition(Duration.millis(1));
        //TODO boolean - need to wait all processes
        delay.setOnFinished(event -> {
            stageManager.switchScene(FxmlView.DASHBOARD);
            stageManager.showPane(FxmlView.COLLECTION);
        });
        delay.play();
    }
}

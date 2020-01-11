package md.leonis.shingler.gui.controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.util.Duration;
import md.leonis.shingler.gui.view.FxmlView;
import md.leonis.shingler.gui.view.StageManager;
import md.leonis.shingler.model.Platform;
import md.leonis.shingler.utils.IOUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

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
    private void initialize() {

        //TODO service, read from disk
        platforms.put("Nintendo NES", new Platform("Nintendo NES", "nes", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".nes", ".unf", ".unif", ".fds", ".bin", ".prg", ".nsf", ".nez", ".7z", ".zip"), Collections.singletonList(0)));
        platforms.put("Sega SG-1000", new Platform("Sega SG-1000", "sg1000", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".sg", ".sc", ".sf7", ".mv", ".sms", ".bin", ".7z", ".zip"), Collections.singletonList(0)));
        platforms.put("Sega Master System", new Platform("Sega Master System", "sms", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".sg", ".sc", ".sf7", ".mv", ".sms", ".bin", ".7z", ".zip"), Collections.singletonList(0)));
        platforms.put("Sega Game Gear", new Platform("Sega Game Gear", "gg", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".gg", ".sms", ".bin", ".ic1", ".u1", ".7z", ".zip"), Collections.singletonList(0)));
        platforms.put("Sega MegaDrive", new Platform("Sega MegaDrive", "megadrive", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".smd", ".md", ".mdx", ".bin", ".32x", ".1", ".gen", ".rom", ".ic1", ".u1", ".7z", ".zip"), Arrays.asList(1, 2)));
        platforms.put("Super Nintendo", new Platform("Super Nintendo", "snes", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".smc", ".fig", ".058", ".078", ".sfc", ".swc", ".048", ".1", ".rom", ".st", ".bs", "bin", ".zzz", ".7z", ".zip"), Arrays.asList(1, 2, 4)));
        platforms.put("Game Boy", new Platform("Game Boy", "gb", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".gb", ".sgb", ".gbc", ".cgb", ".boy", ".7z", ".zip"), Collections.singletonList(0)));

        platforms.values().forEach(p -> platformsByCpu.put(p.getCpu(), p));

        IOUtils.createDirectories(collectionsDir);
        platforms.values().forEach(p -> IOUtils.createDirectories(collectionsDir.resolve(p.getCpu())));

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

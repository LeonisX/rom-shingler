package md.leonis.shingler.gui.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SplashControllerTest {

    @Test
    void matchers() {
        assertTrue("1y by Trilobit (PAL) (PD)".matches(".*\\(PD\\).*"));
        assertTrue("1y by Trilobit (PD)(PAL)".matches(".*\\(PD\\).*"));
        assertTrue("(PD)".matches(".*\\(PD\\).*"));
        assertFalse("8 Eyes to Castlevania Conversion - Weapons Beta Test by elbobelo (8 Eyes Hack)".matches(".*\\(PD\\).*"));

        assertTrue("8 Eyes to Castlevania Conversion - Weapons Beta Test by elbobelo (8 Eyes Hack)".matches("(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)"));
        assertTrue("1942 - Cold Winter (Hack)".matches("(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)"));
        assertFalse("1y by Trilobit (PD)(PAL)".matches("(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)"));

        assertTrue("1942 (JU) [b1]".matches("(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)"));
        assertTrue("1943 - The Battle of Midway (U) [b1][o1]".matches("(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)"));
        assertTrue("1943 - The Battle of Midway (U) [T+RusBeta0.1.2_Artemon(AKA Loke)]".matches("(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)"));
        assertTrue("1944 [p1][T-Chi_MS emumax]".matches("(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)"));
        assertTrue("Ai Sensei no Oshiete - Watashi no Hoshi (J) [hM04]".matches("(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)"));
    }

}
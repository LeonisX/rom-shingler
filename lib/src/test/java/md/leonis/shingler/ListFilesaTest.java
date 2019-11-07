package md.leonis.shingler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListFilesaTest {

    @Test
    void deviation2() { //a < b = ((b-a)/a) * 100
        double d1 = 99.97283387917113;
        double d2 = 99.97291685474407;

        assertEquals(8.299812030666449E-5, ListFilesa.deviation(d1, d2));

        d1 = 100;
        d2 = 100;

        assertEquals(0, ListFilesa.deviation(d1, d2));

        d1 = 100;
        d2 = 1;

        assertEquals(99.0, ListFilesa.deviation(d1, d2));
    }
}

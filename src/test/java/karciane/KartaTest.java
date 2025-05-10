package karciane;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KartaTest {

    @Test
    void testKonstruktor() {
        Karta karta = new Karta(Karta.Ranga.DWA, Karta.Kolor.CZERWONY);
        assertEquals(Karta.Ranga.DWA, karta.getRanga());
        assertEquals(Karta.Kolor.CZERWONY, karta.getKolor());
    }

    @Test
    void testToStringKolorowaKarta() {
        Karta karta = new Karta(Karta.Ranga.DWA, Karta.Kolor.CZERWONY);
        assertEquals("\u001B[31m2\u001B[0m", karta.toString());
    }

    @Test
    void testToStringKartaBezKoloru() {
        Karta karta = new Karta(Karta.Ranga.WEZ_CZTERY_I_ZMIANA_KOLORU, null);
        assertEquals("+4\ud800\udc0f", karta.toString());
    }

    @Test
    void testEquals() {
        Karta karta1 = new Karta(Karta.Ranga.DWA, Karta.Kolor.CZERWONY);
        Karta karta2 = new Karta(Karta.Ranga.DWA, Karta.Kolor.CZERWONY);
        Karta karta3 = new Karta(Karta.Ranga.DWA, Karta.Kolor.ZIELONY);
        assertEquals(karta1, karta2);
        assertNotEquals(karta1, karta3);
    }

    @Test
    void testHashCode() {
        Karta karta1 = new Karta(Karta.Ranga.DWA, Karta.Kolor.CZERWONY);
        Karta karta2 = new Karta(Karta.Ranga.DWA, Karta.Kolor.CZERWONY);
        assertEquals(karta1.hashCode(), karta2.hashCode());
    }

    @Test
    void testLiczbowa() {
        Karta karta1 = new Karta(Karta.Ranga.DWA, Karta.Kolor.CZERWONY);
        Karta karta2 = new Karta(Karta.Ranga.ZMIANA_KIERUNKU, Karta.Kolor.ZIELONY);
        assertTrue(karta1.liczbowa());
        assertFalse(karta2.liczbowa());
    }
}

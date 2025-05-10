package karciane;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sieciowanie.Pokoj;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraczTest {

    private Gracz gracz;
    private Pokoj pokoj;

    @BeforeEach
    void setUp() {
        gracz = new Gracz(null); // SocketChannel null dla testów
        pokoj = new Pokoj("TestowyPokoj", 2);
        gracz.setPokoj(pokoj);

        List<Karta> karty = new ArrayList<>();
        karty.add(new Karta(Karta.Ranga.DWA, Karta.Kolor.CZERWONY));
        karty.add(new Karta(Karta.Ranga.DWA, Karta.Kolor.NIEBIESKI));
        gracz.setKarty(karty);
    }

    @Test
    void testLiczbaKart() {
        assertEquals(2, gracz.liczbaKart());
    }

    @Test
    void testRuchyDostepneKarty() {
        List<Karta> stosik = new ArrayList<>();
        stosik.add(new Karta(Karta.Ranga.DWA, Karta.Kolor.ZIELONY));
        pokoj.setStosik(stosik);

        List<Karta> ruchy = gracz.ruchy();
        assertEquals(2, ruchy.size());
    }

    @Test
    void testDobierzKarty() throws IOException {
        List<Karta> talia = new ArrayList<>();
        talia.add(new Karta(Karta.Ranga.JEDEN, Karta.Kolor.CZERWONY));
        pokoj.setTalia(talia);

        gracz.dobierz(1, false);
        assertEquals(2, gracz.liczbaKart());
    }

    @Test
    void testRuchyBrakMozliwych() {
        List<Karta> stosik = new ArrayList<>();
        stosik.add(new Karta(Karta.Ranga.STOP, Karta.Kolor.ZIELONY));
        pokoj.setStosik(stosik);

        List<Karta> ruchy = gracz.ruchy();
        assertTrue(ruchy.isEmpty());
    }
}

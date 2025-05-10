package sieciowanie;

import karciane.Gracz;
import karciane.Karta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PokojTest {

    private Pokoj pokoj;

    @BeforeEach
    void setUp() {
        pokoj = new Pokoj("Testowy Pokoj", 2);
        pokoj.setTalia(new ArrayList<>());
        pokoj.setStosik(new ArrayList<>());
    }

    @Test
    void testDodawanieGraczy() {
        Gracz gracz1 = new Gracz(null);
        Gracz gracz2 = new Gracz(null);
        pokoj.getGracze().add(gracz1);
        pokoj.getGracze().add(gracz2);

        assertEquals(2, pokoj.getGracze().size());
    }

    @Test
    void testKolorowanieGraczy() {
        Gracz gracz1 = new Gracz(null);
        Gracz gracz2 = new Gracz(null);
        pokoj.getGracze().add(gracz1);
        pokoj.getGracze().add(gracz2);

        pokoj.kolorujGraczy();

        assertEquals(1, gracz1.getKolor());
        assertEquals(5, gracz2.getKolor());
    }

    @Test
    void testNastepnyGracz() {
        Gracz gracz1 = new Gracz(null);
        Gracz gracz2 = new Gracz(null);
        gracz1.setKarty(new ArrayList<>());
        gracz2.setKarty(new ArrayList<>());
        gracz2.getKarty().add(new Karta(Karta.Ranga.ZERO, Karta.Kolor.CZERWONY));
        pokoj.getGracze().add(gracz1);
        pokoj.getGracze().add(gracz2);
        pokoj.setRuch(gracz1);

        Gracz nastepny = pokoj.nastepny();
        assertEquals(gracz2, nastepny);
    }

    @Test
    void testOdnawianieTalii() {
        List<Karta> stosik = new ArrayList<>();
        stosik.add(new Karta(Karta.Ranga.STOP, Karta.Kolor.NIEBIESKI));
        stosik.add(new Karta(Karta.Ranga.DWA, Karta.Kolor.ZOLTY));
        pokoj.setStosik(stosik);

        pokoj.odnowTalie();

        assertEquals(1, pokoj.getStosik().size()); // Zostaje ostatnia karta
        assertEquals(1, pokoj.getTalia().size());
    }
}

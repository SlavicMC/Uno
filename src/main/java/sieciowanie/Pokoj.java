package sieciowanie;

import karciane.Gracz;
import karciane.Karta;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Klasa pokoju serwerowego
 */
@Getter
@Setter
public class Pokoj
{
    String nazwa;
    int maksGraczy;
    boolean rozpoczeta;
    List<Karta> talia;
    List<Karta> stosik;
    Gracz ruch;
    int kierunek = 1;
    List<Gracz> gracze = new ArrayList<>();
    List<Gracz> podium;

    /**
     * Konstruktor pokoju
     *
     * @param nazwa - nazwa pokoju
     * @param maksGraczy - maks graczy w pokoju (mozna pozniej zmienic poleceniem)
     */
    public Pokoj(String nazwa, int maksGraczy)
    {
        this.nazwa = nazwa;
        this.maksGraczy = maksGraczy;
        rozpoczeta = false;
    }

    /**
     * Ustawia kolory graczy w zaleznosci od ich liczby na serwerze
     */
    public void kolorujGraczy()
    {
        int[] kolory = new int[0];
        if(gracze.size() == 2) kolory = new int[]{1, 5};
        else if(gracze.size() == 3) kolory = new int[]{1, 3, 5};
        else if(gracze.size() == 4) kolory = new int[]{1, 3, 4, 5};
        else if(gracze.size() == 5) kolory = new int[]{1, 2, 3, 4, 5};
        else if(gracze.size() == 6) kolory = new int[]{1, 2, 3, 4, 5, 6};
        int i = 0;
        for(Gracz gracz : gracze)
        {
            gracz.setKolor(kolory[i]);
            i++;
        }
    }

    /**
     * Odnawia talie (wyciaga karty ze stosika i je miesza)
     */
    public void odnowTalie()
    {
        for(int i = stosik.size()-2; i >= 0; i--)
        {
            talia.add(stosik.remove(i));
        }
        Collections.shuffle(talia);
        for(Karta k : talia)
        {
            if(k.getRanga() == Karta.Ranga.ZMIANA_KOLORU || k.getRanga() == Karta.Ranga.WEZ_CZTERY_I_ZMIANA_KOLORU) k.setKolor(null);
        }
    }

    /**
     * Wysyla wiadomosc do wszystkich w pokoju
     *
     * @param wiadomosc - wiadomosc do wyslania
     */
    public void wyslijWiadomoscDoWszystkich(String wiadomosc) throws IOException
    {
        if (gracze == null || wiadomosc == null) return;
        if(wiadomosc.isEmpty()) return;
        for (Gracz gracz : gracze)
        {
            gracz.wyslijWiadomosc(wiadomosc);
        }
    }

    /**
     * Zwraca gracza ktory bedzie wykonywal nastepny ruch (z obecnymi ustawieniami)
     *
     * @return znaleziony gracz lub null (przy bledach)
     */
    public Gracz nastepny()
    {
        if(ruch == null) return null;
        int i = gracze.indexOf(ruch);
        if(i == -1) return null;
        int nowyI = (i + kierunek + gracze.size()) % gracze.size();
        int k = kierunek;
        while(gracze.get(nowyI).getKarty().isEmpty())
        {
            k += kierunek;
            nowyI = (i + k + gracze.size()) % gracze.size();
        }
        return gracze.get(nowyI);
    }

    /**
     * Konczy ruch jednego gracza i zaczyna nastepnego (funkcja nastepny())
     */
    public void koniecRuchu() throws IOException
    {
        if(!isRozpoczeta()) return;
        ruch = nastepny();
        wyslijWiadomoscDoWszystkich("Kolej " + ruch.nazwowanie());
        ruch.wyslijWiadomosc("Twoje karty: " + ruch.getKarty() + "\nMożliwe ruchy:\n" + ruch.ruchyJakoString());
    }

    /**
     * Znajduje graczy ktozy jeszcze nie sa na podium i dodaje ich na koncu a nastepnie wyswietla wyniki i zeruje ustawienia pokoju
     */
    public void zakonczGre() throws IOException
    {
        for (Gracz ostatni : gracze)
        {
            if(!ostatni.getKarty().isEmpty()) podium.add(ostatni);
        }
        StringBuilder wynik = new StringBuilder();
        for (int i = 0; i < podium.size(); i++)
        {
            wynik.append(i+1).append(". ").append(podium.get(i)).append("\n");
        }
        wyslijWiadomoscDoWszystkich("Koniec gry!\nWyniki:\n" + wynik + "\nPowrot do poczekalni");
        rozpoczeta = false;
        talia = null;
        stosik = null;
        ruch = null;
        kierunek = 1;
        podium = null;
        for (Gracz gracz : gracze)
        {
            gracz.setKarty(null);
        }
    }
}

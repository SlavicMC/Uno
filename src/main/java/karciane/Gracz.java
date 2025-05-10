package karciane;

import lombok.Getter;
import lombok.Setter;
import sieciowanie.Pokoj;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Klasa gracza (przystosowana do obslugi gry)
 */
@Getter
@Setter
public class Gracz
{
    String nazwa;
    int kolor = 0;
    Pokoj pokoj;
    SocketChannel kanal;
    List<Karta> karty;

    /**
     * Mozliwe kolory graczy (kolory[0] to brak koloru)
     */
    protected static final String[] kolory = {"\u001B[0m", "\u001B[31m", "\u001B[38;5;214m", "\u001B[33m", "\u001B[32m", "\u001B[34m", "\u001B[35m"};

    /**
     * Konstruktor gracza
     *
     * @param kanal - kanal (klient) dla ktorego tworzony jest gracz
     */
    public Gracz(SocketChannel kanal)
    {
        this.kanal = kanal;
    }

    /**
     * Zwraca pokolorowana nazwe gracza (jesli ten ma kolor)
     */
    public String nazwowanie()
    {
        return kolory[kolor] + nazwa + Karta.POWROT;
    }

    @Override
    public String toString()
    {
        if(karty == null) return "Gracz " + nazwowanie();
        else return "Gracz " + nazwowanie() + ", " + karty;
    }

    /**
     * Wysyla wiadomosc na kanal tego gracza
     *
     * @param wiadomosc - wiadomosc do wyslania
     */
    public void wyslijWiadomosc(String wiadomosc) throws IOException
    {
        if(kanal == null) return;
        if(!wiadomosc.endsWith("\n")) wiadomosc += '\n';
        ByteBuffer buffer = ByteBuffer.wrap((wiadomosc).getBytes());
        kanal.write(buffer);
    }

    /**
     * Kladzie karte
     *
     * @param numer - numer ruchu (zaczynajac od 0)
     */
    public void poloz(int numer) throws IOException
    {
        if(!pokoj.isRozpoczeta()) return;
        if(this != pokoj.getRuch())
        {
            wyslijWiadomosc("Teraz kolej " + pokoj.getRuch().nazwowanie());
            return;
        }
        List<Karta> r = ruchy();
        if(numer < 0 || numer >= r.size())
        {
            wyslijWiadomosc("Niepoprawny numer");
            return;
        }
        Karta karta = r.get(numer);
        int k = 0;
        if(karta.getKolor() == null)
        {
            for (int i = 0; i <= numer; i++)
            {
                if(r.get(i) == karta)
                {
                    karta.setKolor(Karta.Kolor.values()[k]);
                    k++;
                }
            }
        }
        karty.remove(karta);
        pokoj.getStosik().add(karta);
        System.out.println(nazwowanie() + "polozyl" + karta + " (" + karta.getRanga() + " : " + karta.getKolor() + ")");
        pokoj.wyslijWiadomoscDoWszystkich(nazwowanie() + " polozyl " + karta);
        if(karta.getRanga() == Karta.Ranga.ZMIANA_KIERUNKU) pokoj.setKierunek(-1 * pokoj.getKierunek());
        else if(karta.getRanga() == Karta.Ranga.STOP) pokoj.setRuch(pokoj.nastepny());
        else if(karta.getRanga() == Karta.Ranga.WEZ_DWA) pokoj.nastepny().dobierz(2, false);
        else if(karta.getRanga() == Karta.Ranga.WEZ_CZTERY_I_ZMIANA_KOLORU) pokoj.nastepny().dobierz(4, false);
        if(karty.isEmpty())
        {
            pokoj.getPodium().add(this);
            pokoj.wyslijWiadomoscDoWszystkich(nazwowanie() + " pozbyl sie wszystkich kart i zakonczyl swoja rozgrywke");
            if(pokoj.getPodium().size() >= pokoj.getGracze().size()-1)
            {
                pokoj.zakonczGre();
            }
        }
        pokoj.koniecRuchu();
    }

    /**
     * Dobiera karte z talii
     *
     * @param liczba - liczba kart do dobrania
     * @param jakoRuch - czy dobranie ma sie liczyc jako ruch gracza
     */
    public void dobierz(int liczba, boolean jakoRuch) throws IOException
    {
        if(pokoj == null) return;
        if(!pokoj.isRozpoczeta()) return;
        if(jakoRuch && this != pokoj.getRuch())
        {
            wyslijWiadomosc("Teraz kolej " + pokoj.getRuch().nazwowanie());
            return;
        }
        List<Karta> talia = pokoj.getTalia();
        if(talia == null) return;
        if(karty == null) karty = new ArrayList<>();
        if(talia.size() < liczba) pokoj.odnowTalie();
        for (int i = 0; i < liczba; i++)
        {
            if(talia.isEmpty()) break;
            karty.add(talia.remove(talia.size() - 1));
        }
        if(jakoRuch)
        {
            pokoj.wyslijWiadomoscDoWszystkich(nazwowanie() + " dobral " + liczba + " kart");
            pokoj.koniecRuchu();
        }
    }

    /**
     * Zwraca liste ruchow (kart do polozenia) jakie moze wykonac gracz (karty czarne zwracane sa 4 razy czyli dla kazdego koloru)
     */
    public List<Karta> ruchy()
    {
        List<Karta> wynik = new ArrayList<>();
        if(karty == null) return wynik;
        Karta polozona = pokoj.getStosik().get(pokoj.getStosik().size() - 1);
        for(Karta karta : karty)
        {
            if(karta.getKolor() == null)
            {
                for(int i = 0; i < 4; i++)
                {
                    wynik.add(karta);
                }
            }
            else if(karta.getKolor() == polozona.getKolor() || karta.getRanga() == polozona.getRanga()) wynik.add(karta);
        }
        return wynik;
    }

    /**
     * Wczytuje ruchy gracza (funkcja ruchy()) i zamienia je na string
     */
    public String ruchyJakoString()
    {
        List<Karta> r = ruchy();
        System.out.println(r);
        for(Karta b : r)
        {
            System.out.println(b.getRanga() + " : " + b.getKolor());
        }
        if(r.isEmpty()) return "Brak dostepnych ruchow. Dobierz karte";
        StringBuilder sb = new StringBuilder();
        int i = 1;
        int k = 0;
        for(Karta karta : r)
        {
            if(karta.getKolor() == null)
            {
                sb.append(i).append(". ").append(Karta.kolory[k%4]).append(karta).append(Karta.POWROT).append("\n");
                k++;
            }
            else sb.append(i).append(". ").append(karta).append("\n");
            i++;
        }
        return sb + "Mozesz tez dobrac karte";
    }

    /**
     * Liczba kart gracza
     */
    public int liczbaKart()
    {
        return karty.size();
    }
}

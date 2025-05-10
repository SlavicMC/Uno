package karciane;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Klasa karty gry
 */
@Getter
@Setter
public class Karta
{
    /**
     * Kolor powrotu (konca koloru, dawany po pokolorowaniu pewnego tekstu)
     */
    public static final String POWROT = "\u001B[0m";
    /**
     * Kolory kart
     */
    static final String[] kolory = {"\u001B[31m", "\u001B[34m", "\u001B[33m", "\u001B[32m",};
    /**
     * Symbole kart
     */
    static final String[] symbole = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "Ø", "⇆", "+2", "+4\uD800\uDC0F", "\uD800\uDC0F"};

    public enum Ranga
    {
        ZERO, JEDEN, DWA, TRZY, CZTERY, PIEC, SZESC, SIEDEM, OSIEM, DZIEWIEC, STOP, ZMIANA_KIERUNKU, WEZ_DWA, WEZ_CZTERY_I_ZMIANA_KOLORU, ZMIANA_KOLORU
    }

    public enum Kolor
    {
        CZERWONY, NIEBIESKI, ZOLTY, ZIELONY
    }

    private Ranga ranga;
    private Kolor kolor;

    /**
     * Konstruktor karty
     *
     * @param ranga - ranga
     * @param kolor - kolor
     */
    public Karta(Ranga ranga, Kolor kolor)
    {
        this.ranga = ranga;
        this.kolor = kolor;
    }

    /**
     * Sprawdza czy ranga karty jest zwykla liczba (karty 0-9)
     */
    public boolean liczbowa()
    {
        return ranga.ordinal() <= 9;
    }

    @Override
    public String toString()
    {
        if(kolor == null) return symbole[ranga.ordinal()];
        else return kolory[kolor.ordinal()] + symbole[ranga.ordinal()] + POWROT;
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Karta karta = (Karta)o;
        return ranga == karta.ranga && kolor == karta.kolor;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(ranga, kolor);
    }
}


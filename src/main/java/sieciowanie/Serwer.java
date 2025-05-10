package sieciowanie;

import karciane.Gracz;
import karciane.Karta;
import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

/**
 * Klasa serwera, odpalana na poczatku
 */
public class Serwer {
    private static final int PORT = 12345;
    private ServerSocketChannel serverChannel;
    private Selector selector;
    @Getter
    private final List<Gracz> gracze = new ArrayList<>();
    @Getter
    private final List<Pokoj> pokoje = new ArrayList<>();

    private static final String POMOC = "/pomoc";

    private final Random random = new Random();

    /**
     * Funkcje glowna, tworzy serwer i go uruchamia (funkcja start())
     */
    public static void main(String[] args) throws IOException
    {
        new Serwer().start();
    }

    /**
     * Pozyskuje gracza z kanalu klienta
     *
     * @param kanalKlienta - kanal
     *
     * @return znaleziony gracz lub null
     */
    public Gracz graczZKanalu(SocketChannel kanalKlienta)
    {
        for (Gracz gracz : gracze)
        {
            if(gracz.getKanal() == kanalKlienta) return gracz;
        }
        return null;
    }

    /**
     * Pozyskuje pokoj serwerowy z nazwy
     *
     * @param nazwa - nazwa pokoju
     *
     * @return znaleziony pokoj lub null
     */
    public Pokoj pokojZNazwy(String nazwa)
    {
        for (Pokoj pokoj : pokoje)
        {
            if(pokoj.getNazwa().equals(nazwa)) return pokoj;
        }
        return null;
    }

    /**
     * Uruchamia i konfiguruje serwer
     */
    public void start() throws IOException
    {
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new java.net.InetSocketAddress(PORT));
        serverChannel.configureBlocking(false);

        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Serwer uruchomiony na porcie " + PORT);

        while (true)
        {
            selector.select();

            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                if (key.isAcceptable())
                {
                    akceptujPolaczenie();
                }
                else if (key.isReadable())
                {
                    wczytajWiadomoscKlienta(key);
                }
            }
        }
    }

    /**
     * Akceptuje i konfiguruje polaczenie klienta do serwera
     */
    private void akceptujPolaczenie() throws IOException
    {
        SocketChannel kanalKlienta = serverChannel.accept();
        kanalKlienta.configureBlocking(false);
        kanalKlienta.register(selector, SelectionKey.OP_READ);
        System.out.println("Nowy klient polaczony: " + kanalKlienta.getRemoteAddress());
        Gracz g = new Gracz(kanalKlienta);
        gracze.add(g);
        g.wyslijWiadomosc("Witaj na serwerze! Podaj swoja nazwe w grze\n");
    }

    /**
     * Wczytuje wyslana wiadomosc i przetwarza na string
     *
     * @param key - klucz
     */
    private void wczytajWiadomoscKlienta(SelectionKey key)
    {
        SocketChannel kanalKlienta = (SocketChannel) key.channel();
        Gracz g = graczZKanalu(kanalKlienta);
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        try
        {
            int odczytaneBajty = kanalKlienta.read(buffer);
            if (odczytaneBajty == -1) {
                gracze.remove(g);
                kanalKlienta.close();
                System.out.println("Klient rozlaczony.");
                return;
            }

            buffer.flip();
            String wiadomosc = new String(buffer.array(), 0, buffer.limit()).trim();
            System.out.println("Odebrano: " + wiadomosc);
            obsluzWiadomoscKlienta(g, wiadomosc);
        }
        catch (Exception e)
        {
            try
            {
                gracze.remove(g);
                g.getKanal().close();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
            System.out.println("Blad podczas obslugi wiadomosci klienta: " + e.getMessage());
        }
    }

    /**
     * Obsluga wczytaj wiadomosci
     *
     * @param g - gracz ktory wyslal wiadomosc
     * @param wiadomosc - wyslana wiadomosc
     */
    private void obsluzWiadomoscKlienta(Gracz g, String wiadomosc)
    {
        try
        {
            if(g.getNazwa() == null)
            {
                if(wiadomosc.length() < 4 || wiadomosc.length() > 10 || !wiadomosc.matches("[a-zA-Z0-9]+"))
                {
                    g.wyslijWiadomosc("Nazwa musi byc dluzsza niz 4, krotsza niz 10 i skaldac sie tylko z postych znakow");
                    return;
                }
                g.setNazwa(wiadomosc);
                g.wyslijWiadomosc("Twoja nazwa zostala ustawiona na: " + wiadomosc + ". Uzyj /pomoc by wyswietlic dostepne polecenia\n");
                System.out.println("Klient ustawil nazwe: " + wiadomosc);
                return;
            }
            if(wiadomosc.startsWith("/"))
            {
                String[] podzielonaWiadomosc = wiadomosc.trim().split(" ", 2);
                String polecenie = podzielonaWiadomosc[0];
                String argument = podzielonaWiadomosc.length > 1 ? podzielonaWiadomosc[1] : "";
                if(g.getPokoj() == null) obsluzPolecenieMenu(g, polecenie, argument);
                else
                {
                    boolean obsluzone;
                    if(g.getPokoj().isRozpoczeta()) obsluzone = obsluzPolecenieRozgrywki(g, polecenie, argument);
                    else obsluzone = obsluzPoleceniePoczekalni(g, polecenie, argument);
                    if(!obsluzone && !obsluzPoleceniePokoju(g, polecenie)) g.getPokoj().wyslijWiadomoscDoWszystkich("[" + g.nazwowanie() + "] " + wiadomosc);
                }
            }
            else
            {
                if(g.getPokoj() != null) g.getPokoj().wyslijWiadomoscDoWszystkich("[" + g.nazwowanie() + "] " + wiadomosc);
            }
        }
        catch (Exception e)
        {
            System.out.println("Blad podczas obslugi wiadomosci klienta: " + e.getMessage());
        }
    }

    /**
     * Obsluga polecenia menu
     *
     * @param g - gracz wysylajacy polecenie
     * @param polecenie - polecenie (pierwsze slowo)
     * @param argument - reszta wiadomosci
     */
    private void obsluzPolecenieMenu(Gracz g, String polecenie, String argument) throws IOException
    {
        switch (polecenie) {
            case POMOC ->
                    g.wyslijWiadomosc("Polecenia menu:\n/pomoc - wyswietla polecenia\n/nazwa - zmienia nazwe uzytkownika\n/dolacz - dolacza do pokoju\n/utworz - tworzy pokoj i do niego dolacza");
            case "/nazwa" ->
            {
                if (argument.length() < 4 || argument.length() > 10 || !argument.matches("[a-zA-Z0-9]+"))
                {
                    g.wyslijWiadomosc("Nazwa musi byc dluzsza niz 4, krotsza niz 10 i skaldac sie tylko z postych znakow");
                    return;
                }
                g.setNazwa(argument);
                g.wyslijWiadomosc("Twoja nazwa zostala ustawiona na: " + argument + "\n");
                System.out.println("Klient ustawil nazwe: " + argument);
            }
            case "/dolacz" -> dolaczDoPokoju(g, argument);
            case "/utworz" -> utworzPokoj(g, argument);
            default -> g.wyslijWiadomosc("Nie znaleziono polecenia");
        }
    }

    /**
     * Obsluga polecenia rozgrywki
     *
     * @param g - gracz wysylajacy polecenie
     * @param polecenie - polecenie (pierwsze slowo)
     * @param argument - reszta wiadomosci
     *
     * @return czy udalo sie obsluzyc (jesli nie zrobi to inna funkcja)
     */
    private boolean obsluzPolecenieRozgrywki(Gracz g, String polecenie, String argument) throws IOException
    {
        switch (polecenie)
        {
            case POMOC ->
            {
                g.wyslijWiadomosc("Polecenia rozgrywki:\n/pomoc - wyswietla polecenia\n/poloz - kladzie karte na stosik\n/dobierz - dobiera karte\n/wyjdz - opuszcza pokoj");
                return true;
            }
            case "/poloz" ->
            {
                int numer;
                try
                {
                    numer = Integer.parseInt(argument);
                }
                catch (NumberFormatException e)
                {
                    g.wyslijWiadomosc("Niepoprawna liczba");
                    return true;
                }
                g.poloz(numer-1);
                return true;
            }
            case "/dobierz" ->
            {
                g.dobierz(1, true);
                return true;
            }
            default ->
            {
                return false;
            }
        }
    }

    /**
     * Obsluga polecenia poczekalni
     *
     * @param g - gracz wysylajacy polecenie
     * @param polecenie - polecenie (pierwsze slowo)
     * @param argument - reszta wiadomosci
     *
     * @return czy udalo sie obsluzyc (jesli nie zrobi to inna funkcja)
     */
    private boolean obsluzPoleceniePoczekalni(Gracz g, String polecenie, String argument) throws IOException
    {
        switch (polecenie)
        {
            case POMOC ->
            {
                g.wyslijWiadomosc("Polecenia pokoju:\n/pomoc - wyswietla polecenia\n/start - rozpoczyna gre\n/pokoj - wyswietla obecny stan pokoju\n/maks - ustawia maksymalna liczbe graczy (2-6)\n/wyjdz - opuszcza pokoj");
                return true;
            }
            case "/start" ->
            {
                rozpocznijGre(g);
                return true;
            }
            case "/maks" ->
            {
                int numer;
                try {
                    numer = Integer.parseInt(argument);
                } catch (NumberFormatException e) {
                    g.wyslijWiadomosc("Niepoprawna liczba graczy");
                    return true;
                }
                if (numer < 2 || numer > 6) {
                    g.wyslijWiadomosc("Liczba graczy musi miescic sie w przedziale 2-6");
                    return true;
                }
                g.getPokoj().setMaksGraczy(numer);
                g.getPokoj().wyslijWiadomoscDoWszystkich(g.nazwowanie() + " ustawil maksymalna liczbe graczy na " + numer);
                return true;
            }
            default ->
            {
                return false;
            }
        }
    }

    /**
     * Obsluga polecenia rozgrywki
     *
     * @param g - gracz wysylajacy polecenie
     * @param polecenie - polecenie (pierwsze slowo)
     *
     * @return czy udalo sie obsluzyc (jesli nie to polecenie zostanie uznane za wiadomosc i wyslane)
     */
    private boolean obsluzPoleceniePokoju(Gracz g, String polecenie) throws IOException
    {
        switch (polecenie) {
            case POMOC ->
            {
                g.wyslijWiadomosc("Polecenia pokoju:\n/pomoc - wyswietla polecenia\n/pokoj - wyswietla obecny stan pokoju\n/wyjdz - opuszcza pokoj");
                return true;
            }
            case "/pokoj" ->
            {
                Pokoj p = g.getPokoj();
                StringBuilder bld = new StringBuilder();
                for (Gracz gracz : p.getGracze()) {
                    bld.append("\n").append(gracz.nazwowanie());
                }
                String lg = bld.toString();
                g.wyslijWiadomosc("Pokoj " + p.getNazwa() + ":\n" + (p.isRozpoczeta() ? "W trakcie rozgrywki" : "Oczekiwanie na rozpoczecie") + "\nGracze (" + p.getGracze().size() + "/" + p.getMaksGraczy() + "):" + lg);
                return true;
            }
            case "/wyjdz" ->
            {
                Pokoj p = g.getPokoj();
                p.getGracze().remove(g);
                g.setPokoj(null);
                p.wyslijWiadomoscDoWszystkich(g.nazwowanie() + " opuscil pokoj");
                g.wyslijWiadomosc("Opusciles " + p.getNazwa());
                return true;
            }
            default -> { return false; }
        }
    }

    /**
     * Tworzy pokoj na serwerze
     *
     * @param gracz - gracz tworzacy
     * @param nazwaPokoju - nazwa pokoju do utworzenia
     */
    public void utworzPokoj(Gracz gracz, String nazwaPokoju) throws IOException
    {
        if(nazwaPokoju.length() < 3)
        {
            gracz.wyslijWiadomosc("Nazwa pokoju musi skladac sie przynajmniej z 3 znakow");
            return;
        }
        if(pokojZNazwy(nazwaPokoju) != null)
        {
            gracz.wyslijWiadomosc("Pokoj o nazwie " + nazwaPokoju + " juz istnieje. Mozesz do niego dolaczyc uzywajac /dolacz\n");
            return;
        }
        pokoje.add(new Pokoj(nazwaPokoju, 2));
        gracz.wyslijWiadomosc("Utworzono pokoj: " + nazwaPokoju + "\n");
        System.out.println("Utworzono pokoj: " + nazwaPokoju);
        dolaczDoPokoju(gracz, nazwaPokoju);
    }

    /**
     * Stara sie dolaczyc do pokoju
     *
     * @param gracz - gracz tworzacy
     * @param nazwaPokoju - nazwa pokoju do dolaczenia
     */
    public void dolaczDoPokoju(Gracz gracz, String nazwaPokoju) throws IOException
    {
        Pokoj p = pokojZNazwy(nazwaPokoju);
        if(p == null)
        {
            gracz.wyslijWiadomosc("Nie znaleziono takiego pokoju. Mozesz go utworzyc przy pomocy /utworz");
            return;
        }
        if(p.getMaksGraczy() == p.getGracze().size())
        {
            gracz.wyslijWiadomosc("Ten pokoj jest juz pelny (" + p.getMaksGraczy() + "/" + p.getGracze().size() + ")\n");
            return;
        }
        if(p.isRozpoczeta())
        {
            gracz.wyslijWiadomosc("Nie mozna dolaczyc do tego pokoju poniewaz gra juz sie rozpoczela\n");
            return;
        }
        p.getGracze().add(gracz);
        gracz.setPokoj(p);
        gracz.wyslijWiadomosc("Dolaczono do pokoju: " + nazwaPokoju + "\n");
        p.wyslijWiadomoscDoWszystkich(gracz.nazwowanie() + " dolaczyl do pokoju");
        System.out.println("Klient " + gracz.nazwowanie() + " dolaczyl do pokoju: " + nazwaPokoju);
    }

    /**
     * Rozpoczyna rozgrywke w pokoju
     *
     * @param gracz - gracz rozpoczynajacy (z niego tez pobierany jest pokoj)
     */
    public void rozpocznijGre(Gracz gracz) throws IOException
    {
        Pokoj pokoj = gracz.getPokoj();
        pokoj.kolorujGraczy();
        List<Gracz> graczePokoju = pokoj.getGracze();
        if(graczePokoju.size() < 2)
        {
            gracz.wyslijWiadomosc("Potrzeba przynajmniej 2 graczy");
            return;
        }
        pokoj.setRozpoczeta(true);
        pokoj.wyslijWiadomoscDoWszystkich(gracz.nazwowanie() + " rozpoczal gre");
        List<Karta> talia = new ArrayList<>();
        pokoj.setTalia(talia);
        for (Karta.Kolor kolor : Karta.Kolor.values())
        {
            talia.add(new Karta(Karta.Ranga.ZERO, kolor));
            for (int i = 0; i < 2; i++)
            {
                talia.add(new Karta(Karta.Ranga.JEDEN, kolor));
                talia.add(new Karta(Karta.Ranga.DWA, kolor));
                talia.add(new Karta(Karta.Ranga.TRZY, kolor));
                talia.add(new Karta(Karta.Ranga.CZTERY, kolor));
                talia.add(new Karta(Karta.Ranga.PIEC, kolor));
                talia.add(new Karta(Karta.Ranga.SZESC, kolor));
                talia.add(new Karta(Karta.Ranga.SIEDEM, kolor));
                talia.add(new Karta(Karta.Ranga.OSIEM, kolor));
                talia.add(new Karta(Karta.Ranga.DZIEWIEC, kolor));
                talia.add(new Karta(Karta.Ranga.STOP, kolor));
                talia.add(new Karta(Karta.Ranga.ZMIANA_KIERUNKU, kolor));
                talia.add(new Karta(Karta.Ranga.WEZ_DWA, kolor));
            }
            talia.add(new Karta(Karta.Ranga.WEZ_CZTERY_I_ZMIANA_KOLORU, null));
            talia.add(new Karta(Karta.Ranga.ZMIANA_KOLORU, null));
        }

        Collections.shuffle(talia);

        List<Karta> stosik = new ArrayList<>();
        pokoj.setStosik(stosik);
        int n = 0;
        while(!talia.get(n).liczbowa())
        {
            n = random.nextInt(talia.size());
        }
        stosik.add(talia.remove(n));

        pokoj.setPodium(new ArrayList<>());
        Gracz pierwszy = graczePokoju.get(0);
        pokoj.setRuch(pierwszy);

        for(Gracz g : graczePokoju)
        {
            g.dobierz(7, false);
        }
        for(Gracz g : graczePokoju)
        {
            g.wyslijWiadomosc("Otrzymano nastepujace karty: " + g.getKarty());
        }

        pokoj.wyslijWiadomoscDoWszystkich("Pierwsza karta na stosie: " + pokoj.stosik.get(0));

        pierwszy.wyslijWiadomosc("Zaczynasz! Możliwe ruchy:\n" + pierwszy.ruchyJakoString());
    }
}

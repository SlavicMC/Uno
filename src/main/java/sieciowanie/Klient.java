package sieciowanie;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

/**
 * Klasa klienta (uzytkownika serwera)
 */
public class Klient
{
    /**
     * Funkcja glowna, podlacza do serwera i wyczekuje wiadomosci
     */
    public static void main(String[] args)
    {
        try (SocketChannel kanalKlienta = SocketChannel.open();
             Scanner skaner = new Scanner(System.in))
        {
            kanalKlienta.connect(new InetSocketAddress("localhost", 12345));
            kanalKlienta.configureBlocking(false);

            Thread watekCzytnika = new Thread(() -> odczytajWiadomosci(kanalKlienta));
            watekCzytnika.setDaemon(true);
            watekCzytnika.start();

            while (skaner.hasNextLine())
            {
                String wiadomosc = skaner.nextLine();
                ByteBuffer bufor = ByteBuffer.wrap(wiadomosc.getBytes());
                kanalKlienta.write(bufor);
            }
        }
        catch (IOException e)
        {
            System.err.println("Blad: " + e.getMessage());
        }
    }

    /**
     * Odczytuje i wypisuje wartosc wyslana z serwera
     */
    private static void odczytajWiadomosci(SocketChannel kanalKlienta)
    {
        ByteBuffer bufor = ByteBuffer.allocate(1024);
        try {
            while (true)
            {
                bufor.clear();
                int odczytaneBajty = kanalKlienta.read(bufor);
                if (odczytaneBajty > 0)
                {
                    bufor.flip();
                    String wiadomosc = new String(bufor.array(), 0, bufor.limit());
                    System.out.print(wiadomosc);
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("Rozlaczono z serwerem.");
        }
    }
}

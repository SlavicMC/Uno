package sieciowanie;

import karciane.Gracz;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.channels.*;

import static org.junit.jupiter.api.Assertions.*;

class SerwerTest {

    private Serwer serwer;

    @BeforeEach
    void setUp() {
        serwer = new Serwer();
    }

    @Test
    void testGraczZKanaluZnajdujeIstniejacegoGracza() throws IOException {
        SocketChannel kanalTestowy = SocketChannel.open();
        Gracz gracz = new Gracz(kanalTestowy);
        serwer.getGracze().add(gracz);

        assertEquals(gracz, serwer.graczZKanalu(kanalTestowy));
    }

    @Test
    void testGraczZKanaluZwracaNullDlaNieistniejacego() throws IOException
    {
        SocketChannel kanalTestowy = SocketChannel.open();

        assertNull(serwer.graczZKanalu(kanalTestowy));
    }

    @Test
    void testPokojZNazwyZnajdujeIstniejacyPokoj()
    {
        Pokoj pokoj = new Pokoj("TestowyPokoj", 2);
        serwer.getPokoje().add(pokoj);

        assertEquals(pokoj, serwer.pokojZNazwy("TestowyPokoj"));
    }

    @Test
    void testPokojZNazwyZwracaNullDlaNieistniejacego()
    {
        assertNull(serwer.pokojZNazwy("NieistniejacyPokoj"));
    }

    @Test
    void testDolaczDoPokoju() throws IOException {
        Pokoj pokoj = new Pokoj("TestowyPokoj", 2);
        serwer.getPokoje().add(pokoj);
        Gracz gracz = new Gracz(null);
        serwer.dolaczDoPokoju(gracz, "TestowyPokoj");

        assertEquals(gracz, pokoj.getGracze().get(0));
    }
}

package edu.pucmm.eict;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTest {

    @Test
    void elContadorPersisteEntreInstancias(@TempDir Path dir) {
        Path archivo = dir.resolve("visitas.txt");
        Main.Contador c1 = new Main.Contador(archivo);
        c1.incrementar();
        c1.incrementar();

        Main.Contador c2 = new Main.Contador(archivo);
        assertEquals(3, c2.incrementar());
    }
}

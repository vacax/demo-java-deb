package edu.pucmm.eict;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;

/**
 * Servicio HTTP mínimo pensado para ejecutarse como daemon de systemd.
 *
 * <p>Toda la configuración llega por variables de ambiente, que es lo que
 * systemd entrega mediante {@code EnvironmentFile=}:</p>
 * <ul>
 *   <li>{@code PUERTO}: puerto TCP donde escucha (por defecto 7070).</li>
 *   <li>{@code MENSAJE}: texto de saludo.</li>
 *   <li>{@code STATE_DIRECTORY}: lo define systemd con {@code StateDirectory=};
 *       ahí se persiste el contador de visitas.</li>
 * </ul>
 *
 * <p>Lo que se imprime por la salida estándar termina en el journal:
 * {@code journalctl -u demo-java-deb -f}.</p>
 */
public class Main {

    static final String VERSION = versionDelPaquete();

    public static void main(String[] args) throws IOException {
        if (args.length > 0 && ("--version".equals(args[0]) || "-v".equals(args[0]))) {
            System.out.println("demo-java-deb " + VERSION);
            return;
        }

        int puerto = Integer.parseInt(env("PUERTO", "7070"));
        String mensaje = env("MENSAJE", "Hola Mundo desde un paquete deb :-D");
        Contador contador = new Contador(directorioDeDatos().resolve("visitas.txt"));

        HttpServer servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        servidor.createContext("/", ex -> {
            long visitas = contador.incrementar();
            String cuerpo = """
                    %s
                    Versión : %s
                    Host    : %s
                    Usuario : %s
                    PID     : %d
                    Java    : %s (%s)
                    Datos   : %s
                    Visitas : %d
                    Fecha   : %s
                    """.formatted(mensaje, VERSION, hostname(), System.getProperty("user.name"),
                    ProcessHandle.current().pid(), System.getProperty("java.version"),
                    System.getProperty("java.home"), contador.archivo(), visitas, LocalDateTime.now());
            responder(ex, 200, cuerpo);
        });
        servidor.createContext("/salud", ex -> responder(ex, 200, "OK\n"));
        servidor.setExecutor(Executors.newFixedThreadPool(4));

        // systemctl stop envía SIGTERM: la JVM ejecuta los shutdown hooks.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Recibida señal de parada, cerrando el servidor...");
            servidor.stop(1);
        }));

        servidor.start();
        System.out.printf("demo-java-deb %s escuchando en el puerto %d (datos en %s)%n",
                VERSION, puerto, contador.archivo());
    }

    static Path directorioDeDatos() {
        // systemd: StateDirectory=demo-java-deb  ->  STATE_DIRECTORY=/var/lib/demo-java-deb
        String dir = System.getenv("STATE_DIRECTORY");
        if (dir == null || dir.isBlank()) {
            dir = env("DIRECTORIO_DATOS", System.getProperty("java.io.tmpdir"));
        }
        return Paths.get(dir.split(":")[0]);
    }

    static void responder(HttpExchange ex, int codigo, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    static String env(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }

    static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (IOException e) {
            return "desconocido";
        }
    }

    static String versionDelPaquete() {
        String v = Main.class.getPackage().getImplementationVersion();
        return v == null ? "desarrollo" : v;
    }

    /** Contador persistente en disco, para mostrar qué sobrevive a remove/purge. */
    static final class Contador {
        private final Path archivo;
        private long valor;

        Contador(Path archivo) {
            this.archivo = archivo;
            try {
                if (Files.exists(archivo)) {
                    valor = Long.parseLong(Files.readString(archivo).trim());
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("No se pudo leer " + archivo + ": " + e.getMessage());
            }
        }

        synchronized long incrementar() {
            valor++;
            try {
                Files.writeString(archivo, Long.toString(valor));
            } catch (IOException e) {
                System.err.println("No se pudo escribir " + archivo + ": " + e.getMessage());
            }
            return valor;
        }

        Path archivo() {
            return archivo;
        }
    }
}

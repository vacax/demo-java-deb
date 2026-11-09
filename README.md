# Demo Java a Deb

Proyecto de la asignatura **ICC-331 Sistemas Operativos (PUCMM)** que muestra cómo
convertir una aplicación Java en un paquete `.deb` que se instala como **servicio
systemd**, de dos formas distintas:

| | Opción A: `debJpackage` | Opción B: `debManual` |
|---|---|---|
| Paquete | `demo-java-deb_1.1.0-1_amd64.deb` | `demo-java-deb-jre_1.1.0-1_all.deb` |
| Herramienta | `jpackage` (JDK) | `dpkg-deb` |
| Runtime Java | **Incluido** (jlink, ~50 MB instalado) | **Dependencia** `default-jre-headless` |
| Arquitectura | `amd64` | `all` |
| Instala en | `/opt/demo-java-deb` | `/usr/bin`, `/usr/share`, `/usr/lib/systemd/system`, `/etc/default` |
| Puerto | 7070 | 7071 |

La aplicación es un servidor HTTP mínimo (sin dependencias, usa `jdk.httpserver`)
que guarda un contador de visitas en `/var/lib/<paquete>/visitas.txt`. Ese archivo
sirve para observar qué conserva `apt remove` y qué borra `apt purge`.

## Requisitos (Ubuntu / Debian)

```bash
sudo apt install openjdk-21-jdk-headless fakeroot binutils dpkg-dev
```

- `openjdk-21-jdk-headless`: trae `jpackage` y `jlink` (cualquier JDK 17+ sirve; con SDKMAN defina `JAVA_HOME`).
- `fakeroot` y `binutils` (`objcopy`): los usa `jpackage` para construir el `.deb`.
- `dpkg-dev`: provee `dpkg-deb`.

> `jpackage` solo genera paquetes del sistema operativo donde se ejecuta: el `.deb`
> debe construirse en Linux (Debian/Ubuntu), no en Windows ni macOS.

## Construir

```bash
./gradlew test          # pruebas unitarias
./gradlew debJpackage   # Opción A
./gradlew debManual     # Opción B
./gradlew paquetes      # ambas
ls build/distribuciones/
```

## Inspeccionar un paquete

```bash
dpkg-deb -I build/distribuciones/demo-java-deb-jre_1.1.0-1_all.deb   # control y scripts
dpkg-deb -c build/distribuciones/demo-java-deb-jre_1.1.0-1_all.deb   # archivos
ar t build/distribuciones/demo-java-deb-jre_1.1.0-1_all.deb          # debian-binary, control.tar.*, data.tar.*
```

## Instalar, probar y desinstalar

Use `apt` (no `dpkg -i`) para que resuelva las dependencias. El `./` es obligatorio:

```bash
sudo apt install ./build/distribuciones/demo-java-deb-jre_1.1.0-1_all.deb

systemctl status demo-java-deb-jre
journalctl -u demo-java-deb-jre -f
curl http://localhost:7071/

# Cambiar la configuración (conffile) y reiniciar
sudo nano /etc/default/demo-java-deb-jre
sudo systemctl restart demo-java-deb-jre

sudo apt remove demo-java-deb-jre   # conserva /etc/default/... y /var/lib/...
sudo apt purge  demo-java-deb-jre   # borra configuración, datos y el usuario de sistema
```

Para la Opción A sustituya el nombre por `demo-java-deb` y el puerto por `7070`.

## Estructura

```
src/main/java/edu/pucmm/eict/Main.java          aplicación (servidor HTTP)
src/packaging/
├── jpackage/                                   Opción A
│   ├── resources/{postinst,prerm,postrm}       reemplazan las plantillas de jpackage (--resource-dir)
│   └── contenido/systemd/demo-java-deb.service queda en /opt/demo-java-deb/lib/systemd (--app-content)
└── deb-manual/                                 Opción B: es el árbol del paquete tal cual
    ├── DEBIAN/control                          metadatos (@VERSION@ e @INSTALLED_SIZE@ los completa Gradle)
    ├── DEBIAN/conffiles                        archivos de configuración que dpkg protege
    ├── DEBIAN/{postinst,prerm,postrm}          scripts del mantenedor
    ├── etc/default/demo-java-deb-jre           configuración (PUERTO, MENSAJE)
    ├── usr/bin/demo-java-deb-jre               lanzador: java -jar ...
    └── usr/lib/systemd/system/demo-java-deb-jre.service
```

### Qué hace cada script del mantenedor

| Script | Cuándo corre | Qué hace aquí |
|---|---|---|
| `postinst configure` | después de copiar los archivos | crea el usuario de sistema, `daemon-reload`, `enable` y `restart` del servicio |
| `prerm remove` / `prerm upgrade` | antes de borrar archivos | `disable --now` / `stop` |
| `postrm purge` | después de borrar, al purgar | borra `/var/lib/<paquete>` y el usuario |

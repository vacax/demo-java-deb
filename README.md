# Demo Java a Deb

Descripción
-----------

Proyecto para demostrar generar un paquete deb a partir de una aplicación Java.

Requisitos
---------
- JDK 17+ (o la versión requerida)
- Maven o Gradle (si el proyecto usa alguno)
- IntelliJ IDEA (opcional, recomendado en Windows)

Estructura
---------
- `src/main/java` - código fuente
- `src/test/java` - pruebas
- `pom.xml` / `build.gradle` - configuración de build

Compilar
-------
Desde IntelliJ IDEA:
1. Abrir el proyecto en `IntelliJ IDEA`.
2. Esperar a que IntelliJ indexe las dependencias.
3. Ejecutar Build \> Build Project.

Desde línea de comandos (Windows):

- Con Maven: 
 
```mvn clean package```

  El artefacto generado quedará en `target/`.

- Con Gradle (wrapper):

```.\gradlew clean build```
  
El artefacto generado quedará en `build/libs/`.

- Con `javac` (proyecto simple sin build tools):

````
javac -d out src\main\java\com\miempresa*.java
````

Ejecutar
-------
Desde IntelliJ IDEA:
- Ejecutar la clase con el método `main` o crear una configuración de ejecución en Run \> Edit Configurations.

Desde línea de comandos (Windows):

- Ejecutar JAR:

````
java -jar target\nombre-del-artefacto.jar
````

o si usas Gradle:

````
java -jar build\libs\nombre-del-artefacto.jar
````

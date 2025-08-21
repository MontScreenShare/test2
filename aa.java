import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.*;

public class InMemoryLoader {

    public static void main(String[] args) throws Exception {
        // args[0] = base64 del JAR (o percorso rete se vuoi leggere direttamente)
        if (args.length < 1) {
            System.err.println("Usage: java InMemoryLoader <base64Jar>");
            return;
        }

        byte[] jarBytes = Base64.getDecoder().decode(args[0]);

        // Legge tutte le classi dal JAR
        Map<String, byte[]> classes = new HashMap<>();
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = jis.read(buffer)) != -1) {
                        baos.write(buffer, 0, read);
                    }
                    String className = entry.getName().replace("/", ".").replace(".class", "");
                    classes.put(className, baos.toByteArray());
                }
            }
        }

        // ClassLoader personalizzato in memoria
        ClassLoader loader = new ClassLoader() {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] b = classes.get(name);
                if (b == null) return super.findClass(name);
                return defineClass(name, b, 0, b.length);
            }
        };

        // Lancia la classe principale (sostituisci "Main" con quella corretta)
        String mainClassName = "Main"; 
        Class<?> mainClass = loader.loadClass(mainClassName);
        Method mainMethod = mainClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) new String[]{});
    }
}

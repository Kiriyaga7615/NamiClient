package me.kiriyaga.nami.util;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClasspathScanner {

    @SuppressWarnings("unchecked")
    public static <T> Set<Class<? extends T>> findAnnotated(Class<T> baseClass, Class<?> annotation) {
        Set<Class<? extends T>> result = new HashSet<>();

        try {
            String resourcePath;

            if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                resourcePath = ""; // ide
            } else {
                resourcePath = "me/kiriyaga/nami"; //prod
            }

            //TODO fucking fix this

            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(resourcePath);

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String filePath = URLDecoder.decode(url.getFile(), "UTF-8");

                if (filePath.startsWith("file:") && filePath.contains("!")) {
                    String[] split = filePath.split("!");
                    try (JarFile jar = new JarFile(split[0].substring("file:".length()))) {
                        result.addAll(scanJar(jar, baseClass, annotation));
                    }
                } else {
                    File dir = new File(filePath);
                    if (dir.exists() && dir.isDirectory()) {
                        result.addAll(scanDirectory(dir, "", baseClass, annotation));
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return result;
    }

    private static <T> Set<Class<? extends T>> scanDirectory(File dir, String pkg, Class<T> base, Class<?> annotation) throws ClassNotFoundException {
        Set<Class<? extends T>> result = new HashSet<>();

        for (File file : Objects.requireNonNull(dir.listFiles())) {
            String fullPkg = pkg.isEmpty() ? "" : pkg + ".";
            if (file.isDirectory()) {
                result.addAll(scanDirectory(file, fullPkg + file.getName(), base, annotation));
            } else if (file.getName().endsWith(".class")) {
                String className = fullPkg + file.getName().replace(".class", "");

                if (file.getName().equals("module-info.class")) {
                    continue;
                }

                if (className.startsWith("me.kiriyaga.nami.mixin.")) {
                    continue;
                }

                Class<?> cls = Class.forName(className);

                if (base.isAssignableFrom(cls) && cls.isAnnotationPresent((Class<? extends Annotation>) annotation)) {
                    result.add((Class<? extends T>) cls);
                }
            }
        }

        return result;
    }

    private static <T> Set<Class<? extends T>> scanJar(JarFile jar, Class<T> base, Class<?> annotation) throws ClassNotFoundException {
        Set<Class<? extends T>> result = new HashSet<>();
        Enumeration<JarEntry> entries = jar.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            if (name.endsWith(".class")) {
                if (name.equals("module-info.class")) {
                    continue;
                }

                if (name.startsWith("META-INF/versions/")) {
                    continue;
                }

                if (name.startsWith("me/kiriyaga/nami/mixin/")) {
                    continue;
                }

                String className = name.replace('/', '.').replace(".class", "");

                if (!className.startsWith("me.kiriyaga.nami.")) {
                    continue;
                }

                Class<?> cls = Class.forName(className, false, Thread.currentThread().getContextClassLoader());

                if (base.isAssignableFrom(cls) && cls.isAnnotationPresent((Class<? extends Annotation>) annotation)) {
                    result.add((Class<? extends T>) cls);
                }
            }
        }

        return result;
    }
}
package com.pipelinecrm.tools.crap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class ProductionClassDetector {

    public boolean hasExecutableClasses(Path classesDir) {
        if (classesDir == null || !Files.isDirectory(classesDir)) {
            return false;
        }
        try (Stream<Path> walk = Files.walk(classesDir)) {
            return walk.anyMatch(ProductionClassDetector::isExecutableClass);
        } catch (IOException ex) {
            throw new CrapCheckException("failed to scan classes directory: " + classesDir, ex);
        }
    }

    private static boolean isExecutableClass(Path path) {
        String fileName = path.getFileName().toString();
        if (!fileName.endsWith(".class")) {
            return false;
        }
        return !fileName.equals("package-info.class") && !fileName.equals("module-info.class");
    }
}

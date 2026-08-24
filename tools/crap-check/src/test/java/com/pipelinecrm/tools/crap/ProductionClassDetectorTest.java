package com.pipelinecrm.tools.crap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProductionClassDetectorTest {

    private final ProductionClassDetector detector = new ProductionClassDetector();

    @Test
    void emptyOrMissingDirectoryIsNotExecutable(@TempDir Path temp) {
        assertThat(detector.hasExecutableClasses(null)).isFalse();
        assertThat(detector.hasExecutableClasses(temp.resolve("missing"))).isFalse();
        assertThat(detector.hasExecutableClasses(temp)).isFalse();
    }

    @Test
    void packageInfoAloneIsNotExecutable(@TempDir Path temp) throws IOException {
        Path pkg = temp.resolve("com/pipelinecrm/domain");
        Files.createDirectories(pkg);
        Files.writeString(pkg.resolve("package-info.class"), "x");
        Files.writeString(pkg.resolve("module-info.class"), "x");
        Files.writeString(pkg.resolve("README.txt"), "x");
        assertThat(detector.hasExecutableClasses(temp)).isFalse();
    }

    @Test
    void realClassFileCounts(@TempDir Path temp) throws IOException {
        Path pkg = temp.resolve("com/pipelinecrm/domain");
        Files.createDirectories(pkg);
        Files.writeString(pkg.resolve("Deal.class"), "x");
        assertThat(detector.hasExecutableClasses(temp)).isTrue();
    }
}

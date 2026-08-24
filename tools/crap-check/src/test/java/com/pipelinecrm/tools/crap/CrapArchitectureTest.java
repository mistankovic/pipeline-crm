package com.pipelinecrm.tools.crap;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CrapArchitectureTest {

    private static final JavaClasses TOOL = new ClassFileImporter().importPath(Path.of("target/classes"));

    @Test
    void production_bytecode_is_present() {
        assertThat(TOOL).isNotEmpty();
    }

    @Test
    void every_production_class_lives_in_the_tools_package() {
        classes()
                .should()
                .resideInAPackage("com.pipelinecrm.tools.crap..")
                .because("CONSTITUTION.md §2.1: crap-check maps to com.pipelinecrm.tools.crap")
                .check(TOOL);
    }

    @Test
    void tool_must_not_use_lombok_or_frameworks() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.projectlombok..",
                        "org.springframework..",
                        "jakarta.persistence..",
                        "javax.persistence..")
                .because("CONSTITUTION.md §2.1: crap-check is JDK only")
                .check(TOOL);
    }
}

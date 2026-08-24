package com.pipelinecrm.adapter.web.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class WebIndependenceTest {

    private static final JavaClasses WEB = new ClassFileImporter().importPath(Path.of("target/classes"));

    @Test
    void production_bytecode_is_present() {
        assertThat(WEB).isNotEmpty();
    }

    @Test
    void every_production_class_lives_in_the_web_package() {
        classes()
                .should()
                .resideInAPackage("com.pipelinecrm.adapter.web..")
                .because("CONSTITUTION.md §2.1: the web module maps to com.pipelinecrm.adapter.web")
                .check(WEB);
    }

    @Test
    void web_must_not_depend_on_persistence_or_bootstrap() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.pipelinecrm.adapter.persistence..", "com.pipelinecrm.bootstrap..")
                .because("CONSTITUTION.md §2.1: adapters do not depend on each other or on the composition root")
                .check(WEB);
    }

    @Test
    void web_must_not_use_lombok() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.projectlombok..")
                .because("CONSTITUTION.md §2.2: Lombok is banned in every module")
                .check(WEB);
    }
}

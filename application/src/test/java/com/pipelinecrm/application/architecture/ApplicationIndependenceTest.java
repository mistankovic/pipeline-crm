package com.pipelinecrm.application.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ApplicationIndependenceTest {

    private static final JavaClasses APPLICATION = new ClassFileImporter().importPath(Path.of("target/classes"));

    @Test
    void production_bytecode_is_present() {
        assertThat(APPLICATION).isNotEmpty();
    }

    @Test
    void every_production_class_lives_in_the_application_package() {
        classes()
                .should()
                .resideInAPackage("com.pipelinecrm.application..")
                .because("CONSTITUTION.md §2.1: the application module maps to com.pipelinecrm.application")
                .check(APPLICATION);
    }

    @Test
    void application_must_not_depend_on_frameworks() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "org.springframework.boot..",
                        "org.springframework.data..",
                        "jakarta.persistence..",
                        "jakarta.servlet..",
                        "jakarta.ws.rs..",
                        "javax.persistence..",
                        "javax.servlet..",
                        "javax.ws.rs..",
                        "javax.ejb..",
                        "org.hibernate..",
                        "com.fasterxml.jackson..",
                        "org.mockito..",
                        "org.projectlombok..")
                .because("CONSTITUTION.md §2: use cases depend on domain, not frameworks")
                .check(APPLICATION);
    }

    @Test
    void application_must_not_depend_on_adapters() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.pipelinecrm.adapter..", "com.pipelinecrm.bootstrap..")
                .because("CONSTITUTION.md §2: dependencies point inward")
                .check(APPLICATION);
    }

    @Test
    void application_may_only_use_domain_and_jdk() {
        classes()
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "com.pipelinecrm.application..",
                        "com.pipelinecrm.domain..",
                        "java..",
                        "javax.crypto..",
                        "javax.net..",
                        "javax.security..",
                        "javax.sql..",
                        "javax.naming..",
                        "javax.management..",
                        "javax.xml..",
                        "javax.annotation.processing..",
                        "javax.lang.model..",
                        "javax.tools..",
                        "javax.transaction.xa..",
                        "javax.imageio..",
                        "javax.print..",
                        "javax.sound..",
                        "javax.script..",
                        "javax.swing..",
                        "javax.accessibility..")
                .because("CONSTITUTION.md §2.1: application compile-time dependencies are domain + JDK")
                .check(APPLICATION);
    }
}

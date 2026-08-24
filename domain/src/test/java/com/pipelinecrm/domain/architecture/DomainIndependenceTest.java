package com.pipelinecrm.domain.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DomainIndependenceTest {

    private static final JavaClasses DOMAIN = new ClassFileImporter().importPath(Path.of("target/classes"));

    @Test
    void production_bytecode_is_present() {
        assertThat(DOMAIN).isNotEmpty();
    }

    @Test
    void every_production_class_lives_in_the_domain_package() {
        classes()
                .should()
                .resideInAPackage("com.pipelinecrm.domain..")
                .because("CONSTITUTION.md §2.1: the domain module maps to com.pipelinecrm.domain")
                .check(DOMAIN);
    }

    @Test
    void domain_must_not_depend_on_frameworks() {
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
                .because("CONSTITUTION.md §2: the domain layer is pure JDK")
                .check(DOMAIN);
    }

    @Test
    void domain_may_only_use_its_own_types_and_the_jdk() {
        classes()
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(
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
                .because("CONSTITUTION.md §2.1: domain compile-time dependencies are JDK only")
                .check(DOMAIN);
    }
}

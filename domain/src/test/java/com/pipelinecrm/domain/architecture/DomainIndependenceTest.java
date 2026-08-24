package com.pipelinecrm.domain.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class DomainIndependenceTest {

    private static final JavaClasses DOMAIN = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.pipelinecrm.domain");

    @Test
    void production_package_is_present() {
        assertThat(DOMAIN).isNotEmpty();
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
                        "org.hibernate..",
                        "com.fasterxml.jackson..",
                        "org.projectlombok..")
                .because("CONSTITUTION.md §2: the domain layer is pure JDK")
                .check(DOMAIN);
    }

    @Test
    void domain_may_only_use_its_own_types_and_the_jdk() {
        classes()
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage("com.pipelinecrm.domain..", "java..", "javax..")
                .because("CONSTITUTION.md §2.1: domain compile-time dependencies are JDK only")
                .check(DOMAIN);
    }
}

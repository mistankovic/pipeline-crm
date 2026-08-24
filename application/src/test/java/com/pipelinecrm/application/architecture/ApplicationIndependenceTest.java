package com.pipelinecrm.application.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ApplicationIndependenceTest {

    private static final JavaClasses APPLICATION = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.pipelinecrm.application");

    @Test
    void production_package_is_present() {
        assertThat(APPLICATION).isNotEmpty();
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
                        "org.hibernate..",
                        "com.fasterxml.jackson..",
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
                        "javax..")
                .because("CONSTITUTION.md §2.1: application compile-time dependencies are domain + JDK")
                .check(APPLICATION);
    }
}

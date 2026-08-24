package com.pipelinecrm.adapter.web.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class WebIndependenceTest {

    private static final JavaClasses WEB = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.pipelinecrm.adapter.web");

    @Test
    void production_package_is_present() {
        assertThat(WEB).isNotEmpty();
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
}

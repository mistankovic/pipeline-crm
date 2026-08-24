package com.pipelinecrm.adapter.persistence.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class PersistenceIndependenceTest {

    private static final JavaClasses PERSISTENCE = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.pipelinecrm.adapter.persistence");

    @Test
    void production_package_is_present() {
        assertThat(PERSISTENCE).isNotEmpty();
    }

    @Test
    void persistence_must_not_depend_on_web_or_bootstrap() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.pipelinecrm.adapter.web..", "com.pipelinecrm.bootstrap..")
                .because("CONSTITUTION.md §2.1: adapters do not depend on each other or on the composition root")
                .check(PERSISTENCE);
    }
}

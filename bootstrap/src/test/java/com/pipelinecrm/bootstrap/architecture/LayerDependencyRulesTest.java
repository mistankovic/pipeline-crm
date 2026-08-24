package com.pipelinecrm.bootstrap.architecture;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class LayerDependencyRulesTest {

    private static final JavaClasses PROJECT = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.pipelinecrm");

    @Test
    void production_packages_are_present() {
        assertThat(PROJECT).isNotEmpty();
    }

    @Test
    void layers_honor_the_dependency_rule() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain")
                .definedBy("com.pipelinecrm.domain..")
                .layer("Application")
                .definedBy("com.pipelinecrm.application..")
                .layer("Persistence")
                .definedBy("com.pipelinecrm.adapter.persistence..")
                .layer("Web")
                .definedBy("com.pipelinecrm.adapter.web..")
                .layer("Bootstrap")
                .definedBy("com.pipelinecrm.bootstrap..")
                .whereLayer("Domain")
                .mayNotAccessAnyLayer()
                .whereLayer("Application")
                .mayOnlyAccessLayers("Domain")
                .whereLayer("Persistence")
                .mayOnlyAccessLayers("Application", "Domain")
                .whereLayer("Web")
                .mayOnlyAccessLayers("Application", "Domain")
                .whereLayer("Bootstrap")
                .mayOnlyAccessLayers("Web", "Persistence", "Application", "Domain")
                .whereLayer("Persistence")
                .mayOnlyBeAccessedByLayers("Bootstrap")
                .whereLayer("Web")
                .mayOnlyBeAccessedByLayers("Bootstrap")
                .whereLayer("Bootstrap")
                .mayNotBeAccessedByAnyLayer()
                .because("CONSTITUTION.md §2: dependencies point strictly inward")
                .check(PROJECT);
    }

    @Test
    void top_level_packages_are_free_of_cycles() {
        slices()
                .matching("com.pipelinecrm.(*)..")
                .should()
                .beFreeOfCycles()
                .because("CONSTITUTION.md §2: no cyclic dependencies between layers")
                .check(PROJECT);
    }

    @Test
    void adapters_are_free_of_cycles() {
        slices()
                .matching("com.pipelinecrm.adapter.(*)..")
                .should()
                .beFreeOfCycles()
                .because("CONSTITUTION.md §2.1: web and persistence adapters are independent")
                .check(PROJECT);
    }
}

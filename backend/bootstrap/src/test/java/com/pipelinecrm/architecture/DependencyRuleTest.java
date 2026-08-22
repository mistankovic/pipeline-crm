package com.pipelinecrm.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * CONSTITUTION.md section 1: source code dependencies point strictly inward. Maven already
 * makes most of this impossible; these rules catch what Maven cannot — a package in the
 * right module depending on the wrong thing, and cycles.
 */
@AnalyzeClasses(packages = Layers.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class DependencyRuleTest {

    @ArchTest
    static final ArchRule dependencies_point_inward = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage(Layers.ROOT + "..")
            .layer(Layers.DOMAIN).definedBy(Layers.DOMAIN_PACKAGES)
            .layer(Layers.APPLICATION).definedBy(Layers.APPLICATION_PACKAGES)
            .layer(Layers.ADAPTER_WEB).definedBy(Layers.ADAPTER_WEB_PACKAGES)
            .layer(Layers.ADAPTER_PERSISTENCE).definedBy(Layers.ADAPTER_PERSISTENCE_PACKAGES)
            .layer(Layers.BOOTSTRAP).definedBy(Layers.BOOTSTRAP_PACKAGES)

            .whereLayer(Layers.BOOTSTRAP).mayNotBeAccessedByAnyLayer()
            .whereLayer(Layers.ADAPTER_WEB).mayOnlyBeAccessedByLayers(Layers.BOOTSTRAP)
            .whereLayer(Layers.ADAPTER_PERSISTENCE).mayOnlyBeAccessedByLayers(Layers.BOOTSTRAP)
            .whereLayer(Layers.APPLICATION)
            .mayOnlyBeAccessedByLayers(Layers.ADAPTER_WEB, Layers.ADAPTER_PERSISTENCE, Layers.BOOTSTRAP)
            .as("dependencies point inward: nothing reaches out to an outer layer");

    @ArchTest
    static final ArchRule the_two_adapters_do_not_know_each_other = slices()
            .matching("com.pipelinecrm.adapter.(*)..")
            .should().notDependOnEachOther()
            .as("an adapter that needs another adapter has to go through a port");

    @ArchTest
    static final ArchRule no_package_anywhere_takes_part_in_a_cycle = slices()
            .matching("com.pipelinecrm.(**)")
            .should().beFreeOfCycles();
}

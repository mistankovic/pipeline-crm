package com.pipelinecrm.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * CONSTITUTION.md section 2.2: business rules live in the domain and the use cases, never
 * in a controller or a repository. These rules police the shape of the adapters, which is
 * the part of that promise a tool can actually check.
 */
@AnalyzeClasses(packages = Layers.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class InterfaceAdapterTest {

    /**
     * Written as a prohibition rather than "controllers should live in the web adapter",
     * because the positive form matches nothing until controllers exist and an ArchUnit rule
     * that matches nothing is a rule that is not protecting anything. This form has classes
     * to check from the first day.
     */
    @ArchTest
    static final ArchRule nothing_outside_the_web_adapter_is_a_controller = noClasses()
            .that().resideOutsideOfPackage(Layers.ADAPTER_WEB_PACKAGES)
            .should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule persistence_types_stay_in_the_persistence_adapter = noClasses()
            .that().resideOutsideOfPackage(Layers.ADAPTER_PERSISTENCE_PACKAGES)
            .should().dependOnClassesThat().resideInAPackage("org.springframework.data..")
            .as("Spring Data is a detail of one adapter");

    @ArchTest
    static final ArchRule nothing_outside_the_web_adapter_speaks_http = noClasses()
            .that().resideOutsideOfPackage(Layers.ADAPTER_WEB_PACKAGES)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.web..", "org.springframework.http..")
            .as("HTTP is a detail of one adapter");

    @ArchTest
    static final ArchRule ports_are_interfaces = classes()
            .that().resideInAnyPackage("com.pipelinecrm.application.port.in..",
                    "com.pipelinecrm.application.port.out..")
            .and().areTopLevelClasses()
            .should().beInterfaces()
            .as("a port is a boundary, not an implementation");
}

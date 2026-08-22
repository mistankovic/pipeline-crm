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

    /**
     * Controllers talk in views, requests and enums. If a controller can reach a {@code Deal},
     * it can ask the deal a question and act on the answer — which is how a business rule ends
     * up in a controller. Making the entity unreachable removes the temptation entirely.
     *
     * <p>Scoped to the controllers rather than to the whole web adapter, because
     * {@code JwtAccessTokenIssuer} implements an **output** port whose signature names a
     * {@code User}. That is the port's vocabulary, chosen by the layer inside, and it is
     * exactly the traffic the dependency rule permits. The first version of this rule failed on
     * it, correctly.
     */
    @ArchTest
    static final ArchRule no_controller_ever_touches_an_entity = noClasses()
            .that().resideInAPackage("com.pipelinecrm.adapter.web.rest..")
            .should().dependOnClassesThat().haveNameMatching(
                    "com\\.pipelinecrm\\.domain\\.(deal\\.Deal|deal\\.DealSnapshot|deal\\.DealTerms"
                            + "|deal\\.DealParties|activity\\.Activity|activity\\.DealActivities"
                            + "|company\\.Company|contact\\.Contact|user\\.User"
                            + "|shared\\.Money|shared\\.Probability|forecast\\.Forecast)")
            .as("no controller can reach an entity, so no controller can ask one a question");

    @ArchTest
    static final ArchRule ports_are_interfaces = classes()
            .that().resideInAnyPackage("com.pipelinecrm.application.port.in..",
                    "com.pipelinecrm.application.port.out..")
            .and().areTopLevelClasses()
            .should().beInterfaces()
            .as("a port is a boundary, not an implementation");
}

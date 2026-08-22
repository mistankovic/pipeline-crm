package com.pipelinecrm.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The domain and the use cases must be able to compile and run with no framework present.
 * Maven's banned-dependency rules already guarantee the jars are absent; these rules state
 * the intent in the language of the architecture, and they catch the day somebody "just
 * adds one annotation" while relaxing the POM.
 */
@AnalyzeClasses(packages = Layers.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class InnerLayerPurityTest {

    private static final String[] FRAMEWORKS = {
        "org.springframework..", "jakarta..", "javax..", "com.fasterxml..",
        "org.hibernate..", "java.sql..", "javax.sql..", "org.slf4j..", "java.util.logging..",
    };

    @ArchTest
    static final ArchRule the_domain_knows_no_framework = noClasses()
            .that().resideInAPackage(Layers.DOMAIN_PACKAGES)
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS)
            .as("the domain depends on the JDK and nothing else");

    @ArchTest
    static final ArchRule the_use_cases_know_no_framework = noClasses()
            .that().resideInAPackage(Layers.APPLICATION_PACKAGES)
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS)
            .as("use cases depend on the domain and the JDK, and nothing else");

    @ArchTest
    static final ArchRule the_domain_carries_no_annotations_from_outside
            = noClasses()
            .that().resideInAnyPackage(Layers.DOMAIN_PACKAGES, Layers.APPLICATION_PACKAGES)
            .should().beAnnotatedWith(com.tngtech.archunit.base.DescribedPredicate.describe(
                    "an annotation from a framework",
                    annotation -> annotation.getRawType().getPackageName().startsWith("org.springframework")
                            || annotation.getRawType().getPackageName().startsWith("jakarta")))
            .as("no entity or use case is annotated by a framework");

    @ArchTest
    static final ArchRule domain_state_is_private_and_mostly_final = fields()
            .that().areDeclaredInClassesThat().resideInAPackage(Layers.DOMAIN_PACKAGES)
            .and().areNotStatic()
            .should().bePrivate()
            .as("no domain object exposes its state directly");
}

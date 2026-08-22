package com.pipelinecrm.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Guards the guards.
 *
 * <p>Run as {@code mvn -pl bootstrap test}, the architecture rules resolve the inner modules
 * from the **local Maven repository** — the last artefacts that were installed, not the
 * working tree — so they can report green against code that no longer exists. I reproduced
 * that: a violation on disk passed the module-only run and failed the reactor run.
 *
 * <p>This rule closes the hole. Every analysed project class must come from somewhere inside
 * this checkout. A reactor build satisfies it (module {@code target/} directories and the
 * jars built there are all under the root); resolving from {@code ~/.m2/repository} does not.
 *
 * <p>See docs/reviews/stage-2-review.md, finding F-2.1.
 */
@AnalyzeClasses(packages = Layers.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class AnalysedCodeTest {

    private static final Path REACTOR_ROOT = reactorRoot();

    private static final ArchCondition<JavaClass> COME_FROM_OUTSIDE_THIS_CHECKOUT =
            new ArchCondition<>("come from outside this checkout") {
                @Override
                public void check(JavaClass candidate, ConditionEvents events) {
                    String location = locationOf(candidate);
                    boolean outside = !location.startsWith(REACTOR_ROOT.toString());
                    events.add(new SimpleConditionEvent(candidate, outside,
                            candidate.getName() + " was analysed from " + location));
                }
            };

    @ArchTest
    static final ArchRule the_architecture_is_checked_against_this_working_tree = noClasses()
            .should(COME_FROM_OUTSIDE_THIS_CHECKOUT)
            .as("every analysed class comes from this checkout, not from the local Maven repository")
            .because("rules evaluated against an installed artefact report green for code that is not there; "
                    + "run the build from the reactor root (mvn -f backend/pom.xml verify)");

    private static String locationOf(JavaClass candidate) {
        Optional<URI> origin = candidate.getSource().map(source -> source.getUri());
        String location = origin.map(URI::toString).orElse("an unknown location");
        return location
                .replaceFirst("^jar:", "")
                .replaceFirst("^file:", "")
                .replaceFirst("!.*$", "")
                .replaceFirst("^/+", "/");
    }

    /** The directory holding {@code .mvn}, which Stage 0 added precisely to be findable. */
    private static Path reactorRoot() {
        Path directory = Paths.get("").toAbsolutePath();
        while (directory != null && !Files.isDirectory(directory.resolve(".mvn"))) {
            directory = directory.getParent();
        }
        if (directory == null) {
            throw new IllegalStateException("no .mvn marker above " + Paths.get("").toAbsolutePath());
        }
        return directory;
    }
}

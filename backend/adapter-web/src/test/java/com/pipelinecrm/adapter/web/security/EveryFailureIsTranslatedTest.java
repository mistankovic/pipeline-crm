package com.pipelinecrm.adapter.web.security;

import com.pipelinecrm.adapter.web.error.HttpTranslation;
import com.pipelinecrm.application.error.ApplicationException;
import com.pipelinecrm.domain.shared.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every way the system can say no is translated to a status code, on purpose.
 *
 * <p>The table in {@code HttpTranslation} is hand-maintained. Without this test, adding a
 * domain exception and forgetting to map it produces a 500 in production and nothing else:
 * no compile error, no failing test. The same reasoning produced {@code EveryPortIsWiredTest}
 * for the hand-wired object graph. See docs/reviews/stage-5-review.md, finding F-5.1.
 */
class EveryFailureIsTranslatedTest {

    @Test
    void every_domain_failure_has_a_status_or_is_a_declared_server_fault() throws Exception {
        assertEveryFailureIsAccountedFor("com/pipelinecrm/domain", DomainException.class);
    }

    @Test
    void every_application_failure_has_a_status_or_is_a_declared_server_fault() throws Exception {
        assertEveryFailureIsAccountedFor("com/pipelinecrm/application", ApplicationException.class);
    }

    private void assertEveryFailureIsAccountedFor(String packagePath, Class<?> root) throws IOException {
        List<Class<?>> failures = concreteSubclassesOf(root, packagePath);

        assertThat(failures)
                .describedAs("no subclasses of %s were found, so this test proves nothing", root.getSimpleName())
                .isNotEmpty();
        assertThat(failures).allSatisfy(failure -> assertThat(HttpTranslation.isTranslated(failure))
                .describedAs("%s must be given a status in HttpTranslation, or listed there as a "
                        + "deliberate server fault. Neither would leave it a silent 500.", failure.getName())
                .isTrue());
    }

    private List<Class<?>> concreteSubclassesOf(Class<?> root, String packagePath) throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        var readers = new CachingMetadataReaderFactory(resolver);
        return Arrays.stream(resolver.getResources("classpath*:" + packagePath + "/**/*.class"))
                .map(resource -> classNameOf(readers, resource))
                .filter(name -> !name.contains("$"))
                .map(EveryFailureIsTranslatedTest::load)
                .filter(root::isAssignableFrom)
                .filter(type -> !Modifier.isAbstract(type.getModifiers()))
                .toList();
    }

    private static String classNameOf(CachingMetadataReaderFactory readers, Resource resource) {
        try {
            return readers.getMetadataReader(resource).getClassMetadata().getClassName();
        } catch (IOException unreadable) {
            throw new IllegalStateException("cannot read " + resource, unreadable);
        }
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException | NoClassDefFoundError missing) {
            throw new IllegalStateException(missing);
        }
    }
}

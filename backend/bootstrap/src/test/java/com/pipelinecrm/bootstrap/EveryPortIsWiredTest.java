package com.pipelinecrm.bootstrap;

import com.pipelinecrm.adapter.persistence.testing.PostgresBackedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every input and output port has exactly one implementation in the running application.
 *
 * <p>The use cases carry no annotations, so nothing scans them into existence: they are wired
 * by hand in {@link UseCaseConfiguration}. That is the price of the dependency rule, and its
 * failure mode is a port somebody forgot — which produces a startup error only if some
 * controller happens to ask for it. This test asks for all of them.
 */
@SpringBootTest
class EveryPortIsWiredTest extends PostgresBackedTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void every_input_port_has_exactly_one_implementation() throws Exception {
        assertEachPortIsWired("com/pipelinecrm/application/port/in");
    }

    @Test
    void every_output_port_has_exactly_one_implementation() throws Exception {
        assertEachPortIsWired("com/pipelinecrm/application/port/out");
    }

    private void assertEachPortIsWired(String packagePath) throws Exception {
        List<Class<?>> ports = portsIn(packagePath);

        assertThat(ports).describedAs("no ports were found in %s, so this test proves nothing", packagePath)
                .isNotEmpty();
        assertThat(ports).allSatisfy(port ->
                assertThat(context.getBeanNamesForType(port))
                        .describedAs("implementations of %s", port.getSimpleName())
                        .hasSize(1));
    }

    private List<Class<?>> portsIn(String packagePath) throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        var readers = new CachingMetadataReaderFactory(resolver);
        return java.util.Arrays.stream(resolver.getResources("classpath*:" + packagePath + "/*.class"))
                .map(resource -> classNameOf(readers, resource))
                // Nested types such as LogActivity.About are part of a port's vocabulary, not
                // ports in their own right, and nothing implements them as a bean.
                .filter(name -> !name.contains("$"))
                .map(EveryPortIsWiredTest::load)
                .filter(Class::isInterface)
                .toList();
    }

    private static String classNameOf(CachingMetadataReaderFactory readers, org.springframework.core.io.Resource r) {
        try {
            return readers.getMetadataReader(r).getClassMetadata().getClassName();
        } catch (IOException unreadable) {
            throw new IllegalStateException("cannot read " + r, unreadable);
        }
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException missing) {
            throw new IllegalStateException(missing);
        }
    }
}

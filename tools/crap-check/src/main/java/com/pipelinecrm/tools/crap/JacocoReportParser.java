package com.pipelinecrm.tools.crap;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class JacocoReportParser {

    public List<MethodCoverage> parse(Path report) {
        Document document = JacocoXml.parse(report);
        Element root = document.getDocumentElement();
        List<MethodCoverage> methods = new ArrayList<>();
        for (Element packageElement : XmlElements.childrenNamed(root, "package")) {
            collectPackage(packageElement, methods);
        }
        return List.copyOf(methods);
    }

    private static void collectPackage(Element packageElement, List<MethodCoverage> sink) {
        String packageName = packageElement.getAttribute("name").replace('/', '.');
        for (Element classElement : XmlElements.childrenNamed(packageElement, "class")) {
            collectClass(packageName, classElement, sink);
        }
    }

    private static void collectClass(String packageName, Element classElement, List<MethodCoverage> sink) {
        String className = classElement.getAttribute("name");
        for (Element methodElement : XmlElements.childrenNamed(classElement, "method")) {
            sink.add(readMethod(packageName, className, methodElement));
        }
    }

    private static MethodCoverage readMethod(String packageName, String className, Element methodElement) {
        JacocoCounters counters = JacocoCounters.from(methodElement);
        return new MethodCoverage(
                packageName,
                className,
                methodElement.getAttribute("name"),
                methodElement.getAttribute("desc"),
                counters.complexity(),
                counters.linesCovered(),
                counters.linesMissed());
    }
}

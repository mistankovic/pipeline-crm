package com.pipelinecrm.tools.crap;

import org.w3c.dom.Element;

final class JacocoCounters {

    private final int complexity;
    private final int linesCovered;
    private final int linesMissed;

    private JacocoCounters(int complexity, int linesCovered, int linesMissed) {
        this.complexity = complexity;
        this.linesCovered = linesCovered;
        this.linesMissed = linesMissed;
    }

    static JacocoCounters from(Element methodElement) {
        int complexity = 0;
        int linesCovered = 0;
        int linesMissed = 0;
        for (Element counter : XmlElements.childrenNamed(methodElement, "counter")) {
            String type = counter.getAttribute("type");
            if ("COMPLEXITY".equals(type)) {
                complexity = missed(counter) + covered(counter);
            } else if ("LINE".equals(type)) {
                linesCovered = covered(counter);
                linesMissed = missed(counter);
            }
        }
        return new JacocoCounters(complexity, linesCovered, linesMissed);
    }

    int complexity() {
        return complexity;
    }

    int linesCovered() {
        return linesCovered;
    }

    int linesMissed() {
        return linesMissed;
    }

    private static int missed(Element counter) {
        return integerAttribute(counter, "missed");
    }

    private static int covered(Element counter) {
        return integerAttribute(counter, "covered");
    }

    private static int integerAttribute(Element counter, String name) {
        String raw = counter.getAttribute(name);
        if (raw.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            throw new CrapCheckException("invalid counter " + name + ": " + raw, ex);
        }
    }
}

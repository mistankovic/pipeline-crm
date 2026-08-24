package com.pipelinecrm.tools.crap;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class XmlElements {

    private XmlElements() {}

    static List<Element> childrenNamed(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (isElementNamed(node, tagName)) {
                result.add((Element) node);
            }
        }
        return result;
    }

    private static boolean isElementNamed(Node node, String tagName) {
        return node instanceof Element element && tagName.equals(element.getTagName());
    }
}

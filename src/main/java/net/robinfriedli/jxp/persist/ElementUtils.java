package net.robinfriedli.jxp.persist;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class ElementUtils {

    public static boolean hasTextContent(Element element) {
        Node childNode = element.getChildNodes().item(0);
        return childNode != null && childNode.getNodeType() == Node.TEXT_NODE && !childNode.getTextContent().trim().equals("");
    }

    public static String getTextContent(Element element) {
        if (hasTextContent(element)) {
            Node childNode = element.getChildNodes().item(0);
            return childNode.getTextContent();
        } else {
            return "";
        }
    }

    public static Map<String, String> getAttributes(Element element) {
        NamedNodeMap attributes = element.getAttributes();
        Map<String, String> attributeMap = new HashMap<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            attributeMap.put(attribute.getNodeName(), attribute.getNodeValue());
        }

        return attributeMap;
    }

    // gets called for the old Element when an XmlElement gets bound to a new Element (typically subelements)
    public static void destroy(Element element) {
        // might be called when a child element gets added to a different parent, thus setting a new Element on an existing
        // XmlElement instance (via XmlElement#setElement), in this case the element may have already been
        // detached from its old parent if XmlElement#removeSubElement was called before adding it to the new parent
        Node parentNode = element.getParentNode();
        if (parentNode != null) {
            parentNode.removeChild(element);
        }
    }

}

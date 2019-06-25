package net.robinfriedli.jxp.api;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.persist.Context;
import org.w3c.dom.Element;

/**
 * Default implementation of {@link XmlElement} that will be instantiated for any XML element with an unknown tag name
 * when creating a new {@link Context}. You can make JXP instantiate your own classes based on the XML element's tag name
 * using {@link StaticXmlElementFactory#mapClass(String, Class)}
 */
public class BaseXmlElement extends AbstractXmlElement {

    public BaseXmlElement(String tagName) {
        super(tagName);
    }

    public BaseXmlElement(String tagName, Map<String, ?> attributeMap) {
        super(tagName, attributeMap);
    }

    public BaseXmlElement(String tagName, Map<String, ?> attributeMap, String textContent) {
        super(tagName, attributeMap, textContent);
    }

    public BaseXmlElement(String tagName, Map<String, ?> attributeMap, List<XmlElement> subElements) {
        super(tagName, attributeMap, subElements);
    }

    // constructor to instantiate new XmlElements with
    public BaseXmlElement(String tagName, Map<String, ?> attributeMap, List<XmlElement> subElements, String textContent) {
        super(tagName, attributeMap, subElements, textContent);
    }

    // invoked when reading from file
    public BaseXmlElement(Element element, Context context) {
        super(element, context);
    }

    // invoked when reading from file
    public BaseXmlElement(Element element, List<XmlElement> subElements, Context context) {
        super(element, subElements, context);
    }

    @Nullable
    @Override
    public String getId() {
        return null;
    }
}

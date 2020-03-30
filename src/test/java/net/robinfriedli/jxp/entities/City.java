package net.robinfriedli.jxp.entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.api.AbstractXmlElement;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.persist.Context;
import org.w3c.dom.Element;

public class City extends AbstractXmlElement {

    @SuppressWarnings("unused")
    public City(Element element, Context context) {
        super(element, context);
    }

    @SuppressWarnings("unused")
    public City(Element element, List<XmlElement> subElements, Context context) {
        super(element, subElements, context);
    }

    public City(String tagName, Map<String, ?> attributeMap, List<XmlElement> subElements, String textContent) {
        super(tagName, attributeMap, subElements, textContent);
    }

    public City(String name, int population) {
        super("city", buildAttributes(name, population));
    }

    private static Map<String, ?> buildAttributes(String name, int population) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", name);
        attributes.put("population", population);
        return attributes;
    }

    @Nullable
    @Override
    public String getId() {
        return getAttribute("name").getValue();
    }
}

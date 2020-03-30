package net.robinfriedli.jxp.entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.api.AbstractXmlElement;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.persist.Context;
import org.w3c.dom.Element;

public class Continent extends AbstractXmlElement {

    public Continent(String name) {
        super("continent", getAttributeMap(name));
    }

    @SuppressWarnings("unused")
    public Continent(Element element, Context context) {
        super(element, context);
    }

    @SuppressWarnings("unused")
    public Continent(Element element, List<XmlElement> subElements, Context context) {
        super(element, subElements, context);
    }

    private static Map<String, ?> getAttributeMap(String name) {
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("name", name);

        return attributeMap;
    }

    @Nullable
    @Override
    public String getId() {
        return getAttribute("name").getValue();
    }

}

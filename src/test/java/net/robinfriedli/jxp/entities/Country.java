package net.robinfriedli.jxp.entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.AbstractXmlElement;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.persist.Context;
import org.w3c.dom.Element;

public class Country extends AbstractXmlElement {

    // invoked by JXP
    @SuppressWarnings("unused")
    public Country(Element element, Context context) {
        super(element, context);
    }

    // invoked by JXP
    @SuppressWarnings("unused")
    public Country(Element element, List<XmlElement> subElements, Context context) {
        super(element, subElements, context);
    }

    public Country() {
        super("country");
    }

    public Country(String name, String englishName, boolean sovereign, List<City> cities) {
        super("country", buildAttributes(name, englishName, sovereign), Lists.newArrayList(cities));
    }


    public Country(String tagName, Map<String, ?> attributeMap, List<XmlElement> subElements, String textContent) {
        super(tagName, attributeMap, subElements, textContent);
    }

    private static Map<String, ?> buildAttributes(String name, String englishName, boolean sovereign) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", name);
        attributes.put("englishName", englishName);
        attributes.put("sovereign", sovereign);
        return attributes;
    }

    @Nullable
    @Override
    public String getId() {
        return getAttribute("englishName").getValue();
    }
}

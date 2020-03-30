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

public class State extends AbstractXmlElement {

    @SuppressWarnings("unused")
    public State(Element element, Context context) {
        super(element, context);
    }

    @SuppressWarnings("unused")
    public State(Element element, List<XmlElement> subElements, Context context) {
        super(element, subElements, context);
    }

    public State(String name, int population, List<City> cities) {
        super("state", buildAttributeMap(name, population), Lists.newArrayList(cities));
    }

    private static Map<String, ?> buildAttributeMap(String name, int population) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("population", population);
        return map;
    }

    @Nullable
    @Override
    public String getId() {
        return getAttribute("name").getValue();
    }

}

package net.robinfriedli.jxp.api;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.ElementUtils;
import org.w3c.dom.Element;

public class StaticXmlElementFactory {

    public static final Map<String, Class<? extends XmlElement>> INSTANTIATION_CONTRIBUTIONS = new HashMap<>();

    public static void mapClass(String tagName, Class<? extends XmlElement> type) {
        INSTANTIATION_CONTRIBUTIONS.put(tagName, type);
    }

    public static XmlElement instantiate(String tagName, Map<String, ?> attributeMap, List<XmlElement> subElements, String textContent) {
        Class<? extends XmlElement> classToInstantiate = INSTANTIATION_CONTRIBUTIONS.get(tagName);

        if (classToInstantiate != null) {
            try {
                Constructor<? extends XmlElement> constructor =
                    classToInstantiate.getConstructor(String.class, Map.class, List.class, String.class);
                return constructor.newInstance(tagName, attributeMap, subElements, textContent);
            } catch (NoSuchMethodException e) {
                // do nothing, just return a BaseXmlElement instead
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new PersistException("Could not instantiate " + classToInstantiate + ". Failed invoking constructor.", e);
            }
        }

        return new BaseXmlElement(tagName, attributeMap, subElements, textContent);
    }

    public static List<XmlElement> instantiateAllElements(Context context) {
        List<XmlElement> xmlElements = Lists.newArrayList();
        List<Element> allTopLevelElements = ElementUtils.getChildren(context.getDocument().getDocumentElement());

        for (Element topElement : allTopLevelElements) {
            xmlElements.add(instantiatePersistentXmlElement(topElement, context));
        }

        return xmlElements;
    }

    public static XmlElement instantiatePersistentXmlElement(Element element, Context context, Element... excludedSubElements) {
        List<Element> subElements = ElementUtils.getChildren(element);
        List<XmlElement> instantiatedSubElems = Lists.newArrayList();
        List<Element> excludedSubs = Arrays.asList(excludedSubElements);

        for (Element subElement : subElements) {
            if (excludedSubs.contains(subElement)) {
                continue;
            }

            instantiatedSubElems.add(instantiatePersistentXmlElement(subElement, context));
        }

        Class<? extends XmlElement> xmlClass = INSTANTIATION_CONTRIBUTIONS.get(element.getTagName());
        if (xmlClass != null) {
            try {
                if (subElements.isEmpty()) {
                    Constructor<? extends XmlElement> constructor = xmlClass.getConstructor(Element.class, Context.class);
                    return constructor.newInstance(element, context);
                } else {
                    Constructor<? extends XmlElement> constructor = xmlClass.getConstructor(Element.class, List.class, Context.class);
                    return constructor.newInstance(element, instantiatedSubElems, context);
                }
            } catch (NoSuchMethodException e) {
                throw new PersistException("Your class " + xmlClass + " does not have the appropriate Constructor", e);
            } catch (IllegalAccessException e) {
                throw new PersistException("Cannot access constructor of class " + xmlClass, e);
            } catch (InstantiationException e) {
                throw new PersistException("Cannot instantiate class " + xmlClass, e);
            } catch (InvocationTargetException e) {
                throw new PersistException("Exception while invoking constructor of " + xmlClass, e);
            }
        } else {
            return new BaseXmlElement(element, instantiatedSubElems, context);
        }
    }

}

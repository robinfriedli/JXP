package net.robinfriedli.jxp.persist;

import java.util.List;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.BaseXmlElement;
import net.robinfriedli.jxp.api.XmlAttribute;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.events.AttributeChangingEvent;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.ValueChangingEvent;
import net.robinfriedli.jxp.exceptions.CommitException;
import net.robinfriedli.jxp.exceptions.PersistException;
import org.w3c.dom.Element;

public class DefaultPersistenceManager {

    private Context context;
    private XmlPersister xmlPersister;

    public void initialize(Context context) {
        this.context = context;
        xmlPersister = new XmlPersister(context);
    }

    public List<XmlElement> getAllElements() {
        List<XmlElement> xmlElements = Lists.newArrayList();
        List<Element> allTopLevelElements = xmlPersister.getAllTopLevelElements();

        for (Element topElement : allTopLevelElements) {
            xmlElements.add(instantiateBaseXmlElement(topElement));
        }

        return xmlElements;
    }

    private XmlElement instantiateBaseXmlElement(Element element) {
        List<Element> subElements = xmlPersister.getChildren(element);
        List<XmlElement> instantiatedSubElems = Lists.newArrayList();

        for (Element subElement : subElements) {
            instantiatedSubElems.add(instantiateBaseXmlElement(subElement));
        }

        return new BaseXmlElement(element, instantiatedSubElems, context);
    }

    public void buildTree(List<XmlElement> elements) {
        for (XmlElement element : elements) {
            if (element.hasSubElements()) {
                for (XmlElement subElement : element.getSubElements()) {
                    setParent(subElement, element);
                }
            }
        }
    }

    private void setParent(XmlElement child, XmlElement parent) {
        child.setParent(parent);

        if (child.hasSubElements()) {
            for (XmlElement subElement : child.getSubElements()) {
                setParent(subElement, child);
            }
        }
    }

    public Context getContext() {
        return context;
    }

    public XmlPersister getXmlPersister() {
        return xmlPersister;
    }

    public void commitElementChanges(ElementChangingEvent event) throws CommitException {
        XmlElement element = event.getSource();
        List<AttributeChangingEvent> attributeChanges = event.getChangedAttributes();

        if (attributeChanges != null && !attributeChanges.isEmpty()) {
            xmlPersister.setAttributes(element, attributeChanges);
            attributeChanges.forEach(c -> c.setCommitted(true));
        }
        if (event.textContentChanged()) {
            xmlPersister.setTextContent(event.getChangedTextContent());
        }
    }

    public void write() throws CommitException {
        xmlPersister.writeToFile();
    }

    public void reload() {
        xmlPersister.reloadDocument();
    }

    public void castElement(XmlElement target, XmlElement source) {
        if (target.matchesStructure(source)) {
            List<AttributeChangingEvent> changedAttributes = Lists.newArrayList();
            ValueChangingEvent<String> changedTextContent = null;

            for (XmlAttribute attribute : source.getAttributes()) {
                XmlAttribute existingAttribute = target.getAttribute(attribute.getAttributeName());
                if (!attribute.getValue().equals(existingAttribute.getValue())) {
                    changedAttributes.add(new AttributeChangingEvent(existingAttribute, attribute.getValue()));
                }
            }

            if (!target.getTextContent().equals(source.getTextContent())) {
                changedTextContent = new ValueChangingEvent<>(target, target.getTextContent(), source.getTextContent());
            }

            List<XmlElement> addedSubElements = Lists.newArrayList();
            List<XmlElement> removedSubElements = Lists.newArrayList();

            for (XmlElement subElement : source.getSubElements()) {
                if (!target.hasSubElement(subElement.getId())) {
                    addedSubElements.add(subElement);
                }
            }

            for (XmlElement subElement : target.getSubElements()) {
                if (!source.hasSubElement(subElement.getId())) {
                    removedSubElements.add(subElement);
                } else {
                    XmlElement newSubElement = source.requireSubElement(subElement.getId());
                    castElement(subElement, newSubElement);
                }
            }

            ElementChangingEvent elementChangingEvent = new ElementChangingEvent(target, changedAttributes, changedTextContent);

            if (!elementChangingEvent.isEmpty()) {
                target.addChange(elementChangingEvent);
            }
            if (!addedSubElements.isEmpty()) {
                target.addSubElements(addedSubElements);
            }
            if (!removedSubElements.isEmpty()) {
                target.removeSubElements(removedSubElements);
            }
        } else {
            throw new PersistException("Could not cast element. Incompatible structures.");
        }
    }

}

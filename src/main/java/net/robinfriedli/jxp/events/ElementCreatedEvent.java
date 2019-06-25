package net.robinfriedli.jxp.events;

import java.util.HashMap;
import java.util.Map;

import net.robinfriedli.jxp.api.XmlAttribute;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.PersistException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ElementCreatedEvent extends Event {

    private final String textContent;
    private final Map<String, String> attributeMap;
    private XmlElement newParent;

    public ElementCreatedEvent(XmlElement element) {
        this(element, null);
    }

    public ElementCreatedEvent(XmlElement element, XmlElement newParent) {
        super(element);
        this.newParent = newParent;
        textContent = element.getTextContent();
        attributeMap = new HashMap<>();

        for (XmlAttribute attribute : element.getAttributes()) {
            attributeMap.put(attribute.getAttributeName(), attribute.getValue());
        }

        element.setState(XmlElement.State.PERSISTING);
    }

    @Override
    public void apply() {
        if (isApplied()) {
            throw new PersistException("Change has already been applied");
        } else {
            XmlElement source = getSource();

            if (newParent != null) {
                source.setParent(newParent);
            }

            if (!source.isSubElement()) {
                source.getContext().addElement(source);
            } else if (newParent != null) {
                newParent.getSubElements().add(source);
            }
            setApplied(true);
            getSource().getContext().getBackend().fireElementCreating(this);
        }
    }

    @Override
    public void revert() {
        if (isApplied()) {
            XmlElement source = getSource();
            if (!source.isSubElement()) {
                source.getContext().removeElement(source);
            } else {
                XmlElement parent = source.getParent();
                parent.getSubElements().remove(source);
                source.removeParent();
            }
        }

        if (isCommitted()) {
            getSource().phantomize();
        }
    }

    @Override
    public void commit() {
        XmlElement source = getSource();
        Document document = source.getContext().getDocument();

        Element elem = document.createElement(source.getTagName());

        for (String attributeName : attributeMap.keySet()) {
            elem.setAttribute(attributeName, attributeMap.get(attributeName));
        }

        if (source.hasTextContent()) {
            elem.setTextContent(textContent);
        }

        Element parentElem;
        if (source.isSubElement()) {
            parentElem = source.getParent().requireElement();
        } else {
            parentElem = document.getDocumentElement();
        }

        parentElem.appendChild(elem);
        source.setElement(elem);

        if (!source.hasChanges()) {
            source.setState(XmlElement.State.CLEAN);
        } else {
            source.setState(XmlElement.State.TOUCHED);
        }

        setCommitted(true);
    }

    public XmlElement getNewParent() {
        return newParent;
    }
}

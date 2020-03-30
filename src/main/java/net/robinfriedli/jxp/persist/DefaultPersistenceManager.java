package net.robinfriedli.jxp.persist;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.StaticXmlElementFactory;
import net.robinfriedli.jxp.api.XmlAttribute;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.events.AttributeChangingEvent;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.TextContentChangingEvent;
import net.robinfriedli.jxp.exceptions.CommitException;
import net.robinfriedli.jxp.exceptions.PersistException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Deprecated as of v1.1 and replaced by the static StaticXmlElementFactory and StaticXmlParser classes
 */
@SuppressWarnings("Duplicates")
@Deprecated
public class DefaultPersistenceManager {

    @Deprecated
    public List<XmlElement> getAllElements(Context context) {
        List<XmlElement> xmlElements = Lists.newArrayList();
        List<Element> allTopLevelElements = getAllTopLevelElements(context.getDocument());

        for (Element topElement : allTopLevelElements) {
            xmlElements.add(instantiateXmlElement(topElement, context));
        }

        return xmlElements;
    }

    @Deprecated
    public XmlElement instantiateXmlElement(Element element, Context context) {
        return StaticXmlElementFactory.instantiatePersistentXmlElement(element, context);
    }

    @Deprecated
    public void commitElementChanges(ElementChangingEvent event) throws CommitException {
        XmlElement element = event.getSource();
        List<AttributeChangingEvent> attributeChanges = event.getChangedAttributes();

        if (attributeChanges != null && !attributeChanges.isEmpty()) {
            for (AttributeChangingEvent attributeChange : attributeChanges) {
                setAttribute(element, attributeChange);
                attributeChange.setCommitted(true);
            }
        }
        if (event.textContentChanged()) {
            //noinspection ConstantConditions
            setTextContent(event.getChangedTextContent());
        }
    }

    @Deprecated
    public void setAttribute(XmlElement xmlElement, AttributeChangingEvent change) throws CommitException {
        Element element = requireElement(xmlElement);
        element.setAttribute(change.getAttribute().getAttributeName(), change.getNewValue());
    }

    @Deprecated
    public void setTextContent(TextContentChangingEvent changedTextContent) throws CommitException {
        Element element = requireElement(changedTextContent.getSource());
        element.setTextContent(changedTextContent.getNewValue());
    }

    @Deprecated
    public void writeToFile(Context context) throws CommitException {
        StaticXmlParser.writeToFile(context);
    }

    @Deprecated
    public Document parseDocument(File xml) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(xml);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new PersistException("Exception while parsing document", e);
        }
    }

    @Deprecated
    public void persistElement(Document document, XmlElement element) {
        persistElement(document, element, null);
    }

    @Deprecated
    public void persistElement(Document doc, XmlElement element, @Nullable Element superElem) {
        Element elem = doc.createElement(element.getTagName());

        List<XmlAttribute> attributes = element.getAttributes();
        for (XmlAttribute attribute : attributes) {
            elem.setAttribute(attribute.getAttributeName(), attribute.getValue());
        }

        if (element.hasTextContent()) {
            elem.setTextContent(element.getTextContent());
        }

        if (superElem != null) {
            superElem.appendChild(elem);
        } else {
            Element rootElem = doc.getDocumentElement();
            rootElem.appendChild(elem);
        }

        element.setElement(elem);
        if (!element.hasChanges()) {
            element.setState(XmlElement.State.CLEAN);
        } else {
            element.setState(XmlElement.State.TOUCHED);
        }
    }

    @Deprecated
    public void castElement(XmlElement target, XmlElement source) {
        if (target.matchesStructure(source)) {
            List<AttributeChangingEvent> changedAttributes = Lists.newArrayList();
            TextContentChangingEvent changedTextContent = null;

            for (XmlAttribute attribute : source.getAttributes()) {
                XmlAttribute existingAttribute = target.getAttribute(attribute.getAttributeName());
                if (!attribute.getValue().equals(existingAttribute.getValue())) {
                    changedAttributes.add(new AttributeChangingEvent(existingAttribute, attribute.getValue()));
                }
            }

            if (!target.getTextContent().equals(source.getTextContent())) {
                changedTextContent = new TextContentChangingEvent(target, target.getTextContent(), source.getTextContent());
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

            for (AttributeChangingEvent changedAttribute : changedAttributes) {
                target.addChange(changedAttribute);
            }
            if (changedTextContent != null) {
                target.addChange(changedTextContent);
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

    public List<Element> getAllTopLevelElements(Document doc) {
        Element documentElement = doc.getDocumentElement();

        if (documentElement == null) {
            throw new PersistException("Invalid document! No root element defined.");
        }

        return getChildren(documentElement);
    }

    @Deprecated
    public List<Element> getChildren(Element parent) {
        NodeList childNodes = parent.getChildNodes();
        List<Element> elements = Lists.newArrayList();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node instanceof Element) {
                elements.add((Element) node);
            }
        }

        return elements;
    }

    @Deprecated
    private Element requireElement(XmlElement xmlElement) throws CommitException {
        try {
            return xmlElement.requireElement();
        } catch (IllegalStateException e) {
            throw new CommitException(e);
        }
    }

}

package net.robinfriedli.jxp.persist;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.BaseXmlElement;
import net.robinfriedli.jxp.api.XmlAttribute;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.events.AttributeChangingEvent;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.ValueChangingEvent;
import net.robinfriedli.jxp.exceptions.CommitException;
import net.robinfriedli.jxp.exceptions.PersistException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class DefaultPersistenceManager {

    public List<XmlElement> getAllElements(Context context) {
        List<XmlElement> xmlElements = Lists.newArrayList();
        List<Element> allTopLevelElements = getAllTopLevelElements(context.getDocument());

        for (Element topElement : allTopLevelElements) {
            xmlElements.add(instantiateXmlElement(topElement, context));
        }

        return xmlElements;
    }

    public XmlElement instantiateXmlElement(Element element, Context context) {
        Map<String, Class<? extends XmlElement>> instantiationContributions = context.getBackend().getInstantiationContributions();
        List<Element> subElements = getChildren(element);
        List<XmlElement> instantiatedSubElems = Lists.newArrayList();

        for (Element subElement : subElements) {
            instantiatedSubElems.add(instantiateXmlElement(subElement, context));
        }

        Class<? extends XmlElement> xmlClass = instantiationContributions.get(element.getTagName());
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
            setTextContent(event.getChangedTextContent());
        }
    }

    public void setAttribute(XmlElement xmlElement, AttributeChangingEvent change) throws CommitException {
        Element element = requireElement(xmlElement);
        element.setAttribute(change.getAttribute().getAttributeName(), change.getNewValue());
    }

    public void setTextContent(ValueChangingEvent<String> changedTextContent) throws CommitException {
        Element element = requireElement(changedTextContent.getSource());
        element.setTextContent(changedTextContent.getNewValue());
    }

    public void writeToFile(Context context) throws CommitException {
        if (!context.isPersistent()) {
            throw new CommitException("Context is not persistent. Cannot write to file");
        }

        Document doc = context.getDocument();
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(context.getFile());

            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new CommitException("Exception while writing to file", e);
        }
    }

    public Document parseDocument(File xml) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(xml);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new PersistException("Exception while parsing document", e);
        }
    }

    public void persistElement(Document document, XmlElement element) {
        persistElement(document, element, null);
    }

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

        if (element.hasSubElements()) {
            for (XmlElement subElement : element.getSubElements()) {
                persistElement(doc, subElement, elem);
            }
        }

        element.setElement(elem);
        if (!element.hasChanges()) {
            element.setState(XmlElement.State.CLEAN);
        } else {
            element.setState(XmlElement.State.TOUCHED);
        }
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

    public List<Element> getAllTopLevelElements(Document doc) {
        Element documentElement = doc.getDocumentElement();

        if (documentElement == null) {
            throw new PersistException("Invalid document! No root element defined.");
        }

        return getChildren(documentElement);
    }

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

    private Element requireElement(XmlElement xmlElement) throws CommitException {
        try {
            return xmlElement.requireElement();
        } catch (IllegalStateException e) {
            throw new CommitException(e);
        }
    }

}

package net.robinfriedli.jxp.persist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.XmlAttribute;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.events.AttributeChangingEvent;
import net.robinfriedli.jxp.events.ValueChangingEvent;
import net.robinfriedli.jxp.exceptions.CommitException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Deprecated
public class XmlPersister {

    private final Context context;
    private Document doc;

    public XmlPersister(Context context) {
        this.context = context;
        this.doc = parseDocument();
    }

    public List<Element> getElements(String tagName) {
        return nodeListToElementList(doc.getElementsByTagName(tagName));
    }

    /**
     * Persist XmlElement to an XmlFile
     *
     * @param element to persist
     */
    public void persistElement(XmlElement element) {
        persistElement(element, null);
    }

    public void removeAll(String tagName) {
        Element rootElem = doc.getDocumentElement();
        nodeListToElementList(doc.getElementsByTagName(tagName)).forEach(rootElem::removeChild);
    }

    public void remove(XmlElement element) throws CommitException {
        Element elementToRemove = requireElement(element);
        elementToRemove.getParentNode().removeChild(elementToRemove);
    }

    public void remove(List<XmlElement> elements) throws CommitException {
        for (XmlElement element : elements) {
            remove(element);
        }
    }

    public void setAttribute(AttributeChangingEvent attributeChange) throws CommitException {
        Element element = requireElement(attributeChange.getSource());
        element.setAttribute(attributeChange.getAttribute().getAttributeName(), attributeChange.getNewValue());
    }

    public void setAttributes(XmlElement xmlElement, AttributeChangingEvent... attributeChanges) throws CommitException {
        setAttributes(xmlElement, Arrays.asList(attributeChanges));
    }

    public void setAttributes(XmlElement xmlElement, List<AttributeChangingEvent> attributeChanges) throws CommitException {
        Element element = requireElement(xmlElement);
        for (AttributeChangingEvent attributeChange : attributeChanges) {
            if (xmlElement == attributeChange.getSource()) {
                element.setAttribute(attributeChange.getAttribute().getAttributeName(), attributeChange.getNewValue());
            } else {
                throw new CommitException("Could not set " + attributeChange.toString() + " on " + xmlElement.toString()
                    + ". Element is not attribute's parent");
            }
        }
    }

    public void setTextContent(ValueChangingEvent<String> changedTextContent) throws CommitException {
        Element element = requireElement(changedTextContent.getSource());
        element.setTextContent(changedTextContent.getNewValue());
    }

    public void addSubElements(XmlElement superElem, List<XmlElement> subElems) throws CommitException {
        Element element = requireElement(superElem);
        for (XmlElement subElem : subElems) {
            persistElement(subElem, element);
        }
    }

    public boolean isSubElementOf(XmlElement subElem, XmlElement superElem) {
        Element element = superElem.requireElement();
        List<Element> subElements = nodeListToElementList(element.getElementsByTagName(subElem.getTagName()));
        return subElements.contains(subElem.requireElement());
    }

    public void persistElement(XmlElement element, @Nullable Element superElem) {
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
                persistElement(subElement, elem);
            }
        }

        element.setElement(elem);
        if (!element.hasChanges()) {
            element.setState(XmlElement.State.CLEAN);
        } else {
            element.setState(XmlElement.State.TOUCHED);
        }
    }

    public List<Element> find(String tagName, String attributeName, String attributeValue) {
        List<Element> elements = nodeListToElementList(doc.getElementsByTagName(tagName));
        return elements.stream().filter(elem -> elem.getAttribute(attributeName).equals(attributeValue)).collect(Collectors.toList());
    }

    public List<Element> find(String tagName, String textContent) {
        List<Element> elements = nodeListToElementList(doc.getElementsByTagName(tagName));
        return elements.stream().filter(elem -> elem.getTextContent().equals(textContent)).collect(Collectors.toList());
    }

    public List<Element> find(String tagName, String textContent, XmlElement parent) {
        Element element = parent.requireElement();
        List<Element> elements = nodeListToElementList(element.getElementsByTagName(tagName));
        return elements.stream().filter(elem -> elem.getTextContent().equals(textContent)).collect(Collectors.toList());
    }

    public List<Element> find(String tagName, String attributeName, String attributeValue, XmlElement parent) {
        Element element = parent.requireElement();
        List<Element> elements = nodeListToElementList(element.getElementsByTagName(tagName));
        return elements.stream().filter(elem -> elem.getAttribute(attributeName).equals(attributeValue)).collect(Collectors.toList());
    }

    public void writeToFile() throws CommitException {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(getFile());

            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    public Document getDocument() {
        return doc;
    }

    public void reloadDocument() {
        doc = parseDocument();
    }

    public boolean deleteFile(Context.BindableContext bindableContext) {
        File file = new File(bindableContext.getPath());
        if (file.exists()) {
            return file.delete();
        }

        return false;
    }

    private Element requireElement(XmlElement xmlElement) throws CommitException {
        try {
            return xmlElement.requireElement();
        } catch (IllegalStateException e) {
            throw new CommitException(e);
        }
    }

    private File getFile() throws CommitException {
        String path = context.getPath();

        File file = new File(path);
        if (file.exists()) {
            return file;
        }

        throw new CommitException("File loading failed");
    }

    public List<Element> getAllTopLevelElements() {
        return getChildren(doc.getDocumentElement());
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

    public Document parseDocument() {
        try {
            File xml = getFile();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(xml);
        } catch (Exception e) {
            throw new IllegalStateException("Exception while parsing document", e);
        }
    }

    private List<Element> nodeListToElementList(NodeList nodeList) {
        List<Element> elements = Lists.newArrayList();
        for (int i = 0; i < nodeList.getLength(); i++) {
            elements.add((Element) nodeList.item(i));
        }

        return elements;
    }

}

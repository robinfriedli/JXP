package net.robinfriedli.jxp.persist;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import net.robinfriedli.jxp.api.JxpBackend;
import net.robinfriedli.jxp.api.StaticXmlElementFactory;
import net.robinfriedli.jxp.api.XmlElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Context implementation that stores all {@link XmlElement} instances for the entirety of its lifespan and never has
 * to re-instantiate its XmlElements or re-parse its DOM document. This is ideal when performance is the main focus
 * and memory is not too limited.
 */
public class CachedContext extends AbstractContext {

    private List<XmlElement> elements;

    public CachedContext(JxpBackend backend, Document document, Logger logger) {
        super(backend, document, logger);
        this.elements = StaticXmlElementFactory.instantiateAllElements(this);
    }

    public CachedContext(JxpBackend backend, File file, Logger logger) {
        super(backend, file, logger);
        this.elements = StaticXmlElementFactory.instantiateAllElements(this);
    }

    @Override
    public void reload() {
        super.reload();
        elements = StaticXmlElementFactory.instantiateAllElements(this);
    }

    @Override
    public List<XmlElement> getElements() {
        return elements;
    }

    @Override
    public void addElement(XmlElement element) {
        elements.add(element);
    }

    @Override
    public void addElements(List<XmlElement> elements) {
        this.elements.addAll(elements);
    }

    @Override
    public void removeElement(XmlElement element) {
        elements.remove(element);
    }

    @Override
    public void removeElements(List<XmlElement> elements) {
        this.elements.removeAll(elements);
    }

    @Override
    protected List<XmlElement> handleXPathResults(List<Element> results) {
        return getElementsRecursive().stream().filter(element -> element.isPersisted() && results.contains(element.getElement())).collect(Collectors.toList());
    }

    @Override
    protected Context instantiate(JxpBackend jxpBackend, Document document, Logger logger) {
        return new CachedContext(jxpBackend, document, logger);
    }

    @Override
    protected Context instantiate(JxpBackend jxpBackend, File document, Logger logger) {
        return new CachedContext(jxpBackend, document, logger);
    }

}
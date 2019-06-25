package net.robinfriedli.jxp.persist;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.JxpBackend;
import net.robinfriedli.jxp.api.StaticXmlElementFactory;
import net.robinfriedli.jxp.api.XmlElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class LazyContext extends AbstractContext {

    private final List<XmlElement> conceivedElements;
    private final List<XmlElement> deletingElements;

    public LazyContext(JxpBackend backend, Document document, Logger logger) {
        super(backend, document, logger);
        conceivedElements = Lists.newArrayList();
        deletingElements = Lists.newArrayList();
    }

    public LazyContext(JxpBackend backend, File file, Logger logger) {
        super(backend, file, logger);
        conceivedElements = Lists.newArrayList();
        deletingElements = Lists.newArrayList();
    }

    @Override
    public List<XmlElement> getElements() {
        List<XmlElement> persistedElements = StaticXmlElementFactory.instantiateAllElements(this);
        persistedElements.addAll(conceivedElements);
        persistedElements.removeAll(deletingElements);
        return persistedElements;
    }

    @Override
    public void addElement(XmlElement element) {
        if (element.getState() == XmlElement.State.CONCEPTION) {
            conceivedElements.add(element);
        }
    }

    @Override
    public void removeElement(XmlElement element) {
        if (element.getState() == XmlElement.State.DELETION) {
            deletingElements.add(element);
        }
    }

    void clear() {
        conceivedElements.clear();
        deletingElements.clear();
    }

    @Override
    protected List<XmlElement> handleXPathResults(List<Element> results) {
        List<XmlElement> elementInstances = Lists.newArrayList();

        for (Element docElement : results) {
            elementInstances.add(StaticXmlElementFactory.instantiatePersistentXmlElement(docElement, this));
        }

        return elementInstances;
    }

}

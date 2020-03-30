package net.robinfriedli.jxp.persist;

import java.io.File;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.JxpBackend;
import net.robinfriedli.jxp.api.StaticXmlElementFactory;
import net.robinfriedli.jxp.api.UninitializedParent;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.queries.xpath.XQueryBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Context implementation with focus on low memory usage. This context type only stores XmlElements in state CONCEPTION
 * or DELETION during a Transaction until the next flush, meaning calls to #getElements or #query result in the XmlElement
 * instances being instantiated. {@link Context#xPathQuery(String)} can be used in combination with this context type
 * to only instantiate XmlElements based on the query result (see {@link XQueryBuilder} to build XPath queries).
 * In combination with {@link Context#invokeSequential(int, Runnable)} this context allows for low memory usage when
 * executing massive transactions.
 */
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
            XmlElement xmlElement = StaticXmlElementFactory.instantiatePersistentXmlElement(docElement, this);
            Node parentNode = docElement.getParentNode();
            if (parentNode instanceof Element && !Objects.equals(parentNode, parentNode.getOwnerDocument().getDocumentElement())) {
                xmlElement.setParent(new UninitializedParent(this, (Element) parentNode, xmlElement));
            }

            elementInstances.add(xmlElement);
        }

        return elementInstances;
    }

    @Override
    protected Context instantiate(JxpBackend jxpBackend, Document document, Logger logger) {
        return new LazyContext(jxpBackend, document, logger);
    }

    @Override
    protected Context instantiate(JxpBackend jxpBackend, File document, Logger logger) {
        return new LazyContext(jxpBackend, document, logger);
    }

}

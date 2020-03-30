package net.robinfriedli.jxp.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.queries.ResultStream;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class UninitializedParent implements XmlElement {

    private final Context context;
    private final Element element;
    private final XmlElement child;

    private XmlElement initialized;

    public UninitializedParent(Context context, Element element, XmlElement child) {
        this.context = context;
        this.element = element;
        this.child = child;
    }

    public XmlElement getChild() {
        return child;
    }

    private XmlElement initialize() {
        XmlElement instantiatedParent = StaticXmlElementFactory.instantiatePersistentXmlElement(element, context, child.requireElement());
        child.setParent(instantiatedParent);
        instantiatedParent.getSubElements().add(child);
        Node parentNode = element.getParentNode();

        if (parentNode instanceof Element && !Objects.equals(parentNode, parentNode.getOwnerDocument().getDocumentElement())) {
            instantiatedParent.setParent(new UninitializedParent(context, (Element) parentNode, instantiatedParent));
        }

        initialized = instantiatedParent;
        return instantiatedParent;
    }

    public XmlElement getInitialized() {
        if (initialized == null) {
            return initialize();
        }

        return initialized;
    }

    @Override
    public void persist(Context context) {
        getInitialized().persist(context);
    }

    @Override
    public void persist(Context context, XmlElement newParent) {
        getInitialized().persist(context, newParent);
    }

    @Override
    public boolean isDetached() {
        return getInitialized().isDetached();
    }

    @Override
    public XmlElement copy(boolean copySubElements, boolean instantiateContributedClass) {
        return getInitialized().copy(copySubElements, instantiateContributedClass);
    }

    @Override
    public void phantomize() {
        getInitialized().phantomize();
    }

    @Nullable
    @Override
    public Element getElement() {
        if (initialized == null) {
            return element;
        }

        return getInitialized().getElement();
    }

    @Override
    public void setElement(Element element) {
        getInitialized().setElement(element);
    }

    @Override
    public Element requireElement() throws IllegalStateException {
        if (initialized == null) {
            return element;
        }

        return getInitialized().requireElement();
    }

    @Override
    public void removeParent() {
        getInitialized().removeParent();
    }

    @Override
    public XmlElement getParent() {
        return getInitialized().getParent();
    }

    @Override
    public void setParent(XmlElement parent) {
        getInitialized().getParent();
    }

    @Override
    public boolean isSubElement() {
        return getInitialized().isSubElement();
    }

    @Override
    public boolean isPersisted() {
        if (initialized == null) {
            return true;
        }
        return getInitialized().isPersisted();
    }

    @Override
    public Context getContext() {
        if (initialized == null) {
            return context;
        }
        return getInitialized().getContext();
    }

    @Override
    public String getTagName() {
        return getInitialized().getTagName();
    }

    @Override
    public List<XmlAttribute> getAttributes() {
        return getInitialized().getAttributes();
    }

    @Override
    public Map<String, XmlAttribute> getAttributeMap() {
        return getInitialized().getAttributeMap();
    }

    @Override
    public XmlAttribute getAttribute(String attributeName) {
        return getInitialized().getAttribute(attributeName);
    }

    @Override
    public void removeAttribute(String attributeName) {
        getInitialized().removeAttribute(attributeName);
    }

    @Override
    public void setAttribute(String attribute, Object value) {
        getInitialized().setAttribute(attribute, value);
    }

    @Override
    public boolean hasAttribute(String attributeName) {
        return getInitialized().hasAttribute(attributeName);
    }

    @Override
    public void addSubElement(XmlElement element) {
        getInitialized().addSubElement(element);
    }

    @Override
    public void addSubElements(List<XmlElement> elements) {
        getInitialized().addSubElements(elements);
    }

    @Override
    public void addSubElements(XmlElement... elements) {
        getInitialized().addSubElements(elements);
    }

    @Override
    public void removeSubElement(XmlElement element) {
        getInitialized().removeSubElement(element);
    }

    @Override
    public void removeSubElements(List<XmlElement> elements) {
        getInitialized().removeSubElements(elements);
    }

    @Override
    public void removeSubElements(XmlElement... elements) {
        getInitialized().removeSubElements(elements);
    }

    @Override
    public List<XmlElement> getSubElements() {
        return getInitialized().getSubElements();
    }

    @Override
    public List<XmlElement> getSubElementsRecursive() {
        return getInitialized().getSubElementsRecursive();
    }

    @Override
    public <E extends XmlElement> List<E> getSubElementsWithType(Class<E> type) {
        return getInitialized().getSubElementsWithType(type);
    }

    @Nullable
    @Override
    public XmlElement getSubElement(String id) {
        return getInitialized().getSubElement(id);
    }

    @Nullable
    @Override
    public <E extends XmlElement> E getSubElement(String id, Class<E> type) {
        return getInitialized().getSubElement(id, type);
    }

    @Override
    public XmlElement requireSubElement(String id) throws IllegalStateException {
        return getInitialized().requireSubElement(id);
    }

    @Override
    public <E extends XmlElement> E requireSubElement(String id, Class<E> type) throws IllegalStateException {
        return getInitialized().requireSubElement(id, type);
    }

    @Override
    public boolean hasSubElements() {
        // before initialization we know this to be true because it is at least the parent of the child of this
        // UninitializedParent but this method should remain accurate in case the subelement is removed after initialization
        if (initialized == null) {
            return true;
        }
        return getInitialized().hasSubElements();
    }

    @Override
    public boolean hasSubElement(String id) {
        if (initialized == null && Objects.equals(id, child.getId())) {
            return true;
        }
        return getInitialized().hasSubElement(id);
    }

    @Override
    public String getTextContent() {
        return getInitialized().getTextContent();
    }

    @Override
    public void setTextContent(String textContent) {
        getInitialized().setTextContent(textContent);
    }

    @Override
    public boolean hasTextContent() {
        return getInitialized().hasTextContent();
    }

    @Nullable
    @Override
    public String getId() {
        return getInitialized().getId();
    }

    @Override
    @Deprecated
    public boolean checkDuplicates(XmlElement elementToCheck) {
        return getInitialized().checkDuplicates(elementToCheck);
    }

    @Override
    public boolean matchesStructure(XmlElement elementToCompare) {
        return getInitialized().matchesStructure(elementToCompare);
    }

    @Override
    public void delete() {
        getInitialized().delete();
    }

    @Override
    public void addChange(ElementChangingEvent change) {
        getInitialized().addChange(change);
    }

    @Override
    public void removeChange(ElementChangingEvent change) {
        getInitialized().removeChange(change);
    }

    @Override
    public void applyChange(ElementChangingEvent change) throws UnsupportedOperationException, PersistException {
        getInitialized().applyChange(change);
    }

    @Override
    public void revertChange(ElementChangingEvent change) throws UnsupportedOperationException, PersistException {
        getInitialized().revertChange(change);
    }

    @Override
    public List<ElementChangingEvent> getChanges() {
        return getInitialized().getChanges();
    }

    @Override
    public boolean hasChanges() {
        return getInitialized().hasChanges();
    }

    @Override
    public boolean attributeChanged(String attributeName) {
        return getInitialized().attributeChanged(attributeName);
    }

    @Override
    public boolean textContentChanged() {
        return getInitialized().textContentChanged();
    }

    @Override
    public void lock() {
        getInitialized().lock();
    }

    @Override
    public void unlock() {
        getInitialized().unlock();
    }

    @Override
    public boolean isLocked() {
        return getInitialized().isLocked();
    }

    @Override
    public State getState() {
        return getInitialized().getState();
    }

    @Override
    public void setState(State state) {
        getInitialized().setState(state);
    }

    @Override
    public <E extends XmlElement> List<E> getInstancesOf(Class<E> c) {
        return getInitialized().getInstancesOf(c);
    }

    @Override
    public <E extends XmlElement> List<E> getInstancesOf(Class<E> c, Class<?>... ignoredSubClasses) {
        return getInitialized().getInstancesOf(c, ignoredSubClasses);
    }

    @Override
    public ResultStream<XmlElement> query(Predicate<XmlElement> condition) {
        return getInitialized().query(condition);
    }

    @Override
    public <E extends XmlElement> ResultStream<E> query(Predicate<XmlElement> condition, Class<E> type) {
        return getInitialized().query(condition, type);
    }
}

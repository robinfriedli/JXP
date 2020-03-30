package net.robinfriedli.jxp.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import net.robinfriedli.jxp.events.AttributeChangingEvent;
import net.robinfriedli.jxp.events.AttributeDeletedEvent;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.ElementCreatedEvent;
import net.robinfriedli.jxp.events.ElementDeletingEvent;
import net.robinfriedli.jxp.events.RecursiveDeletingEvent;
import net.robinfriedli.jxp.events.TextContentChangingEvent;
import net.robinfriedli.jxp.events.VirtualEvent;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.ElementUtils;
import net.robinfriedli.jxp.persist.Transaction;
import net.robinfriedli.jxp.queries.Query;
import net.robinfriedli.jxp.queries.ResultStream;
import org.w3c.dom.Element;

/**
 * Abstract class to extend for classes you wish to able to persist to an XML file.
 * Classes that extend this class can be persisted via the {@link net.robinfriedli.jxp.persist.Context#invoke(Runnable)}
 * method using {@link #persist(Context)}
 */
public abstract class AbstractXmlElement implements XmlElement {

    private final String tagName;

    private final Map<String, XmlAttribute> attributes;

    private final List<XmlElement> subElements;

    private Context context;

    private Element element;

    private XmlElement parent;

    private String textContent;

    private State state;

    private List<ElementChangingEvent> changes = Lists.newArrayList();

    private boolean locked = false;

    // creating new element

    public AbstractXmlElement(String tagName) {
        this(tagName, new HashMap<>());
    }

    public AbstractXmlElement(String tagName, Map<String, ?> attributeMap) {
        this(tagName, attributeMap, Lists.newArrayList(), "");
    }

    public AbstractXmlElement(String tagName, Map<String, ?> attributeMap, String textContent) {
        this(tagName, attributeMap, Lists.newArrayList(), textContent);
    }

    public AbstractXmlElement(String tagName, Map<String, ?> attributeMap, List<XmlElement> subElements) {
        this(tagName, attributeMap, subElements, "");
    }

    public AbstractXmlElement(String tagName,
                              Map<String, ?> attributeMap,
                              List<XmlElement> subElements,
                              String textContent) {
        this.tagName = tagName;
        this.subElements = subElements;
        this.textContent = textContent;
        this.state = State.CONCEPTION;

        Map<String, XmlAttribute> attributes = new HashMap<>();
        for (String attributeName : attributeMap.keySet()) {
            attributes.put(attributeName, new XmlAttribute(this, attributeName, attributeMap.get(attributeName)));
        }
        this.attributes = attributes;

        subElements.forEach(sub -> sub.setParent(this));
    }

    // loading from file

    public AbstractXmlElement(Element element, Context context) {
        this(element, Lists.newArrayList(), context);
    }

    public AbstractXmlElement(Element element, List<XmlElement> subElements, Context context) {
        this.element = element;
        this.tagName = element.getTagName();
        this.subElements = subElements;
        this.textContent = ElementUtils.getTextContent(element);
        this.context = context;
        this.state = State.CLEAN;

        Map<String, XmlAttribute> attributes = new HashMap<>();
        Map<String, String> attributeMap = ElementUtils.getAttributes(element);
        for (String attributeName : attributeMap.keySet()) {
            attributes.put(attributeName, new XmlAttribute(this, attributeName, attributeMap.get(attributeName)));
        }
        this.attributes = attributes;

        subElements.forEach(sub -> sub.setParent(this));
    }

    @Override
    @Nullable
    public abstract String getId();

    @Override
    public void persist(Context context) {
        persist(context, null);
    }

    @Override
    public void persist(Context context, XmlElement newParent) {
        this.context = context;
        List<XmlElement> subElementsToPersist = Lists.newArrayList(getSubElements());

        Transaction transaction = context.getActiveTransaction();
        // in case of an InstantApplyTx this might result in further subElements being added by listeners, so pre existing
        // subElements need to be collected before this happens or #persist would be called twice for added subElements,
        // resulting in failure
        transaction.addChange(new ElementCreatedEvent(this, newParent));
        for (XmlElement xmlElement : subElementsToPersist) {
            xmlElement.persist(context);
        }
    }

    @Override
    public boolean isDetached() {
        return context == null;
    }

    @Override
    public XmlElement copy(boolean copySubElements, boolean instantiateContributedClass) {
        List<XmlElement> subElements = Lists.newArrayList();
        Map<String, String> attributeMap = new HashMap<>();

        if (copySubElements && hasSubElements()) {
            for (XmlElement subElement : Lists.newArrayList(this.subElements)) {
                subElements.add(subElement.copy(true, instantiateContributedClass));
            }
        }

        for (XmlAttribute attribute : Lists.newArrayList(attributes.values())) {
            attributeMap.put(attribute.getAttributeName(), attribute.getValue());
        }

        if (instantiateContributedClass) {
            return StaticXmlElementFactory.instantiate(tagName, attributeMap, subElements, textContent);
        }

        return new BaseXmlElement(tagName, attributeMap, subElements, textContent);
    }

    @Override
    public void phantomize() {
        if (element != null) {
            ElementUtils.destroy(element);
            element = null;
        }

        setState(State.PHANTOM);
    }

    @Override
    @Nullable
    public Element getElement() {
        return element;
    }

    @Override
    public void setElement(Element element) {
        if (this.element != null) {
            throw new IllegalStateException(toString() + " is already persisted as an element. Destroy the old element first.");
        }

        this.element = element;
    }

    @Override
    public Element requireElement() throws IllegalStateException {
        if (element == null) {
            throw new IllegalStateException(toString() + " has no Element");
        }

        return element;
    }

    @Override
    public void removeParent() {
        parent = null;
    }

    @Override
    public XmlElement getParent() {
        return parent;
    }

    @Override
    public void setParent(XmlElement parent) {
        if (this.parent != null) {
            if (this.parent instanceof UninitializedParent) {
                UninitializedParent uninitializedParent = (UninitializedParent) this.parent;
                if (uninitializedParent.getChild() != this) {
                    throw new IllegalArgumentException("The provided UninitializedParent is a parent of a different element");
                }
            } else {
                throw new IllegalStateException(toString() + " already has a parent. Remove it from the old parent first.");
            }
        }

        this.parent = parent;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public boolean isSubElement() {
        return parent != null;
    }

    @Override
    public boolean isPersisted() {
        return element != null;
    }

    @Deprecated
    @Override
    public boolean checkDuplicates(XmlElement element) {
        if (!this.getTagName().equals(element.getTagName())) return false;
        if (this.getId() == null || element.getId() == null) return false;
        return this.getId().equals(element.getId());
    }

    @Override
    public String getTagName() {
        return tagName;
    }

    @Override
    public List<XmlAttribute> getAttributes() {
        return Lists.newArrayList(attributes.values());
    }

    @Override
    public Map<String, XmlAttribute> getAttributeMap() {
        return attributes;
    }

    @Override
    public XmlAttribute getAttribute(String attributeName) {
        List<XmlAttribute> foundAttributes = Lists.newArrayList(attributes.values()).stream()
            .filter(a -> a.getAttributeName().equals(attributeName))
            .collect(Collectors.toList());

        if (foundAttributes.size() == 1) {
            return foundAttributes.get(0);
        } else if (foundAttributes.size() > 1) {
            throw new IllegalStateException("Duplicate attribute: " + attributeName + " on element " + toString());
        } else {
            return new XmlAttribute.Provisional(this, attributeName);
        }
    }

    @Override
    public void removeAttribute(String attributeName) {
        getAttribute(attributeName).remove();
    }

    @Override
    public void setAttribute(String attribute, Object value) {
        XmlAttribute attributeToChange = getAttribute(attribute);
        attributeToChange.setValue(value);
    }

    @Override
    public boolean hasAttribute(String attributeName) {
        return getAttributes().stream().anyMatch(attribute -> attribute.getAttributeName().equals(attributeName));
    }

    @Override
    public void addSubElement(XmlElement element) {
        addSubElements(Collections.singletonList(element));
    }

    @Override
    public void addSubElements(List<XmlElement> elements) {
        if (isDetached()) {
            elements.forEach(elem -> elem.setParent(this));
            subElements.addAll(elements);
        } else {
            if (state.isPhysical()) {
                elements.forEach(elem -> elem.persist(context, this));
            } else {
                VirtualEvent.interceptTransaction(() -> elements.forEach(elem -> elem.persist(context, this)), context);
            }
        }
    }

    @Override
    public void addSubElements(XmlElement... elements) {
        addSubElements(Arrays.asList(elements));
    }

    @Override
    public void removeSubElement(XmlElement element) {
        removeSubElements(Collections.singletonList(element));
    }

    @Override
    public void removeSubElements(List<XmlElement> elements) {
        if (!subElements.containsAll(elements)) {
            throw new IllegalArgumentException("Not all provided elements are subElements of " + toString());
        }

        if (isDetached()) {
            elements.forEach(XmlElement::removeParent);
            subElements.removeAll(elements);
        } else {
            if (state.isPhysical()) {
                elements.forEach(XmlElement::delete);
            } else {
                VirtualEvent.interceptTransaction(() -> elements.forEach(XmlElement::delete), context);
            }
        }
    }

    @Override
    public void removeSubElements(XmlElement... elements) {
        removeSubElements(Arrays.asList(elements));
    }

    @Override
    public List<XmlElement> getSubElements() {
        return subElements;
    }

    @Override
    public List<XmlElement> getSubElementsRecursive() {
        List<XmlElement> elements = Lists.newArrayList();
        for (XmlElement element : Lists.newArrayList(getSubElements())) {
            recursiveAdd(elements, element);
        }
        return elements;
    }

    private void recursiveAdd(List<XmlElement> elements, XmlElement element) {
        elements.add(element);
        if (element.hasSubElements()) {
            for (XmlElement subElement : Lists.newArrayList(element.getSubElements())) {
                recursiveAdd(elements, subElement);
            }
        }
    }

    @Override
    public <E extends XmlElement> List<E> getSubElementsWithType(Class<E> type) {
        return getInstancesOf(type);
    }

    @Override
    public XmlElement getSubElement(String id) {
        return getSubElement(id, XmlElement.class);
    }

    @Override
    public <E extends XmlElement> E getSubElement(String id, Class<E> type) {
        List<E> foundSubElements = Lists.newArrayList(getSubElements()).stream()
            .filter(subElem -> type.isInstance(subElem) && subElem.getId() != null && subElem.getId().equals(id))
            .map(type::cast)
            .collect(Collectors.toList());

        if (foundSubElements.size() == 1) {
            return foundSubElements.get(0);
        } else if (foundSubElements.size() > 1) {
            throw new IllegalStateException("Multiple SubElements found for id " + id + ". Id's must be unique");
        } else {
            return null;
        }
    }

    @Override
    public XmlElement requireSubElement(String id) throws IllegalStateException {
        return requireSubElement(id, XmlElement.class);
    }

    @Override
    public <E extends XmlElement> E requireSubElement(String id, Class<E> type) throws IllegalStateException {
        E subElement = getSubElement(id, type);

        if (subElement != null) {
            return subElement;
        } else {
            throw new IllegalStateException("No SubElement found for id " + id);
        }
    }

    @Override
    public boolean hasSubElements() {
        return subElements != null && !subElements.isEmpty();
    }

    @Override
    public boolean hasSubElement(String id) {
        return Lists.newArrayList(getSubElements()).stream().anyMatch(subElem -> subElem.getId() != null && subElem.getId().equals(id));
    }

    @Override
    public String getTextContent() {
        return textContent;
    }

    @Override
    public void setTextContent(String textContent) {
        String oldValue = String.valueOf(getTextContent());
        addChange(new TextContentChangingEvent(this, oldValue, textContent));
    }

    @Override
    public boolean hasTextContent() {
        return !Strings.isNullOrEmpty(textContent);
    }

    @Override
    public boolean matchesStructure(XmlElement elementToCompare) {
        if (!elementToCompare.getTagName().equals(getTagName())) return false;
        return elementToCompare.getAttributes().stream().allMatch(attribute -> this.hasAttribute(attribute.getAttributeName()))
            && getAttributes().stream().allMatch(attribute -> elementToCompare.hasAttribute(attribute.getAttributeName()));
    }

    @Override
    public void delete() {
        if (!isLocked()) {
            if (isDetached()) {
                throw new PersistException("Cannot delete detached XmlElement. This XmlElement has never been saved in the first place.");
            } else {
                Transaction transaction = context.getActiveTransaction();

                State oldState = state;
                ElementDeletingEvent deletingEvent = new ElementDeletingEvent(this, oldState);
                if (hasSubElements()) {
                    RecursiveDeletingEvent.createRecursive(this, deletingEvent);
                }
                transaction.addChange(deletingEvent);
            }
        } else {
            throw new PersistException("Unable to delete. " + toString() + " is locked");
        }

    }

    @Override
    public void addChange(ElementChangingEvent change) {
        if (!isLocked()) {
            if (isDetached() || !state.isPhysical()) {
                applyChange(change);
            } else {
                Transaction transaction = context.getActiveTransaction();

                if (state.isPhysical()) {
                    if (state == State.CLEAN) {
                        setState(State.TOUCHED);
                    }
                    changes.add(change);
                    transaction.addChange(change);
                } else {
                    transaction.addChange(VirtualEvent.wrap(change));
                }
            }
        } else {
            throw new PersistException("Unable to add Change. " + toString() + " is locked.");
        }
    }

    @Override
    public void removeChange(ElementChangingEvent change) {
        changes.remove(change);

        if (!hasChanges() && state.isPhysical()) {
            setState(State.CLEAN);
        }
    }

    @Override
    public void applyChange(ElementChangingEvent change) throws UnsupportedOperationException, PersistException {
        applyChange(change, false);
    }

    @Override
    public void revertChange(ElementChangingEvent change) throws UnsupportedOperationException, PersistException {
        applyChange(change, true);
    }

    @Override
    public List<ElementChangingEvent> getChanges() {
        return changes;
    }

    @Override
    public boolean hasChanges() {
        return changes != null && !changes.isEmpty();
    }

    @Override
    public boolean attributeChanged(String attributeName) {
        return Lists.newArrayList(changes).stream().anyMatch(change -> change.attributeChanged(attributeName));
    }

    @Override
    public boolean textContentChanged() {
        return Lists.newArrayList(changes).stream().anyMatch(change -> change.getChangedTextContent() != null);
    }

    @Override
    public void lock() {
        locked = true;
    }

    @Override
    public void unlock() {
        locked = false;
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public <E extends XmlElement> List<E> getInstancesOf(Class<E> c) {
        return Lists.newArrayList(getSubElements()).stream()
            .filter(c::isInstance)
            .map(c::cast)
            .collect(Collectors.toList());
    }

    @Override
    public <E extends XmlElement> List<E> getInstancesOf(Class<E> c, Class<?>... ignoredSubClasses) {
        return Lists.newArrayList(getSubElements()).stream()
            .filter(elem -> c.isInstance(elem) && Arrays.stream(ignoredSubClasses).noneMatch(clazz -> clazz.isInstance(elem)))
            .map(c::cast)
            .collect(Collectors.toList());
    }

    @Override
    public ResultStream<XmlElement> query(Predicate<XmlElement> condition) {
        return Query.evaluate(condition).execute(getSubElementsRecursive());
    }

    @Override
    public <E extends XmlElement> ResultStream<E> query(Predicate<XmlElement> condition, Class<E> type) {
        return Query.evaluate(condition).execute(getSubElementsRecursive(), type);
    }

    @Override
    public String toString() {
        return "XmlElement<" + getTagName() + ">:" + getId();
    }

    private void applyChange(ElementChangingEvent change, boolean isRollback) {
        if (change.getSource() != this) {
            throw new UnsupportedOperationException("Source of ElementChangingEvent is not this XmlElement. Change can't be applied.");
        }

        AttributeChangingEvent attributeChange = change.getAttributeChange();
        if (attributeChange != null) {
            XmlAttribute attributeToChange = attributeChange.getAttribute();
            if (isRollback) {
                attributeToChange.revertChange(attributeChange);
            } else {
                attributeToChange.applyChange(attributeChange);
            }
        }

        AttributeDeletedEvent attributeDeletion = change.getAttributeDeletion();
        if (attributeDeletion != null) {
            XmlAttribute attribute = attributeDeletion.getAttribute();
            if (isRollback) {
                attributes.put(attribute.getAttributeName(), attribute);
            } else {
                attributes.remove(attribute.getAttributeName());
            }
        }

        if (change.getChangedTextContent() != null) {
            if (isRollback) {
                this.textContent = change.getChangedTextContent().getOldValue();
                if (change.isCommitted() && isPersisted()) {
                    requireElement().setTextContent(change.getChangedTextContent().getOldValue());
                }
            } else {
                this.textContent = change.getChangedTextContent().getNewValue();
            }
        }
    }

}

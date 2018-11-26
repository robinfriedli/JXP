package net.robinfriedli.jxp.api;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import net.robinfriedli.jxp.events.AttributeChangingEvent;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.ElementCreatedEvent;
import net.robinfriedli.jxp.events.ElementDeletingEvent;
import net.robinfriedli.jxp.events.RecursiveDeletingEvent;
import net.robinfriedli.jxp.events.ValueChangingEvent;
import net.robinfriedli.jxp.events.VirtualEvent;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.ElementUtils;
import net.robinfriedli.jxp.persist.Transaction;
import org.w3c.dom.Element;

/**
 * Abstract class to extend for classes you wish to able to persist to an XML file.
 * Classes that extend this class can be persisted via the {@link net.robinfriedli.jxp.persist.Context#invoke(Runnable)}
 * method using {@link #persist()}
 */
public abstract class AbstractXmlElement implements XmlElement {

    private Element element;

    private XmlElement parent;

    private final Context context;

    private final String tagName;

    private final List<XmlAttribute> attributes;

    private final List<XmlElement> subElements;

    private String textContent;

    private State state;

    private List<ElementChangingEvent> changes = Lists.newArrayList();

    private boolean locked = false;

    // creating new element

    public AbstractXmlElement(String tagName, Map<String, String> attributeMap, Context context) {
        this(tagName, attributeMap, Lists.newArrayList(), "", context);
    }

    public AbstractXmlElement(String tagName, Map<String, String> attributeMap, String textContent, Context context) {
        this(tagName, attributeMap, Lists.newArrayList(), textContent, context);
    }

    public AbstractXmlElement(String tagName, Map<String, String> attributeMap, List<XmlElement> subElements, Context context) {
        this(tagName, attributeMap, subElements, "", context);
    }

    public AbstractXmlElement(String tagName,
                              Map<String, String> attributeMap,
                              List<XmlElement> subElements,
                              String textContent,
                              Context context) {
        this.tagName = tagName;
        this.subElements = subElements;
        this.textContent = textContent;
        this.context = context;
        this.state = State.CONCEPTION;

        List<XmlAttribute> attributes = Lists.newArrayList();
        for (String attributeName : attributeMap.keySet()) {
            attributes.add(new XmlAttribute(this, attributeName, attributeMap.get(attributeName)));
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

        List<XmlAttribute> attributes = Lists.newArrayList();
        Map<String, String> attributeMap = ElementUtils.getAttributes(element);
        for (String attributeName : attributeMap.keySet()) {
            attributes.add(new XmlAttribute(this, attributeName, attributeMap.get(attributeName)));
        }
        this.attributes = attributes;

        subElements.forEach(sub -> sub.setParent(this));
    }

    @Override
    @Nullable
    public abstract String getId();

    @Override
    public void persist() {
        Transaction transaction = context.getActiveTransaction();

        transaction.addChange(new ElementCreatedEvent(this));
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
    public Element requireElement() throws IllegalStateException {
        if (element == null) {
            throw new IllegalStateException(toString() + " has no Element");
        }

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
    public void setParent(XmlElement parent) {
        if (this.parent != null) {
            throw new IllegalStateException(toString() + " already has a parent. Remove it from the old parent first.");
        }

        this.parent = parent;
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
        return attributes;
    }

    @Override
    public XmlAttribute getAttribute(String attributeName) {
        List<XmlAttribute> foundAttributes = attributes.stream()
            .filter(a -> a.getAttributeName().equals(attributeName))
            .collect(Collectors.toList());

        if (foundAttributes.size() == 1) {
            return foundAttributes.get(0);
        } else if (foundAttributes.size() > 1) {
            throw new IllegalStateException("Duplicate attribute: " + attributeName + " on element " + toString());
        } else {
            throw new IllegalStateException("No attribute " + attributeName + " on element " + toString());
        }
    }

    @Override
    public void setAttribute(String attribute, String value) {
        XmlAttribute attributeToChange = getAttribute(attribute);
        attributeToChange.setValue(value);
    }

    @Override
    public boolean hasAttribute(String attributeName) {
        return getAttributes().stream().anyMatch(attribute -> attribute.getAttributeName().equals(attributeName));
    }

    @Override
    public void addSubElement(XmlElement element) {
        Transaction transaction = context.getActiveTransaction();

        if (state.isPhysical()) {
            transaction.addChange(new ElementCreatedEvent(element, this));
        } else {
            transaction.addChange(VirtualEvent.wrap(new ElementCreatedEvent(element, this)));
        }
    }

    @Override
    public void addSubElements(List<XmlElement> elements) {
        elements.forEach(this::addSubElement);
    }

    @Override
    public void addSubElements(XmlElement... elements) {
        addSubElements(Arrays.asList(elements));
    }

    @Override
    public void removeSubElement(XmlElement element) {
        if (!subElements.contains(element)) {
            throw new IllegalArgumentException(element.toString() + " is not a subElement of " + toString());
        }

        if (state.isPhysical()) {
            element.delete();
        } else {
            VirtualEvent.interceptTransaction(context.getActiveTransaction(), element::delete);
        }
    }

    @Override
    public void removeSubElements(List<XmlElement> elements) {
        if (!subElements.containsAll(elements)) {
            throw new IllegalArgumentException("This element does not ");
        }

        if (state.isPhysical()) {
            elements.forEach(XmlElement::delete);
        } else {
            VirtualEvent.interceptTransaction(context.getActiveTransaction(), () -> elements.forEach(XmlElement::delete));
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
    public <E extends XmlElement> List<E> getSubElementsWithType(Class<E> type) {
        return getSubElements().stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
    }

    @Override
    public XmlElement getSubElement(String id) {
        return getSubElement(id, XmlElement.class);
    }

    @Override
    public <E extends XmlElement> E getSubElement(String id, Class<E> type) {
        List<E> foundSubElements = getSubElements().stream()
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
        return getSubElements().stream().anyMatch(subElem -> subElem.getId() != null && subElem.getId().equals(id));
    }

    @Override
    public String getTextContent() {
        return textContent;
    }

    @Override
    public void setTextContent(String textContent) {
        String oldValue = String.valueOf(getTextContent());
        ValueChangingEvent<String> valueChangingEvent = new ValueChangingEvent<>(this, oldValue, textContent);
        addChange(ElementChangingEvent.textContentChange(valueChangingEvent));
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
            Transaction transaction = context.getActiveTransaction();

            State oldState = state;
            state = State.DELETION;
            ElementDeletingEvent deletingEvent = new ElementDeletingEvent(this, oldState);
            if (hasSubElements()) {
                RecursiveDeletingEvent.createRecursive(this, deletingEvent);
            }
            transaction.addChange(deletingEvent);
        } else {
            throw new PersistException("Unable to delete. " + toString() + " is locked");
        }
    }

    @Override
    public void addChange(ElementChangingEvent change) {
        if (!isLocked()) {
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

    @Deprecated
    @Override
    public ElementChangingEvent getFirstChange() {
        return hasChanges() ? getChanges().get(0) : null;
    }

    @Deprecated
    @Override
    public ElementChangingEvent getLastChange() {
        List<ElementChangingEvent> changes = getChanges();
        return hasChanges() ? changes.get(changes.size() - 1) : null;
    }

    @Deprecated
    @Override
    public AttributeChangingEvent getFirstAttributeChange(String attributeName) {
        if (!hasAttribute(attributeName)) {
            throw new IllegalArgumentException(toString() + " does not have an attribute named " + attributeName);
        }

        if (hasChanges()) {
            for (ElementChangingEvent change : getChanges()) {
                if (change.attributeChanged(attributeName)) {
                    return change.getAttributeChange(attributeName);
                }
            }
        }

        return null;
    }

    @Deprecated
    @Override
    public AttributeChangingEvent getLastAttributeChange(String attributeName) {
        if (!hasAttribute(attributeName)) {
            throw new IllegalArgumentException(toString() + " does not have an attribute named " + attributeName);
        }

        AttributeChangingEvent attributeChange = null;
        if (hasChanges()) {
            for (ElementChangingEvent change : getChanges()) {
                if (change.attributeChanged(attributeName)) {
                    attributeChange = change.getAttributeChange(attributeName);
                }
            }
        }

        return attributeChange;
    }

    @Override
    public boolean attributeChanged(String attributeName) {
        return getFirstAttributeChange(attributeName) != null;
    }

    @Deprecated
    @Override
    public ValueChangingEvent<String> getFirstTextContentChange() {
        for (ElementChangingEvent change : getChanges()) {
            if (change.textContentChanged()) {
                return change.getChangedTextContent();
            }
        }

        return null;
    }

    @Override
    public boolean textContentChanged() {
        return getFirstTextContentChange() != null;
    }

    @Deprecated
    @Override
    public void clearChanges() {
        this.changes.clear();
    }

    @Override
    public void lock() {
        locked = true;
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
    public String toString() {
        return "XmlElement<" + getTagName() + ">:" + getId();
    }

    private void applyChange(ElementChangingEvent change, boolean isRollback) {
        if (change.getSource() != this) {
            throw new UnsupportedOperationException("Source of ElementChangingEvent is not this XmlElement. Change can't be applied.");
        }

        if (change.getChangedAttributes() != null) {
            for (AttributeChangingEvent changedAttribute : change.getChangedAttributes()) {
                XmlAttribute attributeToChange = changedAttribute.getAttribute();
                if (isRollback) {
                    attributeToChange.revertChange(changedAttribute);
                } else {
                    attributeToChange.applyChange(changedAttribute);
                }
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

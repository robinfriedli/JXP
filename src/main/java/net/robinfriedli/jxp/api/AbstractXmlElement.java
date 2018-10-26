package net.robinfriedli.jxp.api;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.events.AttributeChangingEvent;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.ElementCreatedEvent;
import net.robinfriedli.jxp.events.ElementDeletingEvent;
import net.robinfriedli.jxp.events.ValueChangingEvent;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.Transaction;
import net.robinfriedli.jxp.persist.XmlElementShadow;
import net.robinfriedli.jxp.persist.XmlPersister;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract class to extend for classes you wish to able to persist to an XML file.
 * Classes that extend this class can be persisted via the {@link net.robinfriedli.jxp.persist.Context#invoke(boolean, Runnable)}
 * method using {@link #persist()}
 */
public abstract class AbstractXmlElement implements XmlElement {

    private XmlElement parent;

    private final Context context;

    private final String tagName;

    private final List<XmlAttribute> attributes;

    private final List<XmlElement> subElements;

    private String textContent;

    private State state;

    private List<ElementChangingEvent> changes = Lists.newArrayList();

    private boolean locked = false;

    private XmlElementShadow shadow;

    public AbstractXmlElement(String tagName, Map<String, String> attributeMap, Context context) {
        this(tagName, attributeMap, Lists.newArrayList(), "", State.CONCEPTION, context);
    }

    public AbstractXmlElement(String tagName, Map<String, String> attributeMap, State state, Context context) {
        this(tagName, attributeMap, Lists.newArrayList(), "", state, context);
    }

    public AbstractXmlElement(String tagName, Map<String, String> attributeMap, String textContent, Context context) {
        this(tagName, attributeMap, Lists.newArrayList(), textContent, State.CONCEPTION, context);
    }

    public AbstractXmlElement(String tagName, Map<String, String> attributeMap, String textContent, State state, Context context) {
        this(tagName, attributeMap, Lists.newArrayList(), textContent, state, context);
    }

    public AbstractXmlElement(String tagName, Map<String, String> attributeMap, List<XmlElement> subElements, Context context) {
        this(tagName, attributeMap, subElements, "", State.CONCEPTION, context);
    }

    public AbstractXmlElement(String tagName,
                              Map<String, String> attributeMap,
                              List<XmlElement> subElements,
                              State state,
                              Context context) {
        this(tagName, attributeMap, subElements, "", state, context);
    }

    public AbstractXmlElement(String tagName,
                              Map<String, String> attributeMap,
                              List<XmlElement> subElements,
                              String textContent,
                              Context context) {
        this(tagName, attributeMap, subElements, textContent, State.CONCEPTION, context);
    }

    public AbstractXmlElement(String tagName,
                              Map<String, String> attributeMap,
                              List<XmlElement> subElements,
                              String textContent,
                              State state,
                              Context context) {
        this.tagName = tagName;
        this.subElements = subElements;
        this.textContent = textContent;
        this.state = state;
        this.context = context;

        List<XmlAttribute> attributes = Lists.newArrayList();
        for (String attributeName : attributeMap.keySet()) {
            attributes.add(new XmlAttribute(this, attributeName, attributeMap.get(attributeName)));
        }
        this.attributes = attributes;

        if (state == State.CONCEPTION) {
            subElements.forEach(sub -> sub.setParent(this));
        } else if (state == State.CLEAN) {
            // when an XmlElement is instantiated with State CLEAN that means it was created while initializing a new Context
            // via DefaultPersistenceManager#getAllElements and thus already exists in the XmlFile. In this case we need
            // to create an XmlElementShadow with the current state of the XmlElement.
            shadow = new XmlElementShadow(this);
        } else {
            throw new PersistException("New XmlElements should be instantiated with either State CONCEPTION (default) " +
                "or CLEAN (if the XmlElement was loaded from the XmlFile)");
        }
    }

    @Override
    @Nullable
    public abstract String getId();

    @Override
    public void persist() {
        Transaction transaction = context.getTransaction();
        if (transaction == null) {
            throw new PersistException("Context has no transaction. Use Context#invoke.");
        }

        if (isPersisted()) {
            throw new PersistException("Cannot persist " + toString() + ". XmlElement is already persisted");
        }

        if (state != State.CONCEPTION) {
            throw new PersistException("Cannot persist " + toString() + ". XmlElement is not in state CONCEPTION");
        }

        if (isSubElement()) {
            throw new PersistException("Cannot persist sub-element " + toString() +
                ". Sub-elements are persist as ElementChangingEvent via their parent");
        }

        transaction.addChange(new ElementCreatedEvent(this));
    }

    @Override
    public void setParent(XmlElement parent) {
        if (!isPersisted()) {
            // this element has not been persisted yet, so it's safe to put it as a child
            this.parent = parent;
        } else if (this.parent != null && this.parent != parent) {
            // this element already has a different parent, remove it from the old parent
            if (this.parent.getState() != State.DELETION) {
                this.parent.removeSubElement(this);
            }
            this.parent = parent;
        } else if (this.parent != null) {
            // parent is already set, do nothing
        } else if (parent.isPersisted()) {
            // both parent and child already exist in the XML file
            // already do set the parent before verifying to help finding the element
            this.parent = parent;
            //verify that this is actually a subElement of parent
            XmlPersister xmlPersister = context.getPersistenceManager().getXmlPersister();
            boolean subElementOf = xmlPersister.isSubElementOf(this, parent);
            if (!subElementOf) {
                this.parent = null;
                throw new PersistException("Can't set parent. " + toString() + " is not a child of " + parent.toString());
            }
        } else {
            // this element already exists but it's parent doesn't
            throw new UnsupportedOperationException("Cannot set parent. Cannot turn " + toString() + " into sub-element");
        }
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
        return shadow != null;
    }

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
        addSubElements(Lists.newArrayList(element));
    }

    @Override
    public void addSubElements(List<XmlElement> elements) {
        elements.forEach(elem -> elem.setParent(this));
        addChange(ElementChangingEvent.subElementsAdded(this, elements));
    }

    @Override
    public void addSubElements(XmlElement... elements) {
        addSubElements(Arrays.asList(elements));
    }

    @Override
    public void removeSubElement(XmlElement element) {
        removeSubElements(Lists.newArrayList(element));
    }

    @Override
    public void removeSubElements(List<XmlElement> elements) {
        addChange(ElementChangingEvent.subElementsRemoved(this, elements));
    }

    @Override
    public void removeSubElements(XmlElement... elements) {
        removeSubElements(Arrays.asList(elements));
    }

    @Override
    public List<XmlElement> getSubElements() {
        return this.subElements;
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
        return textContent != null && !textContent.equals("");
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
            Transaction transaction = context.getTransaction();

            if (transaction == null) {
                throw new PersistException("Context has no transaction. Use Context#invoke");
            }

            if (isSubElement()) {
                getParent().removeSubElement(this);
            } else {
                transaction.addChange(new ElementDeletingEvent(this, getState()));
            }
        } else {
            throw new PersistException("Unable to delete. " + toString() + " is locked");
        }
    }

    @Override
    public void addChange(ElementChangingEvent change) {
        if (!isLocked()) {
            if (state != State.CONCEPTION) {
                Transaction transaction = context.getTransaction();

                if (transaction == null) {
                    throw new PersistException("Context has no transaction. Use Context#invoke");
                }

                if (state == State.CLEAN) {
                    setState(State.TOUCHED);
                }
                changes.add(change);
                transaction.addChange(change);
            } else {
                change.apply();
            }
        } else {
            throw new PersistException("Unable to add Change. " + toString() + " is locked.");
        }
    }

    @Override
    public void removeChange(ElementChangingEvent change) {
        changes.remove(change);

        if (!hasChanges()) {
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
        return this.changes;
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
    public XmlElementShadow getShadow() {
        return shadow;
    }

    @Override
    public void updateShadow() {
        shadow.update();
    }

    @Override
    public void updateShadow(ElementChangingEvent change) {
        shadow.adopt(change);
    }

    @Override
    public void revertShadow(ElementChangingEvent change) {
        shadow.revert(change);
    }

    @Override
    public void createShadow() {
        if (shadow == null) {
            shadow = new XmlElementShadow(this);
        } else {
            throw new PersistException(toString() + " already has a shadow");
        }
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

        if (change.getAddedSubElements() != null) {
            if (isRollback) {
                subElements.removeAll(change.getAddedSubElements());
            } else {
                subElements.addAll(change.getAddedSubElements());
            }
        }

        if (change.getRemovedSubElements() != null) {
            if (isRollback) {
                subElements.addAll(change.getRemovedSubElements());
            } else {
                subElements.removeAll(change.getRemovedSubElements());
            }
        }

        if (change.getChangedTextContent() != null) {
            if (isRollback) {
                this.textContent = change.getChangedTextContent().getOldValue();
            } else {
                this.textContent = change.getChangedTextContent().getNewValue();
            }
        }
    }


}

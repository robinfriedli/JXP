package net.robinfriedli.jxp.api;

import java.util.List;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.events.AttributeChangingEvent;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.ValueChangingEvent;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.persist.Context;
import org.w3c.dom.Element;

/**
 * Enables classes to be persisted as XML elements. Extend {@link AbstractXmlElement} to persist your elements using
 * {@link Context#invoke(Runnable)}.
 */
public interface XmlElement {

    /**
     * Persists a new XmlElement in {@link State#CONCEPTION} to the XML file
     */
    void persist();

    /**
     * Removes the {@link Element} represented by this XmlElement from the XML document. Phantoms can be persisted again
     * by using the {@link #persist()} method, or, for subElements, by adding it to a new parent using {@link #addSubElement(XmlElement)}
     * after removing it from the old one
     */
    void phantomize();

    /**
     * @return the actual {@link Element} in the XML document represented by this XmlElement. Null if in State CONCEPTION
     * or PHANTOM.
     */
    @Nullable
    Element getElement();

    /**
     * Like {@link #getElement()} but throws Exception if null
     */
    Element requireElement() throws IllegalStateException;

    /**
     * Sets the created {@link Element} for this XmlElement. Used after a new XmlElement has been persisted.
     */
    void setElement(Element element);

    /**
     * Defines the parent of a subElement. This is not permitted if the element already has a parent, the element first
     * has to be removed from the old parent using {@link #removeSubElement(XmlElement)}. This method should generally
     * only be used by the API.
     */
    void setParent(XmlElement parent);

    /**
     * Set the parent of this XmlElement to null. Used for subElements when {@link #removeSubElement(XmlElement)} is called
     */
    void removeParent();

    /**
     * @return the parent Element of this XmlElement
     */
    XmlElement getParent();

    /**
     * @return true if this XmlElement has a parent element
     */
    boolean isSubElement();

    /**
     * @return true if the XmlElement has an {@link Element}, meaning it has been persisted to the XML file
     */
    boolean isPersisted();

    /**
     * @return {@link Context} of this XmlElement
     */
    Context getContext();

    /**
     * @return Tag name for Class to be used in XML file
     */
    String getTagName();

    /**
     * @return XML attributes of XmlElement
     */
    List<XmlAttribute> getAttributes();

    /**
     * @param attributeName name of attribute
     * @return XmlAttribute instance with specified name
     */
    XmlAttribute getAttribute(String attributeName);

    /**
     * Set existing attribute to a different value
     *
     * @param attribute XML attribute name to change
     * @param value new value
     */
    void setAttribute(String attribute, String value);

    /**
     * @param attributeName name of attribute to check
     * @return true if this XmlElement has an attribute with the specified name
     */
    boolean hasAttribute(String attributeName);

    /**
     * Add new XmlElement to SubElements
     *
     * @param element to add
     */
    void addSubElement(XmlElement element);

    /**
     * Add new XmlElements to SubElements
     *
     * @param elements to add
     */
    void addSubElements(List<XmlElement> elements);

    /**
     * Add new XmlElements to SubElements
     *
     * @param elements to add
     */
    void addSubElements(XmlElement... elements);

    /**
     * Remove XmlElement from SubElements
     *
     * @param element to remove
     */
    void removeSubElement(XmlElement element);

    /**
     * Remove XmlElements from SubElements
     *
     * @param elements to remove
     */
    void removeSubElements(List<XmlElement> elements);

    /**
     * Remove XmlElements from SubElements
     *
     * @param elements to remove
     */
    void removeSubElements(XmlElement... elements);

    /**
     * @return all SubElements
     */
    List<XmlElement> getSubElements();

    /**
     * Get all subElements of this XmlElement of type {@link E}. Use this to method to treat subElements as instances of
     * their original class rather than XmlElements.
     *
     * @param type Subclass of XmlElement you want to filter and cast the subElements to
     * @param <E> target Type. Subclass of XmlElement
     * @return all filtered SubElements as instances of class E
     */
    <E extends XmlElement> List<E> getSubElementsWithType(Class<E> type);

    /**
     * Get one of the subElements via their unique {@link #getId()}
     *
     * @param id unique id to find the subElement with
     * @return found subElement
     */
    @Nullable
    XmlElement getSubElement(String id);

    /**
     * Same as {@link #getSubElement(String)} but also checks whether the sub element matches the given type and casts
     * it to it
     */
    @Nullable
    <E extends XmlElement> E getSubElement(String id, Class<E> type);

    /**
     * Like {@link #getSubElement(String)} but throws {@link IllegalStateException} if none is found
     *
     * @param id unique id to find the subElement with
     * @return found subElement
     * @throws IllegalStateException if no subElement is found
     */
    XmlElement requireSubElement(String id) throws IllegalStateException;

    /**
     * Same as {@link #requireSubElement(String)} but also checks whether the sub element matches the given type and casts
     * it to it
     */
    <E extends XmlElement> E requireSubElement(String id, Class<E> type) throws IllegalStateException;

    /**
     * @return true if this XmlElement has any SubElements
     */
    boolean hasSubElements();

    /**
     * Checks if this XmlElement has a subElement where {@link #getId()} matches the provided id
     *
     * @param id of subElement
     * @return true if element has subElement with specified id
     */
    boolean hasSubElement(String id);

    /**
     * Get the text content of the XML element.
     *
     * E.g. <element>textContent</element>
     *
     * @return text content of XML element
     */
    String getTextContent();

    /**
     * Set the text content of the XML element.
     *
     * E.g. <element>textContent</element>
     *
     * @param textContent to set
     */
    void setTextContent(String textContent);

    /**
     * @return true if this XmlElement has a text content body
     */
    boolean hasTextContent();

    /**
     * Define some way to identify this XmlElement instance, ideally through either one of its attributes or text content.
     * If there is no way to uniquely identify this XmlElement, just return null.
     *
     * @return unique id for this XmlElement instance
     */
    @Nullable
    String getId();

    /**
     * Check if an XmlElement already exists in the same {@link Context} and thus would result in a duplicate.
     * Ideally you should compare attributes that represent an id of some kind.
     * If you do not want to check your XmlElement for duplicates just return false.
     *
     * The default implementation in {@link AbstractXmlElement} checks if there is another XmlElement with the same
     * same tag and id returned by {@link #getId()}
     *
     * @param elementToCheck XmlElement to compare
     * @return true if the specified XmlElement would duplicate an already existing Element
     */
    @Deprecated
    boolean checkDuplicates(XmlElement elementToCheck);

    /**
     * Checks if the provided XmlElement has the same structure, meaning the same tag name and attributes
     *
     * @param elementToCompare element to compare
     * @return true if tag name and attribute names match
     */
    boolean matchesStructure(XmlElement elementToCompare);

    /**
     * Delete this XmlElement. Creates an {@link net.robinfriedli.jxp.events.ElementDeletingEvent} which, when applied,
     * will set this XmlElement to {@link State#PHANTOM}. Only when committing the XmlElement will be fully removed from
     * its {@link Context} and XML file.
     */
    void delete();

    /**
     * Add an {@link ElementChangingEvent} to this XmlElement
     *
     * @param change to add
     */
    void addChange(ElementChangingEvent change);

    /**
     * Remove an {@link ElementChangingEvent} from this XmlElement
     *
     * @param change to remove
     */
    void removeChange(ElementChangingEvent change);

    /**
     * Applies an {@link ElementChangingEvent} to this XmlElement. The source of the event must be this XmlElement or else
     * an {@link UnsupportedOperationException} is thrown.
     *
     * @param change {@link ElementChangingEvent} to apply
     * @throws UnsupportedOperationException if source of event does not equal this XmlElement
     * @throws PersistException if {@link Context} has no {@link net.robinfriedli.jxp.persist.Transaction}
     */
    void applyChange(ElementChangingEvent change) throws UnsupportedOperationException, PersistException;

    /**
     * Reverts a change made to this XmlElement. This occurs when an exception is thrown during commit.
     *
     * @param change {@link ElementChangingEvent} change to revert
     * @throws UnsupportedOperationException
     * @throws PersistException
     */
    void revertChange(ElementChangingEvent change) throws UnsupportedOperationException, PersistException;

    /**
     * @return all {@link ElementChangingEvent} on this XmlElement
     */
    List<ElementChangingEvent> getChanges();

    /**
     * @return true if this XmlElement has uncommitted {@link ElementChangingEvent}s
     */
    boolean hasChanges();

    /**
     * Returns the first {@link ElementChangingEvent} of this XmlElement that has not been persisted yet. Used to identify
     * this XmlElement within the XML file. At least before {@link XmlElementShadow} was introduced.
     *
     * @return first {@link ElementChangingEvent}
     */
    @Deprecated
    ElementChangingEvent getFirstChange();

    /**
     * Returns last uncommitted {@link ElementChangingEvent} of this XmlElement. Used to persist XmlElement changes.
     *
     * @return last {@link ElementChangingEvent}
     */
    @Deprecated
    ElementChangingEvent getLastChange();

    /**
     * Get first change made to {@link XmlAttribute} with specified name since last commit
     *
     * @param attributeName name of attribute
     * @return first change made to specified attribute since last commit
     */
    @Deprecated
    AttributeChangingEvent getFirstAttributeChange(String attributeName);

    /**
     * Get last change made to {@link XmlAttribute} with specified name since last commit
     *
     * @param attributeName name of attribute
     * @return last change made to specified attribute since last commit
     */
    @Deprecated
    AttributeChangingEvent getLastAttributeChange(String attributeName);

    /**
     * Check if attribute with specified name has any uncommitted changes
     *
     * @param attributeName name of attribute
     * @return true if attribute has changes
     */
    boolean attributeChanged(String attributeName);

    /**
     * @return first uncommitted change made to the XmlElement's text content
     */
    @Deprecated
    ValueChangingEvent<String> getFirstTextContentChange();

    /**
     * @return true if this XmlElement's text content has uncommitted changes
     */
    boolean textContentChanged();

    /**
     * Clear all {@link ElementChangingEvent} on this XmlElement. Changes are cleared on commit but applied to the in
     * memory XmlElement after adding.
     */
    @Deprecated
    void clearChanges();

    /**
     * Lock this XmlElement blocking any changes from being added. This happens when an XmlElement is recognised as a
     * duplicate while adding, meaning they have the same {@link #getId()}. In this case the existing XmlElement would
     * adopt all changes from the new XmlElement if there are any differences and the new XmlElement gets locked, disabling
     * it from being added to any {@link Context} or receiving changes.
     */
    void lock();

    /**
     * @return true if this XmlElement is locked. See {@link #lock()}
     */
    boolean isLocked();

    /**
     * @return {@link State} of this XmlElement
     */
    State getState();

    /**
     * Set the {@link State} of this XmlElement
     *
     * @param state to set
     */
    void setState(State state);

    /**
     * The state of the in memory representation of the XML element
     */
    enum State {
        /**
         * XmlElement instance has just been created but not yet persisted to the XML document
         */
        CONCEPTION(false),

        /**
         * Element exists and has no uncommitted changes
         */
        CLEAN(true),

        /**
         * Element exists in XML file but has uncommitted changes
         */
        TOUCHED(true),

        /**
         * Unlike PHANTOM the Element has not actually been deleted yet and still exists in the XML document. This is
         * state of elements after the delete() method has been called but before the change has been committed.
         *
         * Physical is still false even though that is technically incorrect since this state is only temporary during
         * Transactions and we want to treat those element like phantoms. Even IF the element would get persisted again
         * in the same transaction, it would get persisted with all changes applied. Thus eliminating the need to commit
         * changes as with normal physical elements.
         */
        DELETION(false),

        /**
         * The XmlElement's element has been removed from the XML document
         */
        PHANTOM(false);

        private boolean physical;

        State(boolean physical) {
            this.physical = physical;
        }

        /**
         * @return true if the the element has a physical element in the XML file in this state. Defines whether or not
         * changes made to an element need to be committed.
         */
        public boolean isPhysical() {
            return physical;
        }
    }

}

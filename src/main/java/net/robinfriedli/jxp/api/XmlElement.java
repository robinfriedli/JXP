package net.robinfriedli.jxp.api;

import java.util.List;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.events.AttributeChangingEvent;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.ValueChangingEvent;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.InstantApplyTx;
import net.robinfriedli.jxp.persist.Transaction;
import net.robinfriedli.jxp.persist.XmlElementShadow;

/**
 * Enables classes to be persisted as XML elements. Extend {@link AbstractXmlElement} to persist your elements using
 * {@link Context#invoke(boolean, Runnable)}.
 */
public interface XmlElement {

    /**
     * Persists a new XmlElement in {@link State#CONCEPTION} to the XML file
     */
    void persist();

    /**
     * This method safely defines the parent of this XmlElement. This is only allowed when this XmlElement is newly
     * being created or when building the tree while initializing a new Context, meaning both the parent element
     * and this element are persisted. In that case {@link net.robinfriedli.jxp.persist.XmlPersister#isSubElementOf(XmlElement, XmlElement)}
     * will check if the parent is in fact the parent of this Element in the file.
     *
     * It is also possible to move a sub-element to a different parent, in which case the sub-element will be removed
     * from the old parent, unless the old parent is in {@link State#DELETION} anyway.
     *
     * This method should only be used by the API. Implementers should use {@link #addSubElement(XmlElement)}. In that
     * case and when sub-elements are passed to the constructor the API will set the parent automatically. The API also
     * sets all parents for all existing XmlElements when initializing a new {@link Context} via the
     * {@link net.robinfriedli.jxp.persist.DefaultPersistenceManager#buildTree(List)} method.
     */
    void setParent(XmlElement parent);

    /**
     * @return the parent Element of this XmlElement
     */
    XmlElement getParent();

    /**
     * @return true if this XmlElement has a parent element
     */
    boolean isSubElement();

    /**
     * @return true if the XmlElement has an {@link XmlElementShadow}, meaning it has been persisted to the XML file
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
     * Used to check for duplicates when adding a new element and then automatically applying the changes from the new
     * element to the old element or easily loading an element from the {@link Context}.
     *
     * If there is no way to uniquely identify this XmlElement, just return null. In which case duplicates will not be
     * checked when adding an XmlElement of this type and you'll have to load it from the {@link Context} through your
     * own criteria
     *
     * Note that using an ID drastically affects performance when dealing with very large files because it checks
     * if there already is an element with the same id when adding it to the Context.
     * E.g. creating 100000 elements with id at once in an empty Context takes ~700 seconds (~820 seconds if it's an
     * {@link InstantApplyTx}). The same task takes less than a second without IDs. Detailed performance log:
     *
     * Done initializing after 99 ms
     * Transaction took 93 ms to run. Now applying
     * 10000 milestone reached after 7 seconds
     * 20000 milestone reached after 25 seconds
     * 30000 milestone reached after 59 seconds
     * 40000 milestone reached after 111 seconds
     * 50000 milestone reached after 173 seconds
     * 60000 milestone reached after 251 seconds
     * 70000 milestone reached after 342 seconds
     * 80000 milestone reached after 449 seconds
     * 90000 milestone reached after 571 seconds
     * Transaction finished applying after 707 seconds
     * Transaction finished committing after 708 seconds
     * All elements saved after 708 seconds
     * (before v0.5: 1100 seconds)
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
     * will set this XmlElement to {@link State#DELETION}. Only when committing the XmlElement will be fully removed from
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
     * @return this XmlElements current {@link XmlElementShadow} representing its current state in the XmlFile.
     * Used to identify this XmlElement in the file
     */
    XmlElementShadow getShadow();

    /**
     * Update this XmlElements {@link XmlElementShadow}. Used after committing a change made to this XmlElement.
     */
    @Deprecated
    void updateShadow();

    /**
     * Applies an {@link ElementChangingEvent} to this XmlElement's {@link XmlElementShadow}. Used when said change
     * was committed.
     *
     * @param change the recently committed ElementChangingEvent
     */
    void updateShadow(ElementChangingEvent change);

    /**
     * Reverts an {@link ElementChangingEvent} from this XmlElement's {@link XmlElementShadow}. Used when a
     * {@link Transaction} is rolling back and reverting a change that has been committed and thus applied to the shadow
     * using {@link #updateShadow(ElementChangingEvent)}
     *
     * @param change
     */
    void revertShadow(ElementChangingEvent change);

    /**
     * Create an {@link XmlElementShadow}. Used when committing a new XmlElement in state conception.
     */
    void createShadow();

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
         * Element has been created but not yet persisted
         */
        CONCEPTION,

        /**
         * Element exists and has no uncommitted changes
         */
        CLEAN,

        /**
         * Element exists in XML file but has uncommitted changes
         */
        TOUCHED,

        /**
         * Element is being deleted but still exists in XML file
         */
        DELETION
    }

}

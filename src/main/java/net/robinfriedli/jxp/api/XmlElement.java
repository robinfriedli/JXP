package net.robinfriedli.jxp.api;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.queries.Conditions;
import net.robinfriedli.jxp.queries.QueryResult;
import net.robinfriedli.jxp.queries.ResultStream;
import org.w3c.dom.Element;

/**
 * Enables classes to be persisted as XML elements. Extend {@link AbstractXmlElement} to persist your elements using
 * {@link Context#invoke(Runnable)}.
 */
public interface XmlElement {

    /**
     * Persists a new XmlElement in {@link State#CONCEPTION} to the XML file
     *
     * @param context the context for the document to save to element to
     */
    void persist(Context context);

    /**
     * Method used by {@link #addSubElement(XmlElement)} to append this subElement to a parent
     *
     * @param context   the context for the document to save to element to
     * @param newParent the parent element
     */
    void persist(Context context, XmlElement newParent);

    /**
     * @return true if this XmlElement is not in any Context. This is the case when an XmlElement has just been
     * instantiated before persisting it to a Context
     */
    boolean isDetached();

    /**
     * Creates a new XmlElement with the attributes and text content of this one. This does not require a transaction as
     * it does not persist the copy
     *
     * @param copySubElements             also copies all subelements of this element
     * @param instantiateContributedClass tries to instantiate one of your classes contributed to the {@link JxpBackend}
     *                                    if none found or false returns a {@link BaseXmlElement}. Mind that your class has to have a constructor matching
     *                                    {@link BaseXmlElement} to be instantiated by this method.
     * @return the newly created XmlElement
     */
    XmlElement copy(boolean copySubElements, boolean instantiateContributedClass);

    /**
     * Removes the {@link Element} represented by this XmlElement from the XML document. Phantoms can be persisted again
     * by using the {@link #persist(Context)} method, or, for subElements, by adding it to a new parent using {@link #addSubElement(XmlElement)}
     * after removing it from the old one. This method should generally just be used internally.
     */
    void phantomize();

    /**
     * @return the actual {@link Element} in the XML document represented by this XmlElement. Null if in State CONCEPTION
     * or PHANTOM.
     */
    @Nullable
    Element getElement();

    /**
     * Sets the created {@link Element} for this XmlElement. Used after a new XmlElement has been persisted.
     */
    void setElement(Element element);

    /**
     * Like {@link #getElement()} but throws Exception if null
     */
    Element requireElement() throws IllegalStateException;

    /**
     * Set the parent of this XmlElement to null. Used for subElements when {@link #removeSubElement(XmlElement)} is called
     */
    void removeParent();

    /**
     * @return the parent Element of this XmlElement
     */
    XmlElement getParent();

    /**
     * Defines the parent of a subElement. This is not permitted if the element already has a parent, the element first
     * has to be removed from the old parent using {@link #removeSubElement(XmlElement)}. This method should generally
     * only be used internally.
     */
    void setParent(XmlElement parent);

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
     * @return Tag name for this xml element
     */
    String getTagName();

    /**
     * @return a new list instance containing all {@link XmlAttribute} instances on this XmlElement
     */
    List<XmlAttribute> getAttributes();

    /**
     * @return the actual map instance holding the {@link XmlAttribute} instances for this XmlElement
     */
    Map<String, XmlAttribute> getAttributeMap();

    /**
     * @param attributeName name of attribute
     * @return XmlAttribute instance with specified name or a new {@link XmlAttribute.Provisional}
     */
    XmlAttribute getAttribute(String attributeName);


    /**
     * Deletes the given attribute from this XmlElement. This action requires a Transaction for an XmlElement in a
     * physical state.
     *
     * @param attributeName the name of the attribute to remove
     */
    void removeAttribute(String attributeName);

    /**
     * Set existing attribute to a different value or create a new one.
     *
     * @param attribute XML attribute name to change
     * @param value     new value
     */
    void setAttribute(String attribute, Object value);

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
     * @return all SubElements. This exposes the internal list directly.
     */
    List<XmlElement> getSubElements();

    /**
     * @return all subElements and their subElements
     */
    List<XmlElement> getSubElementsRecursive();

    /**
     * Get all subElements of this XmlElement of type {@link E}. Use this to method to treat subElements as instances of
     * their original class rather than XmlElements. As of v1.1 an alias for method {@link #getInstancesOf(Class)}
     *
     * @param type Subclass of XmlElement you want to filter and cast the subElements to
     * @param <E>  target Type. Subclass of XmlElement
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
     * <p>
     * E.g. <element>textContent</element>
     *
     * @return text content of XML element
     */
    String getTextContent();

    /**
     * Set the text content of the XML element.
     * <p>
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
     * <p>
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
     * will remove the element from its Context and set this XmlElement to {@link State#PHANTOM}. Only when committing
     * the XmlElement will be fully removed from its XML file.
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
     * @throws PersistException              if {@link Context} has no {@link net.robinfriedli.jxp.persist.Transaction}
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
     * @return all {@link ElementChangingEvent} on this XmlElement. This exposes the internal list directly.
     */
    List<ElementChangingEvent> getChanges();

    /**
     * @return true if this XmlElement has uncommitted {@link ElementChangingEvent}s
     */
    boolean hasChanges();

    /**
     * Check if attribute with specified name has any uncommitted changes
     *
     * @param attributeName name of attribute
     * @return true if attribute has changes
     */
    boolean attributeChanged(String attributeName);

    /**
     * @return true if this XmlElement's text content has uncommitted changes
     */
    boolean textContentChanged();

    /**
     * Prevents this XmlElement instance from being changed or deleted.
     */
    void lock();

    void unlock();

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
     * Get all subElements that are instance of {@link E}
     *
     * @param c   Class to check
     * @param <E> Type of Class to check
     * @return All Elements that are an instance of specified Class
     */
    <E extends XmlElement> List<E> getInstancesOf(Class<E> c);

    /**
     * Get all subElements that are instance of {@link E}, ignoring elements that are instance of
     * any of the ignored subClasses. Used to exclude subclasses.
     *
     * @param c                 Class to check
     * @param ignoredSubClasses subclasses to exclude
     * @param <E>               Type to return
     * @return All Elements that are an instance of specified Class but not specified subclasses
     */
    <E extends XmlElement> List<E> getInstancesOf(Class<E> c, Class<?>... ignoredSubClasses);

    /**
     * Checks all subElements for provided {@link Predicate}s and returns {@link QueryResult} with matching elements.
     * See {@link Conditions} for useful predicates.
     *
     * @param condition
     * @return
     */
    ResultStream<XmlElement> query(Predicate<XmlElement> condition);

    /**
     * Like {@link #query(Predicate)} but casts the found elements to the given class
     */
    <E extends XmlElement> ResultStream<E> query(Predicate<XmlElement> condition, Class<E> type);

    /**
     * The state of the in memory representation of the XML element
     */
    enum State {
        /**
         * XmlElement instance has just been created but not yet persisted to the XML document
         */
        CONCEPTION(false),

        /**
         * The XmlElement is in the process of being saved to the document and is already treated as physical
         */
        PERSISTING(true),

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
         * <p>
         * Physical is already false even though that is technically incorrect since this state is only temporary during
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

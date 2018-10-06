package net.robinfriedli.jxp.persist;

import net.robinfriedli.jxp.api.XmlElement;

import javax.annotation.Nullable;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * Main entry to the persistence layer. Represents an XML document and holds all its elements as {@link XmlElement}
 * instances. The {@link #invoke(boolean, boolean, Callable)} methods is mandatory for any action that creates, changes
 * or deletes an XmlElement.
 */
public interface Context {

    /**
     * @return the {@link ContextManager} for this Context
     */
    ContextManager getManager();

    /**
     * @return the {@link DefaultPersistenceManager} for this Context
     */
    DefaultPersistenceManager getPersistenceManager();

    /**
     * @return the path of the XML file of this Context
     */
    String getPath();

    /**
     * @return All Elements saved in this Context
     */
    List<XmlElement> getElements();

    /**
     * @return all XmlElements in this Context that match {@param predicate}
     */
    List<XmlElement> getElements(Predicate<XmlElement> predicate);

    /**
     * @return All Elements that are not in {@link net.robinfriedli.jxp.api.XmlElement.State} DELETION
     */
    List<XmlElement> getUsableElements();

    /**
     * @return all Ids that are used by XmlElements in this Context
     */
    List<String> getUsedIds();

    /**
     * Return an {@link XmlElement} saved in this Context where {@link XmlElement#getId()} matches the provided id
     *
     * @param id to find the {@link XmlElement} with
     * @return found {@link XmlElement}
     */
    @Nullable
    XmlElement getElement(String id);

    /**
     * Return an {@link XmlElement} saved in this Context where {@link XmlElement#getId()} matches the provided id and
     * is an instance of the provided class.
     *
     * @param id to find the {@link XmlElement} with
     * @param type target Class
     * @param <E> Class extending {@link XmlElement}
     * @return found {@link XmlElement} cast to target Class
     */
    @Nullable
    <E extends XmlElement> E getElement(String id, Class<E> type);

    /**
     * Same as {@link #getElement(String)} but throws Exception when null
     *
     * @param id to find the {@link XmlElement} with
     * @return found {@link XmlElement}
     */
    XmlElement requireElement(String id) throws IllegalStateException;

    /**
     * same as {@link #requireElement(String)} but checks if the element matches the passed type and casts it to it
     */
    <E extends XmlElement> E requireElement(String id, Class<E> type) throws IllegalStateException;

    /**
     * Get all XmlElements saved in this Context that are instance of {@link E}
     *
     * @param c Class to check
     * @param <E> Type of Class to check
     * @return All Elements that are an instance of specified Class
     */
    <E extends XmlElement> List<E> getInstancesOf(Class<E> c);

    /**
     * Get all XmlElements saved in this Context that are instance of {@link E}, ignoring elements that are instance of
     * any of the ignored subClasses. Used to exclude subclasses.
     *
     * @param c Class to check
     * @param ingoredSubClasses subclasses to exclude
     * @param <E> Type to return
     * @return All Elements that are an instance of specified Class but not specified subclasses
     */
    <E extends XmlElement> List<E> getInstancesOf(Class<E> c, Class... ingoredSubClasses);

    /**
     * Reload all Elements from the XML File. This overrides any uncommitted changes.
     */
    void reloadElements();

    /**
     * Add Element to memory
     *
     * @param element to add to context
     */
    void addElement(XmlElement element);

    /**
     * Add Elements to memory
     *
     * @param elements to add to context
     */
    void addElements(List<XmlElement> elements);

    /**
     * Add Elements to memory
     *
     * @param elements to add to context
     */
    void addElements(XmlElement... elements);

    void removeElement(XmlElement element);

    void removeElements(List<XmlElement> xmlElements);

    void removeElements(XmlElement... xmlElements);

    /**
     * Commit all previously uncommitted {@link Transaction}s on this Context
     */
    void commitAll();

    /**
     * remove and rollback all uncommitted {@link Transaction}s on this Context
     */
    void revertAll();

    /**
     * @return true if field uncommittedTransactions is not empty
     */
    boolean hasUncommittedTransactions();

    /**
     * @return all saved uncommitted {@link Transaction} in this Context
     */
    List<Transaction> getUncommittedTransactions();

    /**
     * Run a Callable in a {@link Transaction}. For any actions that create, change or delete an {@link XmlElement}.
     *
     * @param commit defines if the changes will be committed to XML file immediately after applying or remain in Context
     * @param instantApply defines whether the changes will be applied instantly or all at once after the task has run.
     * e.g. if false, this code would return the old value of "attribute", not "newValue"
     * <pre>
     *     return context.invoke(true, false, () -> {
     *         someElement.setAttribute("attribute", "newValue");
     *         return someElement.geAttribute("attribute").getValue();
     *     })
     * </pre>
     * however, disabling instantApply will improve performance when dealing with a large number of changes
     * e.g. creating 100000 elements with id in an empty Context takes ~700 seconds without instantApply and ~820 seconds
     * with it enabled. Note that creating the same amount of elements without an id (just return null in {@link XmlElement#getId()})
     * takes < 1 second. So if you're planning on using massive files definitely don't use IDs. For a more detailed
     * performance analysis see javadoc of {@link XmlElement#getId()}
     * @param task any Callable
     * @param <E> return type of Callable
     * @return any Object of type E
     *
     * Notice: calling invoke() within the task will just use the outer transaction, meaning the parameters will be ignored.
     * However, {@link #apply(boolean, Runnable)} can be called within an invoke() task, this saves the current transaction,
     * creates an {@link ApplyOnlyTx}, applies it, and then switches back to the old transaction. The invoke() method only
     * creates a new transaction if there isn't a current one
     */
    <E> E invoke(boolean commit, boolean instantApply, Callable<E> task);

    /**
     * same as {@link #invoke(boolean, boolean, Callable)} with commit = true and instantApply = true as default values
     */
    <E> E invoke(Callable<E> task);

    /**
     * Quickly execute a one-liner that requires a {@link Transaction} like
     * <p>
     * {@code context.invoke(() -> bla.setAttribute("name", "value"));}
     * <p>
     * or several statements without returning anything like
     *
     * <pre>
     *     context.invoke(() -> {
     *        TestXmlElement elem = new TestXmlElement("test", context);
     *        elem.setAttribute("test", "test");
     *        elem.setTextContent("test");
     *     });
     * </pre>
     * <p>
     * A {@link Transaction} is required by any action that creates, changes or deletes an {@link XmlElement}
     *
     * @param commit defines if the changes will be committed to XML file immediately after applying or remain in Context
     * @param task any Runnable
     */
    void invoke(boolean commit, boolean instantApply, Runnable task);

    /**
     * same as {@link #invoke(boolean, boolean, Runnable)} with commit = true and instantApply = true as default values
     */
    void invoke(Runnable task);

    /**
     * Like {@link #invoke(boolean, boolean, Callable)} but also sets this Context's environment variable to the
     * passed Object. For an explanation regarding envVar see {@link #getEnvVar()}.
     *
     * @param commit defines if the changes will be committed to XML file immediately after applying or remain in Context
     * @param task any Callable
     * @param envVar any Object to set as envVar in Context
     * @param <E> return type of Callable
     * @return any Object of type E
     */
    <E> E invoke(boolean commit, boolean instantApply, Callable<E> task, Object envVar);

    /**
     * Like {@link #invoke(boolean, boolean, Runnable)} but also sets this Context's environment variable to the
     * passed Object. For an explanation regarding envVar see {@link #getEnvVar()}.
     *
     * @param commit defines if the changes will be committed to XML file immediately after applying or remain in Context
     * @param task any Runnable
     * @param envVar any Object to set as envVar in Context
     */
    void invoke(boolean commit, boolean instantApply, Runnable task, Object envVar);

    /**
     * Runs a task in an apply-only {@link Transaction} that will never be committed or saved as uncommitted transaction
     * in this Context. Use cautiously if you want to apply the changes to the XML file using the {@link XmlPersister}
     * manually. Used for dealing with changes that would otherwise throw an exception when committing, e.g. dealing
     * with duplicate elements. See {@link ApplyOnlyTx}
     *
     * @param task to run
     */
    void apply(boolean instantApply, Runnable task);

    /**
     * same as {@link #apply(boolean, Runnable)} with instantApply = true as default value
     */
    void apply(Runnable task);

    /**
     * set the variable for this Context. For a description on envVar see {@link #getEnvVar()}
     *
     * @param envVar variable to set
     */
    void setEnvVar(Object envVar);

    /**
     * The environment variable can be anything you might need somewhere els in context with this transaction.
     * The environment variable is set when using either method {@link #invoke(boolean, boolean, Callable, Object)}
     * {@link #invoke(boolean, boolean, Runnable, Object)}
     * <p>
     * E.g. say you're developing a Discord bot and you've implemented an {@link net.robinfriedli.jxp.events.EventListener} that sends a message
     * after an Element has been added. In this case you could set the MessageChannel the command came from as envVar
     * to send the message to the right channel.
     *
     * @return the environment variable of this Context.
     */
    Object getEnvVar();

    /**
     * @return the currently active Transaction
     */
    @Nullable
    Transaction getTransaction();

    /**
     * Partitionable Context that can be bound to an object of Type E. The Context can then be retrieved from the
     * ContextManager via the {@link ContextManager#getContext(Object)} method, this checks whether the passed object
     * equals the BindableContext's boundObject, it does not have to be the same object. Each BindableContext will create
     * a new file based on the base Context (a copy of the path specified in the ContextManager)
     *
     * @param <E> Type the Context may be bound to
     */
    interface BindableContext<E> extends Context {

        /**
         * Get the instance of Type E which the Context is bound to
         *
         * @return object of Type E
         */
        E getBindingObject();

        /**
         * removes this BindableContext from its ContextManager and deletes its file.
         * Use {@link ContextManager#destroyBoundContext(Object)}
         */
        void destroy();

    }

}

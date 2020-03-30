package net.robinfriedli.jxp.persist;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.api.JxpBackend;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.exec.Invoker;
import net.robinfriedli.jxp.exec.QueuedTask;
import net.robinfriedli.jxp.exec.modes.MutexSyncMode;
import net.robinfriedli.jxp.queries.Conditions;
import net.robinfriedli.jxp.queries.QueryResult;
import net.robinfriedli.jxp.queries.ResultStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Interface for classes that represent and manage an XML file and provide access to its {@link XmlElement} instances.
 * The {@link #invoke(Runnable)} methods enable an easy way to run a write transaction that may make changes to the
 * XML file.
 */
public interface Context extends AutoCloseable {

    /**
     * AutoCloseable#close override that does not throw an exception since this implementation never does so no
     * catch block is required
     */
    @Override
    void close();

    /**
     * @return the {@link JxpBackend} instance that created this Context
     */
    JxpBackend getBackend();

    /**
     * @return the {@link Document dom Document} represented by this Context
     */
    Document getDocument();

    /**
     * @return the XML file this Context is persisted to.
     */
    @Nullable
    File getFile();

    /**
     * @return true if this Context is persisted to a file. This also requires the persistent target to be writable, so
     * this returns false for a Context created from an InputStream
     */
    boolean isPersistent();

    /**
     * Delete this Context's XML file
     */
    void deleteFile();

    /**
     * Persist this context to an XML file, if based on a {@link Document} instance.
     *
     * @param path the path to save the Context to.
     * @throws PersistException if persisting to the file fails
     */
    void persist(String path);

    /**
     * Creates a new Context based on the DOM document of this context. Instantiates a new document and creates a deep
     * copy of the root element of this document. The resulting Context is not yet stored to a file.
     *
     * @return the newly create Context
     */
    Context copy();

    /**
     * Copies this Context to create a new BoundContext.
     *
     * @param objectToBind the object to bind the new Context to
     * @return the newly created BindableContext
     */
    <E> BindableContext<E> copy(E objectToBind);

    /**
     * Reloads the elements stored in this Context. This means all current XmlElement instances will be cleared and
     * phantomized and re-instantiated based on the current state of the physical Document and the dom document will be
     * re-parsed. In case of a LazyContext this only does the second part. This action in only possible for persistent
     * contexts.
     */
    void reload();

    /**
     * @return the path of the XML file of this Context
     */
    String getPath();

    /**
     * @return the tag name of the root element this Context represents
     */
    String getRootElem();

    /**
     * @return All Elements saved in this Context
     */
    List<XmlElement> getElements();

    /**
     * @return All Elements including subElements
     */
    List<XmlElement> getElementsRecursive();

    /**
     * @return all XmlElements (including their sub elements) in this Context that match {@param predicate}
     */
    List<XmlElement> getElements(Predicate<XmlElement> predicate);

    /**
     * @return all Ids that are used by XmlElements in this Context
     */
    Set<String> getUsedIds();

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
     * @param id   to find the {@link XmlElement} with
     * @param type target Class
     * @param <E>  Class extending {@link XmlElement}
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
     * @param c   Class to check
     * @param <E> Type of Class to check
     * @return All Elements that are an instance of specified Class
     */
    <E extends XmlElement> List<E> getInstancesOf(Class<E> c);

    /**
     * Get all XmlElements saved in this Context that are instance of {@link E}, ignoring elements that are instance of
     * any of the ignored subClasses. Used to exclude subclasses.
     *
     * @param c                 Class to check
     * @param ignoredSubClasses subclasses to exclude
     * @param <E>               Type to return
     * @return All Elements that are an instance of specified Class but not specified subclasses
     */
    <E extends XmlElement> List<E> getInstancesOf(Class<E> c, Class... ignoredSubClasses);

    /**
     * Checks all XmlElements for provided {@link Predicate}s and returns {@link QueryResult} with matching elements.
     * See {@link Conditions} for useful predicates.
     *
     * @param condition Predicate to filter elements with, see {@link Conditions}
     * @return a stream of found elements
     */
    ResultStream<XmlElement> query(Predicate<XmlElement> condition);

    /**
     * Like {@link #query(Predicate)} but casts the found elements to the given class
     */
    <E extends XmlElement> ResultStream<E> query(Predicate<XmlElement> condition, Class<E> type);

    /**
     * Executes the provided XPath query and returns the {@link XmlElement}s in this Context that are based on the
     * results or, if this is a {@link LazyContext}, instantiates {@link XmlElement}s based on the results.
     *
     * @param xPathQuery the XPath query
     * @return XmlElements instances based on results
     */
    List<XmlElement> xPathQuery(String xPathQuery);

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
     * @param commit       defines if the transaction will be committed to XML file immediately after running or remain as
     *                     uncommitted transaction in the Context
     * @param instantApply defines whether the changes will be applied instantly or all at once after the task has run.
     *                     e.g. if false, this code would return the old value of "attribute", not "newValue"
     *                     <pre>
     *                          return context.invoke(true, false, () -> {
     *                                  someElement.setAttribute("attribute", "newValue");
     *                                  return someElement.geAttribute("attribute").getValue();
     *                              })
     *                     </pre>
     *                     however, disabling instantApply might slightly improve performance when dealing with a large number of changes.
     * @param task         any Callable
     * @param <E>          return type of Callable
     * @return any Object of type E
     * <p>
     * Notice: when calling this method within an invoked task with different parameters, i.e. a different mode, the
     * context will switch to a new transaction an switch back after.
     */
    <E> E invoke(boolean commit, boolean instantApply, Callable<E> task);


    /**
     * Core invoke implementation that is called by each overloading method that runs the task in any given {@link Invoker.Mode}
     *
     * @throws PersistException if a checked exception occurs
     */
    <E> E invoke(Invoker.Mode mode, Callable<E> task);

    /**
     * same as {@link #invoke(boolean, boolean, Callable)} with commit = true and instantApply = true as default values
     */
    <E> E invoke(Callable<E> task);

    /**
     * Invoke a task without a return value
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
     * @param task   any Runnable
     */
    void invoke(boolean commit, boolean instantApply, Runnable task);

    /**
     * Like {@link #invoke(Invoker.Mode, Callable)} but accepts a Runnable
     */
    void invoke(Invoker.Mode mode, Runnable task);

    /**
     * same as {@link #invoke(boolean, boolean, Runnable)} with commit = true and instantApply = true as default values
     */
    void invoke(Runnable task);

    /**
     * Like {@link #invoke(boolean, boolean, Runnable)} but runs in a {@link SequentialTx}, meaning all changes will
     * get committed / flushed each time the amount of changes recorded by the transaction reaches the provided number
     */
    <E> E invokeSequential(int sequence, Callable<E> task);

    /**
     * Like {@link #invokeSequential(int, Callable)} but accepts a runnable instead of a callable
     */
    void invokeSequential(int sequence, Runnable task);

    /**
     * A {@link #invoke(boolean, boolean, Callable)} implementation that will be executed in the future. This is
     * applicable, although as of v1.2 no longer required, for tasks invoked after the Transaction has stopped recording
     * but before the Transaction has been committed and closed. When this happens depends on whether it is an
     * InstantApplyTx or not; if it is an InstantApplyTx the Transaction will keep recording and applying changes until
     * {@link Transaction#commit()} is called, if it is not it's until {@link Transaction#apply()} is called.
     * <p>
     * This is typically used in listeners (for InstantApplyTxs this is only required for the transaction committed
     * event) in which case the returned {@link QueuedTask} will be added to current Transaction and executed
     * after the Transaction is done. This method can also be used freely in which case the implementor decides when to
     * execute it.
     * <p>
     * If this is called within a transaction this task will added as a queued task to the transaction and run after
     * the transaction finished. Else the implementor needs to start the task manually.
     * <p>
     * This is commonly used to run a task after the current transaction has finished in the same thread, if you want to
     * run the task in a separate thread you should use {@link #futureInvoke(boolean, boolean, Invoker.Mode, Callable)}
     * and apply the {@link MutexSyncMode} to enable synchronisation of the task.
     *
     * @param callable         the callable to call in the future
     * @param cancelOnFailure  if the task has been queued to a transaction cancel it when the transaction fails
     * @param triggerListeners trigger or mute listeners for the invoked task
     * @param enqueue          whether to enqueue this task to run after the current transaction (if available) in the same thread.
     * @param <E>              the type the callable returns
     * @return the {@link QueuedTask} that will be executed after the current Transaction is done automatically,
     * if called within Transaction (e.g EventListeners)
     */
    <E> QueuedTask<E> futureInvoke(boolean commit, boolean instantApply, boolean cancelOnFailure, boolean triggerListeners, boolean enqueue, Callable<E> callable);

    /**
     * Core {@link #futureInvoke(boolean, boolean, boolean, boolean, boolean, Callable)} implementation that allows to set
     * a custom mode. This should be used if the resulting QueuedTask is to be executed in a separated thread with the
     * {@link MutexSyncMode} applied (using the mutex returned by {@link #getMutexKey()}).
     *
     * @throws PersistException if enqueue is true but no active transaction exists in the current thread
     */
    <E> QueuedTask<E> futureInvoke(boolean cancelOnFailure, boolean enqueue, Invoker.Mode mode, Callable<E> callable);

    /**
     * calls {@link #futureInvoke(boolean, boolean, boolean, boolean, boolean, Callable)} with default values commit = true,
     * instantApply = true, enqueue = true
     */
    <E> QueuedTask<E> futureInvoke(boolean cancelOnFailure, boolean triggerListeners, Callable<E> callable);

    /**
     * {@link #futureInvoke(boolean, boolean, boolean, boolean, boolean, Callable)} with default values commit = true,
     * instantApply = true, cancelOnFailure = true, triggerListeners = true, enqueue = true
     */
    <E> QueuedTask<E> futureInvoke(Callable<E> callable);

    /**
     * calls {@link #invoke(boolean, boolean, Callable)} and temporarily disables listeners
     */
    <E> E invokeWithoutListeners(boolean commit, boolean instantApply, Callable<E> callable);

    /**
     * {@link #invokeWithoutListeners(boolean, boolean, Callable)} with  default values commit = true
     * and instantApply = true
     */
    <E> E invokeWithoutListeners(Callable<E> callable);

    void invokeWithoutListeners(boolean commit, boolean instantApply, Runnable runnable);

    void invokeWithoutListeners(Runnable runnable);

    /**
     * Runs a task in an {@link ApplyOnlyTx} that will never be committed or saved as uncommitted transaction
     * in this Context. Use cautiously when dealing with changes that would break a regular commit one wy or the other.
     * Was used before JXP v0.7 to deal with duplicate Elements because the XmlPersister could not find / uniquely
     * identify the {@link Element} that needed to be changed. But then 0.7 eliminated the need to locate the Element in
     * the first place meaning all obvious use cases for this class vanished. Only use if you know what you are doing.
     *
     * @param task to run
     */
    void apply(boolean instantApply, Runnable task);

    /**
     * same as {@link #apply(boolean, Runnable)} with instantApply = true as default value
     */
    void apply(Runnable task);

    /**
     * @return the current Transaction
     */
    @Nullable
    Transaction getTransaction();

    /**
     * Set this Context's Transaction. Used internally to switch between Transaction e.g. for VirtualEvents
     *
     * @param transaction to set
     */
    void setTransaction(Transaction transaction);

    /**
     * @return the current Transaction, requires an active Transaction
     */
    Transaction getActiveTransaction();

    /**
     * @return the mutex key that may be used to synchronise tasks using {@link MutexSyncMode}. For persistent Contexts
     * this returns the canonical file path, else this uses the hash code of the document instance.
     */
    String getMutexKey();

    /**
     * Specialized Context that may be "bound" to any Object of type E, as in it can easy be retrieved from the {@link JxpBackend}
     * via {@link JxpBackend#getBoundContext(Object)} in which case the Context is returned if the Object it is bound to
     * is equal to the provided Object according to {@link Object#equals(Object)}.
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

    }

}

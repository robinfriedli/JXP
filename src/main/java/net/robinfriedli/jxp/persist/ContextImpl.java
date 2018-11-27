package net.robinfriedli.jxp.persist;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.JxpBackend;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.CommitException;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.queries.Query;
import net.robinfriedli.jxp.queries.QueryResult;
import org.w3c.dom.Document;

public class ContextImpl implements Context {

    private final JxpBackend backend;

    private final List<XmlElement> elements;

    private final DefaultPersistenceManager persistenceManager;

    private final Document document;

    private String path;

    private File file;

    private Transaction transaction;

    private List<Transaction> uncommittedTransactions = Lists.newArrayList();

    private Object envVar;

    public ContextImpl(JxpBackend backend, DefaultPersistenceManager persistenceManager, Document document) {
        this.backend = backend;
        this.document = document;
        this.persistenceManager = persistenceManager;
        this.elements = persistenceManager.getAllElements(this);
    }

    public ContextImpl(JxpBackend backend, DefaultPersistenceManager persistenceManager, File file) {
        this.backend = backend;
        this.path = file.getPath();
        this.file = file;

        if (!file.exists()) {
            throw new PersistException("File " + file + " does not exist");
        }

        document = persistenceManager.parseDocument(file);
        this.persistenceManager = persistenceManager;
        this.elements = persistenceManager.getAllElements(this);
    }

    @Override
    public JxpBackend getBackend() {
        return backend;
    }

    @Override
    public Document getDocument() {
        return document;
    }

    @Nullable
    @Override
    public File getFile() {
        return file;
    }

    @Override
    public boolean isPersistent() {
        return file != null;
    }

    @Override
    public void deleteFile() {
        if (!isPersistent()) {
            throw new IllegalStateException("This Context is not persisted");
        }

        if (file.exists()) {
            file.delete();
            file = null;
        } else {
            throw new IllegalStateException("This Context's file does not exist");
        }
    }

    @Override
    public void persist(String path) {
        if (isPersistent()) {
            throw new IllegalStateException("This Context is already persistent");
        }

        if (!path.endsWith(".xml")) {
            throw new IllegalArgumentException("Missing file extension .xml");
        }

        File file = new File(path);
        if (file.exists()) {
            throw new IllegalArgumentException("File for path " + path + " already exists");
        }

        try {
            file.createNewFile();
            this.file = file;
            this.path = path;
            persistenceManager.writeToFile(this);
        } catch (IOException | CommitException e) {
            throw new PersistException("Exception persisting Context to file " + path, e);
        }
    }

    @Override
    public Context copy(String targetPath) {
        if (!isPersistent()) {
            throw new UnsupportedOperationException("Can only copy persistent Context");
        }

        Document document = persistenceManager.parseDocument(file);
        Context context = new ContextImpl(backend, persistenceManager, document);
        backend.addContext(context);
        context.persist(targetPath);

        return context;
    }

    @Override
    public <E> BindableContext<E> copy(String targetPath, E objectToBind) {
        if (!isPersistent()) {
            throw new UnsupportedOperationException("Can only copy persistent Context");
        }

        Document document = persistenceManager.parseDocument(file);
        BindableContext<E> context = new BindableContextImpl<>(backend, persistenceManager, document, objectToBind);
        backend.addBoundContext(context);
        context.persist(targetPath);

        return context;
    }

    @Override
    public DefaultPersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getRootElem() {
        return document.getDocumentElement().getTagName();
    }

    @Override
    public List<XmlElement> getElements() {
        return elements;
    }

    @Override
    public List<XmlElement> getElementsRecursive() {
        List<XmlElement> elements = Lists.newArrayList();
        for (XmlElement element : this.elements) {
            recursiveAdd(elements, element);
        }
        return elements;
    }

    private void recursiveAdd(List<XmlElement> elements, XmlElement element) {
        elements.add(element);
        if (element.hasSubElements()) {
            for (XmlElement subElement : element.getSubElements()) {
                recursiveAdd(elements, subElement);
            }
        }
    }

    @Override
    public List<XmlElement> getElements(Predicate<XmlElement> predicate) {
        return elements.stream().filter(predicate).collect(Collectors.toList());
    }

    @Override
    public List<String> getUsedIds() {
        return getElements().stream().map(XmlElement::getId).collect(Collectors.toList());
    }

    @Override
    @Nullable
    public XmlElement getElement(String id) {
        return getElement(id, XmlElement.class);
    }

    @Override
    @Nullable
    public <E extends XmlElement> E getElement(String id, Class<E> type) {
        List<E> foundElements = getElements().stream()
            .filter(element -> type.isInstance(element) && element.getId() != null && element.getId().equals(id))
            .map(type::cast)
            .collect(Collectors.toList());

        if (foundElements.size() == 1) {
            return foundElements.get(0);
        } else if (foundElements.size() > 1) {
            throw new IllegalStateException("Id " + id + " not unique");
        } else {
            return null;
        }
    }

    @Override
    public XmlElement requireElement(String id) throws IllegalStateException {
        return requireElement(id, XmlElement.class);
    }

    @Override
    public <E extends XmlElement> E requireElement(String id, Class<E> type) throws IllegalStateException {
        E element = getElement(id, type);

        if (element != null) {
            return element;
        } else {
            throw new IllegalStateException("No element found for id " + id);
        }
    }

    @Override
    public <E extends XmlElement> List<E> getInstancesOf(Class<E> c) {
        return getElements().stream()
            .filter(c::isInstance)
            .map(c::cast)
            .collect(Collectors.toList());
    }

    @Override
    public <E extends XmlElement> List<E> getInstancesOf(Class<E> c, Class... ignoredSubClasses) {
        return getElements().stream()
            .filter(elem -> c.isInstance(elem) && Arrays.stream(ignoredSubClasses).noneMatch(clazz -> clazz.isInstance(elem)))
            .map(c::cast)
            .collect(Collectors.toList());
    }

    @Override
    public QueryResult<List<XmlElement>> query(Predicate<XmlElement> condition) {
        return Query.evaluate(condition).execute(getElementsRecursive());
    }

    @Override
    public void addElement(XmlElement element) {
        elements.add(element);
    }

    @Override
    public void addElements(List<XmlElement> elements) {
        elements.forEach(this::addElement);
    }

    @Override
    public void addElements(XmlElement... elements) {
        addElements(Arrays.asList(elements));
    }

    @Override
    public void removeElement(XmlElement element) {
        elements.remove(element);
    }

    @Override
    public void removeElements(List<XmlElement> elements) {
        this.elements.removeAll(elements);
    }

    @Override
    public void removeElements(XmlElement... elements) {
        removeElements(Arrays.asList(elements));
    }

    @Override
    public void commitAll() {
        uncommittedTransactions.forEach(tx -> {
            try {
                tx.commit(persistenceManager);
            } catch (CommitException e) {
                e.printStackTrace();
            }
        });

        uncommittedTransactions.clear();
    }

    @Override
    public void revertAll() {
        uncommittedTransactions.forEach(Transaction::rollback);
        uncommittedTransactions.clear();
    }

    @Override
    public boolean hasUncommittedTransactions() {
        return !uncommittedTransactions.isEmpty();
    }

    @Override
    public List<Transaction> getUncommittedTransactions() {
        return uncommittedTransactions;
    }

    @Override
    public <E> E invoke(boolean commit, boolean instantApply, Callable<E> task) {
        boolean nested = false;
        if (transaction != null) {
            if (transaction.isRecording()) {
                nested = true;
            } else {
                throw new PersistException("Context has a current Transaction that is not recording anymore. Use Context#futureInvoke to run your task after this has finished.");
            }
        } else {
            getTx(instantApply, false);
        }

        E returnValue;

        try {
            returnValue = task.call();
        } catch (Throwable e) {
            if (instantApply) {
                transaction.rollback();
            } else {
                transaction.fail();
            }
            closeTx();
            throw new PersistException(e.getClass().getName() + " thrown during task run. Closing transaction.", e);
        }

        if (!nested) {
            try {
                if (instantApply) {
                    transaction.setState(Transaction.State.APPLIED);
                    getBackend().fireTransactionApplied(transaction);
                } else {
                    transaction.apply();
                }
                if (commit) {
                    try {
                        transaction.commit(persistenceManager);
                    } catch (CommitException e) {
                        e.printStackTrace();
                    }
                } else {
                    uncommittedTransactions.add(transaction);
                }
            } finally {
                List<QueuedTask> queuedTasks = transaction.getQueuedTasks();
                boolean failed = transaction.failed();
                closeTx();

                queuedTasks.forEach(t -> {
                    if (failed && t.isCancelOnFailure()) {
                        t.cancel(false);
                    } else {
                        t.execute();
                    }
                });
            }
        }

        return returnValue;
    }

    @Override
    public <E> E invoke(Callable<E> task) {
        return invoke(true, true, task);
    }

    @Override
    public void invoke(boolean commit, boolean instantApply, Runnable task) {
        invoke(commit, instantApply, () -> {
            task.run();
            return null;
        });
    }

    @Override
    public void invoke(Runnable task) {
        invoke(true, true, task);
    }

    @Override
    public <E> E invoke(boolean commit, boolean instantApply, Callable<E> task, Object envVar) {
        this.envVar = envVar;
        return invoke(commit, instantApply, task);
    }

    @Override
    public void invoke(boolean commit, boolean instantApply, Runnable task, Object envVar) {
        this.envVar = envVar;
        invoke(commit, instantApply, task);
    }

    @Override
    public <E> QueuedTask<E> futureInvoke(boolean commit, boolean instantApply, boolean cancelOnFailure, boolean triggerListeners, Callable<E> callable) {
        QueuedTask<E> queuedTask = new QueuedTask<>(this, commit, instantApply, cancelOnFailure, triggerListeners, callable);
        if (transaction != null) {
            transaction.queueTask(queuedTask);
        }

        return queuedTask;
    }

    @Override
    public <E> QueuedTask<E> futureInvoke(boolean cancelOnFailure, boolean triggerListeners, Callable<E> callable) {
        return futureInvoke(true, true, cancelOnFailure, triggerListeners, callable);
    }

    @Override
    public <E> QueuedTask<E> futureInvoke(Callable<E> callable) {
        return futureInvoke(true, true, true, true, callable);
    }

    @Override
    public <E> E invokeWithoutListeners(boolean commit, boolean instantApply, Callable<E> callable) {
        E retVal;
        getBackend().setListenersMuted(true);
        try {
            retVal = invoke(commit, instantApply, callable);
        } finally {
            getBackend().setListenersMuted(false);
        }

        return retVal;
    }

    @Override
    public <E> E invokeWithoutListeners(Callable<E> callable) {
        return invokeWithoutListeners(true, true, callable);
    }

    @Override
    public void invokeWithoutListeners(boolean commit, boolean instantApply, Runnable runnable) {
        invokeWithoutListeners(commit, instantApply, () -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public void invokeWithoutListeners(Runnable runnable) {
        invokeWithoutListeners(true, true, runnable);
    }

    @Override
    public void apply(boolean instantApply, Runnable task) {
        // save the currently ongoing transaction
        Transaction currentTx = transaction;
        // switch to a different transaction
        getTx(instantApply, true);

        try {
            task.run();
        } catch (Throwable e) {
            transaction = currentTx;
            throw new PersistException(e.getClass().getName() + " thrown during task run. Closing transaction.", e);
        }

        if (!instantApply) {
            transaction.apply();
        } else {
            getBackend().fireTransactionApplied(transaction);
        }
        // switch back to old transaction
        transaction = currentTx;
    }

    @Override
    public void apply(Runnable task) {
        apply(true, task);
    }

    @Override
    public void setEnvVar(Object envVar) {
        this.envVar = envVar;
    }

    @Override
    public Object getEnvVar() {
        return envVar;
    }

    @Override
    @Nullable
    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    public Transaction getActiveTransaction() {
        if (transaction == null) {
            throw new PersistException("Context has no transaction. Use Context#invoke");
        }

        if (!transaction.isRecording()) {
            throw new PersistException("Context has a current Transaction that is not recording anymore. Use Context#futureInvoke to run your task after this has finished.");
        }

        return transaction;
    }

    @Override
    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    private void getTx(boolean instantApply, boolean applyOnly) {
        if (instantApply && applyOnly) {
            transaction = Transaction.createInstantApplyOnlyTx(this);
        } else if (instantApply) {
            transaction = Transaction.createInstantApplyTx(this);
        } else if (applyOnly) {
            transaction = Transaction.createApplyOnlyTx(this);
        } else {
            transaction = Transaction.createTx(this);
        }
    }

    private void closeTx() {
        transaction = null;
    }

}
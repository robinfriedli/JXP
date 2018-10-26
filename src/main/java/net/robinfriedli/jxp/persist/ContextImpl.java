package net.robinfriedli.jxp.persist;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.CommitException;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.queries.Query;
import net.robinfriedli.jxp.queries.QueryResult;

public class ContextImpl implements Context {

    private final ContextManager manager;

    private final List<XmlElement> elements;

    private final DefaultPersistenceManager persistenceManager;

    private final String path;

    private final String rootElem;

    private Transaction transaction;

    private List<Transaction> uncommittedTransactions = Lists.newArrayList();

    private Object envVar;

    public ContextImpl(ContextManager manager, DefaultPersistenceManager persistenceManager, String path) {
        this.manager = manager;
        this.path = path;
        persistenceManager.initialize(this);
        rootElem = persistenceManager.getXmlPersister().getDocument().getDocumentElement().getTagName();
        this.persistenceManager = persistenceManager;
        this.elements = persistenceManager.getAllElements();
        persistenceManager.buildTree(elements);
    }

    @Override
    public ContextManager getManager() {
        return this.manager;
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
        return rootElem;
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
    public List<XmlElement> getUsableElements() {
        return getElements(e -> e.getState() != XmlElement.State.DELETION);
    }

    @Override
    public List<String> getUsedIds() {
        return getUsableElements().stream().map(XmlElement::getId).collect(Collectors.toList());
    }

    @Override
    @Nullable
    public XmlElement getElement(String id) {
        return getElement(id, XmlElement.class);
    }

    @Override
    @Nullable
    public <E extends XmlElement> E getElement(String id, Class<E> type) {
        List<E> foundElements = getUsableElements().stream()
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
        return getUsableElements().stream()
            .filter(c::isInstance)
            .map(c::cast)
            .collect(Collectors.toList());
    }

    @Override
    public <E extends XmlElement> List<E> getInstancesOf(Class<E> c, Class... ignoredSubClasses) {
        return getUsableElements().stream()
            .filter(elem -> c.isInstance(elem) && Arrays.stream(ignoredSubClasses).noneMatch(clazz -> clazz.isInstance(elem)))
            .map(c::cast)
            .collect(Collectors.toList());
    }

    @Override
    public QueryResult<List<XmlElement>> query(Predicate<XmlElement> condition) {
        return Query.evaluate(condition).execute(getElementsRecursive());
    }

    @Override
    public void reloadElements() {
        elements.clear();
        elements.addAll(persistenceManager.getAllElements());
    }

    @Override
    public void addElement(XmlElement element) {
        if (element.getId() != null && getUsedIds().contains(element.getId())) {
            throw new PersistException("There already is an element with id " + element.getId() + " in this Context");
        }
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
                persistenceManager.write();
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
        boolean encapsulated = false;
        if (transaction != null) {
            encapsulated = true;
        } else {
            getTx(instantApply, false);
        }

        E returnValue;

        try {
            returnValue = task.call();
        } catch (Throwable e) {
            closeTx();
            throw new PersistException(e.getClass().getName() + " thrown during task run. Closing transaction.", e);
        }

        if (!encapsulated) {
            try {
                if (instantApply) {
                    getManager().fireTransactionApplied(transaction);
                } else {
                    transaction.apply();
                }
                if (commit) {
                    try {
                        transaction.commit(persistenceManager);
                        persistenceManager.write();
                    } catch (CommitException e) {
                        e.printStackTrace();
                    }
                } else {
                    uncommittedTransactions.add(transaction);
                }
            } finally {
                closeTx();
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
            getManager().fireTransactionApplied(transaction);
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
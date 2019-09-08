package net.robinfriedli.jxp.persist;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.JxpBackend;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.CommitException;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.exceptions.QueryException;
import net.robinfriedli.jxp.exec.AbstractTransactionalMode;
import net.robinfriedli.jxp.exec.Invoker;
import net.robinfriedli.jxp.exec.InvokerImpl;
import net.robinfriedli.jxp.exec.ListenersMutedMode;
import net.robinfriedli.jxp.exec.QueuedTask;
import net.robinfriedli.jxp.exec.SequentialMode;
import net.robinfriedli.jxp.queries.Query;
import net.robinfriedli.jxp.queries.ResultStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Default Context that implements all methods that are common between Context implementations
 */
public abstract class AbstractContext implements Context {

    private final JxpBackend backend;
    private final Logger logger;

    private Document document;
    private String path;
    private File file;
    private List<Transaction> uncommittedTransactions = Lists.newArrayList();
    private Object envVar;
    private ThreadLocal<Transaction> threadTransaction = new ThreadLocal<>();

    public AbstractContext(JxpBackend backend, Document document, Logger logger) {
        this.backend = backend;
        this.document = document;
        this.logger = logger;
    }

    public AbstractContext(JxpBackend backend, File file, Logger logger) {
        this.backend = backend;
        this.path = file.getPath();
        this.file = file;
        this.logger = logger;

        if (!file.exists()) {
            throw new PersistException("File " + file + " does not exist");
        }

        document = StaticXmlParser.parseDocument(file);
    }

    @Override
    public void close() {
        backend.removeContext(this);
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
            StaticXmlParser.writeToFile(this);
        } catch (IOException | CommitException e) {
            throw new PersistException("Exception persisting Context to file " + path, e);
        }
    }

    @Override
    public Context copy() {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Node parent = document.importNode(this.document.getDocumentElement(), true);
            document.appendChild(parent);

            return instantiate(backend, document, logger);
        } catch (ParserConfigurationException e) {
            throw new PersistException("Exception while copying context", e);
        }
    }

    @Override
    public <E> BindableContext<E> copy(E objectToBind) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Node parent = document.importNode(this.document.getDocumentElement(), true);
            document.appendChild(parent);

            return new BindableCachedContext<>(backend, document, objectToBind, logger);
        } catch (ParserConfigurationException e) {
            throw new PersistException("Exception while copying context", e);
        }
    }

    protected abstract Context instantiate(JxpBackend jxpBackend, Document document, Logger logger);

    protected abstract Context instantiate(JxpBackend jxpBackend, File document, Logger logger);

    @Override
    public void reload() {
        if (!isPersistent()) {
            throw new UnsupportedOperationException("Can only reload persistent Context");
        }

        document = StaticXmlParser.parseDocument(file);
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
    public List<XmlElement> getElementsRecursive() {
        List<XmlElement> elements = Lists.newArrayList();
        for (XmlElement element : Lists.newArrayList(getElements())) {
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
    public List<XmlElement> getElements(Predicate<XmlElement> predicate) {
        return getElementsRecursive().stream().filter(predicate).collect(Collectors.toList());
    }

    @Override
    public List<String> getUsedIds() {
        return getElementsRecursive().stream().map(XmlElement::getId).collect(Collectors.toList());
    }

    @Override
    @Nullable
    public XmlElement getElement(String id) {
        return getElement(id, XmlElement.class);
    }

    @Override
    @Nullable
    public <E extends XmlElement> E getElement(String id, Class<E> type) {
        List<E> foundElements = getElementsRecursive().stream()
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
        return getElementsRecursive().stream()
            .filter(c::isInstance)
            .map(c::cast)
            .collect(Collectors.toList());
    }

    @Override
    public <E extends XmlElement> List<E> getInstancesOf(Class<E> c, Class... ignoredSubClasses) {
        return getElementsRecursive().stream()
            .filter(elem -> c.isInstance(elem) && Arrays.stream(ignoredSubClasses).noneMatch(clazz -> clazz.isInstance(elem)))
            .map(c::cast)
            .collect(Collectors.toList());
    }

    @Override
    public ResultStream<XmlElement> query(Predicate<XmlElement> condition) {
        return Query.evaluate(condition).execute(getElementsRecursive());
    }

    @Override
    public <E extends XmlElement> ResultStream<E> query(Predicate<XmlElement> condition, Class<E> type) {
        return Query.evaluate(condition).execute(getElementsRecursive(), type);
    }

    @Override
    public List<XmlElement> xPathQuery(String xPathQuery) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList nodeList = (NodeList) xPath.compile(xPathQuery).evaluate(getDocument(), XPathConstants.NODESET);
            List<Element> docElements = ElementUtils.nodeListToElementList(nodeList);
            return handleXPathResults(docElements);
        } catch (XPathExpressionException e) {
            throw new QueryException("Exception while compiling XPath query", e);
        }
    }

    protected abstract List<XmlElement> handleXPathResults(List<Element> results);

    @Override
    public void addElements(List<XmlElement> elements) {
        elements.forEach(this::addElement);
    }

    @Override
    public void addElements(XmlElement... elements) {
        addElements(Arrays.asList(elements));
    }

    @Override
    public void removeElements(List<XmlElement> elements) {
        elements.forEach(this::removeElement);
    }

    @Override
    public void removeElements(XmlElement... elements) {
        removeElements(Arrays.asList(elements));
    }

    @Override
    public void commitAll() {
        uncommittedTransactions.forEach(tx -> {
            try {
                tx.commit();
            } catch (CommitException e) {
                logger.error("Exception during commit of transaction " + tx, e);
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
        return invoke(Invoker.Mode.create().with(getMode(instantApply, false).shouldCommit(commit)), task);
    }

    @Override
    public <E> E invoke(Invoker.Mode mode, Callable<E> task) {
        InvokerImpl invoker = new InvokerImpl();
        return invoker.invoke(mode, task);
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
    public void invoke(Invoker.Mode mode, Runnable task) {
        invoke(mode, () -> {
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
    public void invokeSequential(int sequence, Runnable task) {
        invokeSequential(sequence, () -> {
            task.run();
            return null;
        });
    }

    @Override
    public <E> E invokeSequential(int sequence, Callable<E> task) {
        return invoke(Invoker.Mode.create().with(new SequentialMode(this, sequence)), task);
    }

    @Override
    public <E> QueuedTask<E> futureInvoke(boolean commit, boolean instantApply, boolean cancelOnFailure, boolean triggerListeners, Callable<E> callable) {
        QueuedTask<E> queuedTask = new QueuedTask<>(this, commit, instantApply, cancelOnFailure, triggerListeners, callable, logger);
        Transaction transaction = threadTransaction.get();
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
        Invoker.Mode mode = Invoker.Mode.create()
            .with(new ListenersMutedMode(getBackend()))
            .with(getMode(instantApply, false).shouldCommit(commit));
        return invoke(mode, callable);
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
        Invoker.Mode mode = Invoker.Mode.create().with(getMode(instantApply, true));
        invoke(mode, task);
    }

    @Override
    public void apply(Runnable task) {
        apply(true, task);
    }

    @Override
    public Object getEnvVar() {
        return envVar;
    }

    @Override
    public void setEnvVar(Object envVar) {
        this.envVar = envVar;
    }

    @Override
    @Nullable
    public Transaction getTransaction() {
        return threadTransaction.get();
    }

    @Override
    public void setTransaction(Transaction transaction) {
        threadTransaction.set(transaction);
    }

    @Override
    public Transaction getActiveTransaction() {
        Transaction transaction = threadTransaction.get();
        if (transaction == null) {
            throw new PersistException("Context has no transaction. Use Context#invoke");
        }

        if (!transaction.isActive()) {
            throw new PersistException("Context has a current Transaction that is not active anymore (state: "
                + transaction.getState() + "). Use Context#futureInvoke to run your task after this has finished.");
        }

        return transaction;
    }

    private AbstractTransactionalMode getMode(boolean instantApply, boolean applyOnly) {
        return AbstractTransactionalMode.Builder.create()
            .setInstantApply(instantApply)
            .setApplyOnly(applyOnly)
            .build(this);
    }

}
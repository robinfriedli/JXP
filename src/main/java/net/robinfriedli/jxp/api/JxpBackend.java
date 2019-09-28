package net.robinfriedli.jxp.api;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.ElementCreatedEvent;
import net.robinfriedli.jxp.events.ElementDeletingEvent;
import net.robinfriedli.jxp.events.JxpEventListener;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.logging.LoggerSupplier;
import net.robinfriedli.jxp.persist.BindableCachedContext;
import net.robinfriedli.jxp.persist.CachedContext;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.LazyContext;
import net.robinfriedli.jxp.persist.Transaction;
import org.w3c.dom.Document;

public class JxpBackend {

    private final List<Context> contexts;
    private final List<Context.BindableContext> boundContexts;
    private final List<JxpEventListener> listeners;
    private final Logger logger;
    private final DefaultContextType defaultContextType;
    private ThreadLocal<Boolean> listenersMuted = ThreadLocal.withInitial(() -> false);

    public JxpBackend(List<JxpEventListener> listeners,
                      DefaultContextType defaultContextType) {
        this(Lists.newArrayList(), Lists.newArrayList(), listeners, defaultContextType);
    }

    public JxpBackend(List<Context> contexts,
                      List<Context.BindableContext> boundContexts,
                      List<JxpEventListener> listeners,
                      DefaultContextType defaultContextType) {
        this.contexts = contexts;
        this.boundContexts = boundContexts;
        this.listeners = listeners;
        this.defaultContextType = defaultContextType;
        logger = LoggerSupplier.getLogger();
    }

    public List<Context> getContexts() {
        return contexts;
    }

    public List<Context.BindableContext> getBoundContexts() {
        return boundContexts;
    }

    public Context getContext(String path) {
        return getContext(new File(path));
    }

    /**
     * @param document the DOM document
     * @return either the existing Context for this document that is attached to this JxpBackend instance or a new
     * Context that will be attached to this JxpBackend
     */
    public Context getContext(Document document) {
        Context existingContext = getExistingContext(document);

        if (existingContext != null) {
            return existingContext;
        } else {
            Context context = defaultContextType.getContext(this, document, logger);
            contexts.add(context);
            return context;
        }
    }

    /**
     * @param file the XML file
     * @return either the existing Context for this file that is attached to this JxpBackend instance or a new Context
     * that will be attached to this JxpBackend
     */
    public Context getContext(File file) {
        Context existingContext = getExistingContext(file);

        if (existingContext != null) {
            return existingContext;
        } else {
            Context context = defaultContextType.getContext(this, file, logger);
            contexts.add(context);
            return context;
        }
    }

    @Nullable
    public Context getExistingContext(String path) {
        return getExistingContext(new File(path));
    }

    /**
     * @param document the DOM document
     * @return the existing Context for this document that is attached to this JxpBackend
     */
    @Nullable
    public Context getExistingContext(Document document) {
        List<Context> found = contexts.stream().filter(c -> c.getDocument().equals(document)).collect(Collectors.toList());

        if (found.size() == 1) {
            return found.get(0);
        } else if (found.size() > 1) {
            throw new IllegalStateException("More than one Context for document " + document);
        }

        return null;
    }

    /**
     * @param file the XML file
     * @return the existing Context for this XML file that is attached to this JxpBackend
     */
    @Nullable
    public Context getExistingContext(File file) {
        List<Context> found = contexts
            .stream()
            .filter(c -> {
                try {
                    return c.getFile() != null && c.getFile().getCanonicalPath().equals(file.getCanonicalPath());
                } catch (IOException e) {
                    throw new PersistException(e);
                }
            })
            .collect(Collectors.toList());

        if (found.size() == 1) {
            return found.get(0);
        } else if (found.size() > 1) {
            throw new IllegalStateException("More than one Context for file " + file);
        }

        return null;
    }

    public Context requireExistingContext(String path) {
        return requireExistingContext(new File(path));
    }

    public Context requireExistingContext(Document document) {
        Context context = getExistingContext(document);

        if (context != null) {
            return context;
        } else {
            throw new IllegalStateException("No Context for document " + document);
        }
    }

    public Context requireExistingContext(File file) {
        Context context = getExistingContext(file);

        if (context != null) {
            return context;
        } else {
            throw new IllegalStateException("No Context for file " + file);
        }
    }

    public boolean hasContext(String path) {
        return hasContext(new File(path));
    }

    /**
     * @param document the DOM document
     * @return true if this JxpBackend has an attached Context for this DOM document
     */
    public boolean hasContext(Document document) {
        return contexts.stream().anyMatch(c -> c.getDocument().equals(document));
    }

    /**
     * @param file the XML file
     * @return true if this JxpBackend has an attached Context for this XML file
     */
    public boolean hasContext(File file) {
        return contexts.stream().anyMatch(c -> {
            try {
                return c.getFile() != null && c.getFile().getCanonicalPath().equals(file.getCanonicalPath());
            } catch (IOException e) {
                throw new PersistException(e);
            }
        });
    }

    /**
     * @param boundObject the mapped object
     * @return true if this JxpBackend has a Context mapped to this object
     */
    public boolean hasBoundContext(Object boundObject) {
        return boundContexts.stream().anyMatch(ctx -> ctx.getBindingObject().equals(boundObject));
    }

    public <E> Context.BindableContext<E> requireBoundContext(E boundObject) {
        Context.BindableContext<E> boundContext = getBoundContext(boundObject);

        if (boundContext != null) {
            return boundContext;
        } else {
            throw new IllegalStateException("No Context bound to object equal to " + boundObject);
        }
    }

    /**
     * @param boundObject the mapped object
     * @param <E>         the type of the object
     * @return the Context mapped to the provided object
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <E> Context.BindableContext<E> getBoundContext(E boundObject) {
        List<Context.BindableContext> found = boundContexts
            .stream()
            .filter(c -> c.getBindingObject().equals(boundObject))
            .collect(Collectors.toList());

        if (found.size() == 1) {
            // cast is safe since the type parameter is the class of the object the context is bound to
            return found.get(0);
        } else if (found.size() > 1) {
            throw new IllegalStateException("More than one Context bound to object equal to " + boundObject);
        }

        return null;
    }

    public CachedContext createCachedContext(String path) {
        return createCachedContext(new File(path));
    }

    /**
     * Create a new CachedContext without attaching it to this JxpBackend. This allows several contexts for the same
     * document in different threads, which is only safe for read-only operations, so be careful not to have threads override
     * changes made by a different thread.
     *
     * @param document the document to create a context for
     * @return the created cached context
     */
    public CachedContext createCachedContext(Document document) {
        return new CachedContext(this, document, logger);
    }

    /**
     * Create a new CachedContext without attaching it to this JxpBackend. This allows several contexts for the same
     * file in different threads, which is only safe for read-only operations, so be careful not to have threads override
     * changes made by a different thread.
     *
     * @param file the file to create a context for
     * @return the created cached context
     */
    public CachedContext createCachedContext(File file) {
        return new CachedContext(this, file, logger);
    }

    public LazyContext createLazyContext(String path) {
        return createLazyContext(new File(path));
    }

    /**
     * Create a new LazyContext without attaching it to this JxpBackend. This allows several contexts for the same
     * file in different threads, which is only safe for read-only operations, so be careful not to have threads override
     * changes made by a different thread.
     *
     * @param document the document to create a context for
     * @return the created lazy context
     */
    public LazyContext createLazyContext(Document document) {
        return new LazyContext(this, document, logger);
    }

    /**
     * Create a new LazyContext without attaching it to this JxpBackend. This allows several contexts for the same
     * file in different threads, which is only safe for read-only operations, so be careful not to have threads override
     * changes made by a different thread.
     *
     * @param file the file to create a context for
     * @return the created lazy context
     */
    public LazyContext createLazyContext(File file) {
        return new LazyContext(this, file, logger);
    }

    public <E> Context.BindableContext<E> createBoundContext(String path, E objectToBind) {
        return createBoundContext(new File(path), objectToBind);
    }

    public <E> Context.BindableContext<E> createBoundContext(Document document, E objectToBind) {
        return new BindableCachedContext<>(this, document, objectToBind, logger);
    }

    /**
     * Create a new BoundContext without attaching it to this JxpBackend. This allows several contexts for the same
     * file in different threads, which is only safe for read-only operations, so be careful not to have threads override
     * changes made by a different thread.
     *
     * @param file the file to create a context for
     * @return the created bound context
     */
    public <E> Context.BindableContext<E> createBoundContext(File file, E objectToBind) {
        return new BindableCachedContext<>(this, file, objectToBind, logger);
    }

    /**
     * Attach a Context that was create using one of the createContext methods or manually to this JxpBackend, ensuring
     * only one context exists for the same file / document and enabling it being returned by the getContext method
     *
     * @param context the context to attach
     */
    public void attachContext(Context context) {
        if (context.isPersistent() && hasContext(context.getFile())) {
            throw new PersistException("There already is a Context for file " + context.getFile());
        } else if (hasContext(context.getDocument())) {
            throw new PersistException("There already is a Context for document " + context.getDocument());
        }

        if (context instanceof Context.BindableContext) {
            boundContexts.add((Context.BindableContext) context);
        } else {
            contexts.add(context);
        }
    }

    public void attachContext(Context.BindableContext context) {
        if (hasBoundContext(context.getBindingObject())) {
            throw new PersistException("There already is a Context bound to object equal to " + context.getBindingObject());
        }

        if (context.isPersistent() && hasContext(context.getFile())) {
            throw new PersistException("There already is a Context for file " + context.getFile());
        } else if (hasContext(context.getDocument())) {
            throw new PersistException("There already is a Context for document " + context.getDocument());
        }

        boundContexts.add(context);
    }

    public void removeContext(String path) {
        Context context = requireExistingContext(path);
        contexts.remove(context);
    }

    public void removeContext(Document document) {
        Context context = requireExistingContext(document);
        contexts.remove(context);
    }

    public void removeContext(File file) {
        Context context = requireExistingContext(file);
        contexts.remove(context);
    }

    public void removeContext(Context context) {
        if (context instanceof Context.BindableContext) {
            boundContexts.remove(context);
        } else {
            contexts.remove(context);
        }
    }

    public void removeContext(Context.BindableContext context) {
        boundContexts.remove(context);
    }

    public <E> void removeBoundContext(E boundObject) {
        Context.BindableContext<E> context = requireBoundContext(boundObject);
        boundContexts.remove(context);
    }

    public void addListener(JxpEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(JxpEventListener listener) {
        listeners.remove(listener);
    }

    public boolean isListenersMuted() {
        return listenersMuted.get();
    }

    public void setListenersMuted(boolean listenersMuted) {
        this.listenersMuted.set(listenersMuted);
    }

    public void fireElementCreating(ElementCreatedEvent event) {
        listeners.forEach(emit(listener -> listener.elementCreating(event)));
    }

    public void fireElementDeleting(ElementDeletingEvent event) {
        listeners.forEach(emit(listener -> listener.elementDeleting(event)));
    }

    public void fireElementChanging(ElementChangingEvent event) {
        listeners.forEach(emit(listener -> listener.elementChanging(event)));
    }

    public void fireTransactionApplied(Transaction transaction) {
        listeners.forEach(emit(listener -> listener.transactionApplied(transaction)));
    }

    public void fireTransactionCommitted(Transaction transaction) {
        listeners.forEach(emit(listener -> listener.transactionCommitted(transaction)));
    }

    public void fireOnBeforeFlush(Transaction transaction) {
        listeners.forEach(emit(listener -> listener.onBeforeFlush(transaction)));
    }

    private Consumer<JxpEventListener> emit(Consumer<JxpEventListener> consumer) {
        return listener -> {
            if (!listenersMuted.get()) {
                try {
                    consumer.accept(listener);
                } catch (Throwable e) {
                    if (listener.mayInterrupt()) {
                        throw e;
                    } else {
                        logger.error("One of the EventListeners had an uncaught exception", e);
                    }
                }
            }
        };
    }

    public enum DefaultContextType {

        CACHED() {
            @Override
            public Context getContext(JxpBackend jxpBackend, File file, Logger logger) {
                return new CachedContext(jxpBackend, file, logger);
            }

            @Override
            public Context getContext(JxpBackend jxpBackend, Document document, Logger logger) {
                return new CachedContext(jxpBackend, document, logger);
            }
        },

        LAZY() {
            @Override
            public Context getContext(JxpBackend jxpBackend, File file, Logger logger) {
                return new LazyContext(jxpBackend, file, logger);
            }

            @Override
            public Context getContext(JxpBackend jxpBackend, Document document, Logger logger) {
                return new LazyContext(jxpBackend, document, logger);
            }
        };

        public abstract Context getContext(JxpBackend jxpBackend, File file, Logger logger);

        public abstract Context getContext(JxpBackend jxpBackend, Document document, Logger logger);

    }
}

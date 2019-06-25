package net.robinfriedli.jxp.api;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.ElementCreatedEvent;
import net.robinfriedli.jxp.events.ElementDeletingEvent;
import net.robinfriedli.jxp.events.EventListener;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.logging.LoggerSupplier;
import net.robinfriedli.jxp.persist.AbstractContext;
import net.robinfriedli.jxp.persist.BindableCachedContext;
import net.robinfriedli.jxp.persist.CachedContext;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.LazyContext;
import net.robinfriedli.jxp.persist.Transaction;
import org.w3c.dom.Document;

public class JxpBackend {

    private final List<Context> contexts;
    private final List<Context.BindableContext> boundContexts;
    private final List<EventListener> listeners;
    private final Logger logger;
    private final DefaultContextType defaultContextType;
    private boolean listenersMuted = false;

    public JxpBackend(List<EventListener> listeners,
                      DefaultContextType defaultContextType) {
        this(Lists.newArrayList(), Lists.newArrayList(), listeners, defaultContextType);
    }

    public JxpBackend(List<Context> contexts,
                      List<Context.BindableContext> boundContexts,
                      List<EventListener> listeners,
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

    public Context getContext(Document document) {
        Context existingContext = getExistingContext(document);

        if (existingContext != null) {
            return existingContext;
        } else {
            Class[] parameterTypes = new Class[]{JxpBackend.class, Document.class, Logger.class};
            Object[] parameters = new Object[]{this, document, logger};
            return instantiateDefaultContextType(parameterTypes, parameters);
        }
    }

    public Context getContext(File file) {
        Context existingContext = getExistingContext(file);

        if (existingContext != null) {
            return existingContext;
        } else {
            Class[] parameterTypes = new Class[]{JxpBackend.class, File.class, Logger.class};
            Object[] parameters = new Object[]{this, file, logger};
            return instantiateDefaultContextType(parameterTypes, parameters);
        }
    }

    @Nullable
    public Context getExistingContext(String path) {
        return getExistingContext(new File(path));
    }

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

    public boolean hasContext(Document document) {
        return contexts.stream().anyMatch(c -> c.getDocument().equals(document));
    }

    public boolean hasContext(File file) {
        return contexts.stream().anyMatch(c -> {
            try {
                return c.getFile() != null && c.getFile().getCanonicalPath().equals(file.getCanonicalPath());
            } catch (IOException e) {
                throw new PersistException(e);
            }
        });
    }

    public boolean hasBoundContext(Object boundObject) {
        return boundContexts.stream().anyMatch(ctx -> ctx.getBindingObject().equals(boundObject));
    }

    @SuppressWarnings("unchecked")
    public <E> Context.BindableContext<E> requireBoundContext(E boundObject) {
        Context.BindableContext<E> boundContext = getBoundContext(boundObject);

        if (boundContext != null) {
            return boundContext;
        } else {
            throw new IllegalStateException("No Context bound to object equal to " + boundObject);
        }
    }

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

    public Context createCachedContext(String path) {
        return createCachedContext(new File(path));
    }

    public Context createCachedContext(Document document) {
        if (hasContext(document)) {
            throw new PersistException("Document " + document + " is already used by a Context");
        }

        Context cachedContext = new CachedContext(this, document, logger);
        contexts.add(cachedContext);
        return cachedContext;
    }

    public Context createCachedContext(File file) {
        if (hasContext(file)) {
            throw new PersistException("File " + file + " is already used by a Context");
        }

        Context cachedContext = new CachedContext(this, file, logger);
        contexts.add(cachedContext);
        return cachedContext;
    }

    public Context createLazyContext(String path) {
        return createLazyContext(new File(path));
    }

    public Context createLazyContext(Document document) {
        if (hasContext(document)) {
            throw new PersistException("Document " + document + " is already used by a Context");
        }

        Context lazyContext = new LazyContext(this, document, logger);
        contexts.add(lazyContext);
        return lazyContext;
    }

    public Context createLazyContext(File file) {
        if (hasContext(file)) {
            throw new PersistException("File " + file + " is already used by Context");
        }

        Context lazyContext = new LazyContext(this, file, logger);
        contexts.add(lazyContext);
        return lazyContext;
    }

    public <E> Context.BindableContext<E> createBoundContext(String path, E objectToBind) {
        return createBoundContext(new File(path), objectToBind);
    }

    public <E> Context.BindableContext<E> createBoundContext(Document document, E objectToBind) {
        if (hasContext(document)) {
            throw new PersistException("Document " + document + " is already used by a Context");
        }

        Context.BindableContext<E> context = new BindableCachedContext<>(this, document, objectToBind, logger);
        boundContexts.add(context);
        return context;
    }

    public <E> Context.BindableContext<E> createBoundContext(File file, E objectToBind) {
        if (hasContext(file)) {
            throw new PersistException("File " + file + " is already used by a Context");
        }

        Context.BindableContext<E> context = new BindableCachedContext<>(this, file, objectToBind, logger);
        boundContexts.add(context);
        return context;
    }

    public void addContext(Context context) {
        if (context.isPersistent() && hasContext(context.getFile())) {
            throw new PersistException("There already is a Context for file " + context.getFile());
        } else if (hasContext(context.getDocument())) {
            throw new PersistException("There already is a Context for document " + context.getDocument());
        }

        contexts.add(context);
    }

    public void addBoundContext(Context.BindableContext context) {
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

    public <E> void removeBoundContext(E boundObject) {
        Context.BindableContext<E> context = requireBoundContext(boundObject);
        boundContexts.remove(context);
    }

    public void addListener(EventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(EventListener listener) {
        listeners.remove(listener);
    }

    public boolean isListenersMuted() {
        return listenersMuted;
    }

    public void setListenersMuted(boolean listenersMuted) {
        this.listenersMuted = listenersMuted;
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

    private Consumer<EventListener> emit(Consumer<EventListener> consumer) {
        return listener -> {
            if (!listenersMuted) {
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

    private Context instantiateDefaultContextType(Class[] parameterTypes, Object[] parameters) {
        try {
            Constructor<? extends AbstractContext> constructor
                = defaultContextType.getContextType().getConstructor(parameterTypes);
            Context context = constructor.newInstance(parameters);
            contexts.add(context);
            return context;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Exception instantiating default Context class", e);
        }
    }

    public enum DefaultContextType {
        CACHED(CachedContext.class), LAZY(LazyContext.class);

        private final Class<? extends AbstractContext> contextType;

        DefaultContextType(Class<? extends AbstractContext> contextType) {
            this.contextType = contextType;
        }

        public Class<? extends AbstractContext> getContextType() {
            return contextType;
        }

    }
}

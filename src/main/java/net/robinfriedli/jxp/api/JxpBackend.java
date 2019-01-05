package net.robinfriedli.jxp.api;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.ElementCreatedEvent;
import net.robinfriedli.jxp.events.ElementDeletingEvent;
import net.robinfriedli.jxp.events.EventListener;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.persist.BindableContextImpl;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.ContextImpl;
import net.robinfriedli.jxp.persist.DefaultPersistenceManager;
import net.robinfriedli.jxp.persist.Transaction;
import org.w3c.dom.Document;

public class JxpBackend {

    private final DefaultPersistenceManager persistenceManager;
    private final List<Context> contexts;
    private final List<Context.BindableContext> boundContexts;
    private final List<EventListener> listeners;
    private final Map<String, Class<? extends XmlElement>> instantiationContributions;
    private boolean listenersMuted = false;

    public JxpBackend(DefaultPersistenceManager persistenceManager,
                      List<EventListener> listeners,
                      Map<String, Class<? extends XmlElement>> instantiationContributions) {
        this.persistenceManager = persistenceManager;
        this.contexts = Lists.newArrayList();
        this.boundContexts = Lists.newArrayList();
        this.listeners = listeners;
        this.instantiationContributions = instantiationContributions;
    }

    public JxpBackend(DefaultPersistenceManager persistenceManager,
                      List<Context> contexts,
                      List<Context.BindableContext> boundContexts,
                      List<EventListener> listeners,
                      Map<String, Class<? extends XmlElement>> instantiationContributions) {
        this.persistenceManager = persistenceManager;
        this.contexts = contexts;
        this.boundContexts = boundContexts;
        this.listeners = listeners;
        this.instantiationContributions = instantiationContributions;
    }

    public Context getContext(String path) {
        return getContext(new File(path));
    }

    public Context getContext(Document document) {
        Context existingContext = getExistingContext(document);

        if (existingContext != null) {
            return existingContext;
        } else {
            Context context = new ContextImpl(this, persistenceManager, document);
            contexts.add(context);
            return context;
        }
    }

    public Context getContext(File file) {
        Context existingContext = getExistingContext(file);

        if (existingContext != null) {
            return existingContext;
        } else {
            Context context = new ContextImpl(this, persistenceManager, file);
            contexts.add(context);
            return context;
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

    public <E> Context.BindableContext<E> createBoundContext(String path, E objectToBind) {
        return createBoundContext(new File(path), objectToBind);
    }

    public <E> Context.BindableContext<E> createBoundContext(Document document, E objectToBind) {
        if (hasContext(document)) {
            throw new PersistException("Document " + document + " is already used by a Context");
        }

        Context.BindableContext<E> context = new BindableContextImpl<>(this, persistenceManager, document, objectToBind);
        boundContexts.add(context);
        return context;
    }

    public <E> Context.BindableContext<E> createBoundContext(File file, E objectToBind) {
        if (hasContext(file)) {
            throw new PersistException("File " + file + " is already used by a Context");
        }

        Context.BindableContext<E> context = new BindableContextImpl<>(this, persistenceManager, file, objectToBind);
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

    public <E> void removeBoundContext(E boundObject) {
        Context.BindableContext<E> context = requireBoundContext(boundObject);
        boundContexts.remove(context);
    }

    public Map<String, Class<? extends XmlElement>> getInstantiationContributions() {
        return instantiationContributions;
    }

    public void mapClass(String tagName, Class<? extends XmlElement> classToInstantiate) {
        instantiationContributions.put(tagName, classToInstantiate);
    }

    public void addListener(EventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(EventListener listener) {
        listeners.remove(listener);
    }

    public void setListenersMuted(boolean listenersMuted) {
        this.listenersMuted = listenersMuted;
    }

    public boolean isListenersMuted() {
        return listenersMuted;
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

    private Consumer<EventListener> emit(Consumer<EventListener> consumer) {
        return listener -> {
            if (!listenersMuted) {
                try {
                    consumer.accept(listener);
                } catch (Throwable e) {
                    System.err.println("One of the EventListeners had an uncaught exception:");
                    e.printStackTrace();
                }
            }
        };
    }
}

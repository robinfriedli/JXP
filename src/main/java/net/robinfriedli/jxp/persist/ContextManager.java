package net.robinfriedli.jxp.persist;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.ElementCreatedEvent;
import net.robinfriedli.jxp.events.ElementDeletingEvent;
import net.robinfriedli.jxp.events.EventListener;

/**
 * Manager to retrieve {@link Context} and register {@link EventListener}s
 */
public class ContextManager {

    private final String path;

    private final Context context;

    private final List<EventListener> listeners;

    private final List<Context.BindableContext> boundContexts = Lists.newArrayList();

    public ContextManager(String path, DefaultPersistenceManager persistenceManager) {
        this(path, persistenceManager, Lists.newArrayList());
    }

    public ContextManager(String path, DefaultPersistenceManager persistenceManager, List<EventListener> listeners) {
        this.path = path;
        this.listeners = listeners;
        this.context = new ContextImpl(this, persistenceManager, path);
    }

    public <E> ContextManager(String path, E bindingObject, DefaultPersistenceManager persistenceManager) {
        this(path, bindingObject, persistenceManager, Lists.newArrayList());
    }

    public <E> ContextManager(String path,
                              E bindingObject,
                              DefaultPersistenceManager persistenceManager,
                              List<EventListener> listeners) {
        this.path = path;
        this.listeners = listeners;
        this.context = new ContextImpl(this, persistenceManager, path);
        createBoundContext(bindingObject, persistenceManager);
    }

    public <E> ContextManager(String path, List<E> bindingObjects, DefaultPersistenceManager persistenceManager) {
        this(path, persistenceManager, bindingObjects, Lists.newArrayList());
    }

    public <E> ContextManager(String path,
                              DefaultPersistenceManager persistenceManager,
                              List<E> bindingObjects,
                              List<EventListener> listeners) {
        this.path = path;
        this.listeners = listeners;
        this.context = new ContextImpl(this, persistenceManager, path);
        createBoundContexts(bindingObjects, persistenceManager);
    }

    public String getPath() {
        return path;
    }

    public Context getContext() {
        return context;
    }

    public <E> void createBoundContext(E bindingObject, DefaultPersistenceManager persistenceManager) {
        Context.BindableContext<E> bindableContext = new BindableContextImpl<>(this, bindingObject, persistenceManager);
        boundContexts.add(bindableContext);
    }

    public <E> void createBoundContexts(List<E> bindingObjects, DefaultPersistenceManager persistenceManager) {
        List<Context.BindableContext> bindableContexts = Lists.newArrayList();

        for (E bindingObject : bindingObjects) {
            bindableContexts.add(new BindableContextImpl<>(this, bindingObject, persistenceManager));
        }

        boundContexts.addAll(bindableContexts);
    }

    public <E> Context getContext(E boundObject) {
        return getBindableContext(boundObject);
    }

    public <E> void destroyBoundContext(E boundObject) {
        Context.BindableContext<E> bindableContext = getBindableContext(boundObject);
        bindableContext.destroy();
        boundContexts.remove(bindableContext);
    }

    public void addListener(EventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(EventListener listener) {
        listeners.remove(listener);
    }

    public void fireElementCreating(ElementCreatedEvent event) {
        listeners.forEach(listener -> {
            try {
                listener.elementCreating(event);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    public void fireElementDeleting(ElementDeletingEvent event) {
        listeners.forEach(listener -> {
            try {
                listener.elementDeleting(event);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    public void fireElementChanging(ElementChangingEvent event) {
        listeners.forEach(listener -> {
            try {
                listener.elementChanging(event);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    public void fireTransactionApplied(Transaction transaction) {
        listeners.forEach(listener -> {
            try {
                listener.transactionApplied(transaction);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    public void fireTransactionCommitted(Transaction transaction) {
        listeners.forEach(listener -> {
            try {
                listener.transactionCommitted(transaction);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    private <E> Context.BindableContext<E> getBindableContext(E boundObject) {
        // cast is safe since we know context.getBindingObject() (which returns an Object of BindableContext's type parameter)
        // equals boundObject, so both must be of the same type
        @SuppressWarnings("unchecked")
        List<Context.BindableContext<E>> matchedContexts = boundContexts.stream()
                .filter(context -> context.getBindingObject().equals(boundObject))
                .map(c -> (Context.BindableContext<E>) c)
                .collect(Collectors.toList());

        if (matchedContexts.size() == 1) {
            return matchedContexts.get(0);
        } else if (matchedContexts.size() > 1) {
            throw new IllegalStateException("More than one Context bound to Object " + boundObject.toString());
        } else {
            throw new IllegalStateException("No Context bound to Object " + boundObject.toString());
        }
    }

}
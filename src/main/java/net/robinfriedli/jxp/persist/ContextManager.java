package net.robinfriedli.jxp.persist;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.events.*;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manager to retrieve {@link Context} and register {@link EventListener}s
 */
public class ContextManager {

    private final String path;

    private final Context context;

    private List<EventListener> listeners;

    @Nullable
    private List<Context.BindableContext> boundContexts;

    public ContextManager(String path, DefaultPersistenceManager persistenceManager) {
        this(path, persistenceManager, Lists.newArrayList());
    }

    public ContextManager(String path, DefaultPersistenceManager persistenceManager, List<EventListener> listeners) {
        this.path = path;
        this.boundContexts = null;
        this.listeners = listeners;
        this.context = new ContextImpl(this, persistenceManager, path);
    }

    public <E> ContextManager(String path, DefaultPersistenceManager persistenceManager, E bindingObject, String id) {
        this(path, persistenceManager, bindingObject, id, Lists.newArrayList());
    }

    public <E> ContextManager(String path,
                              DefaultPersistenceManager persistenceManager,
                              E bindingObject,
                              String id,
                              List<EventListener> listeners) {
        this.path = path;
        this.listeners = listeners;
        this.context = new ContextImpl(this, persistenceManager, path);
        createBoundContext(bindingObject, id, persistenceManager);
    }

    public <E> ContextManager(String path, DefaultPersistenceManager persistenceManager, Map<E, String> bindingObjectsWithId) {
        this(path, persistenceManager, bindingObjectsWithId, Lists.newArrayList());
    }

    public <E> ContextManager(String path,
                              DefaultPersistenceManager persistenceManager,
                              Map<E, String> bindingObjectsWithId,
                              List<EventListener> listeners) {
        this.path = path;
        this.listeners = listeners;
        this.context = new ContextImpl(this, persistenceManager, path);
        createBoundContexts(bindingObjectsWithId, persistenceManager);
    }

    public String getPath() {
        return path;
    }

    public Context getContext() {
        return context;
    }

    public <E> void createBoundContext(E bindingObject, String id, DefaultPersistenceManager persistenceManager) {
        Context.BindableContext<E> bindableContext = new BindableContextImpl<>(this, bindingObject, id, persistenceManager);
        if (boundContexts == null) {
            boundContexts = Lists.newArrayList(bindableContext);
        } else {
            boundContexts.add(bindableContext);
        }
    }

    public <E> void createBoundContexts(Map<E, String> bindingObjectsWithId, DefaultPersistenceManager persistenceManager) {
        List<Context.BindableContext> bindableContexts = Lists.newArrayList();

        for (E bindingObject : bindingObjectsWithId.keySet()) {
            bindableContexts.add(new BindableContextImpl<>(this, bindingObject, bindingObjectsWithId.get(bindingObject), persistenceManager));
        }

        if (boundContexts == null) {
            boundContexts = bindableContexts;
        } else {
            boundContexts.addAll(bindableContexts);
        }
    }

    public <E> Context getContext(E boundObject) {
        List<Context.BindableContext> matchedContexts = boundContexts.stream()
            .filter(context -> context.getBindingObject().equals(boundObject))
            .collect(Collectors.toList());

        if (matchedContexts.size() == 1) {
            return matchedContexts.get(0);
        } else if (matchedContexts.size() > 1) {
            throw new IllegalStateException("More than one Context bound to Object " + boundObject.toString());
        } else {
            throw new IllegalStateException("No Context bound to Object " + boundObject.toString());
        }
    }

    public void addListener(EventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(EventListener listener) {
        listeners.remove(listener);
    }

    public void fireElementCreating(ElementCreatedEvent event) {
        listeners.forEach(listener -> listener.elementCreating(event));
    }

    public void fireElementDeleting(ElementDeletingEvent event) {
        listeners.forEach(listener -> listener.elementDeleting(event));
    }

    public void fireElementChanging(ElementChangingEvent event) {
        listeners.forEach(listener -> listener.elementChanging(event));
    }

    public void fireTransactionApplied(List<Event> events, Context context) {
        listeners.forEach(listener -> listener.transactionApplied(events, context));
    }

    public void fireTransactionCommitted(List<Event> events, Context context) {
        listeners.forEach(listener -> listener.transactionCommitted(events, context));
    }

}

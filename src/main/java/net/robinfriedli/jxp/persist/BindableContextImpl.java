package net.robinfriedli.jxp.persist;

import java.io.File;

import net.robinfriedli.jxp.api.JxpBackend;
import org.w3c.dom.Document;

public class BindableContextImpl<E> extends ContextImpl implements Context.BindableContext<E> {

    private final E boundObject;


    public BindableContextImpl(JxpBackend backend, DefaultPersistenceManager persistenceManager, Document document, E boundObject) {
        super(backend, persistenceManager, document);
        this.boundObject = boundObject;
    }

    public BindableContextImpl(JxpBackend backend, DefaultPersistenceManager persistenceManager, File file, E boundObject) {
        super(backend, persistenceManager, file);
        this.boundObject = boundObject;
    }

    @Override
    public E getBindingObject() {
        return boundObject;
    }
}

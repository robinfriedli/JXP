package net.robinfriedli.jxp.persist;

import java.io.File;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.api.JxpBackend;
import net.robinfriedli.jxp.exceptions.PersistException;

public class BindableContextImpl<E> extends ContextImpl implements Context.BindableContext<E> {

    private final E boundObject;
    private final String id;
    @Nullable
    private final Context copyOf;

    // constrictor to create a BindableContext as a copy of a Context
    public BindableContextImpl(Context context, E boundObject, String id) {
        super(context.getBackend(), context.getPersistenceManager(), buildPath(context, id));
        this.boundObject = boundObject;
        this.id = id;

        if (!context.isPersistent()) {
            throw new PersistException("Can only copy persistent Context");
        }

        this.copyOf = context;
    }

    // constructor to create a BindableContext as a normal standalone Context
    public BindableContextImpl(JxpBackend backend, DefaultPersistenceManager persistenceManager, String path, E boundObject) {
        super(backend, persistenceManager, path);
        this.boundObject = boundObject;
        this.id = null;
        this.copyOf = null;
    }

    @Override
    public E getBindingObject() {
        return boundObject;
    }

    @Override
    public String getId() {
        return id;
    }

    @Nullable
    @Override
    public Context getCopyOf() {
        return copyOf;
    }

    private static String buildPath(Context context, String id) {
        // add the id of the BindableContext in front of the last element of the path: ./resources/2343240923elements.xml
        String standardPath = context.getPath();
        String[] pathElems = standardPath.split(File.separator);
        int lastElemIndex = pathElems.length - 1;
        String lastElem = pathElems[lastElemIndex];
        pathElems[lastElemIndex] = id + lastElem;
        String path = String.join(File.separator, pathElems);
        return replaceWhiteSpace(path);
    }

    private static String replaceWhiteSpace(String s) {
        String[] chars = s.split("(?!^)");

        for (int i = 0; i < chars.length; i++) {
            if (chars[i].equals(" ")) {
                chars[i] = "-";
            }
        }

        return String.join("", chars);
    }
}

package net.robinfriedli.jxp.persist;

import java.io.File;

public class BindableContextImpl<E> extends ContextImpl implements Context.BindableContext<E> {

    private final E boundObject;

    public BindableContextImpl(ContextManager manager, E boundObject, DefaultPersistenceManager persistenceManager) {
        super(manager, persistenceManager, buildPath(manager, boundObject.toString()));
        this.boundObject = boundObject;
    }

    @Override
    public E getBindingObject() {
        return boundObject;
    }

    private static String buildPath(ContextManager manager, String id) {
        // add the id of the BindableContext in front of the last element of the path: ./resources/2343240923elements.xml
        String standardPath = manager.getPath();
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

package net.robinfriedli.jxp.exec;

import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.api.JxpBackend;

public class ListenersMutedMode implements Invoker.ModeWrapper {

    private final JxpBackend jxpBackend;
    private Invoker.ModeWrapper delegate;

    public ListenersMutedMode(JxpBackend jxpBackend) {
        this.jxpBackend = jxpBackend;
    }

    @Override
    public <E> Callable<E> wrap(Callable<E> callable) {
        return () -> {
            boolean prevState = jxpBackend.isListenersMuted();
            jxpBackend.setListenersMuted(true);
            try {
                return callable.call();
            } finally {
                jxpBackend.setListenersMuted(prevState);
            }
        };
    }

    @Override
    public Invoker.ModeWrapper combine(Invoker.ModeWrapper mode) {
        setDelegate(mode);
        return mode;
    }

    @Nullable
    @Override
    public Invoker.ModeWrapper getDelegate() {
        return delegate;
    }

    @Override
    public void setDelegate(Invoker.ModeWrapper delegate) {
        this.delegate = delegate;
    }
}

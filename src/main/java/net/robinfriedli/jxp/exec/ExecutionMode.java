package net.robinfriedli.jxp.exec;

import java.util.concurrent.Callable;

import javax.annotation.Nullable;

public class ExecutionMode implements Invoker.ModeWrapper {

    private Invoker.ModeWrapper delegate;

    @Override
    public <E> Callable<E> wrap(Callable<E> callable) {
        return callable;
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

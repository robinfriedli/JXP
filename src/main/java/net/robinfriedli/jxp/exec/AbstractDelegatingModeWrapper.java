package net.robinfriedli.jxp.exec;

import javax.annotation.Nullable;

public abstract class AbstractDelegatingModeWrapper implements Invoker.ModeWrapper {

    private Invoker.ModeWrapper delegate;

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

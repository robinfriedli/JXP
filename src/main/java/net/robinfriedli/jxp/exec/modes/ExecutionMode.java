package net.robinfriedli.jxp.exec.modes;

import java.util.concurrent.Callable;

import net.robinfriedli.jxp.exec.AbstractDelegatingModeWrapper;

public class ExecutionMode extends AbstractDelegatingModeWrapper {

    @Override
    public <E> Callable<E> wrap(Callable<E> callable) {
        return callable;
    }

}

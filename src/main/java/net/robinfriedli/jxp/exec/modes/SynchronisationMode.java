package net.robinfriedli.jxp.exec.modes;

import java.util.concurrent.Callable;

import net.robinfriedli.jxp.exec.AbstractDelegatingModeWrapper;

/**
 * Mode that synchronises the execution of the wrapped task
 */
public class SynchronisationMode extends AbstractDelegatingModeWrapper {

    private final Object synchronisationLock;

    public SynchronisationMode(Object synchronisationLock) {
        this.synchronisationLock = synchronisationLock;
    }

    @Override
    public <E> Callable<E> wrap(Callable<E> callable) {
        return () -> {
            synchronized (synchronisationLock) {
                return callable.call();
            }
        };
    }
}

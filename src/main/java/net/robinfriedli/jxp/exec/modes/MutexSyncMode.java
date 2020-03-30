package net.robinfriedli.jxp.exec.modes;

import java.util.concurrent.Callable;

import net.robinfriedli.jxp.exec.AbstractDelegatingModeWrapper;
import net.robinfriedli.jxp.exec.MutexSync;

public class MutexSyncMode extends AbstractDelegatingModeWrapper {

    private final String mutexKey;

    public MutexSyncMode(String mutexKey) {
        this.mutexKey = mutexKey;
    }

    @Override
    public <E> Callable<E> wrap(Callable<E> callable) {
        return () -> MutexSync.GLOBAL_CONTEXT_SYNC.evaluateChecked(mutexKey, callable);
    }

}

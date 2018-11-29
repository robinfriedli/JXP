package net.robinfriedli.jxp.persist;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class QueuedTask<E> extends FutureTask<E> {

    private final boolean cancelOnFailure;

    public QueuedTask(Context context, boolean commit, boolean instantApply, boolean cancelOnFailure, boolean triggerListeners, Callable<E> callable) {
        super(() -> {
            if (triggerListeners) {
                return context.invoke(commit, instantApply, callable);
            } else {
                return context.invokeWithoutListeners(commit, instantApply, callable);
            }
        });
        this.cancelOnFailure = cancelOnFailure;
    }

    @Override
    public void run() {
        super.run();
        try {
            get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Exception thrown by QueuedTask:");
            e.printStackTrace();
        }
    }

    public boolean isCancelOnFailure() {
        return cancelOnFailure;
    }
}

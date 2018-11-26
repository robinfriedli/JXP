package net.robinfriedli.jxp.persist;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class QueuedTask<E> extends FutureTask<E> {

    private final Context context;
    private final boolean commit;
    private final boolean instantApply;
    private final boolean cancelOnFailure;
    private final boolean triggerListeners;

    public QueuedTask(Context context, boolean commit, boolean instantApply, boolean cancelOnFailure, boolean triggerListeners, Callable<E> callable) {
        super(callable);
        this.context = context;
        this.commit = commit;
        this.instantApply = instantApply;
        this.cancelOnFailure = cancelOnFailure;
        this.triggerListeners = triggerListeners;
    }

    public void execute() {
        if (triggerListeners) {
            context.invoke(commit, instantApply, this);
        } else {
            context.invokeWithoutListeners(commit, instantApply, this);
        }
    }

    public boolean isCancelOnFailure() {
        return cancelOnFailure;
    }
}

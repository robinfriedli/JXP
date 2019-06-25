package net.robinfriedli.jxp.persist;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            Logger logger = LoggerFactory.getLogger("JXP QueuedTasks");
            logger.error("Exception thrown by QueuedTask", e);
        }
    }

    public boolean isCancelOnFailure() {
        return cancelOnFailure;
    }
}

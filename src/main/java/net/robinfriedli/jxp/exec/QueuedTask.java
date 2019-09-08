package net.robinfriedli.jxp.exec;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;

import net.robinfriedli.jxp.persist.Context;

public class QueuedTask<E> extends FutureTask<E> {

    private final boolean cancelOnFailure;
    private final Logger logger;

    public QueuedTask(Context context, boolean commit, boolean instantApply, boolean cancelOnFailure, boolean triggerListeners, Callable<E> callable, Logger logger) {
        super(() -> {
            Invoker.Mode mode = Invoker.Mode.create();

            if (!triggerListeners) {
                mode.with(new ListenersMutedMode(context.getBackend()));
            }

            AbstractTransactionalMode transactionalMode = AbstractTransactionalMode.Builder.create()
                .setInstantApply(instantApply)
                .shouldCommit(commit)
                .build(context);
            mode.with(transactionalMode);
            return context.invoke(mode, callable);
        });
        this.cancelOnFailure = cancelOnFailure;
        this.logger = logger;
    }

    @Override
    public void run() {
        super.run();
        try {
            get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Exception thrown by QueuedTask", e);
        }
    }

    public boolean isCancelOnFailure() {
        return cancelOnFailure;
    }
}

package net.robinfriedli.jxp.exec;

import java.util.List;
import java.util.concurrent.Callable;

import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.exec.modes.ApplyOnlyMode;
import net.robinfriedli.jxp.exec.modes.CollectingApplyMode;
import net.robinfriedli.jxp.exec.modes.InstantApplyMode;
import net.robinfriedli.jxp.exec.modes.InstantApplyOnlyMode;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.Transaction;

public abstract class AbstractTransactionalMode extends AbstractDelegatingModeWrapper {

    private final Context context;
    private Transaction activeTransaction;
    private Transaction newTransaction;
    private Transaction oldTransaction;
    private boolean shouldCommit = true;
    private boolean writeToFile = true;

    protected AbstractTransactionalMode(Context context) {
        this.context = context;
    }

    @Override
    public <E> Callable<E> wrap(Callable<E> callable) {
        return () -> {
            activeTransaction = context.getTransaction();
            newTransaction = getTransaction();
            oldTransaction = null;
            if (activeTransaction != null) {
                switchTx();
            } else {
                openTx();
            }

            E returnValue;

            try {
                returnValue = callable.call();
                activeTransaction.apply();
                if (!activeTransaction.isApplyOnly()) {
                    if (shouldCommit) {
                        activeTransaction.commit(writeToFile);
                    } else {
                        context.getUncommittedTransactions().add(activeTransaction);
                    }
                }
            } catch (Throwable e) {
                activeTransaction.assertRollback();
                throw new PersistException(e.getClass().getSimpleName() + " thrown while running task. Closing transaction.", e);
            } finally {
                List<QueuedTask> queuedTasks = activeTransaction.getQueuedTasks();
                boolean failed = activeTransaction.failed();
                closeTx();

                queuedTasks.forEach(t -> {
                    if (failed && t.isCancelOnFailure()) {
                        t.cancel(false);
                    } else {
                        t.runLoggingErrors();
                    }
                });
            }

            return returnValue;
        };
    }

    public AbstractTransactionalMode shouldCommit(boolean shouldCommit) {
        this.shouldCommit = shouldCommit;
        return this;
    }

    public AbstractTransactionalMode writeToFile(boolean writeToFile) {
        this.writeToFile = writeToFile;
        return this;
    }

    protected abstract Transaction getTransaction();

    protected Context getContext() {
        return context;
    }

    private void closeTx() {
        context.setTransaction(oldTransaction);
    }

    private void openTx() {
        context.setTransaction(newTransaction);
        activeTransaction = newTransaction;
    }

    private void switchTx() {
        oldTransaction = activeTransaction;
        context.setTransaction(newTransaction);
        activeTransaction = newTransaction;
    }

    public static class Builder {

        private boolean isApplyOnly;
        private boolean isInstantApply = true;
        private boolean shouldCommit = true;
        private boolean writeToFile = true;

        public static Builder create() {
            return new Builder();
        }

        public AbstractTransactionalMode build(Context context) {
            AbstractTransactionalMode mode;
            if (isInstantApply && isApplyOnly) {
                mode = new InstantApplyOnlyMode(context);
            } else if (isInstantApply) {
                mode = new InstantApplyMode(context);
            } else if (isApplyOnly) {
                mode = new ApplyOnlyMode(context);
            } else {
                mode = new CollectingApplyMode(context);
            }

            return mode.shouldCommit(shouldCommit).writeToFile(writeToFile);
        }

        public Builder setInstantApply(boolean instantApply) {
            isInstantApply = instantApply;
            return this;
        }

        public Builder setApplyOnly(boolean applyOnly) {
            isApplyOnly = applyOnly;
            return this;
        }

        public Builder shouldCommit(boolean shouldCommit) {
            this.shouldCommit = shouldCommit;
            return this;
        }

        public Builder writeToFile(boolean writeToFile) {
            this.writeToFile = writeToFile;
            return this;
        }
    }

}

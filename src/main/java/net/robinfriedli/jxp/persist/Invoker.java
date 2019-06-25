package net.robinfriedli.jxp.persist;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;

import net.robinfriedli.jxp.exceptions.CommitException;
import net.robinfriedli.jxp.exceptions.PersistException;

class Invoker {

    private final Context context;
    private final Logger logger;

    Invoker(Context context, Logger logger) {
        this.context = context;
        this.logger = logger;
    }

    <E> E invoke(Mode mode, int sequence, Callable<E> task) {
        Transaction transaction = context.getTransaction();
        Class<? extends Transaction> transactionType = mode.getTransactionType();
        boolean nested = false;
        if (transaction != null) {
            if (transaction.isActive()) {
                if (transaction.getClass().equals(transactionType)) {
                    nested = true;
                } else {
                    throw new IllegalStateException("A transaction of a different type is already running.");
                }
            } else {
                throw new PersistException("Context has a current Transaction that is not recording anymore. Use Context#futureInvoke to run your task after this has finished.");
            }
        } else {
            try {
                if (mode == Mode.SEQUENTIAL) {
                    transaction = mode.getTransactionType()
                        .getConstructor(Context.class, Integer.class)
                        .newInstance(context, sequence);
                } else {
                    transaction = mode.getTransactionType().getConstructor(Context.class).newInstance(context);
                }
                context.setTransaction(transaction);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new PersistException("Exception creating Transaction", e);
            }
        }

        E returnValue;

        try {
            returnValue = task.call();
        } catch (Throwable e) {
            if (mode.instantApply) {
                transaction.rollback();
            } else {
                transaction.fail();
            }
            closeTx();
            throw new PersistException(e.getClass().getName() + " thrown during task run. Closing transaction.", e);
        }

        if (!nested) {
            try {
                if (mode.instantApply) {
                    transaction.setState(Transaction.State.APPLIED);
                    context.getBackend().fireTransactionApplied(transaction);
                } else {
                    transaction.apply();
                }
                if (mode.isCommit()) {
                    try {
                        transaction.commit();
                    } catch (CommitException e) {
                        logger.error("Exception during commit of transaction " + transaction, e);
                    }
                } else {
                    context.getUncommittedTransactions().add(transaction);
                }
            } finally {
                List<QueuedTask> queuedTasks = transaction.getQueuedTasks();
                boolean failed = transaction.failed();
                closeTx();

                queuedTasks.forEach(t -> {
                    if (failed && t.isCancelOnFailure()) {
                        t.cancel(false);
                    } else {
                        t.run();
                    }
                });
            }
        }

        return returnValue;
    }

    private void closeTx() {
        context.setTransaction(null);
    }

    enum Mode {

        INSTANT_APPLY(InstantApplyTx.class, true, true),
        COLLECTING_APPLY(Transaction.class, true, false),
        APPLY_ONLY(ApplyOnlyTx.class, false, false),
        INSTANT_APPLY_ONLY(InstantApplyOnlyTx.class, false, true),
        SEQUENTIAL(SequentialTx.class, true, true);

        final Class<? extends Transaction> transactionType;
        final boolean commit;
        final boolean instantApply;

        Mode(Class<? extends Transaction> transactionType, boolean commit, boolean instantApply) {
            this.transactionType = transactionType;
            this.commit = commit;
            this.instantApply = instantApply;
        }

        Class<? extends Transaction> getTransactionType() {
            return transactionType;
        }

        boolean isCommit() {
            return commit;
        }

        boolean isInstantApply() {
            return instantApply;
        }

    }

}

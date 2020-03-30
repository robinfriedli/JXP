package net.robinfriedli.jxp.events;

import java.util.List;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.JxpBackend;
import net.robinfriedli.jxp.exec.QueuedTask;
import net.robinfriedli.jxp.persist.BaseTransaction;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.Transaction;

/**
 * Event wrapper used to apply changes to non-physical elements, meaning they don't have to be committed.
 */
public class VirtualEvent extends Event {

    private final Event event;

    public VirtualEvent(Event event) {
        super(event.getSource());
        this.event = event;
    }

    public static VirtualEvent wrap(Event event) {
        return new VirtualEvent(event);
    }

    /**
     * Swaps the current Transaction for a TransactionMock that wraps each added Event into a VirtualEvent, then adds
     * those to the actual Transaction. Used during a transaction to make changes to non-physical elements
     *
     * @param runnable the runnable in which the Events are created
     */
    public static void interceptTransaction(Runnable runnable, Context context) {
        Transaction transaction = context.getTransaction();
        // if the transaction is apply-only VirtualEvents are not required anyway
        if (transaction == null || !transaction.isApplyOnly()) {
            List<VirtualEvent> virtualEvents = Lists.newArrayList();
            TransactionMock transactionMock = new TransactionMock(context, virtualEvents, transaction == null || transaction.isInstantApply());

            context.setTransaction(transactionMock);
            runnable.run();
            if (transaction != null) {
                transaction.getChanges().addAll(virtualEvents);
            }

            if (!transactionMock.getQueuedTasks().isEmpty()) {
                for (QueuedTask queuedTask : transactionMock.getQueuedTasks()) {
                    if (transaction != null) {
                        transaction.queueTask(queuedTask);
                    } else {
                        queuedTask.run();
                    }
                }
            }

            context.setTransaction(transaction);
        }
    }

    @Override
    public void apply() {
        JxpBackend backend = event.getSource().getContext().getBackend();
        backend.setListenersMuted(true);
        try {
            event.apply();
        } finally {
            backend.setListenersMuted(false);
        }
    }

    @Override
    public void revert() {
        event.revert();
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean isApplied() {
        return event.isApplied();
    }

    @Override
    public boolean isCommitted() {
        return event.isCommitted();
    }

    private static class TransactionMock extends BaseTransaction {

        private List<VirtualEvent> virtualEvents;
        private boolean instantApply;

        private TransactionMock(Context context, List<VirtualEvent> virtualEvents, boolean instantApply) {
            super(context);
            this.virtualEvents = virtualEvents;
            this.instantApply = instantApply;
        }

        @Override
        public void addChange(Event event) {
            isEmpty = false;
            VirtualEvent wrappedEvent = VirtualEvent.wrap(event);
            virtualEvents.add(wrappedEvent);
            if (instantApply) {
                wrappedEvent.apply();
            }
        }

        @Override
        public void addChanges(List<Event> events) {
            events.forEach(this::addChange);
        }

        @Override
        public void apply() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void commit() {
            throw new UnsupportedOperationException();
        }

    }

}

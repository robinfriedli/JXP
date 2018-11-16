package net.robinfriedli.jxp.events;

import java.util.List;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.DefaultPersistenceManager;
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

    @Override
    public void apply() {
        event.apply();
    }

    @Override
    public void revert() {
        event.revert();
    }

    @Override
    public void commit(DefaultPersistenceManager persistenceManager) {
    }

    @Override
    public boolean isApplied() {
        return event.isApplied();
    }

    @Override
    public boolean isCommitted() {
        return event.isCommitted();
    }

    public static VirtualEvent wrap(Event event) {
        return new VirtualEvent(event);
    }

    /**
     * Swaps the current Transaction for a TransactionMock that wraps each added Event into a VirtualEvent, then adds
     * those to the actual Transaction. Used during a transaction to make changes to non-physical elements
     *
     * @param transaction the actual Transaction
     * @param runnable the runnable in which the Events are created
     */
    public static void interceptTransaction(Transaction transaction, Runnable runnable) {
        // if the transaction is apply-only VirtualEvents are not required anyway
        if (!transaction.isApplyOnly()) {
            Context context = transaction.getContext();
            List<VirtualEvent> virtualEvents = Lists.newArrayList();
            TransactionMock transactionMock = new TransactionMock(context, virtualEvents, transaction.isInstantApply());

            context.setTransaction(transactionMock);
            runnable.run();
            transaction.getChanges().addAll(virtualEvents);
            context.setTransaction(transaction);
        }
    }

    private static class TransactionMock extends Transaction {

        private List<VirtualEvent> virtualEvents;
        private boolean instantApply;

        private TransactionMock(Context context, List<VirtualEvent> virtualEvents, boolean instantApply) {
            super(context, Lists.newArrayList());
            this.virtualEvents = virtualEvents;
            this.instantApply = instantApply;
        }

        @Override
        public void addChange(Event event) {
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
        public void commit(DefaultPersistenceManager defaultPersistenceManager) {
            throw new UnsupportedOperationException();
        }

    }

}

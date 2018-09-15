package net.robinfriedli.jxp.events;


import net.robinfriedli.jxp.persist.Transaction;

public abstract class EventListener {

    /**
     * Fired when an {@link ElementCreatedEvent} was applied
     *
     * @param event the ElementCreatedEvent
     */
    public void elementCreating(ElementCreatedEvent event) {
    }

    /**
     * Fired when an {@link ElementDeletingEvent} was applied
     *
     * @param event the ElementDeletingEvent
     */
    public void elementDeleting(ElementDeletingEvent event) {
    }

    /**
     * Fired when an {@link ElementChangingEvent} was applied
     *
     * @param event the ElementChangingEvent
     */
    public void elementChanging(ElementChangingEvent event) {
    }

    /**
     * Fired after a {@link net.robinfriedli.jxp.persist.Transaction} has been applied (to the in memory elements)
     *
     * @param transaction the transaction that has been applied
     */
    public void transactionApplied(Transaction transaction) {
    }

    /**
     * Fired after a {@link net.robinfriedli.jxp.persist.Transaction} has been committed (to the XML file)
     *
     * @param transaction the transaction that has been committed
     */
    public void transactionCommitted(Transaction transaction) {
    }

}

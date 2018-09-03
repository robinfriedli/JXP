package net.robinfriedli.jxp.events;


import java.util.List;

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
     * @param events all {@link Event}s that were applied
     */
    public void transactionApplied(List<Event> events) {
    }

    /**
     * Fired after a {@link net.robinfriedli.jxp.persist.Transaction} has been committed (to the XML file)
     *
     * @param events all {@link Event}s that were committed
     */
    public void transactionCommitted(List<Event> events) {
    }

}

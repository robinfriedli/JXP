package net.robinfriedli.jxp.persist;

import java.util.List;

import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.Event;
import net.robinfriedli.jxp.exceptions.PersistException;

/**
 * Combination of {@link InstantApplyTx} and {@link ApplyOnlyTx}
 */
public class InstantApplyOnlyTx extends InstantApplyTx {

    public InstantApplyOnlyTx(Context context, List<Event> changes) {
        super(context, changes);
    }

    @Override
    public void commit(DefaultPersistenceManager manager) {
        throw new UnsupportedOperationException("Attempting to commit an apply-only Transaction");
    }

    @Override
    void applyChange(Event change) {
        try {
            change.apply();
            if (change instanceof ElementChangingEvent) {
                change.getSource().removeChange((ElementChangingEvent) change);
            }
        } catch (PersistException | UnsupportedOperationException e) {
            rollback();
            throw new PersistException("Exception while applying change. Transaction rolled back", e);
        }
    }
}

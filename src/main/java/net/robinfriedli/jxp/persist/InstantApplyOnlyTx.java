package net.robinfriedli.jxp.persist;

import java.util.List;

import net.robinfriedli.jxp.events.Event;

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
}

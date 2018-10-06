package net.robinfriedli.jxp.persist;

import net.robinfriedli.jxp.events.Event;
import net.robinfriedli.jxp.exceptions.PersistException;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * A transaction that applies all changes as soon as they are added rather than after the task finished. Useful if the
 * changes are needed instantly but negatively affects performance. See {@link Context#invoke(boolean, boolean, Callable)}
 */
public class InstantApplyTx extends Transaction {

    public InstantApplyTx(Context context, List<Event> changes) {
        super(context, changes);
    }

    @Override
    public void addChange(Event change) {
        super.addChange(change);
        applyChange(change);
    }

    @Override
    public void addChanges(List<Event> changes) {
        super.addChanges(changes);
        changes.forEach(this::applyChange);
    }

    @Override
    public void apply() {
        throw new UnsupportedOperationException("Calling apply() not allowed on " + getClass().getName());
    }

    void applyChange(Event change) {
        try {
            change.apply();
        } catch (PersistException | UnsupportedOperationException e) {
            rollback();
            throw new PersistException("Exception while applying change. Transaction rolled back");
        }
    }
}

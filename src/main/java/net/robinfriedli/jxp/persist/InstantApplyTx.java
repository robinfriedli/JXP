package net.robinfriedli.jxp.persist;

import java.util.List;
import java.util.concurrent.Callable;

import net.robinfriedli.jxp.events.Event;

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
        change.apply();
    }

    @Override
    public void addChanges(List<Event> changes) {
        changes.forEach(this::addChange);
    }

    @Override
    public void apply() {
        throw new UnsupportedOperationException("Calling apply() not allowed on " + getClass().getName());
    }
}

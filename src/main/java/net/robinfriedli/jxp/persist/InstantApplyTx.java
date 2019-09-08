package net.robinfriedli.jxp.persist;

import java.util.List;
import java.util.concurrent.Callable;

import net.robinfriedli.jxp.events.Event;

/**
 * A transaction that applies all changes as soon as they are added rather than after the task finished. Useful if the
 * changes are needed instantly but might negatively affect performance ever so slightly.
 * See {@link Context#invoke(boolean, boolean, Callable)}
 */
public class InstantApplyTx extends BaseTransaction {

    public InstantApplyTx(Context context) {
        super(context);
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
        setState(State.APPLIED);
        getContext().getBackend().fireTransactionApplied(this);
    }

    @Override
    public boolean isInstantApply() {
        return true;
    }
}

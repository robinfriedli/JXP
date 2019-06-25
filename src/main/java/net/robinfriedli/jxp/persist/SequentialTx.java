package net.robinfriedli.jxp.persist;

import net.robinfriedli.jxp.events.Event;
import net.robinfriedli.jxp.exceptions.CommitException;
import net.robinfriedli.jxp.exceptions.PersistException;

public class SequentialTx extends InstantApplyTx {

    private final int sequence;

    public SequentialTx(Context context, Integer sequence) {
        super(context);
        this.sequence = sequence;
    }

    @Override
    public void addChange(Event change) {
        if (getChanges().size() == sequence) {
            try {
                flush();
            } catch (CommitException e) {
                throw new PersistException("Exception in flush", e);
            }
        }
        super.addChange(change);
    }
}

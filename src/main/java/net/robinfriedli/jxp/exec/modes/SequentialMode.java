package net.robinfriedli.jxp.exec.modes;

import net.robinfriedli.jxp.exec.AbstractTransactionalMode;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.SequentialTx;
import net.robinfriedli.jxp.persist.Transaction;

public class SequentialMode extends AbstractTransactionalMode {

    private final int sequence;

    public SequentialMode(Context context, int sequence) {
        super(context);
        this.sequence = sequence;
    }

    @Override
    protected Transaction getTransaction() {
        return new SequentialTx(getContext(), sequence);
    }
}

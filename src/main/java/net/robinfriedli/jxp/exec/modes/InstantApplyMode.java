package net.robinfriedli.jxp.exec.modes;

import net.robinfriedli.jxp.exec.AbstractTransactionalMode;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.InstantApplyTx;
import net.robinfriedli.jxp.persist.Transaction;

public class InstantApplyMode extends AbstractTransactionalMode {

    public InstantApplyMode(Context context) {
        super(context);
    }

    @Override
    protected Transaction getTransaction() {
        return new InstantApplyTx(getContext());
    }
}

package net.robinfriedli.jxp.exec.modes;

import net.robinfriedli.jxp.exec.AbstractTransactionalMode;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.InstantApplyOnlyTx;
import net.robinfriedli.jxp.persist.Transaction;

public class InstantApplyOnlyMode extends AbstractTransactionalMode {

    public InstantApplyOnlyMode(Context context) {
        super(context);
    }

    @Override
    protected Transaction getTransaction() {
        return new InstantApplyOnlyTx(getContext());
    }
}

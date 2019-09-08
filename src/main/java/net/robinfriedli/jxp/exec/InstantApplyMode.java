package net.robinfriedli.jxp.exec;

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

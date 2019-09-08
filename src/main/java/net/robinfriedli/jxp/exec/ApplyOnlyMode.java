package net.robinfriedli.jxp.exec;

import net.robinfriedli.jxp.persist.ApplyOnlyTx;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.Transaction;

public class ApplyOnlyMode extends AbstractTransactionalMode {

    public ApplyOnlyMode(Context context) {
        super(context);
    }

    @Override
    protected Transaction getTransaction() {
        return new ApplyOnlyTx(getContext());
    }
}

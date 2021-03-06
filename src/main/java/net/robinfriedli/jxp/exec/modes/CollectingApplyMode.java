package net.robinfriedli.jxp.exec.modes;

import net.robinfriedli.jxp.exec.AbstractTransactionalMode;
import net.robinfriedli.jxp.persist.BaseTransaction;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.Transaction;

public class CollectingApplyMode extends AbstractTransactionalMode {

    public CollectingApplyMode(Context context) {
        super(context);
    }

    @Override
    protected Transaction getTransaction() {
        return new BaseTransaction(getContext());
    }
}

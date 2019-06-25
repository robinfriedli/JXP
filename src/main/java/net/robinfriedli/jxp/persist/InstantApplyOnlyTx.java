package net.robinfriedli.jxp.persist;

/**
 * Combination of {@link InstantApplyTx} and {@link ApplyOnlyTx}
 */
public class InstantApplyOnlyTx extends InstantApplyTx {

    public InstantApplyOnlyTx(Context context) {
        super(context);
    }

    @Override
    public void commit() {
        throw new UnsupportedOperationException("Attempting to commit an apply-only Transaction");
    }
}

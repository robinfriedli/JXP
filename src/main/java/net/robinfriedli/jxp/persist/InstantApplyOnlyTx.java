package net.robinfriedli.jxp.persist;

/**
 * Combination of {@link InstantApplyTx} and {@link ApplyOnlyTx}
 */
public class InstantApplyOnlyTx extends InstantApplyTx {

    public InstantApplyOnlyTx(Context context) {
        super(context);
    }

    @Override
    public boolean isApplyOnly() {
        return true;
    }

    @Override
    protected InstantApplyInternalControl createControl() {
        return new InternalControl();
    }

    private class InternalControl extends InstantApplyInternalControl {

        @Override
        public void commit(boolean writeToFile) {
            throw new UnsupportedOperationException("Attempting to commit an apply-only Transaction");
        }

    }

}

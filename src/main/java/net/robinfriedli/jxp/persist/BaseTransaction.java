package net.robinfriedli.jxp.persist;

public class BaseTransaction extends AbstractTransaction {

    private final DefaultInternalControl internalControl = new DefaultInternalControl();

    public BaseTransaction(Context context) {
        super(context);
    }

    @Override
    public Internals internal() {
        return internalControl;
    }

}

package net.robinfriedli.jxp.events;

import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.persist.DefaultPersistenceManager;

public class HelperEvent extends Event {

    public HelperEvent(XmlElement source) {
        super(source);
    }

    @Override
    public void apply() {
        throw new UnsupportedOperationException("Cannot apply " + this.getClass().getName());
    }

    @Override
    public void revert() {
        throw new UnsupportedOperationException("Cannot revert " + this.getClass().getName());
    }

    @Override
    public void commit(DefaultPersistenceManager persistenceManager) {
        throw new UnsupportedOperationException("Cannot commit " + this.getClass().getName());
    }
}

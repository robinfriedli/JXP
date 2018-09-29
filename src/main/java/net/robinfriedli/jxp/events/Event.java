package net.robinfriedli.jxp.events;

import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.CommitException;
import net.robinfriedli.jxp.persist.DefaultPersistenceManager;

public abstract class Event {

    private final XmlElement source;

    private boolean applied = false;

    private boolean committed = false;

    public Event(XmlElement source) {
        this.source = source;
    }

    public XmlElement getSource() {
        return this.source;
    }

    public abstract void apply();

    public abstract void revert();

    public abstract void commit(DefaultPersistenceManager persistenceManager) throws CommitException;

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    public boolean isApplied() {
        return applied;
    }

    public void setCommitted(boolean committed) {
        this.committed = committed;
    }

    public boolean isCommitted() {
        return committed;
    }
}

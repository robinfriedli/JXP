package net.robinfriedli.jxp.events;

import net.robinfriedli.jxp.api.XmlElement;

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

    public abstract void commit();

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    public boolean isCommitted() {
        return committed;
    }

    public void setCommitted(boolean committed) {
        this.committed = committed;
    }
}

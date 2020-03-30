package net.robinfriedli.jxp.events;

import net.robinfriedli.jxp.api.XmlElement;

public class TextContentChangingEvent extends ElementChangingEvent {

    private final String oldValue;
    private final String newValue;

    public TextContentChangingEvent(XmlElement source, String oldValue, String newValue) {
        super(source);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public void doCommit() {
        getSource().requireElement().setTextContent(newValue);
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    @Override
    protected void revertCommit() {
        getSource().requireElement().setTextContent(oldValue);
    }

}

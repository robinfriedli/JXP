package net.robinfriedli.jxp.events;

import net.robinfriedli.jxp.api.XmlElement;

public class ValueChangingEvent<V> extends HelperEvent {

    private final V oldValue;
    private final V newValue;

    public ValueChangingEvent(XmlElement source, V oldValue, V newValue) {
        super(source);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public V getOldValue() {
        return oldValue;
    }

    public V getNewValue() {
        return newValue;
    }
}

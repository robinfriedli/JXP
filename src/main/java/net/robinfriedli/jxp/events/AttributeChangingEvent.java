package net.robinfriedli.jxp.events;

import net.robinfriedli.jxp.api.XmlAttribute;

public class AttributeChangingEvent extends HelperEvent {

    private final XmlAttribute attribute;
    private final String oldValue;
    private final String newValue;

    public AttributeChangingEvent(XmlAttribute attribute, String newValue) {
        super(attribute.getParentElement());
        this.attribute = attribute;
        oldValue = String.valueOf(attribute.getValue());
        this.newValue = newValue;
    }

    public XmlAttribute getAttribute() {
        return attribute;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }
}

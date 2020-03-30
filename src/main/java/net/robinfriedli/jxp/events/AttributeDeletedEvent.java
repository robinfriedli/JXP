package net.robinfriedli.jxp.events;

import net.robinfriedli.jxp.api.XmlAttribute;
import net.robinfriedli.jxp.api.XmlElement;

public class AttributeDeletedEvent extends ElementChangingEvent {

    private final XmlAttribute attribute;

    public AttributeDeletedEvent(XmlElement source, XmlAttribute attribute) {
        super(source);
        this.attribute = attribute;
    }

    @Override
    public void doCommit() {
        getSource().requireElement().removeAttribute(attribute.getAttributeName());
    }

    public XmlAttribute getAttribute() {
        return attribute;
    }

    @Override
    protected void revertCommit() {
        getSource().requireElement().setAttribute(attribute.getAttributeName(), attribute.getValue());
    }
}

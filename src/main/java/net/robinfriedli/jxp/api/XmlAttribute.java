package net.robinfriedli.jxp.api;

import net.robinfriedli.jxp.events.AttributeChangingEvent;
import net.robinfriedli.jxp.events.ElementChangingEvent;

public class XmlAttribute {

    private final XmlElement parentElement;
    private final String attributeName;
    private String value;

    public XmlAttribute(XmlElement parentElement, String attributeName) {
        this.parentElement = parentElement;
        this.attributeName = attributeName;
        this.value = "";
    }

    public XmlAttribute(XmlElement parentElement, String attributeName, String value) {
        this.parentElement = parentElement;
        this.attributeName = attributeName;
        this.value = value;
    }

    public XmlElement getParentElement() {
        return this.parentElement;
    }

    public String getAttributeName() {
        return this.attributeName;
    }

    public void setValue(Object value) {
        String stringValue = value instanceof String ? (String) value : StringConverter.reverse(value);
        parentElement.addChange(ElementChangingEvent.attributeChange(new AttributeChangingEvent(this, stringValue)));
    }

    public String getValue() {
        return this.value;
    }

    public <V> V getValue(Class<V> target) {
        return StringConverter.convert(value, target);
    }

    public int getInt() {
        return StringConverter.convert(value, Integer.class);
    }

    public boolean getBool() {
        return StringConverter.convert(value, Boolean.class);
    }

    public float getFloat() {
        return StringConverter.convert(value, Float.class);
    }

    public void applyChange(AttributeChangingEvent change) throws UnsupportedOperationException {
        applyChange(change, false);
    }

    public void revertChange(AttributeChangingEvent change) {
        applyChange(change, true);
    }

    @Override
    public String toString() {
        return "XmlAttribute:" + getAttributeName() + "@" + getParentElement().getTagName();
    }

    private void applyChange(AttributeChangingEvent change, boolean isRollback) {
        if (change.getAttribute() != this) {
            throw new UnsupportedOperationException("Change can't be applied to this XmlAttribute since the change does not refer to this attribute");
        }

        if (isRollback) {
            value = change.getOldValue();
            if (change.isCommitted() && parentElement.isPersisted()) {
                parentElement.requireElement().setAttribute(attributeName, change.getOldValue());
            }
        } else {
            value = change.getNewValue();
        }
        change.setApplied(true);
    }

}

package net.robinfriedli.jxp.events;

import java.util.List;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.PersistException;

public abstract class ElementChangingEvent extends Event {

    public ElementChangingEvent(XmlElement source) {
        super(source);
    }

    @Nullable
    public AttributeChangingEvent getAttributeChange() {
        return unwrap(AttributeChangingEvent.class);
    }

    @Nullable
    public AttributeDeletedEvent getAttributeDeletion() {
        return unwrap(AttributeDeletedEvent.class);
    }

    @Nullable
    public TextContentChangingEvent getChangedTextContent() {
        return unwrap(TextContentChangingEvent.class);
    }

    @Nullable
    public <E extends ElementChangingEvent> E unwrap(Class<E> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }

        return null;
    }

    @Override
    public void apply() {
        if (isApplied()) {
            throw new PersistException("Change has already been applied");
        } else {
            getSource().applyChange(this);
            setApplied(true);
            getSource().getContext().getBackend().fireElementChanging(this);
        }
    }

    @Nullable
    @Deprecated
    public List<AttributeChangingEvent> getChangedAttributes() {
        return null;
    }

    @Override
    public void revert() {
        if (isApplied()) {
            getSource().revertChange(this);
            if (isCommitted()) {
                revertCommit();
            } else {
                getSource().removeChange(this);
            }
        }
    }

    protected abstract void doCommit();

    protected abstract void revertCommit();

    @Override
    public void commit() {
        doCommit();

        setCommitted(true);
        getSource().removeChange(this);
    }

    @Deprecated
    public boolean isEmpty() {
        return false;
    }

    public boolean attributeChanged(String attributeName) {
        AttributeChangingEvent attributeChange = getAttributeChange();
        if (attributeChange != null) {
            return attributeChange.getAttribute().getAttributeName().equals(attributeName);
        }

        return false;
    }

    public AttributeChangingEvent getAttributeChange(String attributeName) {
        AttributeChangingEvent attributeChange = getAttributeChange();
        if (attributeChange != null && attributeChange.getAttribute().getAttributeName().equals(attributeName)) {
            return attributeChange;
        }

        return null;
    }

    public boolean textContentChanged() {
        return getChangedTextContent() != null;
    }

}

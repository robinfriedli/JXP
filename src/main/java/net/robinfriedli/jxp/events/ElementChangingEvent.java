package net.robinfriedli.jxp.events;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.CommitException;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.persist.DefaultPersistenceManager;

public class ElementChangingEvent extends Event {

    @Nullable
    private final List<AttributeChangingEvent> changedAttributes;

    @Nullable
    private final ValueChangingEvent<String> changedTextContent;

    public ElementChangingEvent(AttributeChangingEvent changedAttribute) {
        this(changedAttribute.getSource(), Lists.newArrayList(changedAttribute), null);
    }

    public ElementChangingEvent(XmlElement source, List<AttributeChangingEvent> changedAttributes) {
        this(source, changedAttributes, null);
    }

    public ElementChangingEvent(ValueChangingEvent<String> changedTextContent) {
        this(changedTextContent.getSource(), null, changedTextContent);
    }

    public ElementChangingEvent(XmlElement source,
                                @Nullable List<AttributeChangingEvent> changedAttributes,
                                @Nullable ValueChangingEvent<String> changedTextContent) {
        super(source);
        this.changedAttributes = changedAttributes;
        this.changedTextContent = changedTextContent;

        if (changedAttributes != null && changedAttributes.stream().anyMatch(a -> a.getSource() != getSource())) {
            throw new UnsupportedOperationException("Attempting to pass an attribute change to an element change of a different source");
        }

        if (changedTextContent != null && changedTextContent.getSource() != getSource()) {
            throw new UnsupportedOperationException("Attempting to pass text content change to an element change of a different source");
        }
    }

    @Nullable
    public List<AttributeChangingEvent> getChangedAttributes() {
        return changedAttributes;
    }

    @Nullable
    public ValueChangingEvent<String> getChangedTextContent() {
        return changedTextContent;
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

    @Override
    public void revert() {
        if (isApplied()) {
            getSource().revertChange(this);
            if (!isCommitted()) {
                getSource().removeChange(this);
            }
        }
    }

    @Override
    public void commit(DefaultPersistenceManager persistenceManager) throws CommitException {
        persistenceManager.commitElementChanges(this);
        setCommitted(true);
        getSource().removeChange(this);
    }

    public boolean isEmpty() {
        return (changedAttributes == null || changedAttributes.isEmpty())
            && (changedTextContent == null);
    }

    public boolean attributeChanged(String attributeName) {
        if (changedAttributes != null) {
            return changedAttributes.stream().anyMatch(change -> change.getAttribute().getAttributeName().equals(attributeName));
        }

        return false;
    }

    public AttributeChangingEvent getAttributeChange(String attributeName) {
        if (changedAttributes != null) {
            List<AttributeChangingEvent> foundChanges = changedAttributes.stream()
                .filter(change -> change.getAttribute().getAttributeName().equals(attributeName))
                .collect(Collectors.toList());

            if (foundChanges.size() == 1) {
                return foundChanges.get(0);
            } else if (foundChanges.size() > 1) {
                // this should never happen since the event is generated when calling XmlAttribute#setValue
                throw new IllegalStateException("Multiple changes recorded for attribute " + attributeName + " within the same event");
            }
        }

        return null;
    }

    public boolean textContentChanged() {
        return changedTextContent != null;
    }

    public static ElementChangingEvent attributeChange(AttributeChangingEvent attributeChange) {
        return new ElementChangingEvent(attributeChange);
    }

    public static ElementChangingEvent textContentChange(ValueChangingEvent<String> changedTextContent) {
        return new ElementChangingEvent(changedTextContent);
    }

}

package net.robinfriedli.jxp.events;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.PersistException;
import org.w3c.dom.Element;

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
    }

    public static ElementChangingEvent attributeChange(AttributeChangingEvent attributeChange) {
        return new ElementChangingEvent(attributeChange);
    }

    public static ElementChangingEvent textContentChange(ValueChangingEvent<String> changedTextContent) {
        return new ElementChangingEvent(changedTextContent);
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
            if (isCommitted()) {
                Element element = getSource().requireElement();
                List<AttributeChangingEvent> changedAttributes = getChangedAttributes();

                if (changedAttributes != null && !changedAttributes.isEmpty()) {
                    for (AttributeChangingEvent changedAttribute : changedAttributes) {
                        element.setAttribute(changedAttribute.getAttribute().getAttributeName(), changedAttribute.getOldValue());
                    }
                }

                if (textContentChanged()) {
                    //noinspection ConstantConditions
                    element.setTextContent(changedTextContent.getOldValue());
                }
            } else {
                getSource().removeChange(this);
            }
        }
    }

    @Override
    public void commit() {
        XmlElement element = getSource();
        Element elem = element.requireElement();
        List<AttributeChangingEvent> attributeChanges = getChangedAttributes();

        if (attributeChanges != null && !attributeChanges.isEmpty()) {
            for (AttributeChangingEvent attributeChange : attributeChanges) {
                elem.setAttribute(attributeChange.getAttribute().getAttributeName(), attributeChange.getNewValue());
                attributeChange.setCommitted(true);
            }
        }
        if (textContentChanged()) {
            //noinspection ConstantConditions
            elem.setTextContent(changedTextContent.getNewValue());
        }

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

}

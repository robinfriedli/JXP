package net.robinfriedli.jxp.persist;

import com.google.common.base.Strings;
import net.robinfriedli.jxp.api.XmlAttribute;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.events.AttributeChangingEvent;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.stringlist.StringList;
import net.robinfriedli.stringlist.StringListImpl;
import org.openqa.selenium.support.ui.Quotes;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class that represents the current state of the {@link XmlElement} in the XML file.
 * Gets created after committing a new XmlElement or creating an XmlElement with {@link XmlElement.State#CLEAN}
 * (when loading all XmlElements on startup / initializing a {@link Context}) and is updated after committing an
 * {@link ElementChangingEvent}
 */
public class XmlElementShadow {

    private final XmlElement source;

    private final Map<String, String> attributes;

    private String textContent;

    public XmlElementShadow(XmlElement source) {
        this.source = source;
        this.attributes = new HashMap<>();
        this.textContent = source.getTextContent();

        buildAttributeMap();
    }

    public void update() {
        buildAttributeMap();
        this.textContent = source.getTextContent();
    }

    public void adopt(ElementChangingEvent change) {
        apply(change, false);
    }

    public void revert(ElementChangingEvent change) {
        apply(change, true);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getTextContent() {
        return textContent;
    }

    public String getXPath() {
        StringBuilder expression = new StringBuilder();
        expression.append("/").append(source.getContext().getRootElem());
        buildPath(expression, source);

        if (!attributes.keySet().isEmpty() || !Strings.isNullOrEmpty(textContent)) {
            expression.append("[");

            if (!attributes.keySet().isEmpty()) {
                StringList mappedAttributes = StringListImpl.create(
                    attributes.keySet(),
                    attribute -> String.format("@%s=%s", attribute, Quotes.escape(attributes.get(attribute)))
                );
                expression.append(mappedAttributes.toSeparatedString(" and "));
            }

            if (!attributes.keySet().isEmpty() && !Strings.isNullOrEmpty(textContent)) {
                expression.append(" and ");
            }

            if (!Strings.isNullOrEmpty(textContent)) {
                expression.append("text()=").append(Quotes.escape(textContent));
            }

            expression.append("]");
        }

        return expression.toString();
    }

    private void buildPath(StringBuilder builder, XmlElement element) {
        if (element.isSubElement()) {
            buildPath(builder, element.getParent());
        }
        builder.append("/").append(element.getTagName());
    }

    public boolean matches(Element element) {
        Set<String> attributeNames = attributes.keySet();
        return attributeNames.stream().allMatch(name -> element.getAttribute(name).equals(attributes.get(name)))
            && (!source.hasTextContent() || element.getTextContent().equals(textContent));
    }

    private void buildAttributeMap() {
        List<XmlAttribute> attributeInstances = source.getAttributes();
        for (XmlAttribute attributeInstance : attributeInstances) {
            attributes.put(attributeInstance.getAttributeName(), attributeInstance.getValue());
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void apply(ElementChangingEvent change, boolean revert) {
        if (change.getSource() != source) {
            throw new UnsupportedOperationException("Attempting to apply a change to a shadow of a different element");
        }

        if (change.textContentChanged()) {
            if (revert) {
                textContent = change.getChangedTextContent().getOldValue();
            } else {
                textContent = change.getChangedTextContent().getNewValue();
            }
        }

        List<AttributeChangingEvent> changedAttributes = change.getChangedAttributes();
        if (changedAttributes != null && !changedAttributes.isEmpty()) {
            for (AttributeChangingEvent changedAttribute : changedAttributes) {
                if (revert) {
                    attributes.put(changedAttribute.getAttribute().getAttributeName(), changedAttribute.getOldValue());
                } else {
                    attributes.put(changedAttribute.getAttribute().getAttributeName(), changedAttribute.getNewValue());
                }
            }
        }
    }
}

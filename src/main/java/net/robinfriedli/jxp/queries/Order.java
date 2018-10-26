package net.robinfriedli.jxp.queries;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.api.XmlElement;


public class Order {

    @Nullable
    private final String attribute;
    private final Direction direction;
    private final Source source;

    public Order(Direction direction, Source source) {
        this(null, direction, source);
    }

    public Order(@Nullable String attribute, Direction direction, Source source) {
        this.source = source;
        this.direction = direction;
        if (source == Source.ATTRIBUTE) {
            if (attribute != null) {
                this.attribute = attribute;
            } else {
                throw new IllegalArgumentException("Source is ATTRIBUTE but no attribute name provided");
            }
        } else if (attribute != null) {
            throw new IllegalArgumentException("Attribute name provided but Source is not ATTRIBUTE");
        } else {
            this.attribute = null;
        }
    }

    public static Order attribute(String name, Direction direction) {
        return new Order(name, direction, Source.ATTRIBUTE);
    }

    public static Order attribute(String name) {
        return new Order(name, Direction.ASCENDING, Source.ATTRIBUTE);
    }

    public static Order textContent(Direction direction) {
        return new Order(direction, Source.TEXT_CONTENT);
    }

    public static Order textContent() {
        return new Order(Direction.ASCENDING, Source.TEXT_CONTENT);
    }

    public void applyOrder(List<XmlElement> elements) {
        Comparator<XmlElement> comparator = getComparator();
        if (direction == Direction.DESCENDING) {
            elements.sort(comparator.reversed());
        } else {
            elements.sort(comparator);
        }
    }

    private Comparator<XmlElement> getComparator() {
        if (source == Source.ATTRIBUTE) {
            return Comparator.comparing(v -> {
                if (v.hasAttribute(attribute)) {
                    return v.getAttribute(attribute).getValue();
                } else {
                    return "";
                }
            });
        } else {
            return Comparator.comparing(XmlElement::getTextContent);
        }
    }

    public enum Direction {

        ASCENDING,
        DESCENDING

    }

    enum Source {

        ATTRIBUTE,
        TEXT_CONTENT

    }

}

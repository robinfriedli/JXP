package net.robinfriedli.jxp.queries;

import java.util.Comparator;
import java.util.stream.Stream;

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

    public <E extends XmlElement> Stream<E> applyOrder(Stream<E> resultStream) {
        Comparator<XmlElement> comparator = getComparator();
        return resultStream.sorted(comparator);
    }

    public Comparator<XmlElement> getComparator() {
        Comparator<XmlElement> comparator;
        if (source == Source.ATTRIBUTE) {
            comparator = Comparator.comparing(v -> {
                if (v.hasAttribute(attribute)) {
                    return v.getAttribute(attribute).getValue();
                } else {
                    return "";
                }
            });
        } else {
            comparator = Comparator.comparing(XmlElement::getTextContent);
        }

        if (direction == Direction.DESCENDING) {
            return comparator.reversed();
        } else {
            return comparator;
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

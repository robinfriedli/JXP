package net.robinfriedli.jxp.queries;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import net.robinfriedli.jxp.api.XmlElement;

public final class ValueComparator {

    private final static Map<Class, Function<String, ?>> STRING_CONVERSION;
    static {
        ImmutableMap.Builder<Class, Function<String, ?>> builder = ImmutableMap.builder();
        builder.put(Integer.class, Integer::parseInt);
        builder.put(Double.class, Double::valueOf);
        builder.put(Float.class, Float::parseFloat);
        builder.put(Long.class, Long::parseLong);
        builder.put(Boolean.class, Boolean::parseBoolean);
        builder.put(BigDecimal.class, BigDecimal::new);
        STRING_CONVERSION = builder.build();
    }

    private final Source source;
    private String attributeName;

    public ValueComparator(Source source) {
        this(source, null);
    }

    public ValueComparator(Source source, @Nullable String attributeName) {
        this.source = source;
        if (source == Source.ATTRIBUTE) {
            if (attributeName != null) {
                this.attributeName = attributeName;
            } else {
                throw new IllegalArgumentException("Source is ATTRIBUTE but no attribute name provided");
            }
        } else if (attributeName != null) {
            throw new IllegalArgumentException("Attribute name provided but Source is not ATTRIBUTE");
        }
    }

    public <E> Predicate<XmlElement> is(E value) {
        if (value instanceof String) {
            return predicate(value::equals);
        } else if (STRING_CONVERSION.keySet().contains(value.getClass())) {
            return predicate(val -> convert(value.getClass(), val).equals(value));
        }

        throw new IllegalArgumentException("No type conversion available for class " + value.getClass().getSimpleName());
    }

    public Predicate<XmlElement> fuzzyIs(String s) {
        return predicate(s::equalsIgnoreCase);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @SafeVarargs
    public final <E> Predicate<XmlElement> in(E... values) {
        if (values instanceof String[]) {
            return predicate(value -> Arrays.asList((String[]) values).contains(value));
        } else if (STRING_CONVERSION.keySet().contains(values.getClass().getComponentType())) {
            return predicate(value -> Arrays.asList(values).contains(convert(values.getClass().getComponentType(), value)));
        }

        throw new IllegalArgumentException("No type conversion available for class " + values.getClass().getSimpleName());
    }

    public Predicate<XmlElement> isEmpty() {
        return predicate(Strings::isNullOrEmpty);
    }

    @SuppressWarnings("unchecked")
    public Predicate<XmlElement> greaterThan(Comparable<? extends Number> i) {
        return predicate(value -> convert(i.getClass(), value).compareTo(i) > 0);
    }

    @SuppressWarnings("unchecked")
    public Predicate<XmlElement> greaterEquals(Comparable<? extends Number> i) {
        return predicate(value -> convert(i.getClass(), value).compareTo(i) >= 0);
    }

    @SuppressWarnings("unchecked")
    public Predicate<XmlElement> lowerThan(Comparable<? extends Number> i) {
        return predicate(value -> convert(i.getClass(), value).compareTo(i) < 0);
    }

    @SuppressWarnings("unchecked")
    public Predicate<XmlElement> lowerEquals(Comparable<? extends Number> i) {
        return predicate(value -> convert(i.getClass(), value).compareTo(i) <= 0);
    }

    public Predicate<XmlElement> startsWith(String s) {
        return predicate(value -> value.startsWith(s));
    }

    public Predicate<XmlElement> endsWith(String s) {
        return predicate(value -> value.endsWith(s));
    }

    public Predicate<XmlElement> contains(String s) {
        return predicate(value -> value.contains(s));
    }

    public Predicate<XmlElement> matches(Function<String, Boolean> expressionToCheck) {
        return predicate(expressionToCheck);
    }

    private <E> E convert(Class<E> type, String value) {
        Function<String, ?> conversion = STRING_CONVERSION.get(type);
        if (conversion == null) {
            throw new IllegalArgumentException("No conversion available for type " + type.getName());
        }

        try {
            return type.cast(conversion.apply(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert '" + value + "' to " + type.getSimpleName(), e);
        }
    }

    private Predicate<XmlElement> predicate(Function<String, Boolean> matchFunc) {
        switch (source) {
            case ATTRIBUTE:
                return element -> element.hasAttribute(attributeName)
                    && matchFunc.apply(element.getAttribute(attributeName).getValue());
            case TEXT_CONTENT:
                return element -> matchFunc.apply(element.getTextContent());
        }

        throw new IllegalStateException("Illegal source " + source);
    }

    enum Source {

        ATTRIBUTE,
        TEXT_CONTENT

    }

}

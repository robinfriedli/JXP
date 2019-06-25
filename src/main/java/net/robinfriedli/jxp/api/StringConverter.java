package net.robinfriedli.jxp.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.exceptions.ConversionException;

public class StringConverter {

    private static final List<StringConversionContribution<?>> stringConversions = Lists.newArrayList();

    static {
        stringConversions.add(new StringConversionContribution<>(Integer.class, Integer::parseInt, Object::toString));
        stringConversions.add(new StringConversionContribution<>(Double.class, Double::new, Object::toString));
        stringConversions.add(new StringConversionContribution<>(Float.class, Float::new, Object::toString));
        stringConversions.add(new StringConversionContribution<>(Long.class, Long::new, Object::toString));
        stringConversions.add(new StringConversionContribution<>(Boolean.class, Boolean::new, Object::toString));
        stringConversions.add(new StringConversionContribution<>(BigDecimal.class, BigDecimal::new, BigDecimal::toString));
        stringConversions.add(new StringConversionContribution<>(String.class, String::toString, String::toString));
    }


    public static <V> V convert(String s, Class<V> target) throws ConversionException {
        StringConversionContribution<V> converter = getConverter(target);

        try {
            return converter.convert(s);
        } catch (Throwable e) {
            throw new ConversionException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <V> String reverse(V objectToReverse) throws ConversionException {
        StringConversionContribution<V> converter = (StringConversionContribution<V>) getConverter(objectToReverse.getClass());
        try {
            return converter.reverse(objectToReverse);
        } catch (Throwable e) {
            throw new ConversionException(e);
        }
    }

    public static <V> void map(Class<V> targetClass, Function<String, V> conversionFunc, Function<V, String> reverseFunc) {
        if (!canConvert(targetClass)) {
            stringConversions.add(new StringConversionContribution<>(targetClass, conversionFunc, reverseFunc));
        }
    }

    public static boolean canConvert(Class target) {
        return stringConversions.stream().anyMatch(c -> c.getClassToConvert().equals(target));
    }

    @SuppressWarnings("unchecked")
    private static <V> StringConversionContribution<V> getConverter(Class<V> classToConvert) {
        Optional<StringConversionContribution<?>> stringConverter = stringConversions
            .stream()
            .filter(c -> c.getClassToConvert().equals(classToConvert))
            .findFirst();

        if (stringConverter.isPresent()) {
            return (StringConversionContribution<V>) stringConverter.get();
        } else {
            throw new IllegalStateException("No conversion available for class " + classToConvert.getSimpleName()
                + ". Add with StringConverter#map");
        }
    }

}

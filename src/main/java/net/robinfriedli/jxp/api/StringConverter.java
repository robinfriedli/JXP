package net.robinfriedli.jxp.api;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class StringConverter {

    private static final Map<Class, Function<String, ?>> stringConversion = new HashMap<>();

    static {
        stringConversion.put(Integer.class, Integer::parseInt);
        stringConversion.put(Double.class, Double::valueOf);
        stringConversion.put(Float.class, Float::parseFloat);
        stringConversion.put(Long.class, Long::parseLong);
        stringConversion.put(Boolean.class, Boolean::parseBoolean);
        stringConversion.put(BigDecimal.class, BigDecimal::new);
        stringConversion.put(String.class, String::toString);
    }


    @SuppressWarnings("unchecked")
    public static <V> V convert(String s, Class<V> target) {
        Function<String, ?> stringConverter = stringConversion.get(target);
        if (stringConverter != null) {
            return (V) stringConverter.apply(s);
        }

        throw new IllegalArgumentException("No conversion available for class " + target.getSimpleName());
    }

    public static <V> void map(Class<V> targetClass, Function<String, V> conversionFunc) {
        stringConversion.put(targetClass, conversionFunc);
    }

    public static boolean canConvert(Class target) {
        return stringConversion.get(target) != null;
    }

}

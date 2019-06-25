package net.robinfriedli.jxp.queries;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.QueryException;

public class Query {

    private final Predicate<XmlElement> expression;
    private Order order;

    public Query(Predicate<XmlElement> expression) {
        this.expression = expression;
    }

    public static Query evaluate(Predicate<XmlElement> expression) {
        return new Query(expression);
    }

    private static QueryException getCastException(Class type, Throwable cause) {
        return new QueryException("Some found elements could not be converted to " + type + ". " +
            "Consider expanding your query with the Conditions#instanceOf condition.", cause);
    }

    public ResultStream<XmlElement> execute(Collection<XmlElement> elements) {
        return execute(elements, XmlElement.class);
    }

    public <E extends XmlElement> ResultStream<E> execute(Collection<XmlElement> elements, Class<E> type) {
        try {
            Stream<E> found = elements.stream().filter(expression).map(type::cast);

            if (order != null) {
                found = order.applyOrder(found);
            }

            return new ResultStream<>(found);
        } catch (ClassCastException e) {
            throw getCastException(type, e);
        }
    }

    public Query order(Order order) {
        this.order = order;
        return this;
    }

}

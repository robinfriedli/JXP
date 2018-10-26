package net.robinfriedli.jxp.queries;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.robinfriedli.jxp.api.XmlElement;

public class Query {

    private final Predicate<XmlElement> expression;

    public Query(Predicate<XmlElement> expression) {
        this.expression = expression;
    }

    public static Query evaluate(Predicate<XmlElement> expression) {
        return new Query(expression);
    }

    public QueryResult<List<XmlElement>> execute(Collection<XmlElement> elements) {
        Stream<XmlElement> found = elements.stream().filter(expression);

        return new QueryResult<>(found.collect(Collectors.toList()));
    }

    public <C extends Collection<XmlElement>> QueryResult<C> execute(Collection<XmlElement> elements, Collector<XmlElement, ?, C> collector) {
        Stream<XmlElement> found = elements.stream().filter(expression);

        return new QueryResult<>(found.collect(collector));
    }

    public long count(Collection<XmlElement> elements) {
        Stream<XmlElement> found = elements.stream().filter(expression);
        return found.count();
    }

}

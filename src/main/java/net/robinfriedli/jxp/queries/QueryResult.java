package net.robinfriedli.jxp.queries;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.api.XmlElement;

public class QueryResult<C extends Collection<XmlElement>> {

    private final C found;

    public QueryResult(C found) {
        this.found = found;
    }

    public C collect() {
        return found;
    }

    @Nullable
    public XmlElement getFirstResult() {
        if (found.size() > 0) {
            return found.iterator().next();
        }

        return null;
    }

    public XmlElement requireFirstResult() {
        expectAtLeast(1);
        return getFirstResult();
    }

    @Nullable
    public XmlElement getOnlyResult() {
        expectAtMost(1);
        return getFirstResult();
    }

    public XmlElement requireOnlyResult() {
        expect(1);
        return requireFirstResult();
    }

    @SuppressWarnings("unchecked")
    public QueryResult<C> order(Order order) {
        if (found instanceof List) {
            order.applyOrder((List<XmlElement>) found);
        } else {
            throw new IllegalStateException("Cannot sort " + found.getClass().getSimpleName() + ". Needs to be list.");
        }

        return this;
    }

    public void expect(int i) throws IllegalStateException {
        if (found.size() != i) {
            throw new IllegalStateException("Expected " + i + " elements but found " + found.size());
        }
    }

    public void expectAtLeast(int i) throws IllegalStateException {
        if (found.size() < i) {
            throw new IllegalStateException("Expected at least " + i + " elements but found " + found.size());
        }
    }

    public void expectAtMost(int i) throws IllegalStateException {
        if (found.size() > i) {
            throw new IllegalStateException("Expected at most " + i + " elements but found " + found.size());
        }
    }

    public void expectBetween(int min, int max) throws IllegalStateException {
        try {
            expectAtLeast(min);
            expectAtMost(max);
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Expected " + min + " to " + max + " elements but found " + found.size());
        }
    }
}

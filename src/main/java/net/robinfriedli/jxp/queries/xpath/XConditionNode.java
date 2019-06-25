package net.robinfriedli.jxp.queries.xpath;

public class XConditionNode implements XNode {

    private final String condition;

    public XConditionNode(String condition) {
        this.condition = condition;
    }

    @Override
    public String asString() {
        return condition;
    }
}

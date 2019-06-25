package net.robinfriedli.jxp.queries.xpath;

public class XComparingNode implements XNode {

    private final String left;
    private final String right;
    private final String operator;

    public XComparingNode(String left, String right, String operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public String asString() {
        return left + operator + right;
    }
}

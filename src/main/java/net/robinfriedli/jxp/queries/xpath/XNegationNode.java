package net.robinfriedli.jxp.queries.xpath;

public class XNegationNode implements XNode {

    private final XNode nodeToNegate;

    public XNegationNode(XNode nodeToNegate) {
        this.nodeToNegate = nodeToNegate;
    }

    @Override
    public String asString() {
        return "not(" + nodeToNegate.asString() + ")";
    }
}

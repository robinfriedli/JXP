package net.robinfriedli.jxp.queries.xpath;

public final class XConditions {

    public static XNode and(XNode... nodes) {
        return new XJunctionNode(XJunctionNode.Type.AND, nodes);
    }

    public static XNode or(XNode... nodes) {
        return new XJunctionNode(XJunctionNode.Type.OR, nodes);
    }

    public static XNodeBuilder attribute(String name) {
        return new XNodeBuilder("@" + name);
    }

    public static XNodeBuilder textContent() {
        return new XNodeBuilder("text()");
    }

    public static XNode condition(String xPathCondition) {
        return new XConditionNode(xPathCondition);
    }

    public static XNode not(XNode nodeToNegate) {
        return new XNegationNode(nodeToNegate);
    }

}

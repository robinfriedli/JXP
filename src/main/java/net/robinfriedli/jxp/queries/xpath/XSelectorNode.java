package net.robinfriedli.jxp.queries.xpath;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class XSelectorNode implements XNode {

    private final XNode[] nodes;
    private final String selector;

    public XSelectorNode(String selector, XNode... nodes) {
        this.selector = selector;
        this.nodes = nodes;
    }

    @Override
    public String asString() {
        Joiner selectorJoiner = Joiner.on(" " + selector + " ");
        List<String> nodes = Lists.newArrayList();
        for (XNode node : this.nodes) {
            if (node instanceof XSelectorNode) {
                // put sub selector in brackets
                nodes.add("(" + node.asString() + ")");
            } else {
                nodes.add(node.asString());
            }
        }
        return selectorJoiner.join(nodes);
    }
}

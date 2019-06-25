package net.robinfriedli.jxp.queries.xpath;

import com.google.common.base.Joiner;

public class XFunctionNode implements XNode {

    private final String funtion;
    private final String[] args;

    public XFunctionNode(String funtion, String... args) {
        this.funtion = funtion;
        this.args = args;
    }

    @Override
    public String asString() {
        Joiner joiner = Joiner.on(",");
        return funtion + "(" + joiner.join(args) + ")";
    }
}

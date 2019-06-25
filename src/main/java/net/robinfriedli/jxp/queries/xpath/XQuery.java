package net.robinfriedli.jxp.queries.xpath;

import net.robinfriedli.jxp.persist.Context;

public class XQuery {

    private final String pathAfterRoot;
    private final XNode rootNode;

    public XQuery(String pathAfterRoot, XNode rootNode) {
        this.pathAfterRoot = pathAfterRoot;
        this.rootNode = rootNode;
    }

    public String getXPath(Context context) {
        String path = "/" + context.getRootElem() + "/" + pathAfterRoot;
        return path + "[" + rootNode.asString() + "]";
    }

}

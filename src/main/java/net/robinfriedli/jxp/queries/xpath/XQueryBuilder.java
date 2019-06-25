package net.robinfriedli.jxp.queries.xpath;

public class XQueryBuilder {

    private final String pathAfterRoot;

    public XQueryBuilder(String pathAfterRoot) {
        this.pathAfterRoot = pathAfterRoot;
    }

    public static XQueryBuilder find(String elementPath) {
        return new XQueryBuilder(elementPath);
    }

    public XQuery where(XNode rootNode) {
        return new XQuery(pathAfterRoot, rootNode);
    }

}

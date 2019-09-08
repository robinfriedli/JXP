package net.robinfriedli.jxp.queries.xpath;

/**
 * Utility class to build XPath query Strings in a fluent API style
 * e.g.
 * <pre>
 *     {@code
 *     XQuery query2 = XQueryBuilder.find("country/city").where(or(
 *         and(
 *                 not(
 *                         and(
 *                                 attribute("population").in(200000, 400000, 1000000, 8900000),
 *                                 attribute("name").in("Birmingham", "New York")
 *                         )
 *                 ),
 *                 attribute("population").greaterThan(200000),
 *                 attribute("population").lowerThan(1000000)
 *         ),
 *         and(
 *                 or(
 *                         attribute("population").greaterThan(8000000),
 *                         attribute("population").lowerThan(1000000)
 *                 ),
 *                 attribute("name").in("London", "Manchester", "Birmingham"),
 *                 not(
 *                         or(
 *                                 attribute("name").in("New York"),
 *                                 attribute("name").contains("ches"),
 *                                 attribute("name").endsWith("ham")
 *                         )
 *                 )
 *         )
 *     ));
 *     String xPath2 = query2.getXPath(context);
 *     List<XmlElement> foundInstances = context.xPathQuery(xPath2);
 * }
 * </pre>
 */
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

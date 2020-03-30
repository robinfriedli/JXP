package net.robinfriedli.jxp.persist;

import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.annotations.*;

import com.google.common.collect.Lists;
import com.google.common.truth.Truth;
import net.robinfriedli.jxp.AbstractTest;
import net.robinfriedli.jxp.api.JxpBackend;
import net.robinfriedli.jxp.api.JxpBuilder;
import net.robinfriedli.jxp.api.UninitializedParent;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.entities.City;
import net.robinfriedli.jxp.entities.Country;
import net.robinfriedli.jxp.queries.Conditions;
import net.robinfriedli.jxp.queries.xpath.XQuery;
import net.robinfriedli.jxp.queries.xpath.XQueryBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static net.robinfriedli.jxp.queries.xpath.XConditions.*;
import static org.testng.Assert.*;

public class LazyContextTest extends AbstractTest {

    @Override
    protected JxpBackend setupJxp() {
        return new JxpBuilder()
            .setDefaultContextType(JxpBackend.DefaultContextType.LAZY)
            .mapClass("country", Country.class)
            .mapClass("city", City.class)
            .build();
    }

    @Test
    public void testXPathQuery() throws ParserConfigurationException {
        City london = new City("London", 8900000);
        City manchester = new City("Manchester", 500000);
        City birmingham = new City("Birmingham", 1000000);

        City zurich = new City("Zurich", 400000);
        City geneva = new City("Geneva", 200000);

        City newYork = new City("New York", 8800000);
        City sanFrancisco = new City("San Francisco", 880000);
        City chicago = new City("Chicago", 2700000);

        Country uk = new Country("United Kingdom", "United Kingdom", true, Lists.newArrayList(london, manchester, birmingham));
        Country switzerland = new Country("Schweiz", "Switzerland", true, Lists.newArrayList(zurich, geneva));
        Country england = new Country("England", "England", false, Lists.newArrayList());
        Country us = new Country("United States", "United States", true, Lists.newArrayList(newYork, sanFrancisco, chicago));

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element rootElem = document.createElement("countries");
        rootElem.setAttribute("xmlns", "countrySpace");
        document.appendChild(rootElem);

        Context context = jxp.getContext(document);
        context.persist("src/test/resources/output/xPathQueryTest" + System.currentTimeMillis() + ".xml");
        context.invoke(() -> {
            uk.persist(context);
            switzerland.persist(context);
            england.addSubElement(london.copy(true, true));
            england.addSubElement(birmingham.copy(true, true));
            england.persist(context);
            us.persist(context);
        });

        XQuery query = XQueryBuilder.find("country/city").where(and(
            or(
                attribute("population").greaterEquals(1000000),
                attribute("population").is(400000)
            ),
            or(
                attribute("name").startsWith("Zu"),
                attribute("name").endsWith("on"),
                attribute("name").contains("ew")
            )
        ));
        String xPath = query.getXPath(context);
        List<XmlElement> xmlElements = context.xPathQuery(xPath);
        assertEquals(xmlElements.size(), 4);
        for (XmlElement xmlElement : xmlElements) {
            assertTrue(xmlElement.getParent() instanceof UninitializedParent);
            Truth.assertThat(xmlElement.getAttribute("name").getValue()).isAnyOf("London", "Zurich", "New York");
            String parentName = xmlElement.getParent().getAttribute("englishName").getValue();
            XmlElement parent = xmlElement.getParent();
            assertFalse(parent instanceof UninitializedParent);
            Truth.assertThat(parentName).isAnyOf("United Kingdom", "England", "United States", "Switzerland");
            assertTrue(parent.getSubElements().contains(xmlElement));
            switch (parentName) {
                case "United Kingdom":
                case "United States":
                    assertEquals(parent.getSubElements().size(), 3);
                    break;
                case "England":
                case "Switzerland":
                    assertEquals(parent.getSubElements().size(), 2);
                    break;
                default:
                    fail();
                    break;
            }
        }

        XQuery query1 = XQueryBuilder.find("country").where(and(
            or(
                and(
                    attribute("sovereign").is(false),
                    attribute("englishName").startsWith("Eng"),
                    attribute("name").contains("ngl"),
                    attribute("name").endsWith("land")
                ),
                attribute("name").startsWith("United"),
                attribute("englishName").endsWith("land")
            ),
            not(
                and(
                    attribute("name").startsWith("United"),
                    attribute("name").endsWith("States")
                )
            )
        ));
        String xPath1 = query1.getXPath(context);
        List<XmlElement> xmlElements1 = context.xPathQuery(xPath1);
        assertEquals(xmlElements1.size(), 3);
        for (XmlElement xmlElement : xmlElements1) {
            Truth.assertThat(xmlElement.getAttribute("name").getValue()).isAnyOf("United Kingdom", "Schweiz", "England");
        }

        XQuery query2 = XQueryBuilder.find("country/city").where(or(
            and(
                not(
                    and(
                        attribute("population").in(200000, 400000, 1000000, 8900000),
                        attribute("name").in("Birmingham", "New York")
                    )
                ),
                attribute("population").greaterThan(200000),
                attribute("population").lowerThan(1000000)
            ),
            and(
                or(
                    attribute("population").greaterThan(8000000),
                    attribute("population").lowerThan(1000000)
                ),
                attribute("name").in("London", "Manchester", "Birmingham"),
                not(
                    or(
                        attribute("name").in("New York"),
                        attribute("name").contains("ches"),
                        attribute("name").endsWith("ham")
                    )
                )
            )
        ));
        String xPath2 = query2.getXPath(context);
        List<XmlElement> xmlElements2 = context.xPathQuery(xPath2);
        assertEquals(xmlElements2.size(), 5);
        for (XmlElement xmlElement : xmlElements2) {
            Truth.assertThat(xmlElement.getAttribute("name").getValue()).isAnyOf("London", "Manchester", "Zurich", "San Francisco");
        }
    }

    @Test
    public void testLazyParentInitialization() {
        Context context = jxp.createContext(getTestResource("/countries.xml"));

        String xPath = XQueryBuilder.find("country/state/city").where(
            attribute("name").is("London")
        ).getXPath(context);

        List<XmlElement> xmlElements = context.xPathQuery(xPath);
        assertEquals(xmlElements.size(), 1);

        XmlElement xmlElement = xmlElements.get(0);
        assertTrue(xmlElement.getParent() instanceof UninitializedParent);
        String parentName = xmlElement.getParent().getAttribute("name").getValue();
        XmlElement parent = xmlElement.getParent();
        assertEquals(parentName, "Greater London");
        assertFalse(parent instanceof UninitializedParent);
        assertEquals(parent.getSubElements().size(), 1);
        assertTrue(parent.getSubElements().contains(xmlElement));

        assertTrue(parent.getParent() instanceof UninitializedParent);
        String parentParentName = parent.getParent().getAttribute("name").getValue();
        XmlElement parentParent = parent.getParent();
        assertEquals(parentParentName, "United Kingdom");
        assertNull(parentParent.getParent());
        assertTrue(parentParent instanceof Country);
        assertEquals(parentParent.getSubElements().size(), 3);
        assertEquals(parentParent.getSubElementsRecursive().size(), 6);

        XmlElement manchester = parentParent.query(Conditions.attribute("name").is("Manchester")).getOnlyResult();
        assertNotNull(manchester);
        XmlElement manchesterParent = manchester.getParent();
        assertEquals(manchesterParent.getAttribute("name").getValue(), "Greater Manchester");
        assertEquals(manchesterParent.getParent(), parentParent);
    }

}

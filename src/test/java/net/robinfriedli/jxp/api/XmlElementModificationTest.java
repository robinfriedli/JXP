package net.robinfriedli.jxp.api;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.testng.annotations.*;

import com.google.common.collect.Lists;
import com.google.common.truth.Truth;
import net.robinfriedli.jxp.AbstractTest;
import net.robinfriedli.jxp.entities.City;
import net.robinfriedli.jxp.entities.Country;
import net.robinfriedli.jxp.entities.State;
import net.robinfriedli.jxp.entities.TestElem;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.queries.QueryResult;
import net.robinfriedli.jxp.queries.ResultStream;

import static net.robinfriedli.jxp.queries.Conditions.*;
import static org.testng.Assert.*;

public class XmlElementModificationTest extends AbstractTest {

    @Override
    protected JxpBackend setupJxp() {
        return new JxpBuilder()
            .mapClass("city", City.class)
            .mapClass("state", State.class)
            .mapClass("country", Country.class)
            .build();
    }

    @Test
    public void testRemoveAttribute() {
        doWithCopiedContext("/countries.xml", context -> {
            State zurich = context.requireElement("Zurich", State.class);
            context.invoke(() -> zurich.removeAttribute("population"));

            XmlAttribute provisionalPopulation = zurich.getAttribute("population");
            assertEquals(provisionalPopulation.getClass(), XmlAttribute.Provisional.class);
            assertEquals(provisionalPopulation.getValue(), "");
            assertEquals((int) provisionalPopulation.getValue(Integer.class), 0);

            Country unitedKingdom = context.requireElement("United Kingdom", Country.class);
            context.invoke(() -> unitedKingdom.getAttribute("englishName").remove());
            expectException(IllegalArgumentException.class, () -> unitedKingdom.getAttribute("englishName").remove());
        });
    }

    @Test
    public void testSimpleElementChange() {
        Context context = jxp.createContext(getTestResource("/countries.xml"));
        City london = new City("London", 8908000);
        XmlElement unitedKingdom = context.requireElement("United Kingdom");

        context.invoke(() -> unitedKingdom.addSubElement(london));

        context.invoke(() -> {
            XmlElement originalLondon = unitedKingdom.query(and(
                attribute("population").is(8900000),
                attribute("name").is("London")
            )).requireOnlyResult();
            originalLondon.delete();
        });

        XmlElement effectiveLondon = unitedKingdom.query(attribute("name").is("London")).getOnlyResult();
        assertNotNull(effectiveLondon);
        assertEquals(effectiveLondon.getAttribute("population").getInt(), 8908000);
        assertEquals(context.query(and(
            attribute("name").is("London"),
            parentMatches(attribute("englishName").is("United Kingdom"))
        )).count(), 1);
    }

    @Test
    public void testInsertDelete() {
        doWithCopiedContext("/countries.xml", context -> {
            XmlElement england = context.requireElement("England");
            City london = england.requireSubElement("London", City.class);
            City manchester = england.requireSubElement("Manchester", City.class);

            context.invoke(() -> england.removeSubElement(london));
            context.invoke(() -> england.addSubElement(london));

            assertTrue(england.getSubElements().contains(london));
            assertEquals(london.getParent(), england);

            context.invoke(() -> {
                england.removeSubElement(london);
                england.addSubElement(london);
            });

            assertTrue(england.getSubElements().contains(london));
            assertEquals(london.getParent(), england);

            context.invoke(() -> {
                List<XmlElement> subElements = Lists.newArrayList(england.getSubElements());
                england.delete();
                england.persist(context);
                england.addSubElements(subElements);
            });

            assertEquals(england.getSubElements().size(), 2);
            assertTrue(england.getSubElements().contains(london));

            City birmingham = new City("Birmingham", 1000000);
            context.invoke(() -> {
                england.addSubElement(birmingham);
                england.delete();
                birmingham.delete();
                england.persist(context);
                england.addSubElement(birmingham);
                england.addSubElement(london);
            });

            assertEquals(england.getSubElements().size(), 2);
            assertTrue(england.getSubElements().contains(london));
            assertTrue(england.getSubElements().contains(birmingham));

            context.invoke(true, false, () -> {
                england.delete();
                england.addSubElement(london);
                england.removeSubElement(london);
                england.addSubElement(london);
                england.addSubElement(birmingham);
                england.persist(context);
                england.addSubElement(manchester);
                birmingham.setAttribute("population", "1086000");
            });

            assertEquals(england.getSubElements().size(), 3);
            assertEquals(birmingham.getAttribute("population").getInt(), 1086000);
        });
    }

    @Test
    public void testElementCopy() {
        doWithCopiedContext("/countries.xml", context -> {
            ResultStream<Country> query = context.query(attribute("englishName").is("Switzerland"), Country.class);
            QueryResult<Country, Set<Country>> result = query.getResult(Collectors.toSet());
            System.out.println(result.requireOnlyResult());

            TestElem elem = context.invoke(() -> {
                TestElem test = new TestElem("test", "test");
                test.addSubElement(new TestElem("test1", "test1"));
                test.addSubElement(new TestElem("test2", "test2"));
                test.persist(context);
                return test;
            });

            context.invoke(() -> {
                XmlElement copy = elem.copy(true, true);
                elem.setTextContent("test");
                assertEquals(elem.getTextContent(), "test");
                assertEquals(copy.getTextContent(), "");
                assertEquals(copy.getAttribute("testAtr1").getValue(), "test");
                assertEquals(copy.getAttribute("testAtr2").getValue(), "test");
                List<XmlElement> subElements = copy.getSubElements();
                assertEquals(subElements.size(), 2);
                for (XmlElement subElement : subElements) {
                    Truth.assertThat(subElement.getAttribute("testAtr1").getValue()).isAnyOf("test1", "test2");
                    Truth.assertThat(subElement.getAttribute("testAtr2").getValue()).isAnyOf("test1", "test2");
                }
                elem.delete();
            });
        });
    }

}

# JXP
Java XML Persistence API

## Maven
```xml
    <repository>
      <id>JXP</id>
      <url>https://raw.github.com/robinfriedli/JXP/repository/</url>
    </repository>

    <dependency>
      <groupId>net.robinfriedli.JXP</groupId>
      <artifactId>JXP</artifactId>
      <version>1.0</version>
    </dependency>
```

## Setup

Creating the JxpBackend object:
```java
    JxpBackend jxp = new JxpBuilder()
        .addListeners(new MyEventListener(), new CountryListener)
        // map your XmlElement implementation to an XML tag name to have it instantiated automatically when creating a
        // Context
        .mapClass("city", City.class)
        .mapClass("country", Country.class)
        .build();
```
Initializing a Context:
```java
    // build a Context based on an existing XML file
    Context context = jxp.getContext("./resources/countries.xml");

    // build a Context based on a dom Document instance, this Context can later be saved to a file using Context#persist
    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element rootElem = document.createElement("tests");
    rootElem.setAttribute("xmlns", "testSpace");
    document.appendChild(rootElem);
    Context context = jxp.getContext(document);
```

## AbstractXmlElement and BaseXmlElement

AbstractXmlElement is the class to extend for any class you want to persist as an XML element. Its default implementation,
BaseXmlElement, is instantiated for each XML element in your file when creating a new Context via the
DefaultPersistenceManager#getAllElements method, if no class is mapped to the corresponding XML tag name in the
JxpBackend. It is recommended however that you create your own implementation forAbstractXmlElement which allows you to
define a unique id for your element (BaseXmlElement has null as id), which is useful for loading XmlElements more easily.
Then instantiate your own class by simply mapping your class with the matching tag name and add it to the JxpBackend
using JxpBackend#mapClass

When extending AbstractXmlElement you need to pass the tag name of your XML element, a Map with all attributes
(with the attribute name as key and value as value) and a List of sub elements to the constructor of the super class.
Your XmlElement should have two constructors; one to create a new XmlElement with the provided values and one to
instantiate the XmlElement when loading it from the file, to which you pass the corresponding org.w3c.dom.Element

Example:
```java
    package net.robinfriedli.jxp.api;

    import java.util.HashMap;
    import java.util.Map;

    import javax.annotation.Nullable;

    import net.robinfriedli.jxp.persist.Context;

    public class City extends AbstractXmlElement {

        public City(String name, int population, Context context) {
            super("city", buildAttributes(name, population), context);
        }

        // constructor invoked by DefaultPersistenceManager when creating a Context
        public City(Element element, Context context) {
            super(element, context);
        }

        @Nullable
        @Override
        public String getId() {
            return getAttribute("name").getValue();
        }

        private static Map<String, ?> buildAttributes(String name, int population) {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("name", name);
            attributes.put("population", population);
            return attributes;
        }
    }
```
```java
    package net.robinfriedli.jxp.api;

    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

    import javax.annotation.Nullable;

    import com.google.common.collect.Lists;
    import net.robinfriedli.jxp.persist.Context;

    public class Country extends AbstractXmlElement {

        public Country(String name, String englishName, boolean sovereign, List<City> cities, Context context) {
            super("country", buildAttributes(name, englishName, sovereign), Lists.newArrayList(cities), context);
        }

        // constructor invoked by DefaultPersistenceManager when creating a Context
        public Country(Element element, List<City> cities, Context context) {
            super(element, Lists.newArrayList(cities), context);
        }

        @Nullable
        @Override
        public String getId() {
            return getAttribute("englishName").getValue();
        }

        public static Map<String, ?> buildAttributes(String name, String englishName, boolean sovereign) {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("name", name);
            attributes.put("englishName", englishName);
            attributes.put("sovereign", sovereign);
            return attributes;
        }
    }
```

## Context, JxpBackend and BindableContext

The Context class represents an XML document and / or File and is used to make any changes to it. It stores all
XmlElement instances of that document / file and applies changes to them by starting a Transaction, adding changes to it
and then commit it using the invoke() method. This is required for any action that creates, deletes or changes an
XmlElement. BindableContext is a Context that can be bound to any object and can be retrieved from the JxpBackend using
that object. The JxpBackend holds all Context instances created by it, all BindableContexts and all EventListeners.

### Using the invoke method:

parameter | description
--- | ---
first parameter (optional): | commit (boolean), set true if all changes made during this task should be committed to the XML file. If false the transaction will be added to the Context's uncommitted transactions. Default: true
second parameter (optional): | instantApply (boolean). Defines whether all changes should be applied to the XmlElement instance upon adding it to the transaction immediately. That means all changes are available within the invoked task and not only after the transaction. E.g. if false changing an attribute using elem.setAttribute("test", "value") and then calling elem.getAttribute("test").getValue() will still return the old value if still inside the transaction
third parameter: | the actual task to run, a Callable or Runnable depending on whether your task should return something or not
fourth parameter (optional): | any Object to set as this Context's environment variable. Could be any object you need anywhere in Context with this transaction. E.g. say you're developing a Discord bot and you've implemented an EventListener that sends a message after an Element has been added. In this case you could set the MessageChannel the command came from as envVar to send the message to the right channel.

There are two other variants of the invoke method: invokeWithoutListeners() and futureInvoke(). futureInvoke, if used
within a Transaction, adds a QueuedTask to the Transaction which will be executed after the Transaction has finished. This
is necessary if the current Transaction is in a state where it is not recording changes anymore, e.g. in listeners.
(For instant-apply transactions this is only required for the transactionCommitted event, otherwise no invoke method
call is required at all). Alternatively, if there is no current Transaction, the method can still be used to invoke
tasks asynchronously, in which case the implement has to execute the task at some point. futureInvoke has two additional
boolean parameters after commit any instantApply: cancelOnFailure and triggerListeners. The first one is relevant when
queueing the task to the transaction and defines whether the task gets cancelled if the transaction fails (rolls back).
triggerListeners defines whether the task will be executed within a normal invoke or invokeWithoutListeners if false.
The default option for both is true.

The quickest way to add a new XmlElement to the file:
```java
    context.invoke(() -> new Country("Italia", "Italy", true, Lists.newArrayList(rome, florence, venice), context).persist());
```

Example:
```java
    Context context = contextManager.getContext();
    context.invoke(() -> {
        Country ch = new Country("Schweiz", "Switzerland", true, Lists.newArrayList(this), context);
        ch.persist();

        Country unitedKingdom = context.getElement("United Kingdom", Country.class);
        unitedKingdom.setAttribute("sovereign", Boolean.toString(true));

        Country france = context.getElement("France", Country.class);
        france.delete();
    });
```

Async example:
```java
    QueuedTask<Void> future = context.futureInvoke(() -> {
        City london = new City("London", 8900000, context);
        context.requireElement("England").addSubElement(london);
        return null;
    });
    new Thread(future).start();
    long count1 = Query.evaluate(attribute("name").is("London")).count(context.getElementsRecursive());
    System.out.println("Count " + count1 + ", expected: 0");
    future.get();
    long count2 = Query.evaluate(attribute("name").is("London")).count(context.getElementsRecursive());
    System.out.println("Count " + count2 + ", expected: 1");

```
## Queries

The Conditions class offers many static methods to build predicates to find XmlElements with. Use Context#query
to run a query over a context's elements (including all subelements recursively), which returns a QueryResult<List<XmlElement>>,
or use the static method Query#evaluate to build a query and then run it over any Collection of XmlElements using the
Query#execute method. Use Query#count to get the amount of results a query returns or
Query#execute(Collection, Collector) to select what kind of Collection to collect the results with.
You can also use QueryResult#order to order the results by an attribute or text content, if results were collected to a
List.

Examples:
```java
    XmlElement london = context.query(
            and(
                    attribute("population").greaterThan(800000),
                    subElementOf(england),
                    instanceOf(City.class),
                    textContent().isEmpty()
            )
    ).requireOnlyResult();
```
```java
    Set<XmlElement> largeCities = Query.evaluate(attribute("population").greaterThan(8000000))
        .execute(context.getElementsRecursive(), Collectors.toSet())
        .collect();
```
```java
    context.invoke(() -> {
        if (Query.evaluate(attribute("name").is("France")).count(context.getElements()) == 0) {
            City paris = new City("Paris", 2000000, context);
            Country france = new Country("France", "France", true, Lists.newArrayList(paris), context);
            france.persist();
        }
        if (Query.evaluate(attribute("englishName").startsWith("Swe")).count(context.getElements()) == 0) {
            City stockholm = new City("Stockholm", 950000, context);
            Country sweden = new Country("Sverige", "Sweden", true, Lists.newArrayList(stockholm), context);
            sweden.persist();
        }
    });
```
```java
    XmlElement london = Query.evaluate(attribute("population").greaterEquals(400000))
            .execute(context.getElementsRecursive())
            .order(Order.attribute("name"))
            .requireFirstResult();

    XmlElement zurich = Query.evaluate(attribute("population").greaterEquals(400000))
            .execute(context.getElementsRecursive())
            .order(Order.attribute("name", Order.Direction.DESCENDING))
            .requireFirstResult();
```

## Transaction

All changes made by a task are added to the Context's current transaction. At the end of the task all changes will be applied
to the in memory XmlElements (if instantApply is false, else the changes are applied upon adding them to the transaction
until the Transaction is not recording anymore, i.e. until Transaction#commit is called) and then, if commit is true,
persisted to the XML document and written to the file. If commit is false the transaction will be added to the Context's
uncommitted transactions. These can later be committed by calling Context#commitAll() or reverted by calling
Context#revertAll(). If a commit fails all changes made by the invoked task will be reverted. If committing
a change is known to fail (e.g. (before v0.7) because the affected XmlElement is duplicate) you can use Context#apply to create an
apply-only Transaction to make sure the change never gets committed and commit the change manually using the XmlPersister.
You can get the XmlPersister via context.getPersistenceManager().getXmlPersister()

## EventListener

The EventListener offers 3 methods that get fired when the corresponding Event gets applied plus 2 collecting events
for when a Transaction gets applied or committed. If the transaction is an InstantApplyTx transactionApplied gets fired
after the invoked task is done, however it can still apply changes made by listeners.

If you are using an InstantApplyTx you can simply make changes within the Listener without having to worry about using
the invoke method, safe for the transactionCommitted event. Otherwise you need to use the Context#futureInvoke method
to invoke your task in a new Transaction after the current one has finished since it is not recording and applying
changes anymore.

```java
    /**
     * Fired when an {@link ElementCreatedEvent} was applied
     *
     * @param event the ElementCreatedEvent
     */
    public void elementCreating(ElementCreatedEvent event) {
    }

    /**
     * Fired when an {@link ElementDeletingEvent} was applied
     *
     * @param event the ElementDeletingEvent
     */
    public void elementDeleting(ElementDeletingEvent event) {
    }

    /**
     * Fired when an {@link ElementChangingEvent} was applied
     *
     * @param event the ElementChangingEvent
     */
    public void elementChanging(ElementChangingEvent event) {
    }

    /**
     * Fired after a {@link net.robinfriedli.jxp.persist.Transaction} has been applied (to the in memory elements)
     *
     * @param transaction the transaction that has been applied
     */
    public void transactionApplied(Transaction transaction) {
    }

    /**
     * Fired after a {@link net.robinfriedli.jxp.persist.Transaction} has been committed (to the XML file)
     *
     * @param transaction the transaction that has been committed
     */
    public void transactionCommitted(Transaction transaction) {
    }
```

## StringConverter

StringConverter is a static utility method to convert a String into any other class used by XmlAttribute#getValue and
the ValueComparator for queries. You can extend to support more classes by using StringConverter#map
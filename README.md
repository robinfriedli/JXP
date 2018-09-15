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
      <version>0.3</version>
    </dependency>
```
## AbstractXmlElement and BaseXmlElement

AbstractXmlElement is the class to extend for any class you want to persist to an XML file. Its default implementation,
BaseXmlElement, is instantiated for each XML element in your file when creating a new Context via the
DefaultPersistenceManager#getAllElements method. It is recommended however that you create your own implementation for
AbstractXmlElement which allows you to define a unique id for your element (BaseXmlElement has null as id), which is
useful for loading XmlElements more easily and also prevents duplicate elements. Then instantiate your own class by
overriding DefaultPersistenceManger#getAllElements and pass your implementation of DefaultPersistenceManager to the
ContextManager.

When extending AbstractXmlElement you need to pass the tag name of your XML element, a Map with all attributes
(with the attribute name as key and value as value) and a List of sub elements to the constructor of the super class.
Your XmlElement should have two constructors; one that requires a state and one that doesn't. The one with state is used
for your implementation of the DefaultPersistenceManager#getAllElements method where you should pass XmlElement.State.CLEAN
signaling that this element already exists; the other one requires no State and uses the default State: CONCEPTION.
Initializing an XmlElement in this State triggers and ElementCreatedEvent and requires a Transaction, meaning it should
happen within the Context#invoke method.

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

        public City(String name, int population, State state, Context context) {
            super("city", buildAttributes(name, population), state, context);
        }

        @Nullable
        @Override
        public String getId() {
            return getAttribute("name").getValue();
        }

        private static Map<String, String> buildAttributes(String name, int population) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("name", name);
            attributes.put("population", String.valueOf(population));
            return attributes;
        }
    }

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

        public Country(String name, String englishName, boolean sovereign, List<City> cities, State state, Context context) {
            super("country", buildAttributes(name, englishName, sovereign), Lists.newArrayList(cities), state, context);
        }

        @Nullable
        @Override
        public String getId() {
            return getAttribute("englishName").getValue();
        }

        public static Map<String, String> buildAttributes(String name, String englishName, boolean sovereign) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("name", name);
            attributes.put("englishName", englishName);
            attributes.put("sovereign", Boolean.toString(sovereign));
            return attributes;
        }
    }
```
Default implementation of the DefaultPersistenceManger#getAllElements method:
```java
    public List<XmlElement> getAllElements() {
        List<XmlElement> xmlElements = Lists.newArrayList();
        List<Element> allTopLevelElements = xmlPersister.getAllTopLevelElements();

        for (Element topElement : allTopLevelElements) {
            xmlElements.add(instantiateBaseXmlElement(topElement));
        }

        return xmlElements;
    }

    private XmlElement instantiateBaseXmlElement(Element element) {
        List<Element> subElements = xmlPersister.getChildren(element);
        List<XmlElement> instantiatedSubElems = Lists.newArrayList();
        for (Element subElement : subElements) {
            instantiatedSubElems.add(instantiateBaseXmlElement(subElement));
        }

        NamedNodeMap attributes = element.getAttributes();
        Map<String, String> attributeMap = new HashMap<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            attributeMap.put(attribute.getNodeName(), attribute.getNodeValue());
        }

        Node childNode = element.getChildNodes().item(0);
        String textContent = childNode != null && childNode.getNodeType() == Node.TEXT_NODE && !childNode.getTextContent().trim().equals("") ? childNode.getTextContent() : "";


        return new BaseXmlElement(element.getTagName(), attributeMap, instantiatedSubElems, textContent, XmlElement.State.CLEAN, context);
    }
```
Hint: in your implementation of DefaultPersistenceManager you can use the getContext and getXmlPersister methods to get the
respective classes

## Context, ContextManager and BindableContext

The Context class is the entry point to the persistence layer. It stores all XmlElement instances and is used to make any
changes to elements using the invoke() method. This is required for any action that creates, deletes or changes an XmlElement.
BindableContext is a Context that can be bound to any object and can be retrieved from the ContextManager using that object.
The ContextManager holds the base Context, all BindableContexts, all EventListeners and the path for the XML file.

Using the invoke method:
first parameter: commit (boolean), set true if all changes made during this task should be committed to the XML file
second parameter: the actual task to run, a Callable or Runnable depending on whether your task should return something or not
optional third parameter: any Object to set as this Context's environment variable.

The quickest way to add a new XmlElement to the file:
```java
    context.invoke(true, () -> new Country("Italia", "Italy", true, Lists.newArrayList(rome, florence, venice), context).persist());
```
The environment variable can be anything you might need somewhere els in context with this transaction.
E.g. say you're developing a Discord bot and you've implemented an EventListener that sends a message
after an Element has been added. In this case you could set the MessageChannel the command came from as envVar
to send the message to the right channel.

Example:
```java
    Context context = contextManager.getContext();
    context.invoke(true, () -> {
        Country country = new Country("Schweiz", "Switzerland", true, Lists.newArrayList(this), context);

        Country unitedKingdom = context.getElement("United Kingdom", Country.class);
        unitedKingdom.setAttribute("sovereign", Boolean.toString(true));

        Country france = context.getElement("France", Country.class);
        france.delete();
    });
```
## Transaction

All changes made by a task are added to the Context's current transaction. At the end of the task all changes will be applied
to the in memory XmlElements and then, if commit is true, persisted to the XML file. If commit is false the transaction
will be added to the Context's uncommitted transactions. These can later be commit by calling Context#commitAll() or reverted
by calling Context#revertAll(). If a commit fails all changes will be reverted and the document reloaded. If committing
a change is known to fail (e.g. because the affected XmlElement is duplicate) you can use Context#apply to create an
apply-only Transaction to make sure the change never gets committed and commit the change manually using the XmlPersister.
You can get the XmlPersister via context.getPersistenceManager().getXmlPersister()

## EventListener

The EventListener offers 3 methods that get fired when the corresponding Event gets applied.
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
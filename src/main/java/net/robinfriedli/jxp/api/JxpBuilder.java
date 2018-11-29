package net.robinfriedli.jxp.api;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.robinfriedli.jxp.events.EventListener;
import net.robinfriedli.jxp.persist.DefaultPersistenceManager;
import org.w3c.dom.Document;

public class JxpBuilder {

    private DefaultPersistenceManager persistenceManager;
    private List<EventListener> listeners = Lists.newArrayList();
    private Map<String, Class<? extends XmlElement>> instantiationContributions = new HashMap<>();
    private Map<String, Object> objectsToBindContext = new HashMap<>();
    private Set<File> contextFiles = Sets.newHashSet();
    private Set<Document> contextDocuments = Sets.newHashSet();

    public JxpBuilder setPersistenceManager(DefaultPersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
        return this;
    }

    public JxpBuilder addListeners(EventListener... listeners) {
        this.listeners.addAll(Arrays.asList(listeners));
        return this;
    }

    public JxpBuilder addListeners(Collection<EventListener> listeners) {
        this.listeners.addAll(listeners);
        return this;
    }

    public <C extends XmlElement> JxpBuilder mapClass(String tagName, Class<C> classToInstantiate) {
        instantiationContributions.put(tagName, classToInstantiate);
        return this;
    }

    public <C> JxpBuilder createBoundContext(String id, C objectToBind) {
        objectsToBindContext.put(id, objectToBind);
        return this;
    }

    public JxpBuilder createContext(String xmlPath) {
        contextFiles.add(new File(xmlPath));
        return this;
    }

    public JxpBuilder createContext(File file) {
        contextFiles.add(file);
        return this;
    }

    public JxpBuilder createContext(Document document) {
        contextDocuments.add(document);
        return this;
    }

    public JxpBackend build() {
        if (persistenceManager == null) {
            persistenceManager = new DefaultPersistenceManager();
        }

        JxpBackend jxpBackend = new JxpBackend(persistenceManager, listeners, instantiationContributions);
        contextFiles.forEach(jxpBackend::getContext);
        contextDocuments.forEach(jxpBackend::getContext);

        return jxpBackend;
    }

}

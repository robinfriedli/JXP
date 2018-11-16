package net.robinfriedli.jxp.events;

import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.persist.DefaultPersistenceManager;
import net.robinfriedli.jxp.persist.XmlPersister;

public class ElementCreatedEvent extends Event {

    private XmlElement newParent;

    public ElementCreatedEvent(XmlElement element) {
        super(element);
    }

    public ElementCreatedEvent(XmlElement element, XmlElement newParent) {
        super(element);
        this.newParent = newParent;
    }

    @Override
    public void apply() {
        if (isApplied()) {
            throw new PersistException("Change has already been applied");
        } else {
            XmlElement source = getSource();

            if (newParent != null) {
                source.setParent(newParent);
            }

            if (!source.isSubElement()) {
                source.getContext().addElement(source);
            } else {
                XmlElement parent = source.getParent();
                parent.getSubElements().add(source);
            }
            setApplied(true);
            getSource().getContext().getManager().fireElementCreating(this);
        }
    }

    @Override
    public void revert() {
        if (isApplied()) {
            XmlElement source = getSource();
            if (!source.isSubElement()) {
                source.getContext().removeElement(source);
            } else {
                XmlElement parent = source.getParent();
                parent.getSubElements().remove(source);
                source.removeParent();
            }
        }
    }

    @Override
    public void commit(DefaultPersistenceManager persistenceManager) {
        XmlPersister xmlPersister = persistenceManager.getXmlPersister();
        if (!getSource().isSubElement()) {
            xmlPersister.persistElement(getSource());
        } else {
            xmlPersister.persistElement(getSource(), getSource().getParent().requireElement());
        }
        setCommitted(true);
    }
}

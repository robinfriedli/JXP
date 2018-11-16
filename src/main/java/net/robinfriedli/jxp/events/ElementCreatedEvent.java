package net.robinfriedli.jxp.events;

import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.persist.DefaultPersistenceManager;

public class ElementCreatedEvent extends Event {

    public ElementCreatedEvent(XmlElement element) {
        super(element);
    }

    @Override
    public void apply() {
        if (isApplied()) {
            throw new PersistException("Change has already been applied");
        } else {
            XmlElement source = getSource();
            if (!source.isSubElement()) {
                source.getContext().addElement(source);
                setApplied(true);
                getSource().getContext().getManager().fireElementCreating(this);
            }
        }
    }

    @Override
    public void revert() {
        if (isApplied()) {
            XmlElement source = getSource();
            source.getContext().removeElement(source);
        }
    }

    @Override
    public void commit(DefaultPersistenceManager persistenceManager) {
        if (!getSource().isSubElement()) {
            persistenceManager.getXmlPersister().persistElement(getSource());
            setCommitted(true);
        }
    }
}

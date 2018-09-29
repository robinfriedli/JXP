package net.robinfriedli.jxp.events;

import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.CommitException;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.persist.DefaultPersistenceManager;

public class ElementDeletingEvent extends Event {

    private final XmlElement.State previousState;

    public ElementDeletingEvent(XmlElement element, XmlElement.State previousState) {
        super(element);
        this.previousState = previousState;
    }

    @Override
    public void apply() {
        if (isApplied()) {
            throw new PersistException("Change has already been applied");
        } else {
            getSource().setState(XmlElement.State.DELETION);
            setApplied(true);
            getSource().getContext().getManager().fireElementDeleting(this);
        }
    }

    @Override
    public void revert() {
        if (isApplied()) {
            getSource().setState(previousState);
        }
    }

    @Override
    public void commit(DefaultPersistenceManager persistenceManager) throws CommitException {
        persistenceManager.getXmlPersister().remove(getSource());
        persistenceManager.getContext().removeElement(getSource());
        setCommitted(true);
    }
}

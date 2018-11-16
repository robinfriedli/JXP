package net.robinfriedli.jxp.events;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.persist.DefaultPersistenceManager;

public class ElementDeletingEvent extends Event {

    private XmlElement oldParent;
    private final XmlElement.State oldState;

    public ElementDeletingEvent(XmlElement element, XmlElement.State oldState) {
        super(element);
        this.oldState = oldState;
    }

    @Override
    public void apply() {
        if (isApplied()) {
            throw new PersistException("Change has already been applied");
        } else {
            XmlElement source = getSource();
            if (!source.isSubElement()) {
                source.getContext().removeElement(source);
            } else {
                oldParent = source.getParent();
                oldParent.getSubElements().remove(source);
                source.removeParent();
            }
            setApplied(true);
            source.getContext().getManager().fireElementDeleting(this);
        }
    }

    @Override
    public void revert() {
        if (isApplied()) {
            if (!getSource().isSubElement()) {
                getSource().getContext().addElement(getSource());
            } else {
                oldParent.getSubElements().add(getSource());
                getSource().setParent(oldParent);
            }
            getSource().setState(oldState);
        }
    }

    @Override
    public void commit(DefaultPersistenceManager persistenceManager) {
        getSource().phantomize();
        setCommitted(true);
    }

    @Nullable
    public XmlElement getOldParent() {
        return oldParent;
    }

}

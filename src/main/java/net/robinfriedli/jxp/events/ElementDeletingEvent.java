package net.robinfriedli.jxp.events;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.PersistException;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.DefaultPersistenceManager;

public class ElementDeletingEvent extends Event {

    private XmlElement oldParent;
    private final XmlElement.State oldState;
    private final List<RecursiveDeletingEvent> recursiveDeletingEvents;

    public ElementDeletingEvent(XmlElement element, XmlElement.State oldState) {
        super(element);
        this.oldState = oldState;
        element.setState(XmlElement.State.DELETION);
        recursiveDeletingEvents = Lists.newArrayList();
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

            if (!recursiveDeletingEvents.isEmpty()) {
                recursiveDeletingEvents.forEach(RecursiveDeletingEvent::apply);
            }

            setApplied(true);
            source.getContext().getBackend().fireElementDeleting(this);
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

        if (!recursiveDeletingEvents.isEmpty()) {
            recursiveDeletingEvents.forEach(RecursiveDeletingEvent::revert);
        }

        if (isCommitted()) {
            Context context = getSource().getContext();
            // re-persist the element in a new transaction queued after this one, this ensures that all other changes
            // made to this XmlElement will be reverted so that the XmlElement will be persisted in its former state
            context.futureInvoke(false, false, () -> {
                getSource().persist();
                return null;
            });
        }
    }

    @Override
    public void commit(DefaultPersistenceManager persistenceManager) {
        getSource().phantomize();

        if (!recursiveDeletingEvents.isEmpty()) {
            recursiveDeletingEvents.forEach(recurEvent -> recurEvent.commit(persistenceManager));
        }

        setCommitted(true);
    }

    @Nullable
    public XmlElement getOldParent() {
        return oldParent;
    }

    public void addRecursiveEvent(RecursiveDeletingEvent event) {
        recursiveDeletingEvents.add(event);
    }

    public List<RecursiveDeletingEvent> getRecursiveDeletingEvents() {
        return recursiveDeletingEvents;
    }

}

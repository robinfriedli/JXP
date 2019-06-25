package net.robinfriedli.jxp.events;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.exceptions.PersistException;

/**
 * DeletingEvent for subElements that have been deleted recursively after their parent was deleted. Does not need to
 * re-persist the Element after a failed commit since that will happen via the deleted parent
 */
public class RecursiveDeletingEvent extends ElementDeletingEvent {

    private final XmlElement.State oldState;
    private final ElementDeletingEvent parentEvent;
    private XmlElement oldParent;

    public RecursiveDeletingEvent(XmlElement element, XmlElement.State oldState, ElementDeletingEvent parentEvent) {
        super(element, oldState);
        this.oldState = oldState;
        this.parentEvent = parentEvent;
    }

    public static void createRecursive(XmlElement element, ElementDeletingEvent parentEvent) {
        for (XmlElement subElement : element.getSubElements()) {
            XmlElement.State oldState = subElement.getState();
            RecursiveDeletingEvent deletingEvent = new RecursiveDeletingEvent(subElement, oldState, parentEvent);
            parentEvent.addRecursiveEvent(deletingEvent);
            if (subElement.hasSubElements()) {
                createRecursive(subElement, deletingEvent);
            }
        }
    }

    @Override
    public void apply() {
        if (isApplied()) {
            throw new PersistException("Change has already been applied");
        } else {
            XmlElement source = getSource();
            oldParent = source.getParent();
            oldParent.getSubElements().remove(source);
            source.removeParent();

            if (!getRecursiveDeletingEvents().isEmpty()) {
                getRecursiveDeletingEvents().forEach(RecursiveDeletingEvent::apply);
            }

            setApplied(true);
            source.getContext().getBackend().fireElementDeleting(this);
        }
    }

    @Override
    public void revert() {
        if (isApplied()) {
            oldParent.getSubElements().add(getSource());
            getSource().setParent(oldParent);
            getSource().setState(oldState);
        }

        if (!getRecursiveDeletingEvents().isEmpty()) {
            getRecursiveDeletingEvents().forEach(RecursiveDeletingEvent::revert);
        }
    }

    @Override
    public void commit() {
        getSource().phantomize();

        if (!getRecursiveDeletingEvents().isEmpty()) {
            getRecursiveDeletingEvents().forEach(RecursiveDeletingEvent::commit);
        }

        setCommitted(true);
    }

    @Nullable
    public XmlElement getOldParent() {
        return oldParent;
    }

    public ElementDeletingEvent getParentEvent() {
        return parentEvent;
    }

}

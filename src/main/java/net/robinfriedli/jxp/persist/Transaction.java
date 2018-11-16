package net.robinfriedli.jxp.persist;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.ElementCreatedEvent;
import net.robinfriedli.jxp.events.ElementDeletingEvent;
import net.robinfriedli.jxp.events.Event;
import net.robinfriedli.jxp.exceptions.CommitException;
import net.robinfriedli.jxp.exceptions.PersistException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Holds all {@link Event}s that have been created within the current {@link Context#invoke(boolean, Callable)} method
 * call and applies them to the in memory elements after the Runnable / Callable is done and, if commit is true, saves
 * them to the XML file. Also reverts changes if the transaction failed.
 */
public class Transaction {

    private final Context context;
    private final List<Event> changes;
    private boolean rollback = false;

    public Transaction(Context context, List<Event> changes) {
        this.context = context;
        this.changes = changes;
    }

    public Context getContext() {
        return context;
    }

    public void addChange(Event change) {
        changes.add(change);
    }

    public void addChanges(List<Event> changes) {
        this.changes.addAll(changes);
    }

    public void addChanges(Event... changes) {
        addChanges(Arrays.asList(changes));
    }

    public List<Event> getChanges() {
        return changes;
    }

    public List<ElementCreatedEvent> getCreatedElements() {
        return changes.stream()
            .filter(change -> change instanceof ElementCreatedEvent)
            .map(change -> (ElementCreatedEvent) change)
            .collect(Collectors.toList());
    }

    public List<ElementChangingEvent> getElementChanges() {
        return changes.stream()
            .filter(change -> change instanceof ElementChangingEvent)
            .map(change -> (ElementChangingEvent) change)
            .collect(Collectors.toList());
    }

    public List<ElementChangingEvent> getChangesForElement(XmlElement element) {
        return getElementChanges().stream().filter(change -> change.getSource() == element).collect(Collectors.toList());
    }

    public Set<XmlElement> getChangedElements() {
        return getElementChanges().stream().map(Event::getSource).collect(Collectors.toSet());
    }

    public List<ElementDeletingEvent> getDeletedElements() {
        return changes.stream()
            .filter(change -> change instanceof ElementDeletingEvent)
            .map(change -> (ElementDeletingEvent) change)
            .collect(Collectors.toList());
    }

    public boolean isRollback() {
        return rollback;
    }

    public void apply() {
        for (Event change : changes) {
            try {
                change.apply();
            } catch (PersistException | UnsupportedOperationException e) {
                rollback();
                throw new PersistException("Exception while applying transaction. Rolled back.", e);
            }
        }

        context.getManager().fireTransactionApplied(this);
    }

    public void commit(DefaultPersistenceManager manager) throws CommitException {
        if (!isRollback()) {
            try {
                for (Event change : changes) {
                    if (change.isApplied()) {
                        change.commit(manager);
                    } else {
                        throw new CommitException("Trying to commit a change that has not been applied.");
                    }
                }
            } catch (CommitException e) {
                rollback();
                manager.reload();
                throw new CommitException("Exception during commit. Transaction rolled back.", e);
            }

            context.getManager().fireTransactionCommitted(this);
        } else {
            throw new CommitException("Cannot commit transaction that was rolled back");
        }
    }

    public void rollback() {
        rollback = true;
        changes.forEach(Event::revert);
    }

    public boolean isApplyOnly() {
        return this instanceof ApplyOnlyTx || this instanceof InstantApplyOnlyTx;
    }

    public boolean isInstantApply() {
        return this instanceof InstantApplyTx;
    }

    public static Transaction createTx(Context context) {
        return new Transaction(context, Lists.newArrayList());
    }

    public static ApplyOnlyTx createApplyOnlyTx(Context context) {
        return new ApplyOnlyTx(context, Lists.newArrayList());
    }

    public static InstantApplyTx createInstantApplyTx(Context context) {
        return new InstantApplyTx(context, Lists.newArrayList());
    }

    public static InstantApplyOnlyTx createInstantApplyOnlyTx(Context context) {
        return new InstantApplyOnlyTx(context, Lists.newArrayList());
    }
}

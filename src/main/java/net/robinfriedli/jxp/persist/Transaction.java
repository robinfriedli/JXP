package net.robinfriedli.jxp.persist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.ElementCreatedEvent;
import net.robinfriedli.jxp.events.ElementDeletingEvent;
import net.robinfriedli.jxp.events.Event;
import net.robinfriedli.jxp.exceptions.CommitException;
import net.robinfriedli.jxp.exceptions.PersistException;

/**
 * Holds all {@link Event}s that have been created within the current {@link Context#invoke(Callable)} method
 * call and applies them to the in memory elements after the Runnable / Callable is done and, if commit is true, saves
 * them to the XML file. Also reverts changes if the transaction failed.
 */
public class Transaction {

    private final Context context;
    private final List<Event> changes;
    private final List<QueuedTask> queuedTasks;
    private boolean rollback = false;
    private boolean failed = false;
    private State state;

    public Transaction(Context context) {
        this.context = context;
        this.changes = Lists.newArrayList();
        state = State.RUNNING;
        queuedTasks = Lists.newArrayList();
    }

    public static Transaction createTx(Context context) {
        return new Transaction(context);
    }

    public static ApplyOnlyTx createApplyOnlyTx(Context context) {
        return new ApplyOnlyTx(context);
    }

    public static InstantApplyTx createInstantApplyTx(Context context) {
        return new InstantApplyTx(context);
    }

    public static InstantApplyOnlyTx createInstantApplyOnlyTx(Context context) {
        return new InstantApplyOnlyTx(context);
    }

    public Context getContext() {
        return context;
    }

    public void addChange(Event change) {
        if (isActive()) {
            changes.add(change);
        } else {
            throw new IllegalStateException("Transaction is not recording changes");
        }
    }

    public void addChanges(List<Event> changes) {
        if (isActive()) {
            this.changes.addAll(changes);
        } else {
            throw new IllegalStateException("Transaction is not recording changes");
        }
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

    public Set<XmlElement> getAffectedElements() {
        return changes.stream().map(Event::getSource).collect(Collectors.toSet());
    }

    public boolean isRollback() {
        return rollback;
    }

    public void queueTask(QueuedTask queuedTask) {
        queuedTasks.add(queuedTask);
    }

    public List<QueuedTask> getQueuedTasks() {
        return queuedTasks;
    }

    public void apply() {
        setState(State.APPLYING);
        for (Event change : changes) {
            try {
                change.apply();
            } catch (PersistException | UnsupportedOperationException e) {
                rollback();
                throw new PersistException("Exception while applying transaction. Rolled back.", e);
            }
        }

        setState(State.APPLIED);
        context.getBackend().fireTransactionApplied(this);
    }

    public void commit() throws CommitException {
        if (!isRollback()) {
            setState(State.COMMITTING);
            flush();

            if (context.isPersistent()) {
                StaticXmlParser.writeToFile(context);
            }

            setState(State.COMMITTED);
            context.getBackend().fireTransactionCommitted(this);
        } else {
            throw new CommitException("Cannot commit transaction that was rolled back");
        }
    }

    void flush() throws CommitException {
        context.getBackend().fireOnBeforeFlush(this);
        try {
            for (Event change : changes) {
                if (change.isApplied()) {
                    change.commit();
                } else {
                    throw new CommitException("Trying to commit a change that has not been applied.");
                }
            }
        } catch (Throwable e) {
            rollback();
            throw new CommitException("Exception during commit. Transaction rolled back.", e);
        } finally {
            changes.clear();
            if (context instanceof LazyContext) {
                ((LazyContext) context).clear();
            }
        }
    }

    void rollback() {
        setState(State.ROLLING_BACK);
        rollback = true;
        fail();
        // reverse list so that the first change added is the last one to get rolled back to restore data step by step correctly
        Collections.reverse(changes);
        changes.forEach(Event::revert);
        setState(State.ROLLED_BACK);
    }

    public boolean isApplyOnly() {
        return this instanceof ApplyOnlyTx || this instanceof InstantApplyOnlyTx;
    }

    public boolean isInstantApply() {
        return this instanceof InstantApplyTx;
    }

    public boolean isActive() {
        if (isInstantApply()) {
            return getState().isUseable();
        } else {
            return getState() == State.RUNNING;
        }
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void fail() {
        failed = true;
    }

    public boolean failed() {
        return failed;
    }

    enum State {

        /**
         * the Transaction has been created by the {@link Context#invoke(Runnable)} method and is listening for changes
         */
        RUNNING(true),

        /**
         * The Transaction is applying all changes added to it. For {@link InstantApplyOnlyTx} this state will never
         * occur.
         */
        APPLYING(true),

        /**
         * The changes from this Transaction have been applied to the XmlElements but the Transaction has not been
         * committed yet. Transaction applied event listeners will be fired after this.
         */
        APPLIED(true),

        /**
         * The Transaction is being committed.
         */
        COMMITTING(false),

        /**
         * This Transaction has been fully committed. Transaction committed event listeners will be fired after this.
         */
        COMMITTED(false),

        /**
         * An exception occurred while applying or committing the Transaction meaning all changes will be reverted.
         * This also means any queued tasks added by listeners will be cancelled if cancelOnFailure is true.
         */
        ROLLING_BACK(false),

        /**
         * This Transaction has finished rolling back.
         */
        ROLLED_BACK(false);

        private final boolean useable;

        State(boolean useable) {
            this.useable = useable;
        }

        public boolean isUseable() {
            return useable;
        }

    }
}

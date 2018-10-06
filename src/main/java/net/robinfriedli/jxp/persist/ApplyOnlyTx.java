package net.robinfriedli.jxp.persist;

import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.Event;
import net.robinfriedli.jxp.exceptions.PersistException;

import java.util.List;

/**
 * Type of transaction used by the Context#apply method. Use cautiously when dealing with changes that would break a
 * regular commit, e.g. dealing with elements that can't uniquely be identified. In this case you need to commit the
 * changes manually using the {@link XmlPersister}. Be sure to update the XmlElement's {@link XmlElementShadow} if
 * needed using the {@link net.robinfriedli.jxp.api.XmlElement#updateShadow(ElementChangingEvent)} method
 *
 * Example:
 * <pre>
 *  getContext().apply(() -> emoji.removeKeywords(duplicates));
 *  List<Element> duplicateElements = getXmlPersister().find("keyword", keyword.getTextContent(), emoji);
 *  duplicateElements.forEach(elem -> elem.getParentNode().removeChild(elem));
 * </pre>
 */
public class ApplyOnlyTx extends Transaction {

    public ApplyOnlyTx(Context context, List<Event> changes) {
        super(context, changes);
    }

    @Override
    public void apply() {
        for (Event change : getChanges()) {
            try {
                change.apply();
                if (change instanceof ElementChangingEvent) {
                    change.getSource().removeChange((ElementChangingEvent) change);
                }
            } catch (PersistException | UnsupportedOperationException e) {
                rollback();
                throw new PersistException("Exception while applying transaction. Rolled back.", e);
            }
        }

        getContext().getManager().fireTransactionApplied(this);
    }

    @Override
    public void commit(DefaultPersistenceManager manager) {
        throw new UnsupportedOperationException("Attempting to commit an apply-only Transaction");
    }
}

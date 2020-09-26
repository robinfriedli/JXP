package net.robinfriedli.jxp.exec;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Type used for mutexes contained in a map that automatically removes itself from the map when there is no thread
 * using it anymore.
 */
public class ReferenceCountedMutex<T> {

    private final T key;
    private final Map<T, ReferenceCountedMutex<T>> containingMap;
    private final AtomicInteger rc = new AtomicInteger(0);

    public ReferenceCountedMutex(T key, Map<T, ReferenceCountedMutex<T>> containingMap) {
        this.key = key;
        this.containingMap = containingMap;
    }

    public synchronized int incrementRc() {
        return rc.getAndIncrement();
    }

    /**
     * @return true if the mutex removed itself from the map
     */
    public synchronized boolean decrementRc() {
        int currentRc = rc.decrementAndGet();

        if (currentRc < 1) {
            // decrement rc again to a negative number to mark the mutex as invalid
            rc.decrementAndGet();
            return containingMap.remove(key) != null;
        }

        return false;
    }


}

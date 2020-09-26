package net.robinfriedli.jxp.exec;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Mutex utility that synchronises tasks with a mutex mapped to a value of type T. This utilizes a {@link ReferenceCountedMutex}
 * to make sure mutexes don't stay in the map indefinitely and leak memory while ensuring they are not cleared up too
 * early in a more than 2 threads scenario. E.g. without reference counting the following would happen: thread A has
 * created a mutex and starts executing its task, meanwhile thread B starts, finds the mutex and waits for its lock.
 * Then thread A finishes and removes the mutex from the map and thread B starts executing. At the same time thread C starts
 * and because thread A has already removed the mutex from the map it can't find it anymore and immediately starts executing
 * its task despite thread B still being active. See the comments in code for an illustration.
 *
 * @param <T> type of key the mutexes are mapped to.
 */
public class MutexSync<T> {

    public static final MutexSync<String> GLOBAL_CONTEXT_SYNC = new MutexSync<>();

    private final Map<T, ReferenceCountedMutex<T>> mutexMap = new ConcurrentHashMap<>();

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public <E> E evaluateChecked(T key, Callable<E> toRun) throws Exception {
        ReferenceCountedMutex<T> mutex;

        for (; ; ) {
            mutex = mutexMap.computeIfAbsent(key, k -> new ReferenceCountedMutex<>(k, mutexMap));

            int oldRc = mutex.incrementRc();

            // It is possible that some thread cleared the mutex just after this thread retrieved it from the map
            // but before this thread increments the rc. In this case the thread that removed the mutex also decremented
            // the rc again to a negative integer so we can check here whether the previous rc was indeed negative when
            // incrementing and retry getting the mutex. The order of these operations (the removing thread decrementing
            // rc twice then this thread incrementing rc) is guaranteed to be correct since incrementRc() and decrementRc()
            // are both synchronised methods.
            if (oldRc >= 0) {
                break;
            }
        }

        // -- thread C is here
        synchronized (mutex) {
            try {
                return toRun.call(); // - thread B is still here
            } finally {
                mutex.decrementRc();
                // -- thread A is here (without reference counting it would have removed the mutex)
            }
        }
    }

    public <E> E evaluate(T key, Supplier<E> toRun) {
        try {
            return evaluateChecked(key, toRun::get);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // suppliers do not throw checked exceptions
            throw new RuntimeException(e);
        }
    }

    public void run(T key, Runnable toRun) {
        evaluate(key, () -> {
            toRun.run();
            return null;
        });
    }

}

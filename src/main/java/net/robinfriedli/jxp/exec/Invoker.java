package net.robinfriedli.jxp.exec;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.robinfriedli.jxp.exec.modes.ExecutionMode;

/**
 * Executor that manages running tasks within a transaction
 */
public interface Invoker {

    /**
     * Runs the given callable with the given mode applied
     *
     * @param mode the custom mode that defines the task execution
     * @param task the callable to run
     * @param <E>  the return type of the callable
     * @return the result of the callable
     */
    <E> E invoke(Mode mode, Callable<E> task) throws Exception;

    /**
     * Same as {@link #invoke(Mode, Callable)} but accepts a Runnable and does not throw a checked exception as Runnables
     * cannot throw checked exceptions
     */
    void invoke(Mode mode, Runnable runnable);

    /**
     * Runs the task with the given mode applied and handles checked exceptions by wrapping them into runtime exceptions
     * using the given function.
     *
     * @param exceptionMapper the function that returns a runtime exception based on the caught checked exception
     */
    <E> E invoke(Mode mode, Callable<E> task, Function<Exception, RuntimeException> exceptionMapper);

    /**
     * Runs the task wrapping checked exceptions into {@link RuntimeException}. This is equivalent to calling
     * <code>invoke(mode, task, e -> new RuntimeException(e))</code>
     */
    <E> E invokeChecked(Mode mode, Callable<E> task);

    /**
     * Applies a mode to a task by wrapping the task in a new callable. When iterating over the mode it unwraps the
     * mode that was combined into this one. When applying the mode wrappers to the task this iterator is iterated
     * backwards so that the task of the innermost wrapper is wrapped inside the task of the outer wrapper.
     */
    interface ModeWrapper extends Iterable<ModeWrapper> {

        <E> Callable<E> wrap(Callable<E> callable);

        ModeWrapper combine(ModeWrapper mode);

        @Nullable
        ModeWrapper getDelegate();

        void setDelegate(ModeWrapper delegate);

        @Override
        default Iterator<ModeWrapper> iterator() {
            return new Iterator<ModeWrapper>() {

                private ModeWrapper current = null;

                @Override
                public boolean hasNext() {
                    // current == null means the iterator has not been accessed yet, the iterator always has at least
                    // the current ModeWrapper as item
                    return current == null || current.getDelegate() != null;
                }

                @Override
                public ModeWrapper next() {
                    if (current == null) {
                        // the iterator has not been accessed before
                        current = ModeWrapper.this;
                    } else {
                        current = current.getDelegate();
                    }
                    return current;
                }
            };
        }
    }

    /**
     * Helper class to build a mode with the given {@link ModeWrapper} applied
     */
    class Mode {

        private final ModeWrapper rootWrapper = new ExecutionMode();
        private ModeWrapper currentWrapper = rootWrapper;

        public static Mode create() {
            return new Mode();
        }

        /**
         * Add an additional mode. Depending on the implementation of the {@link ModeWrapper#combine(ModeWrapper)} method,
         * this normally wraps the new wrapper within the current wrapper, meaning the task of the added wrapper will run
         * inside the task of the current wrapper.
         *
         * @param wrapper the new wrapper to add, normally wrapping it inside the current wrapper
         * @return this mode with the new wrapper applied
         */
        public Mode with(ModeWrapper wrapper) {
            this.currentWrapper = this.currentWrapper.combine(wrapper);
            return this;
        }

        public ModeWrapper getRootWrapper() {
            return rootWrapper;
        }

    }

}

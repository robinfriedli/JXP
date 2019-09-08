package net.robinfriedli.jxp.exec;

import java.util.List;
import java.util.concurrent.Callable;

import com.google.common.collect.Lists;

public class InvokerImpl implements Invoker {

    @Override
    public <E> E invoke(Mode mode, Callable<E> task) {
        Callable<E> wrappedTask = task;

        List<ModeWrapper> modeWrappers = Lists.newArrayList(mode.getRootWrapper());
        // iterate wrappers backwards to wrap the innermost wrappers inside the outermost wrappers
        for (int i = modeWrappers.size() - 1; i >= 0; i--) {
            ModeWrapper innerWrapper = modeWrappers.get(i);
            wrappedTask = innerWrapper.wrap(wrappedTask);
        }

        try {
            return wrappedTask.call();
        } catch (Throwable e) {
            throw new RuntimeException("Exception in task", e);
        }
    }

}

package net.robinfriedli.jxp.exec;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

import com.google.common.collect.Lists;

public class BaseInvoker implements Invoker {

    @Override
    public <E> E invoke(Mode mode, Callable<E> task) throws Exception {
        Callable<E> wrappedTask = task;

        List<ModeWrapper> modeWrappers = Lists.newArrayList(mode.getRootWrapper());
        // iterate wrappers backwards to wrap the innermost wrappers inside the outermost wrappers
        for (int i = modeWrappers.size() - 1; i >= 0; i--) {
            ModeWrapper innerWrapper = modeWrappers.get(i);
            wrappedTask = innerWrapper.wrap(wrappedTask);
        }

        return wrappedTask.call();
    }

    @Override
    public void invoke(Mode mode, Runnable runnable) {
        invokeChecked(mode, () -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public <E> E invoke(Mode mode, Callable<E> task, Function<Exception, RuntimeException> exceptionMapper) {
        try {
            return invoke(mode, task);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw exceptionMapper.apply(e);
        }
    }

    @Override
    public <E> E invokeChecked(Mode mode, Callable<E> task) {
        return invoke(mode, task, RuntimeException::new);
    }

}

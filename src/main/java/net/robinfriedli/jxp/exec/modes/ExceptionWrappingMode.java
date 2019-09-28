package net.robinfriedli.jxp.exec.modes;

import java.util.concurrent.Callable;
import java.util.function.Function;

import net.robinfriedli.jxp.exec.AbstractDelegatingModeWrapper;

/**
 * Mode that maps any thrown exception by the wrapped task to any other exception using the provided mapperFunction
 */
public class ExceptionWrappingMode extends AbstractDelegatingModeWrapper {

    private final Function<Throwable, Exception> mapperFunction;

    public ExceptionWrappingMode(Function<Throwable, Exception> mapperFunction) {
        this.mapperFunction = mapperFunction;
    }

    @Override
    public <E> Callable<E> wrap(Callable<E> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (Throwable e) {
                throw mapperFunction.apply(e);
            }
        };
    }

}

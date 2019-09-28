package net.robinfriedli.jxp.exec.modes;

import java.util.concurrent.Callable;

import net.robinfriedli.jxp.api.JxpBackend;
import net.robinfriedli.jxp.exec.AbstractDelegatingModeWrapper;

public class ListenersMutedMode extends AbstractDelegatingModeWrapper {

    private final JxpBackend jxpBackend;

    public ListenersMutedMode(JxpBackend jxpBackend) {
        this.jxpBackend = jxpBackend;
    }

    @Override
    public <E> Callable<E> wrap(Callable<E> callable) {
        return () -> {
            boolean prevState = jxpBackend.isListenersMuted();
            jxpBackend.setListenersMuted(true);
            try {
                return callable.call();
            } finally {
                jxpBackend.setListenersMuted(prevState);
            }
        };
    }

}

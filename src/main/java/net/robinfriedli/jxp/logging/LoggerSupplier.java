package net.robinfriedli.jxp.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerSupplier {

    public static Logger getLogger() {
        Logger logger;
        try {
            // do this even if no StaticLoggerBinder set up so the slf4j message shows
            logger = LoggerFactory.getLogger("JXP");
            Class.forName("org.slf4j.impl.StaticLoggerBinder");
        } catch (ClassNotFoundException e) {
            logger = new ConsoleLogger();
            logger.warn("No Logger set up. Defaulting to ConsoleLogger.");
        }

        return logger;
    }

}

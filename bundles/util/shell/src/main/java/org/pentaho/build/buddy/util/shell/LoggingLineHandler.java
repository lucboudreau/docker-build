package org.pentaho.build.buddy.util.shell;

import org.pentaho.build.buddy.bundles.api.result.LineHandler;
import org.apache.log4j.Level;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by bryan on 2/17/16.
 */
public class LoggingLineHandler implements LineHandler {
    private final Logger logger;
    private final boolean log;
    private final Method logMethod;

    public LoggingLineHandler(Logger logger, Level level) {
        this.logger = logger;
        String levelToString = level.toString();
        String methodName = levelToString.toLowerCase();
        String isEnabled = "is" + levelToString.substring(0, 1).toUpperCase() + levelToString.substring(1).toLowerCase() + "Enabled";
        boolean log = false;
        try {
            log = (boolean) Logger.class.getMethod(isEnabled).invoke(logger);
        } catch (Exception e) {
            logger.error("Unable to determine " + isEnabled, e);
        }
        Method logMethod = null;
        if (log) {
            try {
                logMethod = Logger.class.getMethod(methodName, new Class[]{String.class});
            } catch (NoSuchMethodException e) {
                logger.error("Unable to get method " + methodName, e);
                log = false;
            }
        }
        this.logMethod = logMethod;
        this.log = log;
    }

    @Override
    public void handle(String line) {
        if (log) {
            try {
                logMethod.invoke(logger, line);
            } catch (IllegalAccessException e) {
                logger.error("Unable to invoke " + logMethod, e);
            } catch (InvocationTargetException e) {
                logger.error("Error invoking " + logMethod, e);
            }
        }
    }

    @Override
    public void handle(Exception e) {
        logger.error(e.getMessage(), e);
    }
}

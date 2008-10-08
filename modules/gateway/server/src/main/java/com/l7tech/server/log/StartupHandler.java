package com.l7tech.server.log;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.FileHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.File;

/**
 * Java logging Handler implementation used on java startup.
 *
 * <p>This handler will use either a ConsoleHandler or a FileHandler
 * depending on a system property value.</p>
 *
 * <p>Logging to the console is disabled by setting a system property
 * ({@link #SYSPROP_LOG_TO_CONSOLE})</p>
 *
 * <p>When logging to a file, the output is stopped after the first
 * call the the notifyStarted method.</p>
 *
 * @author Steve Jones
 */
public class StartupHandler extends Handler implements StartupAwareHandler {

    //- PUBLIC

    public static final String SYSPROP_LOG_TO_CONSOLE = StartupHandler.class.getName() + ".logToConsole";

    public StartupHandler() throws IOException {
        if ( logToConsole )  {
            delegate = new ConsoleHandler();
        } else {
            File file = getStartupLogFile();
            String filepath = file.getAbsolutePath();
            delegate =  new FileHandler( filepath, LOG_LIMIT, LOG_COUNT, LOG_APPEND);
        }
    }

    /**
     * Notify that the system is started so output may be stopped.
     */
    public void notifyStarted() {
        enabled.set(false);
    }

    /**
     * 
     */
    public boolean isConsole() {
        return logToConsole;
    }

    @Override
    public void close() throws SecurityException {
        delegate.close();
    }

    @Override
    @SuppressWarnings({"NonSynchronizedMethodOverridesSynchronizedMethod"})
    public void flush() {
        if ( isEnabled() ) {
            delegate.flush();
        }
    }

    @Override
    public String getEncoding() {
         return delegate.getEncoding();
    }

    @Override
    public ErrorManager getErrorManager() {
        return delegate.getErrorManager();
    }

    @Override
    public Filter getFilter() {
        return delegate.getFilter();
    }

    @Override
    public Formatter getFormatter() {
        return delegate.getFormatter();
    }

    @Override
    @SuppressWarnings({"NonSynchronizedMethodOverridesSynchronizedMethod"})
    public Level getLevel() {
        return delegate.getLevel();
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        return isEnabled() && delegate.isLoggable(record);
    }

    @Override
    public void publish(LogRecord record) {
        if ( isEnabled() ) {
            delegate.publish(record);
        }
    }

    @Override
    public void setEncoding(String encoding) throws SecurityException, UnsupportedEncodingException {
        delegate.setEncoding(encoding);
    }

    @Override
    public void setErrorManager(ErrorManager em) {
        delegate.setErrorManager(em);
    }

    @Override
    public void setFilter(Filter newFilter) throws SecurityException {
        delegate.setFilter(newFilter);
    }

    @Override
    public void setFormatter(Formatter newFormatter) throws SecurityException {
        delegate.setFormatter(newFormatter);
    }

    @Override
    @SuppressWarnings({"NonSynchronizedMethodOverridesSynchronizedMethod"})
    public void setLevel(Level newLevel) throws SecurityException {
        delegate.setLevel(newLevel);
    }

    //- PRIVATE

    private static final int LOG_LIMIT = 1024 * 1024; //1M
    private static final int LOG_COUNT = 2; // 1 in use, 1 rotated
    private static final boolean LOG_APPEND = true; // preserve old start info

    private static final AtomicBoolean enabled = new AtomicBoolean(true);

    private final boolean logToConsole = Boolean.valueOf(System.getProperty(SYSPROP_LOG_TO_CONSOLE, "true"));
    private final Handler delegate;

    private boolean isEnabled() {
        return logToConsole || enabled.get();
    }

    private File getStartupLogFile() {                              
        String path = System.getProperty("com.l7tech.server.home", "/opt/SecureSpan/Gateway/node/default");

        return new File(new File(new File(path), "var/logs"), "startup_%g_%u.log");
    }
}

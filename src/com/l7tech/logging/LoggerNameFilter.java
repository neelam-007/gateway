/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.logging;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Filter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * The <code>LoggerNameFilter</code> filter implementation provides a fine
 * grained control by logger name of what is logged. The instance is configured
 * from the logging configuration  with properties :
 * <ul>
 * <li>   com.l7tech.logging.LoggerNameFilter.includes
 * specifies the comma separated list of logger names (or prefixes) that must
 * be included; all loggers are included when omitted.
 * <li>   com.l7tech.logging.LoggerNameFilter.excludes
 * specifies comma separated list of logger names that must be excluded; no loggers
 * are excluded when omitted.
 * </ul>
 * The instance is configured for the <code>Handler</code> using the logging config
 * properties; for example to assign the
 * java.util.logging.FileHandler.filter =
 * <i>Note:
 * When configuring make sure the filter is visible to the class loader that will load
 * the filter; for example the JDK handlers <code>FileHandler</code> and the
 * <code>ConsoleHandler</code> require the filter to be visible to the system classloader,
 * that is, the filter must be specified on the CLASSPATH. The <code>ServerLogHandler</code>
 * will attepmt loading the class using his class loader.
 * </i>
 *
 * @author emil
 * @version Sep 29, 2004
 */
public class LoggerNameFilter implements Filter, PropertyChangeListener {
    private String[] includes = new String[]{};
    private String[] excludes = new String[]{};
    private LogManager manager = LogManager.getLogManager();

    public LoggerNameFilter() {
        configure();
        manager.addPropertyChangeListener(this);
    }

    public String[] getExcludes() {
        return excludes;
    }

    public void setExcludes(String[] excludes) {
        this.excludes = excludes;
    }

    public String[] getIncludes() {
        return includes;
    }

    public void setIncludes(String[] includes) {
        this.includes = includes;
    }


    /**
     * This method gets called when a bound property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        configure();
    }

    private void configure() {
        String cname = LoggerNameFilter.class.getName();
        setIncludes(getStringArrayProperty(cname + ".includes", new String[]{}));
        setExcludes(getStringArrayProperty(cname + ".excludes", new String[]{}));
    }

    /**
     * Check if a given log record should be published. Traverses the list of include
     * logger names (or prefixes); all loggers are included when omitted; and
     * traverses the list of logger names that must be excluded; no loggers are excluded
     * when omitted.
     *
     * @param record a LogRecord
     * @return true if the log record should be published.
     */
    public boolean isLoggable(LogRecord record) {
        boolean included = includes.length == 0;

        for (int i = 0; i < includes.length; i++) {
            String include = includes[i];
            if (record.getLoggerName().startsWith(include)) {
                included = true;
                break;
            }
        }
        for (int i = 0; i < excludes.length; i++) {
            String exclude = excludes[i];
            if (record.getLoggerName().startsWith(exclude)) {
                included = false;
                break;
            }
        }
        return included;
    }

    private String[] getStringArrayProperty(String name, String[] defaultValue) {
        String val = manager.getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        val = val.trim();
        List list = Arrays.asList(val.split(","));
        List out = new ArrayList();
        for (Iterator iterator = list.iterator(); iterator.hasNext();) {
            String s = iterator.next().toString().trim();
            if (s.length() > 0) {
                out.add(s);
            }
        }
        return (String[])out.toArray(new String[]{});
    }
}
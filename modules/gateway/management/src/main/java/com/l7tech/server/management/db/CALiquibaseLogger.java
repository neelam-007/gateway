package com.l7tech.server.management.db;

import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.logging.LogLevel;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adding a logger to capture logging messages from liquibase. This passes the log messages to our logger.
 */
@SuppressWarnings("UnusedDeclaration") //this is actually used! It is referenced by reflection from liquibase. See the LiquibaseDBManager constructor.
public class CALiquibaseLogger implements liquibase.logging.Logger {
    private static final Logger logger = Logger.getLogger(CALiquibaseLogger.class.getName());

    @Override
    public void setName(String name) {
        //do nothing, use our loggers name
    }

    @Override
    public void setLogLevel(String level) {
        //do nothing, use our loggers level
    }

    @Override
    public void setLogLevel(LogLevel level) {
        //do nothing, use our loggers level
    }

    @Override
    public void setLogLevel(String logLevel, String logFile) {
        //do nothing, use our loggers level
    }

    @Override
    public void severe(String message) {
        logger.log(Level.SEVERE, message);
    }

    @Override
    public void severe(String message, Throwable e) {
        logger.log(Level.SEVERE, message, e);
    }

    @Override
    public void warning(String message) {
        logger.log(Level.WARNING, message);
    }

    @Override
    public void warning(String message, Throwable e) {
        logger.log(Level.WARNING, message, e);
    }

    @Override
    public void info(String message) {
        logger.log(Level.INFO, message);
    }

    @Override
    public void info(String message, Throwable e) {
        logger.log(Level.INFO, message, e);
    }

    @Override
    public void debug(String message) {
        logger.log(Level.FINE, message);
    }

    @Override
    public LogLevel getLogLevel() {
        if (Level.SEVERE.equals(logger.getLevel())){
            return LogLevel.SEVERE;
        } else if (Level.WARNING.equals(logger.getLevel())){
            return LogLevel.WARNING;
        } else if (Level.FINE.equals(logger.getLevel()) || Level.FINER.equals(logger.getLevel()) || Level.FINEST.equals(logger.getLevel())){
            return LogLevel.DEBUG;
        } else if (Level.OFF.equals(logger.getLevel())){
            return LogLevel.OFF;
        } else {
            return LogLevel.INFO;
        }
    }

    @Override
    public void debug(String message, Throwable e) {
        logger.log(Level.FINE, message, e);
    }

    @Override
    public void setChangeLog(DatabaseChangeLog databaseChangeLog) {
        //do nothing
    }

    @Override
    public void setChangeSet(ChangeSet changeSet) {
        //do nothing
    }

    @Override
    public int getPriority() {
        return 10;
    }
}

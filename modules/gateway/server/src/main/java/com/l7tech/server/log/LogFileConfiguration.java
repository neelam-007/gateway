package com.l7tech.server.log;

import com.l7tech.common.log.SerializableFilter;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.util.ConfigurableLogFormatter;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * Serialiable bean for storing FileHandler configuration.
 */
public class LogFileConfiguration implements Serializable {

    //- PUBLIC

    public LogFileConfiguration( final String filepat,
                                 final int limit,
                                 final int count,
                                 final boolean append,
                                 final int level,
                                 final String formatPattern,
                                 final SerializableFilter filter,
                                 final boolean rollingEnabled,
                                 final SinkConfiguration.RollingInterval rollingInterval) {
        this.filepat = filepat;
        this.limit = limit;
        this.count = count;
        this.append = append;
        this.level = level;
        this.formatPattern = formatPattern;
        this.filter = filter;
        this.rollingEnabled = rollingEnabled;
        this.rollingInterval = rollingInterval;
    }

    public LogFileConfiguration( final LogFileConfiguration config,
                                 final int level ) {
        this( config.getFilepat(),
              config.getLimit(),
              config.getCount(),
              config.isAppend(),
              level,
              config.getFormatPattern(),
              config.getFilter(),
              config.isRollingEnabled(),
              config.getRollingInterval());
    }

    public Handler buildFileHandler() throws IOException {
        Handler fileHandler;
        if(rollingEnabled){
            fileHandler = new RollingFileHandler(filepat, rollingInterval);
        }
        else {
            fileHandler = new StartupAwareFileHandler( filepat, limit, count, append );
        }
        fileHandler.setFormatter(new ConfigurableLogFormatter(formatPattern));
        fileHandler.setLevel(Level.parse(Integer.toString(level)));
        fileHandler.setFilter(filter);
        return fileHandler;
    }

    public String getFilepat() {
        return filepat;
    }

    public int getLimit() {
        return limit;
    }

    public int getCount() {
        return count;
    }

    public boolean isAppend() {
        return append;
    }

    public int getLevel() {
        return level;
    }

    public String getFormatPattern() {
        return formatPattern;
    }

    public SerializableFilter getFilter() {
        return filter;
    }

    public boolean isRollingEnabled() {
        return rollingEnabled;
    }

    public SinkConfiguration.RollingInterval getRollingInterval() {
        return rollingInterval;
    }

    //- PRIVATE

    private static final long serialVersionUID = 1L;

    private final String filepat;
    private final int limit;
    private final int count;
    private final boolean append;
    private final int level;
    private final String formatPattern;
    private final SerializableFilter filter;
    private final boolean rollingEnabled;
    private final SinkConfiguration.RollingInterval rollingInterval;

    private static final class StartupAwareFileHandler extends FileHandler implements StartupAwareHandler{
        private StartupAwareFileHandler(String pattern, int limit, int count, boolean append) throws IOException, SecurityException {
            super(pattern, limit, count, append);
        }
    }

}

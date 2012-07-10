package com.l7tech.server.log;

import com.l7tech.common.log.SerializableFilter;
import com.l7tech.util.ConfigurableLogFormatter;

import java.io.Serializable;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;

/**
 * Serialiable bean for storing FileHandler configuration.
 */
public class LogFileConfiguration implements Serializable {

    //- PUBLIC

    public LogFileConfiguration( final String filepath,
                                 final int limit,
                                 final int count,
                                 final boolean append,
                                 final int level,
                                 final String formatPattern,
                                 final SerializableFilter filter ) {
        this.filepath = filepath;
        this.limit = limit;
        this.count = count;
        this.append = append;
        this.level = level;
        this.formatPattern = formatPattern;
        this.filter = filter;
    }

    public LogFileConfiguration( final LogFileConfiguration config,
                                 final int level ) {
        this( config.getFilepath(),
              config.getLimit(),
              config.getCount(),
              config.isAppend(),
              level,
              config.getFormatPattern(),
              config.getFilter() );
    }

    public FileHandler buildFileHandler() throws IOException {
        FileHandler fileHandler = new StartupAwareFileHandler( filepath, limit, count, append );
        fileHandler.setFormatter(new ConfigurableLogFormatter(formatPattern));
        fileHandler.setLevel(Level.parse(Integer.toString(level)));
        fileHandler.setFilter(filter);
        return fileHandler;
    }

    public String getFilepath() {
        return filepath;
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

    //- PRIVATE

    private static final long serialVersionUID = 1L;

    private final String filepath;
    private final int limit;
    private final int count;
    private final boolean append;
    private final int level;
    private final String formatPattern;
    private final SerializableFilter filter;

    private static final class StartupAwareFileHandler extends FileHandler implements StartupAwareHandler{
        private StartupAwareFileHandler(String pattern, int limit, int count, boolean append) throws IOException, SecurityException {
            super(pattern, limit, count, append);
        }
    }

}

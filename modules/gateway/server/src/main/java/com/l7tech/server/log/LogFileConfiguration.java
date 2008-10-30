package com.l7tech.server.log;

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

    public LogFileConfiguration( String filepat, int limit, int count, boolean append, int level, String formatPattern  ) {
        this.filepat = filepat;
        this.limit = limit;
        this.count = count;
        this.append = append;
        this.level = level;
        this.formatPattern = formatPattern;        
    }

    public FileHandler buildFileHandler() throws IOException {
        FileHandler fileHandler = new StartupAwareFileHandler( filepat, limit, count, append );
        fileHandler.setFormatter(new ConfigurableLogFormatter(formatPattern));
        fileHandler.setLevel(Level.parse(Integer.toString(level)));
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

    //- PRIVATE

    private static final long serialVersionUID = 1L;

    private final String filepat;
    private final int limit;
    private final int count;
    private final boolean append;
    private final int level;
    private final String formatPattern;

    private static final class StartupAwareFileHandler extends FileHandler implements StartupAwareHandler{
        public StartupAwareFileHandler(String pattern, int limit, int count, boolean append) throws IOException, SecurityException {
            super(pattern, limit, count, append);
        }
    }

}

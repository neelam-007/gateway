package com.l7tech.gateway.common.log;

import java.io.File;
import java.io.Serializable;

/**
 * Information for a log file
 */
public class LogFileInfo implements Cloneable, Serializable {
    private final String name;
    private final long lastModified;

    public LogFileInfo( final File file ) {
        this( file.getName(),
              file.lastModified() );
    }
    public LogFileInfo( final String name, final long lastModified ) {
        this.name = name;
        this.lastModified = lastModified;
    }

    public String getName() {
        return name;
    }

    public long getLastModified() {
        return lastModified;
    }
}

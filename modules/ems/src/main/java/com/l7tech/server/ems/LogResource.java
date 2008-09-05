package com.l7tech.server.ems;

import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.value.ValueMap;
import org.apache.wicket.markup.html.WebResource;
import org.apache.wicket.protocol.http.WebResponse;

import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Web resource for log files. 
 */
public class LogResource extends WebResource {

    //- PUBLIC

    @Override
    public IResourceStream getResourceStream() {
        IResourceStream resource = null;
        ValueMap parameters = getParameters();

        if ( parameters.containsKey("id") ) {
            String name = parameters.getString("id");
            File logFile = getLogFileIfValid( name );
            if ( logFile != null ) {
                logger.fine("Processing resource request for log file '"+logFile.getAbsolutePath()+"'.");
                resource = new FileResourceStream(logFile);
            } else {
                logger.warning("Not processing resource request for log file '"+name+"'.");
            }
        }

        if ( resource == null ){
            logger.warning("Not processing resource request for log file, no id found.");
            resource = new StringResourceStream( "" );
        }

        return resource;
    }

    public static File getLogFileIfValid( final String name ) {
        File files = getLogDirectory();
        File logFile = new File(files, name);

        // ensure file exists and is in the expected location
        return logFile.isFile() && files.equals(logFile.getParentFile()) ? logFile : null;
    }

    public static List<File> listLogFiles() {
        List<File> files = new ArrayList<File>();

        files.addAll(Arrays.asList(getLogDirectory().listFiles(getLogFilter())));

        return files;
    }

    //- PROTECTED

    @Override
    protected void setHeaders(WebResponse webResponse) {
        super.setHeaders(webResponse);

        ValueMap parameters = getParameters();
        if ( "attachment".equals(parameters.getString("disposition"))) {
            File logFile = getLogFileIfValid( parameters.getString("id") );
            if ( logFile != null ) {
                webResponse.setAttachmentHeader( logFile.getName() );
            }
        }
    }

    //- PACKAGE

    static File getLogDirectory() {
        return logDirectoryRef.get();
    }

    static void setLogDirectory( final File logDirectory ) {
        LogResource.logDirectoryRef.set(logDirectory);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(LogResource.class.getName());
    private static final String DEFAULT_LOGS_DIRECTORY = "var/logs";

    private static AtomicReference<File> logDirectoryRef = new AtomicReference<File>(new File(DEFAULT_LOGS_DIRECTORY));

    private static FilenameFilter getLogFilter() {
        return new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".log");
            }
        };
    }

}

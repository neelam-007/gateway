package com.l7tech.server.ems;

import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.value.ValueMap;
import org.apache.wicket.protocol.http.WebResponse;

import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import com.l7tech.gateway.common.security.rbac.AttemptedReadAll;
import com.l7tech.objectmodel.EntityType;

/**
 * Web resource for log files. 
 */
public class LogResource extends SecureResource {

    //- PUBLIC

    /**
     *
     */
    public LogResource() {
        super( new AttemptedReadAll(EntityType.LOG_RECORD) );
        setCacheable( false );
    }

    /**
     * Get file if it is a valid log file.
     *
     * @param name The name of the file.
     * @return The file or null
     */
    public static File getLogFileIfValid( final String name ) {
        File files = getLogDirectory();
        File logFile = new File(files, name);

        // ensure file exists and is in the expected location
        return logFile.isFile() && files.equals(logFile.getParentFile()) ? logFile : null;
    }

    /**
     * List the available log files.
     *
     * @return The list of files (may be empty but not null)
     */
    public static List<File> listLogFiles() {
        List<File> files = new ArrayList<File>();

        File[] fileList = getLogDirectory().listFiles(getLogFilter());
        if ( fileList != null ) {
            files.addAll(Arrays.asList(fileList));
        }

        return files;
    }

    //- PROTECTED

    @Override
    protected IResourceStream getSecureResourceStream() {
        IResourceStream resource = null;
        ValueMap parameters = getParameters();

        if ( parameters.containsKey("id") ) {
            String name = parameters.getString("id");
            File logFile = getLogFileIfValid( name );
            if ( logFile != null ) {
                logger.fine("Processing resource request for log file '"+logFile.getAbsolutePath()+"'.");
                resource = new FileResourceStream(logFile){
                    @Override
                    public String getContentType() {
                        return "text/plain";
                    }
                };
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
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".log");
            }
        };
    }

}

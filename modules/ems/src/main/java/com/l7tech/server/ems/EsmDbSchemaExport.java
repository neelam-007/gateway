package com.l7tech.server.ems;

import com.l7tech.util.ConfigFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import java.io.File;
import java.util.logging.Logger;

/**
 * Bean to export the DB DDL.
 */
public class EsmDbSchemaExport {

    //- PUBLIC

    public static final String PROP_SCRIPT = "com.l7tech.ems.outputDbScript";

    /**
     *
     */
    public EsmDbSchemaExport( final Configuration configuration,
                               final File file ) {
        this.configuration = configuration;
        this.file = file;
    }

    /**
     * Perform export of schema to file.
     */
    public void init() {
        String scriptOption = ConfigFactory.getProperty( PROP_SCRIPT );

        if ( scriptOption != null ) {
            if ( file.getParentFile().isDirectory() ) {
                if ( file.exists() && scriptOption.equals("true") ) {
                    logger.info("Not overwriting existing schema file '"+file.getAbsolutePath()+"'.");
                } else if ( !file.exists() || scriptOption.equals("overwrite") ) {
                    SchemaExport export = new SchemaExport( configuration );
                    export.setOutputFile( file.getAbsolutePath() );
                    export.setFormat( true );
                    export.create( false, false );
                }
            } else {
                logger.warning("The DB script directory does not exist '"+file.getParentFile().getAbsolutePath()+"'.");
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EsmDbSchemaExport.class.getName() );

    private final Configuration configuration;
    private final File file;
}

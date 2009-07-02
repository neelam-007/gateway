/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 24, 2009
 * Time: 11:10:58 AM
 */
package com.l7tech.skunkworks.backuprestore;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import com.l7tech.gateway.config.backuprestore.ImportExportUtilities;
import com.l7tech.gateway.config.backuprestore.Exporter;
import com.l7tech.gateway.config.backuprestore.Backup;
import com.l7tech.gateway.config.backuprestore.BackupRestoreFactory;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.util.FileUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.DatabaseConfig;

public class ExporterTestsOffline {

    private File tmpSsgHome;
    private File tmpSecureSpanHome;

    @Before
    public void setUp() throws IOException {
        final String tmpSecureSpanHomeStr = ImportExportUtilities.createTmpDirectory();
        tmpSecureSpanHome = new File(tmpSecureSpanHomeStr);
        tmpSsgHome = new File(tmpSecureSpanHome, ImportExportUtilities.GATEWAY);
        tmpSsgHome.mkdir();
    }

    @After
    public void tearDown(){
        if(tmpSecureSpanHome != null){
            if(tmpSecureSpanHome.exists()){
                FileUtils.deleteDir(tmpSecureSpanHome);
            }
        }
    }

    /**
     * Copy all test resources into our temporary directory
     * tearDown deletes the tmpSsgHome directory so do not need to worry about cleaning up files copied via this method
     * @throws IOException
     */
    public void createTestEnvironment() throws IOException {
        //Copy resources into this temp directory
        final URL nodePropUrl =
                this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/node.properties");
        final URL ompFileUrl = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/omp.dat");

        final File confDir = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR);
        FileUtils.ensurePath(confDir);
        FileUtils.copyFile(new File(nodePropUrl.getPath()), new File(confDir, ImportExportUtilities.NODE_PROPERTIES));
        FileUtils.copyFile(new File(ompFileUrl.getPath()), new File(confDir, ImportExportUtilities.OMP_DAT));
    }
    
    /**
     * Ensure both the mappings for cluster properties and ip address are created based on known data in a
     * dev / test db 
     */
    @Test
    public void testMappingsCreatedCorrectly() throws Exception {
        createTestEnvironment();
        
        final Class clazz = Exporter.class;
        final Constructor constructor = clazz.getDeclaredConstructor(File.class, PrintStream.class, String.class,
                boolean.class);
        constructor.setAccessible(true);

        final String tmpDir = ImportExportUtilities.createTmpDirectory();
        final Backup backup = BackupRestoreFactory.getBackupInstance(tmpSecureSpanHome, null, "notusedhere", true,
                true, System.out);
        
        final File mappingFile = new File(tmpDir, "mapping.xml");

        final File nodeProp = new File(tmpSsgHome + File.separator +
                ImportExportUtilities.NODE_CONF_DIR, ImportExportUtilities.NODE_PROPERTIES);

        System.setProperty("com.l7tech.util.buildVersion", "5.1.0");        
        final NodeConfig nodeConfig = NodeConfigurationManager.loadNodeConfig("default", nodeProp, true);
        final DatabaseConfig databaseConfig = nodeConfig.getDatabase( DatabaseType.NODE_ALL,
                            NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER );

        final File confDir = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR);
        final File ompFile = new File(confDir, ImportExportUtilities.OMP_DAT);
        final MasterPasswordManager decryptor =
                new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword());
        databaseConfig.setNodePassword( new String(decryptor.decryptPasswordIfEncrypted(databaseConfig.getNodePassword())) );

        backup.backUpComponentMainDb(mappingFile.getAbsolutePath(),databaseConfig);

    }
}

package com.l7tech.server.config;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBInformation;
import org.apache.commons.lang.StringUtils;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.sql.*;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Dec 4, 2007
 * Time: 1:56:02 PM
 */
public class SilentConfigurator {

    private static final Logger logger = Logger.getLogger(SilentConfigurator.class.getName());

    public static final String SYS_PROP_CONFIGDATA_FILENAME = "com.l7tech.config.silentConfigFile";
    byte[] configBlob;

    String configPath = null;
    OSSpecificFunctions osf;

    public SilentConfigurator(OSSpecificFunctions osf) {
        this.osf = osf;
        configPath = System.getProperty(SYS_PROP_CONFIGDATA_FILENAME,"");
    }

    public byte[] loadConfigFromDb(DBInformation dbinfo) {
        logger.info("Connecting to Database using " + dbinfo.getUsername() + "@" + dbinfo.getHostname() + "/" + dbinfo.getDbName());
        InputStream is = null;

        byte[] configBytes = null;
        try {
            if (StringUtils.isNotEmpty(configPath)) {
                is = new FileInputStream(configPath);
                configBytes = HexUtils.slurpStream(is);
            } else {
                //read it from the db
                DBActions dba = new DBActions(osf);
                Connection conn = null;
                Statement stmt = null;
                ResultSet rs = null;

                try {
                    conn = dba.getConnection(dbinfo);
                    stmt = conn.createStatement();
                    rs = stmt.executeQuery("select configdata from config_data");
                    while (rs.next()) {
                      configBytes = rs.getBytes("configdata");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    ResourceUtils.closeQuietly(rs);
                    ResourceUtils.closeQuietly(stmt);
                    ResourceUtils.closeQuietly(conn);
                }
            }
        } catch (FileNotFoundException e) {
            logger.severe("Could not load the configuration: " + ExceptionUtils.getMessage(e));
        } catch (ClassNotFoundException e) {
            logger.severe("Could not load the configuration: " + ExceptionUtils.getMessage(e));
        } catch (IOException e) {
            logger.severe("Could not load the configuration: " + ExceptionUtils.getMessage(e));
        }
        return configBytes;
    }

    public SilentConfigData decryptConfigSettings(String passphrase, byte[] configBytes) {
        XMLDecoder xdec = null;
        SilentConfigData config = null;
        byte[] decryptedConfigBytes = decryptWithPassphrase(configBytes, passphrase);
        xdec = new XMLDecoder(new ByteArrayInputStream(decryptedConfigBytes));
        Object obj = xdec.readObject();
        config = (SilentConfigData) obj;
        return config;
    }

    private byte[] decryptWithPassphrase(byte[] configBytes, String passphrase) {
        //TODO implement decryption
        return configBytes;
    }

    public void saveConfigToDb(DBInformation dbinfo, String passphrase, SilentConfigData configData) {
        OutputStream os = null;
        XMLEncoder xenc = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            xenc = new XMLEncoder(baos);
            xenc.writeObject(configData);
            xenc.close();
            xenc = null;

            byte[] configBytes = encryptWithPassphrase(baos.toByteArray(), passphrase);

            if (StringUtils.isNotEmpty(configPath)) {
                os = new FileOutputStream(configPath);
                HexUtils.spewStream(configBytes, os);
            } else {
                Connection conn = null;
                PreparedStatement stmt = null;
                DBActions dba = null;
                try {
                    dba = new DBActions(osf);
                    conn = dba.getConnection(dbinfo);
                    stmt = conn.prepareStatement("update config_data set configdata=? where objectid=1");
                    stmt.setBinaryStream(1, new ByteArrayInputStream(configBytes), configBytes.length);
                    stmt.addBatch();
                    stmt.executeBatch();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    ResourceUtils.closeQuietly(stmt);
                    ResourceUtils.closeQuietly(conn);
                }
            }
        } catch (FileNotFoundException e) {
            logger.severe("Could not save the configuration: " + ExceptionUtils.getMessage(e));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (xenc != null) xenc.close();
        }
    }

    private byte[] encryptWithPassphrase(byte[] configBytes, String passphrase) {
        //TODO implement the encryption
        return configBytes;
    }
}

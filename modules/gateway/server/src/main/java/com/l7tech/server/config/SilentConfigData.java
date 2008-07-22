package com.l7tech.server.config;

import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.partition.PartitionInformation;

import java.util.Collection;

/**
 * User: megery
 * Date: Dec 5, 2007
 * Time: 2:31:55 PM
 */
public class SilentConfigData {
    private Collection<ConfigurationCommand> commands;
    private DBInformation dbInfo;
    private PartitionInformation partitionInfo;
    private byte[] sslKeystore;
    private byte[] caKeystore;

    private byte[] sslCert;
    private byte[] caCert;

    private KeystoreType keystoreType;

    public Collection<ConfigurationCommand> getCommands() {
        return commands;
    }

    public void setCommands(Collection<ConfigurationCommand> commands) {
        this.commands = commands;
    }

    public DBInformation getDbInfo() {
        return dbInfo;
    }

    public void setDbInfo(DBInformation dbInfo) {
        this.dbInfo = dbInfo;
    }

    public PartitionInformation getPartitionInfo() {
        return partitionInfo;
    }

    public void setPartitionInfo(PartitionInformation partitionInfo) {
        this.partitionInfo = partitionInfo;
    }

    public byte[] getSslKeystore() {
        return sslKeystore;
    }

    public void setSslKeystore(byte[] sslKeystore) {
        this.sslKeystore = sslKeystore;
    }

    public byte[] getCaKeystore() {
        return caKeystore;
    }

    public void setCaKeystore(byte[] caKeystore) {
        this.caKeystore = caKeystore;
    }

    public byte[] getSslCert() {
        return sslCert;
    }

    public void setSslCert(byte[] sslCert) {
        this.sslCert = sslCert;
    }

    public byte[] getCaCert() {
        return caCert;
    }

    public void setCaCert(byte[] caCert) {
        this.caCert = caCert;
    }

    public KeystoreType getKeystoreType() {
        return keystoreType;
    }

    public void setKeystoreType(KeystoreType keystoreType) {
        this.keystoreType = keystoreType;
    }
}

package com.l7tech.server.processcontroller;

import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;
import com.l7tech.server.processcontroller.patching.PatchUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.FileUtils;
import com.l7tech.objectmodel.DeleteException;

import java.io.IOException;
import java.io.File;
import java.security.cert.X509Certificate;
import java.security.PrivateKey;
import java.util.Set;

/**
 * ConfigService implementation for testing; returns mock configuration configuration data that's needed for testing.   
 *
 * @author jbufu
 */
public class ConfigServiceStub implements ConfigService {

    // - PUBLIC

    @Override
    public HostConfig getHost() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public void addServiceNode(NodeConfig node) throws IOException {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public void updateServiceNode(NodeConfig node) throws IOException {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public File getNodeBaseDirectory() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public File getPatchesDirectory() {
        return repository;
    }

    @Override
    public String getPatchesLog() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public String getServicesContextBasePath() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public String getApiEndpoint(ApiWebEndpoint endpoint) {
        return "";
    }

    @Override
    public File getApplianceLibexecDirectory() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public Pair<X509Certificate[], PrivateKey> getSslKeypair() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public int getSslPort() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public String getSslIPAddress() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public Set<X509Certificate> getTrustedRemoteNodeManagementCerts() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public Set<X509Certificate> getTrustedPatchCerts() {
        return trustedPatchCerts;        
    }

    @Override
    public File getJavaBinary() {
        return PatchUtils.getJavaBinary();
    }

    @Override
    public void deleteNode(String nodeName) throws DeleteException, IOException {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public MonitoringConfiguration getCurrentMonitoringConfiguration() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public boolean isResponsibleForClusterMonitoring() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public void pushMonitoringConfiguration(MonitoringConfiguration config) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public boolean isUseSca() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public int getIntProperty(String propertyName, int defaultValue) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    // - PRIVATE

    private Set<X509Certificate> trustedPatchCerts;
    private File repository = createRepositoryDir();

    private File createRepositoryDir() {
        try {
            return FileUtils.createTempDirectory("patchPackages", "", null, false);
        } catch (IOException e) {
            throw new RuntimeException("Can't create temporary sub-directory");
        }
    }
}

package com.l7tech.server.processcontroller.patching;

import org.apache.cxf.interceptor.InInterceptors;
import org.apache.cxf.interceptor.OutFaultInterceptors;

import javax.jws.WebService;
import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.*;
import java.security.cert.X509Certificate;

import com.l7tech.util.IOUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.processcontroller.ConfigService;
import com.l7tech.server.processcontroller.ApiWebEndpoint;
import com.l7tech.server.processcontroller.PCUtils;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.common.io.ProcResult;

/**
 * @author jbufu
 */
@WebService(name="PatchServiceApi",
            targetNamespace="http://ns.l7tech.com/secureSpan/5.0/component/processController/patchServiceApi",
            endpointInterface="com.l7tech.server.processcontroller.patching.PatchServiceApi")
@InInterceptors(interceptors = "org.apache.cxf.interceptor.LoggingInInterceptor")
@OutFaultInterceptors(interceptors = "org.apache.cxf.interceptor.LoggingOutInterceptor")
public class PatchServiceApiImpl implements PatchServiceApi {

    // - PUBLIC

    public PatchServiceApiImpl() { }

    public PatchServiceApiImpl(ConfigService config, PatchPackageManager packageManager, PatchRecordManager recordManager) {
        this.config = config;
        this.packageManager = packageManager;
        this.recordManager = recordManager;
    }

    @Override
    public PatchStatus uploadPatch(byte[] patchData) throws PatchException {
        File tempPatchFile = null;
        try {
            tempPatchFile = File.createTempFile("patchzip", null);
            tempPatchFile.deleteOnExit();
            IOUtils.copyStream(new ByteArrayInputStream(patchData), new FileOutputStream(tempPatchFile));
            PatchPackage patch = new PatchPackageImpl(tempPatchFile);
            checkTrustedCertificates(patch);
            logger.info("Uploading patch: " + patch.getProperty(PatchPackage.Property.ID));
            PatchStatus status = packageManager.savePackage(patch);
            recordManager.save(new PatchRecord(System.currentTimeMillis(), patch.getProperty(PatchPackage.Property.ID), Action.UPLOAD));
            return status;

        } catch (IOException e) {
            throw new PatchException("Error uploading patch file: " + ExceptionUtils.getMessage(e), e);
        } finally {
            if (tempPatchFile != null && tempPatchFile.exists())
                tempPatchFile.delete();
        }

    }

    @Override
    public PatchStatus installPatch(String patchId, Collection<String> nodes) throws PatchException {
        PatchStatus status1 = packageManager.getPackageStatus(patchId);
        if (! PatchStatus.State.UPLOADED.name().equals(status1.getField(PatchStatus.Field.STATE)) &&
            ! PatchStatus.State.ROLLED_BACK.name().equals(status1.getField(PatchStatus.Field.STATE)))
            throw new PatchException("Cannot install patch when status is " + status1.getField(PatchStatus.Field.STATE));

        PatchPackage patch = packageManager.getPackage(patchId);
        ProcResult result;
        String rollback = patch.getProperty(PatchPackage.Property.ROLLBACK_FOR_ID);
        if (rollback != null) {
            if (! PatchStatus.State.INSTALLED.name().equals(packageManager.getPackageStatus(rollback).getField(PatchStatus.Field.STATE)))
                throw new PatchException("Package ID " + rollback + " is not INSTALLED, it cannot be rolled back.");
            if (! Boolean.toString(true).equals(packageManager.getPackage(rollback).getProperty(PatchPackage.Property.ROLLBACK_ALLOWED)))
                throw new PatchException("Package ID " + rollback + " does not allow rollback.");
            logger.log(Level.INFO, "Installing patch " + patchId + ", rollback for " + rollback);
        } else {
            logger.log(Level.INFO, "Installing patch " + patchId);
        }

        try {
            // todo: exec with timeout?
            List<String> commandLine = new ArrayList<String>();
            getPatchLauncher(commandLine, patch);
            getInstallParams(commandLine, patch, nodes);
            logger.log(Level.INFO, "Executing " + commandLine);
            result = ProcUtils.exec(commandLine.toArray(new String[commandLine.size()]));
            recordManager.save(new PatchRecord(System.currentTimeMillis(), patchId, Action.INSTALL, nodes));
            if (rollback != null)
                recordManager.save(new PatchRecord(System.currentTimeMillis(), rollback, Action.ROLLBACK, nodes));
        } catch (IOException e) {
            throw new PatchException("Error installing patch: " + patchId + " : " + ExceptionUtils.getMessage(e), e);
        }

        // check exec result and update status
        PatchStatus status2;
        if (result.getExitStatus() == 0) {
            status1.setField(PatchStatus.Field.STATE, PatchStatus.State.INSTALLED.name());
            byte[] output = result.getOutput();
            status1.setField(PatchStatus.Field.STATUS_MSG, output != null ? new String(output) : "");
            status2 = packageManager.setPackageStatus(status1);
            if (rollback != null)
                packageManager.updatePackageStatus(rollback, PatchStatus.Field.STATE, PatchStatus.State.ROLLED_BACK.name());
            logger.log(Level.INFO, "Patch " + patchId + " is installed.");
        } else {
            byte[] errOut = result.getOutput();
            status1.setField(PatchStatus.Field.STATE, PatchStatus.State.ERROR.name());
            status1.setField(PatchStatus.Field.STATUS_MSG, errOut != null ? new String(errOut) : "");
            status2 = packageManager.setPackageStatus(status1);
            logger.log(Level.WARNING, "Error installing " + patchId + " : " + new String(errOut));
        }

        return status2;
    }

    @Override
    public PatchStatus deletePackageArchive(String patchId) throws PatchException {
        logger.log(Level.INFO, "Deleting patch ID: " + patchId);
        PatchStatus status = packageManager.deletePackage(patchId);
        recordManager.save(new PatchRecord(System.currentTimeMillis(), patchId, Action.PACKAGE_DELETE));
        return status;
    }

    @Override
    public Collection<PatchStatus> listPatches() {
        logger.log(Level.INFO, "Listing patches...");
        Collection<PatchStatus> statuses = packageManager.listPatches();
        recordManager.save(new PatchRecord(System.currentTimeMillis(), "", Action.LIST));
        return statuses;
    }

    @Override
    public PatchStatus getStatus(String patchId) throws PatchException {
        logger.log(Level.INFO, "Getting status for patch ID: " + patchId);
        PatchStatus patchStatus = packageManager.getPackageStatus(patchId);
        recordManager.save(new PatchRecord(System.currentTimeMillis(), patchId, Action.STATUS));
        return patchStatus;
    }

    // - PRIVATE

    @Resource
    private PatchPackageManager packageManager;

    @Resource
    private PatchRecordManager recordManager;

    @Resource
    private ConfigService config;

    private static final Logger logger = Logger.getLogger(PatchServiceApiImpl.class.getName());

    private static final String APPLIANCE_PATCH_LAUNCHER = "/opt/SecureSpan/Appliance/libexec/patch_launcher";

    private void checkTrustedCertificates(PatchPackage patch) throws PatchException {
        for(List<X509Certificate> certPath : patch.getCertificatePaths()) {
            X509Certificate signer = certPath.get(0); // only verify individual certs, not certificate paths to trusted CAs
            boolean isTrusted = false;
            for(X509Certificate trusted : config.getTrustedPatchCerts()) {
                if (CertUtils.certsAreEqual(trusted, signer)) {
                    isTrusted = true;
                    break;
                }
            }
            if (! isTrusted)
                throw new PatchException("Certificate is not trusted for signing patches: " + signer);
        }
        logger.log(Level.FINE, "Patch signature is trusted for " + patch.getProperty(PatchPackage.Property.ID));
    }

    /** Builds the parameter list to be passed to the patch installer */
    private void getInstallParams(List<String> commandLine, PatchPackage patch, Collection<String> nodes) {
        commandLine.add("-D" + ApiWebEndpoint.NODE_MANAGEMENT.getPropName() + "=" + config.getApiEndpoint(ApiWebEndpoint.NODE_MANAGEMENT));
        commandLine.add("-D" + ApiWebEndpoint.OS.getPropName() + "=" + config.getApiEndpoint(ApiWebEndpoint.OS));
        //commandLine.add("-Xdebug");
        //commandLine.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8998");
        if(nodes != null && ! nodes.isEmpty()) {
            StringBuilder nodeList = new StringBuilder();
            for(String node : nodes) {
                nodeList.append(node).append(",");
            }
            nodeList.deleteCharAt(nodeList.length() -1);
            commandLine.add("-D" + TARGET_NODE_IDS + "=" + nodeList.toString());
        }
        commandLine.add("-jar"); commandLine.add(patch.getFile().getAbsolutePath());
    }

    private void getPatchLauncher(List<String> commandLine, PatchPackage patch) {
        if (PCUtils.isAppliance()) {
            commandLine.add("/usr/bin/sudo");
            commandLine.add(APPLIANCE_PATCH_LAUNCHER);
        }
        commandLine.add(getJavaBinary(patch));
    }

    private String getJavaBinary(PatchPackage patch) {
        String requestedJava = patch.getProperty(PatchPackage.Property.JAVA_BINARY);
        return requestedJava != null && ! "".equals(requestedJava) ? requestedJava : config.getJavaBinary().getAbsolutePath();
    }
}

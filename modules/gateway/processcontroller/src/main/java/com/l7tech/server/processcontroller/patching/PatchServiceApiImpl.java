package com.l7tech.server.processcontroller.patching;

import org.apache.cxf.interceptor.InInterceptors;
import org.apache.cxf.interceptor.OutFaultInterceptors;

import javax.jws.WebService;
import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.security.cert.X509Certificate;

import com.l7tech.util.IOUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.processcontroller.ConfigService;
import com.l7tech.server.processcontroller.ApiWebEndpoint;
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

    @Override
    public PatchStatus uploadPatch(byte[] patchData) throws PatchException {
        File tempPatchFile = null;
        try {
            tempPatchFile = File.createTempFile("patchzip", null);
            tempPatchFile.deleteOnExit();
            IOUtils.copyStream(new ByteArrayInputStream(patchData), new FileOutputStream(tempPatchFile));
            PatchPackage patch = new PatchPackageImpl(tempPatchFile);
            checkTrustedCertificates(patch);
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
        PatchPackage patch = packageManager.getPackage(patchId);

        ProcResult result;
        try {
            // todo: exec with timeout?
            result = ProcUtils.exec(getJavaBinary(patch), getInstallParams(patch, nodes));
            recordManager.save(new PatchRecord(System.currentTimeMillis(), patchId, Action.INSTALL));
            String rollback = patch.getProperty(PatchPackage.Property.ROLLBACK_FOR_ID);
            if (rollback != null)
                recordManager.save(new PatchRecord(System.currentTimeMillis(), patchId, Action.ROLLBACK));
        } catch (IOException e) {
            throw new PatchException("Error installing patch: " + patchId + " : " + ExceptionUtils.getMessage(e), e);
        }

        // check exec result and update status
        PatchStatus status;
        if (result.getExitStatus() == 0) {
            status = packageManager.setPackageStatus(patchId, PatchStatus.State.INSTALLED, null, patch.getProperty(PatchPackage.Property.ROLLBACK_FOR_ID));
        } else {
            byte[] errOut = result.getOutput();
            status = packageManager.setPackageStatus(patchId, PatchStatus.State.ERROR, errOut != null ? new String(errOut) : "", patch.getProperty(PatchPackage.Property.ROLLBACK_FOR_ID));
        }

        return status;
    }

    @Override
    public PatchStatus deletePackageArchive(String patchId) throws PatchException {
        PatchStatus status = packageManager.deletePackage(patchId);
        recordManager.save(new PatchRecord(System.currentTimeMillis(), patchId, Action.PACKAGE_DELETE));
        return status;
    }

    @Override
    public Collection<PatchStatus> listPatches() {
        return packageManager.listPatches();
    }

    @Override
    public PatchStatus getStatus(String patchId) throws PatchException {
        return packageManager.getPackageStatus(patchId);
    }

    // - PRIVATE

    @Resource
    private PatchPackageManager packageManager;

    @Resource
    private PatchRecordManager recordManager;

    @Resource
    private ConfigService config;

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
    }

    /** Builds the parameter list to be passed to the patch installer */
    private String[] getInstallParams(PatchPackage patch, Collection<String> nodes) {
        List<String> params = new ArrayList<String>();
        params.add("-jar"); params.add(patch.getFile().getAbsolutePath());
        params.add("-D" + ApiWebEndpoint.NODE_MANAGEMENT.getPropName() + "=" + config.getApiEndpoint(ApiWebEndpoint.NODE_MANAGEMENT));
        params.add("-D" + ApiWebEndpoint.OS.getPropName() + "=" + config.getApiEndpoint(ApiWebEndpoint.OS));
        if(nodes != null && ! nodes.isEmpty()) {
            StringBuilder nodeList = new StringBuilder();
            for(String node : nodes) {
                nodeList.append(node).append(",");
            }
            nodeList.deleteCharAt(nodeList.length() -1);
            params.add("-D" + TARGET_NODE_IDS + "=" + nodeList.toString());
        }

        return params.toArray(new String[params.size()]);
    }

    private File getJavaBinary(PatchPackage patch) {
        String requestedJava = patch.getProperty(PatchPackage.Property.JAVA_BINARY);
        return requestedJava != null && ! "".equals(requestedJava) ? new File(requestedJava) : config.getJavaBinary();
    }
}

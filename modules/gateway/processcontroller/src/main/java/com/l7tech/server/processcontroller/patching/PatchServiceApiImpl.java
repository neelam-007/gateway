package com.l7tech.server.processcontroller.patching;

import com.l7tech.server.processcontroller.ApiWebEndpoint;
import com.l7tech.server.processcontroller.ConfigService;
import com.l7tech.server.processcontroller.PCUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;

import javax.activation.DataHandler;
import javax.inject.Inject;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author jbufu
 */
public class PatchServiceApiImpl implements PatchServiceApi {

    // - PUBLIC

    public PatchServiceApiImpl() { }

    public PatchServiceApiImpl(ConfigService config, PatchPackageManager packageManager, PatchRecordManager recordManager) {
        this.config = config;
        this.packageManager = packageManager;
        this.recordManager = recordManager;
    }

    @Override
    public PatchStatus uploadPatch(DataHandler patchData) throws PatchException {
        File tempPatchFile = null;
        try {
            tempPatchFile = File.createTempFile("patchzip", null);
            tempPatchFile.deleteOnExit();
            IOUtils.copyStream(patchData.getInputStream(), new FileOutputStream(tempPatchFile));
            PatchPackage patch = PatchVerifier.getVerifiedPackage(tempPatchFile, config.getTrustedPatchCerts());
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
        verifyNodes( nodes );

        PatchPackage patch = packageManager.getPackage(patchId);

        int processExitCode;

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
            List<String> commandLine = new ArrayList<>();
            getPatchLauncher(commandLine, patch);
            getInstallParams(commandLine, patch, nodes);
            logger.log(Level.INFO, "Executing " + commandLine);

            ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            InputStream processStdOut = process.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(processStdOut);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
                System.out.flush();
                logger.log(Level.INFO, "Output from patch install: " + line);
            }
            processExitCode = process.waitFor();
            System.out.println("Patch exit code: " + processExitCode);
            logger.log(Level.INFO, "Patch exit code: " + processExitCode);

            recordManager.save(new PatchRecord(System.currentTimeMillis(), patchId, Action.INSTALL, nodes));
            if (rollback != null)
                recordManager.save(new PatchRecord(System.currentTimeMillis(), rollback, Action.ROLLBACK, nodes));
        }
        catch (IOException | InterruptedException e) {
            throw new PatchException("Error installing patch: " + patchId + " : " + ExceptionUtils.getMessage(e), e);
        }

        // check exec result and update status
        PatchStatus status2;
        if(processExitCode == 0) {
            // revert any rollback's statuses from INSTALLED to ROLLED_BACK
            if (PatchStatus.State.ROLLED_BACK.name().equals(status1.getField(PatchStatus.Field.STATE))) {
                for(String maybeInstalledRolledback : packageManager.getRollbacksFor(patchId)) {
                    PatchStatus rollbackStatus = packageManager.getPackageStatus(maybeInstalledRolledback);
                    if (PatchStatus.State.INSTALLED.name().equals(rollbackStatus.getField(PatchStatus.Field.STATE))) {
                        packageManager.updatePackageStatus(maybeInstalledRolledback, PatchStatus.Field.STATE, PatchStatus.State.ROLLED_BACK.name());
                        recordManager.save(new PatchRecord(System.currentTimeMillis(), maybeInstalledRolledback, Action.ROLLBACK));
                    }
                }
            }

            status1.setField(PatchStatus.Field.STATE, PatchStatus.State.INSTALLED.name());
            status1.setField(PatchStatus.Field.STATUS_MSG, "");
            status2 = packageManager.setPackageStatus(status1);
            if (rollback != null)
                packageManager.updatePackageStatus(rollback, PatchStatus.Field.STATE, PatchStatus.State.ROLLED_BACK.name());
            logger.log(Level.INFO, "Patch " + patchId + " is installed.");
        } else {
            String errMsg = "";
            status1.setField(PatchStatus.Field.STATE, PatchStatus.State.ERROR.name());
            status1.setField(PatchStatus.Field.STATUS_MSG, errMsg);
            status2 = packageManager.setPackageStatus(status1);
            logger.log(Level.WARNING, "Error installing " + patchId);
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

    private static final String NODE_PATTERN = "[a-zA-Z0-9_-]{1,1024}";

    @Inject
    private PatchPackageManager packageManager;

    @Inject
    private PatchRecordManager recordManager;

    @Inject
    private ConfigService config;

    private static final Logger logger = Logger.getLogger(PatchServiceApiImpl.class.getName());

    private static final String APPLIANCE_PATCH_LAUNCHER = "/opt/SecureSpan/Appliance/libexec/patch_launcher";

    /** Builds the parameter list to be passed to the patch installer */
    private void getInstallParams(List<String> commandLine, PatchPackage patch, Collection<String> nodes) {
        commandLine.add("-D" + ApiWebEndpoint.NODE_MANAGEMENT.getPropName() + "=" + config.getApiEndpoint(ApiWebEndpoint.NODE_MANAGEMENT));
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

    private void verifyNodes( final Collection<String> nodes ) throws PatchException {
        if ( nodes != null ) {
            final Pattern pattern = Pattern.compile(NODE_PATTERN);
            for ( final String node : nodes ) {
                if ( !pattern.matcher( node ).matches() ) {
                    throw new PatchException("Invalid node '"+node+"'");
                }
            }
        }
    }
}

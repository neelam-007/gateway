package com.l7tech.server.config.packageupdater.installer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.Arrays;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 3:16:28 PM
 */
public class UpdatePackageInstaller {
    private File stagesDir;
    private File[] checkFiles;

    public UpdatePackageInstaller() {
        stagesDir = new File("bin\\stages");
    }

    public static void main(String[] args) {
        UpdatePackageInstaller installer = new UpdatePackageInstaller();
        try {
            installer.doInstall();
        } catch (InstallerException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    public void doInstall() throws InstallerException {
        ensureCheckScripts();
        System.out.println("Performing the package update pre-checks");
        executeCheckScripts();
        executeBinScripts();
    }

    private void executeBinScripts() throws InstallerException {
        File[] stages = stagesDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return !name.equalsIgnoreCase("check");
            }
        });

        Arrays.sort(stages);
        for (File stage : stages) {
            executeScriptsInDirectory(stage);
        }
    }

    private void executeScriptsInDirectory(File stage) throws InstallerException {
        File[] stageFiles = stage.listFiles();
        for (File stageFile : stageFiles) {
            executeSingleScript(stageFile);
        }
    }

    private void executeCheckScripts() throws InstallerException {
        for (File checkFile : checkFiles) {
            executeSingleScript(checkFile);
        }
    }

    private void executeSingleScript(File scriptFile) throws InstallerException {
        ProcessBuilder pb = new ProcessBuilder(scriptFile.getName());
        pb.directory(scriptFile.getParentFile());
        pb.redirectErrorStream(true);
        Process p;
        InputStream is = null;
        try {
            p = pb.start();
            is = p.getInputStream();
            byte[] buf = new byte[512];
            while(is.read(buf) != -1) {
                System.out.println(new String(buf));
            }
            int retCode = p.waitFor();
            if (retCode != 0) {
                throw new InstallerException(MessageFormat.format("Script \"{0}\" executed and returned a non-zero result. Aborting the update!", scriptFile.getAbsolutePath()));
            }

        } catch (IOException e) {
            throw new InstallerException(MessageFormat.format("Could not execute script \"{0}\". {1}", scriptFile.getAbsolutePath(), e.getMessage()));
        } catch (InterruptedException e) {
            throw new InstallerException(MessageFormat.format("Could not execute script \"{0}\". {1}", scriptFile.getAbsolutePath(), e.getMessage()));
        } finally {
            if (is != null) try {is.close();} catch (IOException e) {}
        }

    }

    private void ensureCheckScripts() throws InstallerException {
        File checkDir = new File(stagesDir, "check");
        if (!checkDir.exists())
            throw new InstallerException("Cannot proceed without a pre-update check. Please rebuild the update package");

        checkFiles = checkDir.listFiles();
        if (checkFiles == null || checkFiles.length == 0)
            throw new InstallerException("Cannot proceed without a pre-update check. Please rebuild the update package");       
    }

    public class InstallerException extends Exception {

        public InstallerException(String message) {
            super(message);
        }

        public InstallerException(String message, Throwable cause) {
            super(message, cause);
        }

        public InstallerException(Throwable cause) {
            super(cause);
        }
    }
}

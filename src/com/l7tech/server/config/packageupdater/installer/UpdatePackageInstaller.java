package com.l7tech.server.config.packageupdater.installer;

import java.io.*;
import java.text.MessageFormat;
import java.util.Set;
import java.util.TreeSet;
import java.util.Arrays;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 3:16:28 PM
 */
public class UpdatePackageInstaller {
    private static final String BIN_DIRECTORY = "bin";
    Set<File> checkFiles;
    private File checkDir;
    private File stagesDir;
    private File binDir;

    public UpdatePackageInstaller() {
        checkFiles = new TreeSet<File>();
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
        ensureStructure();
        System.out.println("Found expected directory structure.");

        ensureCheckScripts();
        System.out.println("Found pre-update checks, now checking to see if it's ok to install the updates.");

        executeCheckScripts();

        executeBinScripts();
    }

    private void ensureStructure() throws InstallerException {
        binDir = new File("bin");
        if (!binDir.exists())
            throw new InstallerException("No bin directory found. Cannot proceed with the update");

        stagesDir = new File(binDir, "stages");
        if (!stagesDir.exists())
            throw new InstallerException("No stages directory found. Cannot proceed with the update");

        checkDir = new File(stagesDir, "check");
        if (!checkDir.exists())
            throw new InstallerException("No check directory found. Cannot proceed with the update");
    }

    private void executeBinScripts() throws InstallerException {
        File[] stages = binDir.listFiles(new FilenameFilter() {
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
            System.out.println("Executing " + scriptFile.getAbsolutePath());
            p = pb.start();
            BufferedInputStream bis = new BufferedInputStream(p.getInputStream());
            byte[] buf = new byte[512];
            while(bis.read(buf) != -1) {
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
        File[] checkFilesList = checkDir.listFiles();
        if (checkFilesList == null) {
            throw new InstallerException("No pre-update check scripts were found. Cannot proceed with update");            
        }

        for (File checkFileName : checkFilesList) {
            checkFiles.add(checkFileName);
        }
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

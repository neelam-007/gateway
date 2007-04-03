package com.l7tech.server.config.packageupdater.installer;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 3:16:28 PM
 */
public class UpdatePackageInstaller {
    private static final String BIN_DIRECTORY = "bin";
    private static final String STAGES_DIRECTORY = "stages";
    private static final String CHECK_DIRECTORY = "check";
    private static final String PASSED_STRING = " - PASSED";
    Set<File> checkFiles;
    private File baseDir;
    private File checkDir;
    private File  binDir;
    private File  stagesDir;

    public UpdatePackageInstaller(File workingDirectory) throws InstallerException {
        if (workingDirectory == null)
            workingDirectory = new File(".");

        if (!workingDirectory.exists()) {
            throw new InstallerException("Error while initializing the installer. Could not determine the base directory.");
        }
        baseDir = workingDirectory;
        checkFiles = new TreeSet<File>();
    }

    public static void main(String[] args) throws Exception {
        UpdatePackageInstaller installer = new UpdatePackageInstaller(null);
        int retCode = installer.doInstall();
        if (retCode != 0)
            System.out.println("Error while running the update installer.");
        else
            System.out.println("The update installer compleleted successfully.");
    }

    public int doInstall() {
        int status = 0;
        try {
            ensureStructure();
            ensureCheckScripts();

            printMsg("");   //empty line
            executeCheckScripts();

            printMsg("");   //empty line
            executeBinScripts();
        } catch (InstallerException e) {
            System.out.println("*************************************************************");
            System.out.println(e.getMessage());
            System.out.println("*************************************************************");
            status = 1;
        }
        return status;
    }

    private void ensureStructure() throws InstallerException {
        String msg = "Checking for expected directory structure at " + baseDir.getAbsolutePath();
        try {
            ensureBinDirectory();
            ensureStagesDirectory();
            ensureCheckDir();
            msg += PASSED_STRING;
        } finally {
            printMsg(msg);
        }
    }

    private void ensureBinDirectory() throws InstallerException {
        binDir = new File(baseDir, BIN_DIRECTORY);
        if (!binDir.exists())
            throw new InstallerException("No bin directory found. Cannot proceed with the update");
    }

    private void ensureStagesDirectory() throws InstallerException {
        if (binDir == null)
            ensureBinDirectory();

        stagesDir= new File(binDir, STAGES_DIRECTORY);
        if (!stagesDir.exists())
            throw new InstallerException("No stages directory found. Cannot proceed with the update");
    }

    private void ensureCheckDir() throws InstallerException {
        checkDir = new File(baseDir, BIN_DIRECTORY + File.separator + STAGES_DIRECTORY + File.separator+ CHECK_DIRECTORY);
        if (!checkDir.exists())
            throw new InstallerException("No check directory found. Cannot proceed with the update");
    }

    private void executeBinScripts() throws InstallerException {
        File[] stages = stagesDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return (pathname != null) && !CHECK_DIRECTORY.equals(pathname.getName());
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
        printMsg("Executing pre-update checks.");
        for (File checkFile : checkFiles) {
            executeSingleScript(checkFile);
        }
        printMsg("All checks completed successfully");
    }

    private void executeSingleScript(File scriptFile) throws InstallerException {
        List<String> commandLine = getCommandLineForPlatform(scriptFile.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(commandLine);
        pb.directory(scriptFile.getParentFile());
        pb.redirectErrorStream(true);
        Process p;
        InputStream is = null;
        try {
            printMsg("Executing " + pb.command());
            p = pb.start();
            BufferedInputStream bis = new BufferedInputStream(p.getInputStream());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[512];
            int got = 0;
            while((got = bis.read(buf)) > 0) {
                out.write(buf, 0, got);
            }
            printMsg(new String(out.toByteArray()));

            int retCode = p.waitFor();
            if (retCode != 0) {
                throw new InstallerException(MessageFormat.format("Script \"{0}\" executed and returned a non-zero result. Aborting the update!", scriptFile.getAbsolutePath()));
            }
            printMsg(pb.command() + " executed successfully and returned " + retCode);
        } catch (IOException e) {
            throw new InstallerException(MessageFormat.format("Could not execute script \"{0}\". {1}", pb.command(), e.getMessage()));
        } catch (InterruptedException e) {
            throw new InstallerException(MessageFormat.format("Could not execute script \"{0}\". {1}", pb.command(), e.getMessage()));
        } finally {
            if (is != null) try {is.close();} catch (IOException e) {}
        }

    }

    private void printMsg(String msg) {
        if (msg == null || msg.length() == 0)
            return;
        System.out.println(msg);
    }

    private List<String> getCommandLineForPlatform(String absolutePath) {
        List<String> commands = new ArrayList<String>();
        if (isUnix())
            commands.add("sh");
        commands.add(absolutePath);
        return commands;
    }

    private boolean isUnix() {
        String osName = System.getProperty("os.name");
        if (osName != null) {
            if (osName.contains("Windows"))
                return false;
            return true;
        }
        //assume unix if we don't know what this is
        return true;
    }

    private void ensureCheckScripts() throws InstallerException {
        String msg = "Checking for pre-update check scripts.";
        try {
            File[] checkFilesList = checkDir.listFiles();
            if (checkFilesList == null) {
                throw new InstallerException("No pre-update check scripts were found. Cannot proceed with update");
            }

            for (File checkFileName : checkFilesList) {
                checkFiles.add(checkFileName);
            }
            msg += PASSED_STRING;
        } finally {
            printMsg(msg);
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

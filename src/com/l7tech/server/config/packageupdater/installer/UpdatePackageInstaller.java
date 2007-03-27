package com.l7tech.server.config.packageupdater.installer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Set;
import java.util.TreeSet;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 3:16:28 PM
 */
public class UpdatePackageInstaller {
    private static final String BIN_DIRECTORY = "bin";
    Set<File> checkFiles;
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
        ensureCheckScripts();
        System.out.println("Performing the package update pre-checks");
        executeCheckScripts();
        executeBinScripts();
    }

    private void executeBinScripts() throws InstallerException {
//        File[] stages = stagesDir.listFiles(new FilenameFilter() {
//            public boolean accept(File dir, String name) {
//                return !name.equalsIgnoreCase("check");
//            }
//        });
//
//        Arrays.sort(stages);
//        for (File stage : stages) {
//            executeScriptsInDirectory(stage);
//        }
    }

    private void executeScriptsInDirectory(File stage) throws InstallerException {
        File[] stageFiles = stage.listFiles();
        for (File stageFile : stageFiles) {
            executeSingleScript(stageFile);
        }
    }

    private void executeCheckScripts() throws InstallerException {
//        for (File checkFile : checkFiles) {
//            executeSingleScript(checkFile);
//        }
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
        URL binDirUrl = getClass().getClassLoader().getResource(BIN_DIRECTORY);
        if (binDirUrl == null)
            throw new InstallerException("No bin directory found. Cannot proceed with the update");

        if ("file".equals(binDirUrl.getProtocol())) {
            try {
                File stagesDir = new File(new File(binDirUrl.toURI()), "stages");
                if (!stagesDir.exists())
                    throw new InstallerException("No stages directory found. Cannot proceed with the update");

                File checkDir = new File(stagesDir, "check");
                if (!checkDir.exists())
                    throw new InstallerException("No check directory found. Cannot proceed with the update");

                File[] checkFilesList = checkDir.listFiles();
                if (checkFilesList != null) {
                    for (File checkFileName : checkFilesList) {
                        checkFiles.add(checkFileName);
                    }
                }
            } catch(URISyntaxException use) {
                throw new InstallerException("Error while listing the files in the check directory. Cannot proceed with the update");
            }
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

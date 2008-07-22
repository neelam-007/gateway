package com.l7tech.server.config.packageupdater;

import com.l7tech.util.FileUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.common.io.IOUtils;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.ui.console.BaseConsoleStep;
import com.l7tech.server.config.ui.console.ConfigurationWizard;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 2:00:39 PM
 */
public class UpdateWizardPackageQuestionsStep extends BaseConsoleStep {
    private static final Logger logger = Logger.getLogger(UpdateWizardPackageQuestionsStep.class.getName());

    private static final String TITLE = "SecureSpan Gateway Appliance Update Wizard - Locate Update Packages";
    private static final String UPDATE_PACKAGES_BASE_DIR = "updatepackages";

    PackageUpdateConfigBean configBean;

    List<File> availableUpdatePackages;
    private PackageUpdateConfigBean.UpdatePackageInfo selectedUpdatePackage;

    private static final String CHECKSUM_ALGORITHM_NAME = ".MD5";

    //CONSTRUCTOR
    public UpdateWizardPackageQuestionsStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
        configBean = new PackageUpdateConfigBean("Package Update Information","");
        configCommand = new PackageUpdateConfigCommand(configBean);
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {
        try {
            askSpecifyPackageQuestions(null);
            storeInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //check if it's ok to proceed to next step
    public boolean validateStep() {
        return isOnePackageSelected();
    }
    
    private void askSpecifyPackageQuestions(List<File> listOfPackages) throws IOException, WizardNavigationException {
        String updateLocation = null;
        if (listOfPackages != null) {
            updateLocation = chooseUpdateFromList(listOfPackages);
        } else {
            updateLocation = chooseUpdateLocation();
        }

        try {
            selectedUpdatePackage = checkUpdateLocation(updateLocation);
            checkValidSinglePackage(selectedUpdatePackage);
            String confirmMessage = getConfirmationMessage();

            if (getConfirmationFromUser(confirmMessage, "n")) {
                configBean.setPackageInformation(selectedUpdatePackage);
                storeInput();
            } else {
                askSpecifyPackageQuestions(null);
            }
        } catch (UpdateWizardException e) {
            String message = MessageFormat.format("*** {0} *** {1}", e.getMessage(), getEolChar());
            printText(message);
            logger.warning(message);
            List<File> updateList = null;
            if (!getAvailableUpdatePackages().isEmpty()) {
                updateList = getAvailableUpdatePackages();
            } else {
                getAvailableUpdatePackages().clear();
            }
            askSpecifyPackageQuestions(updateList);
        }
    }

    private String getConfirmationMessage() {
        String description = selectedUpdatePackage.getDescription();
        if (StringUtils.isEmpty(description))
            description = "";

        return "The following update package will be installed." + getEolChar() +
               "\t" + "Package: " + selectedUpdatePackage.getOriginalLocation() + getEolChar() +
               "\t" + "Description: " + description + getEolChar() +
               "Is this correct?";
    }

    private String chooseUpdateLocation() throws IOException, WizardNavigationException {
        String updateLocation;
        String[] prompts = new String[]{"Enter the path to the update package you wish to install:"};
        updateLocation = getData(
                                prompts,
                                "",
                                (Pattern) null,
                                "Invalid Path");
        return updateLocation;
    }

    private String chooseUpdateFromList(List<File> listOfPackages) throws IOException, WizardNavigationException {
        String updateLocation;
        List<String> strings = new ArrayList<String>();
        strings.add("Select the update package you wish to install" + getEolChar());
        String[] allowedEntries = new String[listOfPackages.size()];
        for (int i = 0; i < listOfPackages.size(); i++) {
            allowedEntries[i] = String.valueOf(i+1);
            String pkg = listOfPackages.get(i).getAbsolutePath();
            strings.add("\t" + String.valueOf(i+1) + ": " + pkg + getEolChar());
        }
        strings.add("Please make a selection: [1] ");
        String[] prompts = strings.toArray(new String[0]);
        String whichChoice = getData(
                                prompts,
                                "1",
                                allowedEntries,
                                "Invalid Path");
        updateLocation = listOfPackages.get(Integer.parseInt(whichChoice) -1).getAbsolutePath();
        return updateLocation;
    }

    private boolean isOnePackageSelected() {
        if (selectedUpdatePackage == null) {
            printText("*** No Update Packages are selected ***" + getEolChar());
            return false;
        }
        return true;
    }

    private PackageUpdateConfigBean.UpdatePackageInfo checkUpdateLocation(String path) throws UpdateWizardException {
        if (StringUtils.isEmpty(path)) {
            throw new UpdateWizardException("A valid path to the update package was not specified.");
        }

        File updatePath = new File(path);
        if (!updatePath.exists())
            throw new UpdateWizardException(MessageFormat.format("The path \"{0}\" does not exist", path));

        return checkUpdateFile(updatePath);
    }

    private void checkValidSinglePackage(PackageUpdateConfigBean.UpdatePackageInfo updatePackage) throws UpdateWizardException {
        //checkSignature on the jar
        checkJarSignature(updatePackage);
        checkChecksums(updatePackage);
    }

    private void checkJarSignature(PackageUpdateConfigBean.UpdatePackageInfo updatePackage) throws UpdateWizardException {
        try {
            logger.info("Checking jar signature");
            JarFile jarFile = new JarFile(new File(updatePackage.getExpandedLocation(), PackageUpdateConfigBean.INSTALLER_JAR_FILENAME));
            Enumeration entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) entries.nextElement();
                jarFile.getInputStream(zipEntry);
            }
        } catch (IOException e) {
            String message = MessageFormat.format("Error while loading the update installer from {0}. ({1})", updatePackage.getExpandedLocation(), e.getMessage());
            throw new UpdateWizardException(message);
        }
    }

    private void checkChecksums(PackageUpdateConfigBean.UpdatePackageInfo updatePackage) throws UpdateWizardException {
        logger.info("Verifying Checksums");
        //get listing of scripts from the updatelist.txt
        String upgradePackageWorkingDir = updatePackage.getExpandedLocation();

        JarFile updatejar = null;
        try {
            updatejar = new JarFile(new File(upgradePackageWorkingDir, PackageUpdateConfigBean.INSTALLER_JAR_FILENAME));
        } catch (IOException e) {
            throw new UpdateWizardException(MessageFormat.format("Could not find the installer jar in package \"{0}\". Cannot proceed with the update", updatePackage.getExpandedLocation()));
        }

        File updateList = new File(upgradePackageWorkingDir, PackageUpdateConfigBean.UPDATE_FILE_LIST);
        if (!updateList.exists())
            throw new UpdateWizardException(MessageFormat.format("Could not find the list of update files in package \"{0}\". Cannot proceed with the update", updatePackage.getExpandedLocation()));

        List<String> updates = readUpdateList(updateList);

        //for each listing locate the real file and the checksum file in the jar
        for (String update : updates) {
            InputStream originalFileInputStream = null;
            InputStream jarEntryInputStream = null;

            File originalFile = new File(upgradePackageWorkingDir, update);
            if (!originalFile.exists())
                throw new UpdateWizardException(MessageFormat.format("The file \"{0}\" could not be found in location \"{1}\". The Update Package is corrupt. Cannot proceed with the update", update, updatePackage.getExpandedLocation()));

            String checkSumName = "checksums/" + update + CHECKSUM_ALGORITHM_NAME;
            JarEntry checkSumEntry = updatejar.getJarEntry(checkSumName);
            if (checkSumEntry == null)
                throw new UpdateWizardException(MessageFormat.format("Could not find the checksum for file \"{0}\" in the update installer at \"{1}\". The Update Package is corrupt. Cannot proceed with the update", update, updatePackage.getExpandedLocation()));

            try {
                originalFileInputStream = new FileInputStream(originalFile);
                byte[] originalFileBytes = IOUtils.slurpStream(originalFileInputStream);

                jarEntryInputStream = updatejar.getInputStream(checkSumEntry);
                byte[] checkSumBytes = IOUtils.slurpStream(jarEntryInputStream);

                 //checksum the real file and compare with the checksum in the jar
                boolean checkSumsMatch = compareCheckSums(checkSumBytes, originalFileBytes);
                if (!checkSumsMatch)
                    throw new UpdateWizardException(MessageFormat.format("File {0} has been changed since being created by Layer 7 Technologies. This is not a valid SecureSpan Update package.", update));            
            } catch (IOException e) {
                throw new UpdateWizardException(MessageFormat.format("Error while reading the checksum for file {0}. [{1}].", update, e.getMessage()));
            } finally {
                ResourceUtils.closeQuietly(originalFileInputStream);
                ResourceUtils.closeQuietly(jarEntryInputStream);
            }
        }
    }

    private boolean compareCheckSums(byte[] checkSumBytes, byte[] originalFileBytes) {
        byte[] originalFileChecksum = HexUtils.getMd5Digest(originalFileBytes);
        byte[] encodedBytes = HexUtils.encodeMd5Digest(originalFileChecksum).getBytes();

        return MessageDigest.isEqual(checkSumBytes, encodedBytes);
    }

    private List<String> readUpdateList(File updateList) {
        List<String> lines = new ArrayList<String>();

        BufferedReader reader = null;
        try {
            InputStream is = new FileInputStream(updateList);
            reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ResourceUtils.closeQuietly(reader);
        }
        return lines;
    }

    private PackageUpdateConfigBean.UpdatePackageInfo checkUpdateFile(File updatePath) throws UpdateWizardException {
        getAvailableUpdatePackages().clear();
        File[] updateFiles = null;
        if (updatePath.isDirectory()) {
            updateFiles = updatePath.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(PackageUpdateConfigBean.PACKAGE_UPDATE_EXTENSION);
                }
            });

            if (updateFiles == null || updateFiles.length == 0)
                throw new UpdateWizardException("No SecureSpan Update Packages could be found at the path specified \"" + updatePath.getAbsolutePath() + "\".");


            if (updateFiles.length > 1) {
                for (File updateFile : updateFiles)
                    getAvailableUpdatePackages().add(updateFile);

                throw new UpdateWizardException(MessageFormat.format("Multiple SecureSpan Update Packages were found in \"{0}\". Please specify the package you wish to install.", updatePath.getAbsolutePath()));
            }
            updatePath = updateFiles[0];
        } else {
            if (!updatePath.getName().endsWith(PackageUpdateConfigBean.PACKAGE_UPDATE_EXTENSION))
                 throw new UpdateWizardException("Could not find a valid SecureSpan Update package using: \"" + updatePath.getAbsolutePath() + "\"");
        }
        return getUpdateInfo(updatePath);
    }

    private List<File> getAvailableUpdatePackages() {
        if (availableUpdatePackages == null) availableUpdatePackages = new ArrayList<File>();
        return availableUpdatePackages;
    }

    private PackageUpdateConfigBean.UpdatePackageInfo getUpdateInfo(File updateFile) {
        PackageUpdateConfigBean.UpdatePackageInfo info = null;

        try {
            File zipOutputDir = unzipUpdatePackage(updateFile);
            checkStructure(zipOutputDir);
            String description = getDescription(zipOutputDir);
            info = populateUpdateInfo(updateFile, zipOutputDir, description);
        } catch (FileNotFoundException e) {
            logger.warning(MessageFormat.format("Error while reading update package \"{0}\". ({1})", updateFile.getAbsolutePath(), e.getMessage()));
        } catch (IOException e) {
            logger.warning(MessageFormat.format("Error while reading update package \"{0}\". ({1})", updateFile.getAbsolutePath(), e.getMessage()));
        } catch (UpdateWizardException e) {
            logger.warning(MessageFormat.format("Error while reading update package \"{0}\". ({1})", updateFile.getAbsolutePath(), e.getMessage()));
        }
        return info;
    }

    private void checkStructure(File zipOutputDir) throws UpdateWizardException {
        //now check to see if things we expect are there.
        if (!new File(zipOutputDir, PackageUpdateConfigBean.INSTALLER_JAR_FILENAME).exists()) {
            String msg = MessageFormat.format("Could not find a {0} in the update package. This is not a valid SecureSpan update package.", PackageUpdateConfigBean.INSTALLER_JAR_FILENAME);
            throw new UpdateWizardException(msg);
        }

        if (!new File(zipOutputDir, PackageUpdateConfigBean.UPDATE_FILE_LIST).exists()) {
            String msg = MessageFormat.format("Could not find a {0} in the update package. This is not a valid SecureSpan update package.", PackageUpdateConfigBean.UPDATE_FILE_LIST);
            throw new UpdateWizardException(msg);
        }

        File descriptionFile = new File(zipOutputDir, PackageUpdateConfigBean.DESCRIPTION_FILENAME);
        if (!descriptionFile.exists()) {
            logger.warning(MessageFormat.format("Could not find a {0} in the update package. This is not a valid SecureSpan update package.", PackageUpdateConfigBean.DESCRIPTION_FILENAME));
        }
    }

    private PackageUpdateConfigBean.UpdatePackageInfo populateUpdateInfo(File updateFile, File zipOutputDir, String description) {
        PackageUpdateConfigBean.UpdatePackageInfo info;
        info = new PackageUpdateConfigBean.UpdatePackageInfo();
        info.setOriginalLocation(updateFile.getAbsolutePath());
        info.setExpandedLocation(zipOutputDir.getAbsolutePath());
        info.setDescription(description);
        return info;
    }

    private File unzipUpdatePackage(File updateFile) throws IOException {
        File updatePackagesDir = new File(UPDATE_PACKAGES_BASE_DIR);
        updatePackagesDir.mkdir();
        File tempDir = getTempDir(updatePackagesDir);

        File zipOutputDir = new File(tempDir,  updateFile.getName());

        ZipFile zipFile = new ZipFile(updateFile);
        if (zipFile.size() == 0) {
            logger.warning(MessageFormat.format("Update package {0} is empty. This is not a valid SecureSpan update package.",
                                                updateFile.getAbsolutePath()));
            return null;
        }

        if (zipOutputDir.exists()) {
            FileUtils.deleteDir(zipOutputDir);
        }
        zipOutputDir.mkdirs();
        BufferedOutputStream dest = null;
        Enumeration e = zipFile.entries();
        try {
            while (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                String name = ze.getName();
                logger.info("Extracting " + name);

                BufferedInputStream bis = null;
                bis = new BufferedInputStream(zipFile.getInputStream(ze));

                File destFile = new File(zipOutputDir, name);
                if (ze.isDirectory()) {
                    destFile.mkdirs();
                } else {
                    dest = new BufferedOutputStream(new FileOutputStream(destFile));

                    byte[] buf = new byte[512];
                    int len = 0;
                    while ((len = bis.read(buf)) != -1) {
                        dest.write(buf, 0, len);
                    }
                    dest.flush();
                    dest.close();
                }
                bis.close();
            }
        } finally {
            try {if (dest != null) dest.flush();} catch (IOException e1) {}
            ResourceUtils.closeQuietly(dest);
        }
        return zipOutputDir;
    }

    private File getTempDir(File updatePackagesDir) throws IOException {
        //create a temporary directory.
        File tempFile = File.createTempFile("ssg-update", null, updatePackagesDir);
        String tempName = tempFile.getName();
        tempFile.delete();

        return new File(updatePackagesDir, tempName);
    }

    private String getDescription(File zipDir) throws UpdateWizardException {
        BufferedInputStream bis = null;
        String description = null;
        try {
            File descriptionFile = new File(zipDir, PackageUpdateConfigBean.DESCRIPTION_FILENAME);
            bis = new BufferedInputStream(new FileInputStream(descriptionFile));
            byte[] buf = IOUtils.slurpStream(bis);
            description = new String(buf);
        } catch (FileNotFoundException e) {
            String message = "No description file found. This is not a valid SecureSpan Update package.";
            logger.warning(message);
        } catch (IOException e) {
            String message = MessageFormat.format("Error while reading description file. Cannot proceed ({0}).", e.getMessage());
            logger.warning(message);
        } finally{
            ResourceUtils.closeQuietly(bis);
        }
        return description;
    }

    public String getTitle() {
        return TITLE;
    }
}
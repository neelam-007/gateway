package com.l7tech.server.config.packageupdater;

import com.l7tech.common.util.FileUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.ui.console.BaseConsoleStep;
import com.l7tech.server.config.ui.console.ConfigurationWizard;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
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

            if (getConfirmationFromUser(confirmMessage)) {
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
        } catch (IOException e) {
            String message = MessageFormat.format("Error while loading the update installer from {0}. ({1})", updatePackage.getExpandedLocation(), e.getMessage());
            logger.warning(message);
            throw new UpdateWizardException(message);
        }
    }

    private void checkChecksums(PackageUpdateConfigBean.UpdatePackageInfo updatePackage) throws UpdateWizardException {
//        throw new UpdateWizardException("The update package has been changed since being created by Layer 7 Technologies. This is not a valid SecureSpan Update package.");
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
        } else {
            if (!updatePath.getName().endsWith(PackageUpdateConfigBean.PACKAGE_UPDATE_EXTENSION))
                 throw new UpdateWizardException("Could not find a valid SecureSpan Update package using: \"" + updatePath.getAbsolutePath() + "\"");
        }
        if (updateFiles == null)
            return null;
        return getUpdateInfo(updateFiles[0]);
    }

    private List<File> getAvailableUpdatePackages() {
        if (availableUpdatePackages == null) availableUpdatePackages = new ArrayList<File>();
        return availableUpdatePackages;
    }

    private PackageUpdateConfigBean.UpdatePackageInfo getUpdateInfo(File updateFile) {
        PackageUpdateConfigBean.UpdatePackageInfo info = null;

        try {
            File updatePackagesDir = new File(UPDATE_PACKAGES_BASE_DIR);
            updatePackagesDir.mkdir();
            File tempDir = getTempDir(updatePackagesDir);

            File zipOutputDir = new File(tempDir,  updateFile.getName());
//            tempDir.deleteOnExit();

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

            unzipUpdatePackage(zipFile, zipOutputDir);

            //now check to see if things we expect are there.
            if (!new File(zipOutputDir, PackageUpdateConfigBean.INSTALLER_JAR_FILENAME).exists()) {
                logger.warning(MessageFormat.format("Could not find a {0} in the update package {1}. This is not a valid SecureSpan update package.", PackageUpdateConfigBean.INSTALLER_JAR_FILENAME, updateFile.getAbsolutePath()));
                return null;
            }

            File descriptionFile = new File(zipOutputDir, PackageUpdateConfigBean.DESCRIPTION_FILENAME);
            if (!descriptionFile.exists()) {
                logger.warning(MessageFormat.format("Could not find a {0} in the update package {1}. This is not a valid SecureSpan update package.", PackageUpdateConfigBean.DESCRIPTION_FILENAME, updateFile.getAbsolutePath()));
            }
            String description = getDescription(descriptionFile);
            info = populateUpdateInfo(updateFile, zipOutputDir, description);
        } catch (FileNotFoundException e) {
            logger.warning(MessageFormat.format("Could not read update package from file \"{0}\". ({1})", updateFile.getAbsolutePath(), e.getMessage()));
        } catch (IOException e) {
            logger.warning(MessageFormat.format("Could not read update package from file \"{0}\". ({1})", updateFile.getAbsolutePath(), e.getMessage()));
        } catch (UpdateWizardException e) {
            logger.warning(MessageFormat.format("Could not read update package from file \"{0}\". ({1})", updateFile.getAbsolutePath(), e.getMessage()));
        }
        return info;
    }

    private PackageUpdateConfigBean.UpdatePackageInfo populateUpdateInfo(File updateFile, File zipOutputDir, String description) {
        PackageUpdateConfigBean.UpdatePackageInfo info;
        info = new PackageUpdateConfigBean.UpdatePackageInfo();
        info.setOriginalLocation(updateFile.getAbsolutePath());
        info.setExpandedLocation(zipOutputDir.getAbsolutePath());
        info.setDescription(description);
        return info;
    }

    private void unzipUpdatePackage(ZipFile zipFile, File zipOutputDir) throws IOException {
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
    }

    private File getTempDir(File updatePackagesDir) throws IOException {
        //create a temporary directory.
        File tempFile = File.createTempFile("ssg-update", null, updatePackagesDir);
        String tempName = tempFile.getName();
        tempFile.delete();

        return new File(updatePackagesDir, tempName);
    }

    private String getDescription(File descriptionFile) throws UpdateWizardException {
        BufferedInputStream bis = null;
        String description = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(descriptionFile));
            byte[] buf = HexUtils.slurpStream(bis);
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
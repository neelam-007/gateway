package com.l7tech.server.config.packageupdater;

import com.l7tech.common.util.FileUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.HexUtils;
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

    PackageUpdateConfigBean configBean;

    List<PackageUpdateConfigBean.UpdatePackageInfo> updatePackages;

    //CONSTRUCTOR
    public UpdateWizardPackageQuestionsStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
        configBean = new PackageUpdateConfigBean("Package Update Information","");
    }

    //check if it's ok to proceed to next step
    public boolean validateStep() {
        return isOnePackageSelected();
    }

    //if 0 packages selected, or if there's more than one package available, we bail.
    private boolean isOnePackageSelected() {
        switch (getUpdatePackages().size()) {
            case 0:
                printText("No Update Packages are selected");
                return false;
            case 1:
                return true;
            default:
                printText("More than one update packages is available to be installed. Please select one");
                return false;
        }
    }

    private void checkUpdateLocation(String path) throws UpdateWizardException {
        if (StringUtils.isEmpty(path)) {
            throw new UpdateWizardException("A valid path to the update package was not specified.");
        }

        File updatePath = new File(path);
        if (updatePath.exists()) {
            checkUpdateFile(updatePath);
        } else {
            throw new UpdateWizardException("The path \"" + path + "\" does not exist");
        }
    }

    private boolean checkValidSinglePackage(PackageUpdateConfigBean.UpdatePackageInfo updatePackage) throws UpdateWizardException {
        boolean isValid = false;
        //checkSignature on the jar
        checkJarSignature(updatePackage);
        checkChecksums(updatePackage);
        isValid = true;
        return isValid;
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

    private void checkUpdateFile(File updatePath) throws UpdateWizardException {
        getUpdatePackages().clear();
        if (updatePath.isDirectory()) {
            File[] updateFiles = updatePath.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(PackageUpdateConfigBean.PACKAGE_UPDATE_EXTENSION);
                }
            });

            if (updateFiles == null || updateFiles.length == 0) {
                throw new UpdateWizardException("No SecureSpan Update Packages could be found at the path specified \"" + updatePath.getAbsolutePath() + "\".");
            }

            for (File updateFile : updateFiles) {
                PackageUpdateConfigBean.UpdatePackageInfo info = getUpdateInfo(updateFile);
                if (info != null) getUpdatePackages().add(info);
            }
        } else {
            if (!updatePath.getName().endsWith(PackageUpdateConfigBean.PACKAGE_UPDATE_EXTENSION)) {
                 throw new UpdateWizardException("Could not find a valid SecureSpan Update package using: \"" + updatePath.getAbsolutePath() + "\"");
            }

            PackageUpdateConfigBean.UpdatePackageInfo info = getUpdateInfo(updatePath);
            if (info != null) getUpdatePackages().add(info);
        }
    }

    private List<PackageUpdateConfigBean.UpdatePackageInfo> getUpdatePackages() {
        if (updatePackages == null)
            updatePackages = new ArrayList<PackageUpdateConfigBean.UpdatePackageInfo>();
        return updatePackages;
    }

    private PackageUpdateConfigBean.UpdatePackageInfo getUpdateInfo(File updateFile) {
        PackageUpdateConfigBean.UpdatePackageInfo info = null;
        File zipOutputDir = new File("updatepackages",  updateFile.getName()+"expanded");
        BufferedOutputStream dest = null;
        try {
            ZipFile zipFile = new ZipFile(updateFile);
            if (zipFile.size() == 0) {
                logger.warning(MessageFormat.format("Update package {0} is empty. This is not a valid SecureSpan update package.", updateFile.getAbsolutePath()));
                return null;
            }

            if (zipOutputDir.exists()) {
                FileUtils.deleteDir(zipOutputDir);
            }
            zipOutputDir.mkdirs();

            Enumeration e = zipFile.entries();
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

            //now check to see if things we expect are there.
            File fileToCheck = new File(zipOutputDir, PackageUpdateConfigBean.INSTALLER_JAR_FILENAME);
            if (!fileToCheck.exists()) {
                logger.warning(MessageFormat.format("Could not find a {0} in the update package {1}. This is not a valid SecureSpan update package.", PackageUpdateConfigBean.INSTALLER_JAR_FILENAME, updateFile.getAbsolutePath()));
                return null;
            }

            fileToCheck = new File(zipOutputDir, PackageUpdateConfigBean.DESCRIPTION_FILENAME);
            if (!fileToCheck.exists()) {
                logger.warning(MessageFormat.format("Could not find a {0} in the update package {1}. This is not a valid SecureSpan update package.", PackageUpdateConfigBean.DESCRIPTION_FILENAME, updateFile.getAbsolutePath()));
            }
            String description = getDescription(fileToCheck);

            info = new PackageUpdateConfigBean.UpdatePackageInfo();
            info.setOriginalLocation(updateFile.getAbsolutePath());
            info.setExpandedLocation(zipOutputDir.getAbsolutePath());
            info.setDescription(description);
        } catch (FileNotFoundException e) {
            logger.warning(MessageFormat.format("Could not read update package from file \"{0}\". ({1})", updateFile.getAbsolutePath(), e.getMessage()));
        } catch (IOException e) {
            logger.warning(MessageFormat.format("Could not read update package from file \"{0}\". ({1})", updateFile.getAbsolutePath(), e.getMessage()));
        } catch (UpdateWizardException e) {
            logger.warning(MessageFormat.format("Could not read update package from file \"{0}\". ({1})", updateFile.getAbsolutePath(), e.getMessage()));
        } finally {
            ResourceUtils.closeQuietly(dest);
        }
        return info;
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
            throw new UpdateWizardException(message);
        } catch (IOException e) {
            String message = MessageFormat.format("Error while reading description file. Cannot proceed ({0}).", e.getMessage());
            logger.warning(message);
            throw new UpdateWizardException(message);
        } finally{
            ResourceUtils.closeQuietly(bis);
        }
        return description;
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {
        try {
            doSpecifyPackageQuestions();
            storeInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doSpecifyPackageQuestions() throws IOException, WizardNavigationException {
        String updateLocation = getData(
                new String[]{
                        "Enter the path to the update package you wish to install:"
                },
                "",
                (Pattern) null,
                "Invalid Path");

        try {
            checkUpdateLocation(updateLocation);
            if (getUpdatePackages().isEmpty()) {
                printText("No upgrade package was found at this location. Please specify the package you wish to install." + getEolChar());
                logger.warning("No upgrade packages found.");
                doSpecifyPackageQuestions();
            } else if (getUpdatePackages().size() > 1) {
                printText("Multiple upgrade packages found at this location. Please specify the package you wish to install." + getEolChar());
                logger.warning("Multiple upgrade packages found.");
                doSpecifyPackageQuestions();
            } else {
                PackageUpdateConfigBean.UpdatePackageInfo goodOne = getUpdatePackages().iterator().next();
                if (!checkValidSinglePackage(goodOne)) {
                    doSpecifyPackageQuestions();
                } else {
                    String description = goodOne.getDescription();
                    if (StringUtils.isEmpty(description))
                        description = "";

                    String confirmMessage = "The following update package will be installed." + getEolChar() +
                                            "\t" + goodOne.getOriginalLocation() + getEolChar() +
                                            "\t" + description + getEolChar() +
                                            "Is this correct?";

                    if (!getConfirmationFromUser(confirmMessage)) {
                        doSpecifyPackageQuestions();
                    }
                }
            }

        } catch (UpdateWizardException e) {
            printText("*** " + e.getMessage() + " ***" + getEolChar());
            getUpdatePackages().clear();
            doSpecifyPackageQuestions();  
        }
    }

    public String getTitle() {
        return TITLE;
    }

    protected void storeInput() {
        if (!getUpdatePackages().isEmpty()) {
            configBean.setPackageInformation(getUpdatePackages().iterator().next());
        }
    }
}
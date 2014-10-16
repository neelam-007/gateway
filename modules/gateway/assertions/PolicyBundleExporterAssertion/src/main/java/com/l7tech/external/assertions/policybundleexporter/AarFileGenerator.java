package com.l7tech.external.assertions.policybundleexporter;

import com.l7tech.external.assertions.policybundleexporter.asm.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import com.l7tech.util.RandomUtil;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * Class that will generate a .aar file embodying a particular policy bundle installer.
 * Some borrowing from com.l7tech.external.assertions.script.console.CompiledScriptAarFileGenerator
 */
public class AarFileGenerator {
    private static final String COMMON_PATH_PREFIX = "com/l7tech/external/assertions/";
    private static final String COMMON_PACKAGE_PREFIX = "com.l7tech.external.assertions.";
    private static final String TEMPLATE_RESOURCE_PATH = "policybundleexporter/template/";

    /* FooBarInstallerAssertion-8.2.aar:

       META-INF/MANIFEST.MF - generated using a template (Template_MANIFEST.txt)
       AAR-INF/assertion.index
       AAR-INF/console.index
       com/l7tech/external/assertions/foobarinstaller/FooBarInstallerAssertion.class
       com/l7tech/external/assertions/foobarinstaller/FooBarInstallerAssertion$1.class
       com/l7tech/external/assertions/foobarinstaller/FooBarInstallerAdminImpl.class
       com/l7tech/external/assertions/foobarinstaller/bundles/FooBarPolicyBundleInfo.xml
       com/l7tech/external/assertions/foobarinstaller/bundles/restman/BundleInfo.xml
       com/l7tech/external/assertions/foobarinstaller/bundles/restman/Template_MigrationBundle.xml
       com/l7tech/external/assertions/foobarinstaller/console/FooBarInstallerAction.class
       com/l7tech/external/assertions/foobarinstaller/console/FooBarInstallerAction$1.class
       com/l7tech/external/assertions/foobarinstaller/console/FooBarInstallerDialog.class
       com/l7tech/external/assertions/foobarinstaller/console/FooBarInstallerDialog$1.class
       com/l7tech/external/assertions/foobarinstaller/server/ServerFooBarInstallerAssertion.class
     */

    /**
     * Generate a Policy Bundle Installer .aar file that installs through the Policy Manager.
     *
     * @param exportProperties Policy bundle properties for generating Policy Bundle Installer .aar file.  Required.
     * @return the bytes of the .aar file.  Never null.
     * @throws java.io.IOException if an .aar file cannot be generated
     */
    public byte[] generateInstallerAarFile(@NotNull final PolicyBundleExporterProperties exportProperties) throws IOException {
        final String bundleName = exportProperties.getBundleName();
        final String bundleVersion = exportProperties.getBundleVersion();
        String bundleFolderPath = exportProperties.getBundleFolder().getPath();
        // remove "/" at start and end of folder path.
        if (bundleFolderPath.startsWith("/")) {
            bundleFolderPath = bundleFolderPath.substring(1);
        }
        if (bundleFolderPath.endsWith("/")) {
            bundleFolderPath = bundleFolderPath.substring(0, bundleFolderPath.length() -1);
        }

        final String installerBundleName = bundleName + "Installer";
        final String subPackageInstallerName = installerBundleName.toLowerCase();

        // TODO track version of template code so we can trace back any bugs
        final String secureSpanVersion = BuildInfo.getProductVersionMajor() + "." + BuildInfo.getProductVersionMinor() + "." + BuildInfo.getProductVersionSubMinor();

        // build .AAR file
        ByteArrayOutputStream aarOut = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(aarOut);
        zos.setComment(installerBundleName + "Assertion-" + secureSpanVersion + ".aar");

        // manifest
        zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
        zos.write(makeManifestBytes(subPackageInstallerName, installerBundleName));

        StringBuilder assertionIndex = new StringBuilder();
        StringBuilder consoleIndex = new StringBuilder();

        // assertion class
        zos.putNextEntry(new ZipEntry(COMMON_PATH_PREFIX + subPackageInstallerName + "/"));
        String zipEntryPath = COMMON_PATH_PREFIX + subPackageInstallerName + "/" + installerBundleName + "Assertion.class";
        zos.putNextEntry(new ZipEntry(zipEntryPath));
        AssertionClassMaker assertionClassMaker = new AssertionClassMaker(subPackageInstallerName, installerBundleName, bundleName);
        zos.write(assertionClassMaker.generate());
        assertionIndex.append(zipEntryPath).append(System.lineSeparator());
        zipEntryPath = COMMON_PATH_PREFIX + subPackageInstallerName + "/" + installerBundleName + "Assertion$1.class";
        zos.putNextEntry(new ZipEntry(zipEntryPath));
        zos.write(assertionClassMaker.generate$1());
        assertionIndex.append(zipEntryPath).append(System.lineSeparator());

        // admin impl class
        zipEntryPath = COMMON_PATH_PREFIX + subPackageInstallerName + "/" + installerBundleName + "AdminImpl.class";
        zos.putNextEntry(new ZipEntry(zipEntryPath));
        zos.write(new AdminImplClassMaker(subPackageInstallerName, installerBundleName, bundleName).generate());
        assertionIndex.append(zipEntryPath).append(System.lineSeparator());

        // server assertion class
        zos.putNextEntry(new ZipEntry(COMMON_PATH_PREFIX + subPackageInstallerName + "/server/"));
        zipEntryPath = COMMON_PATH_PREFIX + subPackageInstallerName + "/server/Server" + installerBundleName + "Assertion.class";
        zos.putNextEntry(new ZipEntry(zipEntryPath));
        zos.write(new ServerAssertionClassMaker(subPackageInstallerName, installerBundleName, bundleName).generate());
        // don't need to add server assertion class to to assertion index

        // action icon
        //
        String actionIconResourcePath = "com/l7tech/console/resources/interface.gif"; // This is the default icon.
        final String actionIconFilePath = exportProperties.getActionIconFilePath();
        if (actionIconFilePath != null) {
            File file = new File(actionIconFilePath);
            if (file.exists()) {
                final String actionIconFilenameOnly = file.getName();
                zos.putNextEntry(new ZipEntry(COMMON_PATH_PREFIX + subPackageInstallerName + "/console/resources/"));
                zipEntryPath = COMMON_PATH_PREFIX + subPackageInstallerName + "/console/resources/" + actionIconFilenameOnly;
                zos.putNextEntry(new ZipEntry(zipEntryPath));
                zos.write(IOUtils.slurpFile(new File(actionIconFilePath)));
                actionIconResourcePath = COMMON_PATH_PREFIX + subPackageInstallerName + "/console/resources/" + actionIconFilenameOnly;
            }
        }

        // action class
        zos.putNextEntry(new ZipEntry(COMMON_PATH_PREFIX + subPackageInstallerName + "/console/"));
        zipEntryPath = COMMON_PATH_PREFIX + subPackageInstallerName + "/console/" + installerBundleName + "Action.class";
        zos.putNextEntry(new ZipEntry(zipEntryPath));
        final ActionClassMaker actionClassMaker = new ActionClassMaker(subPackageInstallerName, installerBundleName, bundleName, actionIconResourcePath);
        zos.write(actionClassMaker.generate());
        consoleIndex.append(zipEntryPath).append(System.lineSeparator());
        zipEntryPath = "com/l7tech/external/assertions/" + subPackageInstallerName + "/console/" + installerBundleName + "Action$1.class";
        zos.putNextEntry(new ZipEntry(zipEntryPath));
        zos.write(actionClassMaker.generate$1());
        consoleIndex.append(zipEntryPath).append(System.lineSeparator());

        // dialog class
        zipEntryPath = COMMON_PATH_PREFIX + subPackageInstallerName + "/console/" + installerBundleName + "Dialog.class";
        zos.putNextEntry(new ZipEntry(zipEntryPath));
        DialogClassMaker dialogClassMaker = new DialogClassMaker(subPackageInstallerName, installerBundleName, bundleName, bundleFolderPath);
        zos.write(dialogClassMaker.generate());
        consoleIndex.append(zipEntryPath).append(System.lineSeparator());
        zipEntryPath = COMMON_PATH_PREFIX + subPackageInstallerName + "/console/" + installerBundleName + "Dialog$1.class";
        zos.putNextEntry(new ZipEntry(zipEntryPath));
        zos.write(dialogClassMaker.generate$1());
        consoleIndex.append(zipEntryPath).append(System.lineSeparator());

        // bundle
        addPolicyBundleInfo(zos, subPackageInstallerName, bundleName, bundleVersion, assertionIndex);

        // component(s)
        String componentId;
        for (ComponentInfo componentInfo : exportProperties.getComponentInfoList()) {
            zos.putNextEntry(new ZipEntry(COMMON_PATH_PREFIX + subPackageInstallerName + "/bundles/" + componentInfo.getName() + "/"));

            // generate id if not provided (e.g. user selected auto generate)
            componentId = componentInfo.getId();
            if (componentId == null) {
                byte[] idBytes = new byte[16];
                RandomUtil.nextBytes(idBytes);
                componentId = new Goid(idBytes).toString();
            }

            addBundleInfo(zos, subPackageInstallerName, componentId, componentInfo.getVersion(), componentInfo.getName(), componentInfo.getDescription(), assertionIndex);
            addMigrationBundle(zos, subPackageInstallerName, componentInfo.getVersion(), componentInfo.getName(), assertionIndex, exportProperties.getComponentRestmanBundleXmls().get(componentInfo.getFolderHeader().getGoid()));
        }

        // indexes
        zos.putNextEntry(new ZipEntry("AAR-INF/assertion.index"));
        zos.write(assertionIndex.toString().getBytes());
        zos.putNextEntry(new ZipEntry("AAR-INF/console.index"));
        zos.write(consoleIndex.toString().getBytes());

        zos.close();
        return aarOut.toByteArray();
    }

    private byte[] makeManifestBytes(final String subPackageInstallerName, final String assertionInstallerCamelName) throws IOException {
        final String secureSpanVersion = BuildInfo.getProductVersionMajor() + "." + BuildInfo.getProductVersionMinor() + "." + BuildInfo.getProductVersionSubMinor();
        final byte[] manifestTemplate = IOUtils.slurpStream(getClass().getClassLoader().getResourceAsStream(COMMON_PATH_PREFIX + TEMPLATE_RESOURCE_PATH + "Template_MANIFEST.txt"));
        return MessageFormat.format(new String(manifestTemplate), COMMON_PACKAGE_PREFIX, subPackageInstallerName, assertionInstallerCamelName, secureSpanVersion).getBytes(Charsets.UTF8);
    }

    private void addPolicyBundleInfo(final ZipOutputStream zos, final String subPackageInstallerName, final String camelName, final String bundleVersion, final StringBuilder assertionIndex) throws IOException {
        zos.putNextEntry(new ZipEntry(COMMON_PATH_PREFIX + subPackageInstallerName + "/bundles/"));
        // TODO configurable namespace, version, comment
        String policyBundleName;
        if (camelName.contains("PolicyBundle")) {
            policyBundleName = camelName;
        } else {
            policyBundleName = camelName + "PolicyBundle";
        }
        final String zipEntryPath = COMMON_PATH_PREFIX + subPackageInstallerName + "/bundles/" + policyBundleName + "Info.xml";
        zos.putNextEntry(new ZipEntry(zipEntryPath));
        final byte[] policyBundleInfoTemplate = IOUtils.slurpStream(getClass().getClassLoader().getResourceAsStream(COMMON_PATH_PREFIX + TEMPLATE_RESOURCE_PATH + "bundles/Template_PolicyBundleInfo.xml"));
        zos.write(MessageFormat.format(new String(policyBundleInfoTemplate), policyBundleName, policyBundleName.toLowerCase(), bundleVersion).getBytes(Charsets.UTF8));
        assertionIndex.append(zipEntryPath).append(System.lineSeparator());
    }

    private void addBundleInfo(final ZipOutputStream zos, final String subPackageInstallerName, final String componentId, final String componentVersion, final String componentName, final String componentDescription, final StringBuilder assertionIndex) throws IOException {
        final String zipEntryPath = COMMON_PATH_PREFIX + subPackageInstallerName + "/bundles/" + componentName + "/BundleInfo.xml";
        zos.putNextEntry(new ZipEntry(zipEntryPath));
        final byte[] bundleInfoTemplate = IOUtils.slurpStream(getClass().getClassLoader().getResourceAsStream(COMMON_PATH_PREFIX + TEMPLATE_RESOURCE_PATH + "bundles/restman/Template_BundleInfo.xml"));
        zos.write(MessageFormat.format(new String(bundleInfoTemplate), componentId, componentVersion, componentName, componentDescription).getBytes(Charsets.UTF8));
        assertionIndex.append(zipEntryPath).append(System.lineSeparator());
    }

    private void addMigrationBundle(final ZipOutputStream zos, final String subPackageInstallerName, final String componentVersion, final String componentName, final StringBuilder assertionIndex, final String restmanBundleXml) throws IOException {
        final String zipEntryPath = COMMON_PATH_PREFIX + subPackageInstallerName + "/bundles/" + componentName + "/MigrationBundle" + componentVersion + ".xml";
        zos.putNextEntry(new ZipEntry(zipEntryPath));
        final byte[] migrationBundleTemplate = IOUtils.slurpStream(getClass().getClassLoader().getResourceAsStream(COMMON_PATH_PREFIX + TEMPLATE_RESOURCE_PATH + "bundles/restman/Template_MigrationBundle.xml"));
        zos.write(MessageFormat.format(new String(migrationBundleTemplate), restmanBundleXml).getBytes(Charsets.UTF8));
        assertionIndex.append(zipEntryPath).append(System.lineSeparator());
    }
}

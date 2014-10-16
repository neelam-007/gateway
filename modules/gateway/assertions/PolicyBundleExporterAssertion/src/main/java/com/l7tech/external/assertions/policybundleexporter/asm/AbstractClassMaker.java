package com.l7tech.external.assertions.policybundleexporter.asm;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;

/**
 * Base class for generating of Policy Bundle Installer classes (using ASM byte code library).
 * See SimplePolicyBundleInstallerAssertion for a sample installer.
 */
public abstract class AbstractClassMaker implements Opcodes {
    protected final String subPackageInstallerName;
    protected final String installerCamelName;
    protected final String camelName;
    protected final String folderName;

    // package names
    protected final String consolePackageName;
    protected final String consolePackageDotName;
    protected final String packageName;
    protected final String packageDotName;
    protected final String serverPackageName;

    // class names
    protected final String actionClassName;
    protected final String actionDotClassName;
    protected final String action$1ClassName;
    protected final String adminImplClassName;
    protected final String adminImplDotClassName;
    protected final String assertionClassName;
    protected final String assertion$1ClassName;
    protected final String dialogClassName;
    protected final String dialog$1ClassName;
    protected final String serverAssertionClassName;

    protected AbstractClassMaker(final String subPackageInstallerName, final String installerCamelName, final String camelName) {
        this(subPackageInstallerName, installerCamelName, camelName, null);
    }

    protected AbstractClassMaker(final String subPackageInstallerName, final String installerCamelName, final String camelName, @Nullable final String folderName) {
        this.subPackageInstallerName = subPackageInstallerName;
        this.installerCamelName = installerCamelName;
        this.camelName = camelName;
        this.folderName = folderName;

        consolePackageName = "com/l7tech/external/assertions/" + subPackageInstallerName.toLowerCase() + "/console/";
        consolePackageDotName = "com.l7tech.external.assertions." + subPackageInstallerName.toLowerCase() + ".console.";
        packageName = "com/l7tech/external/assertions/" + subPackageInstallerName.toLowerCase() + "/";
        packageDotName = "com.l7tech.external.assertions." + subPackageInstallerName.toLowerCase() + ".";
        serverPackageName = "com/l7tech/external/assertions/" + subPackageInstallerName.toLowerCase() + "/server/";

        actionClassName = consolePackageName + installerCamelName + "Action";
        actionDotClassName = consolePackageDotName + installerCamelName + "Action";
        action$1ClassName = consolePackageName + installerCamelName + "Action$1";

        adminImplClassName = packageName + installerCamelName + "AdminImpl";
        adminImplDotClassName = packageDotName + installerCamelName + "AdminImpl";

        assertionClassName = packageName  + installerCamelName + "Assertion";
        assertion$1ClassName = packageName + installerCamelName + "Assertion$1";

        dialogClassName = consolePackageName + installerCamelName + "Dialog";
        dialog$1ClassName = consolePackageName + installerCamelName + "Dialog$1";

        serverAssertionClassName = serverPackageName + "Server" + installerCamelName + "Assertion";
    }
}

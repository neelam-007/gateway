package com.l7tech.server.config;

import com.l7tech.server.partition.PartitionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * User: megery
 * Date: Aug 29, 2006
 * Time: 1:08:23 PM
 */
public class ManualStepsManager {

    private final String eol = System.getProperty("line.separator");

    private final String unixLunaConfigCopy =
                "<li>" +
                    "LUNA CONFIGURATION: Copy the etc/Chrystoki.conf file from the primary node to each SSG in the cluster" + eol +
                        "<dl><dt></dt></dl>" + eol +
                "</li>" + eol;

    private final String windowsLunaConfigCopy =
            "<li>" + eol +
                "LUNA CONFIGURATION: Copy the LUNA_INSTALL_DIR/crystoki.ini file from the primary node to each SSG in the cluster" + eol +
                "<dl><dt></dt></dl>" + eol +
            "</li>" + eol;

    private final String windowsLunaString =
            "<dl>" + eol +
                "<dt>[Misc]<br>" + eol +
                    "ApplicationInstance=HTTP_SERVER<br>" + eol +
                    "AppIdMajor=1<br>" + eol +
                    "AppIdMinor=1<br>" + eol +
                "</dt>" + eol +
            "</dl>" + eol +
            "where AppIdMajor and AppIdMinor correspond to your Luna configuration" + eol;

    private final String windowsUpdateCrystokiLine =
            "<li>LUNA CONFIGURATION: Append the following to the LUNA_INSTALL_DIR/crystoki.ini file:" + eol +
                windowsLunaString + eol +
            "</li>" + eol;

    private final String linuxLunaString =
            "<dl>" + eol +
                "<dt>Misc = {</dt>" + eol +
                    "<dd>ApplicationInstance=HTTP_SERVER;</dd>" + eol +
                    "<dd>AppIdMajor=1;</dd>" + eol +
                    "<dd>AppIdMinor=1;</dd>" + eol +
                "<dt>}</dt>" + eol +
            "</dl>" + eol +
            "where AppIdMajor and AppIdMinor correspond to your Luna configuration" + eol;

    private final String unixUpdateCrystokiLine =
            "<li>LUNA CONFIGURATION: Append the following to the etc/Chrystoki.conf file:" + eol +
                 linuxLunaString + eol +
            "</li>" + eol;

    private final String runSSgConfigLine =
            "<li>RUN THE SSG CONFIGURATION WIZARD: run the wizard on each of the <br> " +
                "members of the cluster to generate the keystores" + eol +
                "<dl>" + eol +
                    "<dt>Note:</dt>" + eol +
                        "<dd>Use the same password for the keystore on each of the members of the cluster</dd>" + eol +
                "</dl>" + eol +
            "</li>" + eol;

    private final String timeSyncLine =
        "<li>TIME SYNCHRONIZATION:" +
            "<p>Please ensure time is synchronized among all SSG nodes within the cluster" + eol +
        "</li>" + eol;

    private SharedWizardInfo sharedWizardInfo;

    public ManualStepsManager() {
        sharedWizardInfo = SharedWizardInfo.getInstance();
    }

    private OSSpecificFunctions getOsFunctions() {
        return PartitionManager.getInstance().getActivePartition().getOSSpecificFunctions();
    }

    public List<String> getManualSteps(){

        String copykeysLine  = "<li>COPY THE KEYS: copy the contents of the keystore directory on the first node of the cluster to the keystore directory <br> " + eol +
            " on the other SSGs in the cluster" + eol +
                "<dl>" + eol +
                    "<dt>Note:</dt>" + eol +
                        "<dd>The keystore directory on this SSG is: \"" + getOsFunctions().getKeystoreDir() + "\"</dd>" + eol +
                "</dl>" + eol +
            "</li>" + eol;

        List<String> steps = new ArrayList<String>();

        if (getClusteringType() != ClusteringType.CLUSTER_MASTER &&
            getClusteringType() != ClusteringType.UNDEFINED) {
            steps.add(timeSyncLine);
            steps.add("<br>");
        }

        if (getKeystoreType() != KeystoreType.LUNA_KEYSTORE_NAME &&
            getKeystoreType() != KeystoreType.UNDEFINED) {
            switch (getClusteringType()) {
                case CLUSTER_MASTER:
                case CLUSTER_CLONE:
                    return steps;

//                case CLUSTER_JOIN:
//                    steps.add(runSSgConfigLine);
//                    steps.add(copykeysLine);
//                    break;
                case UNDEFINED:
                    break;

            }
        } else {
            switch (getClusteringType()) {
                case CLUSTER_MASTER:
                case CLUSTER_CLONE:
                    steps.add(getOsFunctions().isUnix()? unixUpdateCrystokiLine :windowsUpdateCrystokiLine);
                    break;

//                case CLUSTER_JOIN:
//                    steps.add(getOsFunctions().isUnix()? unixLunaConfigCopy :windowsLunaConfigCopy);
//                    break;
                case UNDEFINED:
                    break;
            }
        }

        if (!steps.isEmpty()) steps.add("<br>");
        return steps;
    }

    private ClusteringType getClusteringType() {
        return sharedWizardInfo.getClusterType();
    }

    private KeystoreType getKeystoreType() {
        return sharedWizardInfo.getKeystoreType();
    }
}

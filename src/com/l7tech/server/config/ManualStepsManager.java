package com.l7tech.server.config;

import java.util.ArrayList;
import java.util.List;

/**
 * User: megery
 * Date: Aug 29, 2006
 * Time: 1:08:23 PM
 */
public class ManualStepsManager {

    private final String eol = System.getProperty("line.separator");
    private final OSSpecificFunctions osFunctions = OSDetector.getOSSpecificFunctions(); ;

    private final String linuxLunaConfigCopy =
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

    private final String linuxUpdateCrystokiLine =
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

    private final String updateHostsFileLine =
        "<li>UPDATE HOSTS FILE:" +
            "<p>add a line containing the IP address for this SSG node, then the cluster host name, then this SSG node's hostname" + eol +
            "<dl>" + eol +
                "<dt>ex:</dt>" + eol +
                    "<dd>192.168.1.186      ssgcluster.domain.com ssgnode1.domain.com</dd>" + eol +
            "</dl>" + eol +
        "</li>" + eol;

    private final String timeSyncLine =
        "<li>TIME SYNCHRONIZATION:" +
            "<p>Please ensure time is synchronized among all SSG nodes within the cluster" + eol +
        "</li>" + eol;

    private final String copykeysLine = "<li>COPY THE KEYS: copy the contents of the keystore directory on the first node<br> " + eol +
            "of the cluster to the keystore directory on the other SSGs in the cluster" + eol +
                "<dl>" + eol +
                    "<dt>Note:</dt>" + eol +
                        "<dd>The SSG keystore directory is: \"" + osFunctions.getKeystoreDir() + "\"</dd>" + eol +
                "</dl>" + eol +
            "</li>" + eol;


    private KeystoreType keystoreType;
    private ClusteringType clusteringType;


    public ManualStepsManager() {
    }

    public List<String> getManualSteps(){
        List<String> steps = new ArrayList<String>();

        if (getClusteringType() != ClusteringType.CLUSTER_NONE) {
            steps.add(updateHostsFileLine);
            steps.add(timeSyncLine);
            steps.add("<br>");
        }

        if (getKeystoreType() != KeystoreType.LUNA_KEYSTORE_NAME) {
            switch (getClusteringType()) {
                case CLUSTER_NONE:
                case CLUSTER_NEW:
                    return steps;

                case CLUSTER_JOIN:
                    steps.add(runSSgConfigLine);
                    steps.add(copykeysLine);
                    break;

            }
        } else {
            switch (getClusteringType()) {
                case CLUSTER_NONE:
                case CLUSTER_NEW:
                    steps.add(osFunctions.isLinux()?linuxUpdateCrystokiLine:windowsUpdateCrystokiLine);
                    break;

                case CLUSTER_JOIN:
                    steps.add(osFunctions.isLinux()?linuxLunaConfigCopy:windowsLunaConfigCopy);
                    break;
            }
        }

        steps.add("<br>");
        return steps;
    }

    public void setKeystoreType(KeystoreType keystoreType) {
        this.keystoreType = keystoreType;
    }

    public void setClusteringType(ClusteringType clusteringType) {
        this.clusteringType = clusteringType;
    }

    private ClusteringType getClusteringType() {
        return clusteringType;
    }

    private KeystoreType getKeystoreType() {
        return keystoreType;
    }
}

package com.l7tech.server.config.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.KeyStoreConstants;
import com.l7tech.server.config.ListHandler;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.beans.ClusteringConfigBean;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 30, 2005
 * Time: 2:56:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigWizardResultsPanel extends ConfigWizardStepPanel {
    private JPanel mainPanel;

    private JButton saveButton;
    private JTextPane messageText;
    private JTextArea logsView;
    private JPanel logsPanel;
    private JButton viewManualSteps;
    private JButton saveManualSteps;
    private JPanel manualStepsPanel;
    private JLabel manualStepsMessage;

    private ArrayList steps;
    private StringBuffer stepsBuffer;
    private String eol;

    public ConfigWizardResultsPanel(WizardStepPanel next, OSSpecificFunctions functions) {
        super(next, functions);
        init();
    }

    private void init() {
        steps = new ArrayList();
        stepsBuffer = new StringBuffer();
        eol = osFunctions.isWindows()?"\r\n":"\n";

        setShowDescriptionPanel(false);
        configBean = null;
        configCommand = null;
        stepLabel = "Configuration Results";
        saveButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                doSaveLogs();
            }
        });

        viewManualSteps.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                doViewManualSteps();
            }
        });

        saveManualSteps.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSaveManualSteps();
            }
        });
        logsView.setBackground(mainPanel.getBackground());
        messageText.setBackground(mainPanel.getBackground());
        messageText.setFont(messageText.getFont().deriveFont(Font.BOLD));

        manualStepsMessage.setForeground(Color.RED);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    private void doSaveManualSteps() {
        File selectedFile = getUserSelectedFile("save", "html", "HTML files");
        if (selectedFile != null) {
            PrintStream ps = null;
            try {
                ps = new PrintStream(new FileOutputStream(selectedFile));
                String line = stepsBuffer.toString();
                ps.println(line);
            } catch (FileNotFoundException e) {
            } finally {
                if (ps != null) {
                    ps.close();
                }
            }
        }
    }

    private void doViewManualSteps() {
        String regex = Pattern.compile(eol).toString();
        String message = stepsBuffer.toString().replaceAll(regex, "");
        JOptionPane.showMessageDialog(this, message.toString());
    }

    private File getUserSelectedFile(String chooserType, final String fileFilter, final String fileDescription) {
        JFileChooser fc = new JFileChooser(".");
        if (StringUtils.isNotEmpty(fileFilter) && StringUtils.isNotEmpty(fileDescription)) {
            fc.setFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    return  f.isDirectory() || f.getName().toUpperCase().endsWith(fileFilter.toUpperCase());
                }

                public String getDescription() {
                    return fileDescription;
                }
            });
        }

        int retval = 0;
        if (chooserType.equalsIgnoreCase("open")) {
            retval = fc.showOpenDialog(this);
        } else if (chooserType.equalsIgnoreCase("save")) {
            retval = fc.showSaveDialog(this);
        } else {
            retval = fc.showOpenDialog(this);
        }

        File selectedFile = null;
        if (retval == JFileChooser.CANCEL_OPTION) {
        } else if (retval == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
        }
        return selectedFile;
    }

    private void doSaveLogs() {
        File selectedFile = getUserSelectedFile("save");
        if (selectedFile != null) {
            PrintStream ps = null;
            try {
                ps = new PrintStream(new FileOutputStream(selectedFile));

                ps.print(logsView.getText());

                ps.close();
                ps = null;
            } catch (FileNotFoundException e) {
            } finally{
                if (ps != null) {
                    ps.close();
                }
            }
        }
    }

    private File getUserSelectedFile(String s) {
        return getUserSelectedFile(s, null, null);
    }

    protected void updateModel(HashMap settings) {
    }


    private void doApplyConfig() {
        getParentWizard().applyConfiguration();
        ArrayList logs = ListHandler.getLogList();
        ConfigurationWizard wizard = getParentWizard();
        if (wizard != null) {
            wizard.setCancelEnabled(false);
            wizard.setEnableBackButton(false);
        }
        if (logs.size() > 0) {
            Iterator logIter = logs.iterator();
            StringBuffer logBuffer = new StringBuffer();
            while (logIter.hasNext()) {
                logBuffer.append((String)logIter.next()).append("\n");
            }
            logsView.setText(logBuffer.toString());
        }
        boolean hadFailures = getParentWizard().isHadFailures();
        if (hadFailures) {
            messageText.setForeground(Color.RED);
            messageText.setText("There were errors during configuration, see below for details");
        } else {
            messageText.setText("The configuration was successfully applied" + eol +
                    "You must restart the SSG in order for the configuration to take effect.");
        }

        setupManualStepsPanel(wizard.getClusteringType(), wizard.getKeystoreType());
    }

    private void doUpdateView() {
        getParentWizard().getBackButton().setEnabled(false);
        getParentWizard().getNextButton().setEnabled(false);
        getParentWizard().getFinishButton().setEnabled(false);
        getParentWizard().getCancelButton().setEnabled(false);

        messageText.setText("Please wait while the configuration is being applied");

        //clear panel
        hideControls();
        invalidate();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                doApplyConfig();
                showControls();
                invalidate();
            }
        });
     }

    protected void updateView(final HashMap settings) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                doUpdateView();
            }
        });
    }

    private void showControls() {
        logsPanel.setVisible(true);
        saveButton.setVisible(true);
        getParentWizard().getFinishButton().setEnabled(true);
    }

    private void hideControls() {
        manualStepsPanel.setVisible(false);
        logsPanel.setVisible(false);
        saveButton.setVisible(false);
    }

    private void setupManualStepsPanel(int clusteringType, String keystoreType) {
        boolean lunaMentioned = false;
        String infoLine = "<h3>The following <u>manual</u> steps are required to complete the configuration of the SSG</h3>" + eol;
        String linuxLunaConfigCopy =    "<li>" + eol +
                                            "LUNA CONFIGURATION: Copy the etc/Chrystoki.conf file from the primary node to each SSG in the cluster" + eol +
                                            "<dl><dt></dt></dl>" + eol +
                                        "</li>" + eol;

        String windowsLunaConfigCopy =  "<li>" + eol +
                                            "LUNA CONFIGURATION: Copy the LUNA_INSTALL_DIR/crystoki.ini file from the primary node to each SSG in the cluster" + eol +
                                            "<dl><dt></dt></dl>" + eol +
                                        "</li>" + eol;

        String windowsLunaString =  "<dl>" + eol +
                                        "<dt>[Misc]<br>" + eol +
                                            "ApplicationInstance=HTTP_SERVER<br>" + eol +
                                            "AppIdMajor=1<br>" + eol +
                                            "AppIdMinor=1<br>" + eol +
                                        "</dt>" + eol +
                                    "</dl>" + eol +
                                    "where AppIdMajor and AppIdMinor correspond to your Luna configuration" + eol;

        String windowsUpdateCrystokiLine =  "<li>LUNA CONFIGURATION: Append the following to the LUNA_INSTALL_DIR/crystoki.ini file:" + eol +
                                                windowsLunaString + eol +
                                            "</li>" + eol;

        String linuxLunaString =    "<dl>" + eol +
                                        "<dt>Misc = {</dt>" + eol +
                                            "<dd>ApplicationInstance=HTTP_SERVER;</dd>" + eol +
                                            "<dd>AppIdMajor=1;</dd>" + eol +
                                            "<dd>AppIdMinor=1;</dd>" + eol +
                                        "<dt>}</dt>" + eol +
                                    "</dl>" + eol +
                                    "where AppIdMajor and AppIdMinor correspond to your Luna configuration" + eol;

        String linuxUpdateCrystokiLine =    "<li>LUNA CONFIGURATION: Append the following to the etc/Chrystoki.conf file:" + eol +
                                                 linuxLunaString + eol +
                                            "</li>" + eol;

        String updateHostsFileLine =    "<li>UPDATE HOSTS FILE: add a line which contains the IP address for this SSG node, then the <br>" +
                                        "cluster host name, then this SSG node's hostname" + eol +
                                        "<dl>" + eol +
                                            "<dt>ex:</dt>" + eol +
                                                "<dd>192.168.1.186      ssgcluster.domain.com ssgnode1.domain.com</dd>" + eol +
                                        "</dl>" + eol +
                                        "</li>" + eol;

        String timeSyncLine =   "<li>" + eol +
                                    "TIME SYNCHRONIZATION: Please ensure time is synchronized among all SSG nodes <br>" +
                                    "within the cluster" + eol +
                                "</li>" + eol;

        String runSSgConfigLine =   "<li>RUN THE SSG CONFIGURATION WIZARD: run the wizard on each of the <br> " +
                                    "members of the cluster to generate the keystores" + eol +
                                    "<dl>" + eol +
                                        "<dt>Note:</dt>" + eol +
                                            "<dd>Use the same password for the keystore on each of the members of the cluster</dd>" + eol +
                                    "</dl>" + eol +
                                    "</li>";

        String copykeysLine =   "<li>COPY THE KEYS: copy the contents of the keystore directory on the first node<br> " + eol +
                                "of the cluster to the keystore directory on the other SSGs in the cluster" + eol +
                                "<dl>" + eol +
                                    "<dt>Note:</dt>" + eol +
                                        "<dd>The SSG keystore directory is: \"" + osFunctions.getKeystoreDir() + "\"</dd>" + eol +
                                "</dl>" + eol +
                                "</li>" + eol;

        if (clusteringType == ClusteringConfigBean.CLUSTER_NONE && !keystoreType.equalsIgnoreCase(KeyStoreConstants.LUNA_KEYSTORE_NAME)) {
            manualStepsPanel.setVisible(false);
        } else {
            stepsBuffer.append("<html>" + eol).append(
                            "<head><title></title></head>" + eol).append(
                            "<body>" + eol).append(
                                    infoLine);

            if (clusteringType != ClusteringConfigBean.CLUSTER_NONE) {
                stepsBuffer.append(     "<ul>" + eol).append(
                                    updateHostsFileLine).append(
                                    timeSyncLine);


                if (clusteringType == ClusteringConfigBean.CLUSTER_JOIN) {

                    if (keystoreType == KeyStoreConstants.LUNA_KEYSTORE_NAME) {
                        lunaMentioned = true;
                        if (osFunctions.isLinux()) {
                            stepsBuffer.append(linuxLunaConfigCopy);
                        } else {
                            stepsBuffer.append(windowsLunaConfigCopy);
                        }
                    }
                    else {
                        stepsBuffer.append(runSSgConfigLine).append(
                            copykeysLine);
                    }
                }

                if (clusteringType == ClusteringConfigBean.CLUSTER_NEW) {
                    if (keystoreType.equalsIgnoreCase(KeyStoreConstants.LUNA_KEYSTORE_NAME)) {
                        lunaMentioned = true;
                        //instructions for luna in a clustered environment
                        if (osFunctions.isLinux()) {
                            stepsBuffer.append(linuxUpdateCrystokiLine);
                        } else {
                            stepsBuffer.append(windowsUpdateCrystokiLine);
                        }
                    }
                }
                stepsBuffer.append(     "</ul>" + eol);
            } else {
                stepsBuffer.append("<ul>" + eol);
                if (keystoreType == KeyStoreConstants.LUNA_KEYSTORE_NAME && !lunaMentioned) {
                    if (osFunctions.isLinux()) {
                        stepsBuffer.append(linuxUpdateCrystokiLine);
                    } else {
                        stepsBuffer.append(windowsUpdateCrystokiLine);
                    }
                    stepsBuffer.append("</ul>" + eol);
                }
            }
            stepsBuffer.append("</body>" + eol);
            stepsBuffer.append("</html>" + eol);
            manualStepsPanel.setVisible(true);
        }
    }

    public boolean canFinish() {
        return true;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();

        StringBuffer message = new StringBuffer();
        message.append("<html>\n").append(
                "<body>\n" +
                "\t<ul><li><dl><dt>A DT</dt></dl></li></ul></body></html>");

        String regex = Pattern.compile("\n").toString();
        String s = message.toString();
        String s2 = s.replaceAll(regex, "");
        JOptionPane.showMessageDialog(frame,s2);
        System.exit(0);

    }
}
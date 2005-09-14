package com.l7tech.server.config.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.ListHandler;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.beans.ClusteringConfigBean;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

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

    public ConfigWizardResultsPanel(WizardStepPanel next, OSSpecificFunctions functions) {
        super(next, functions);
        init();
    }

    private void init() {
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

        manualStepsMessage.setForeground(Color.RED);
        steps = new ArrayList();

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    private void doSaveManualSteps() {
        File selectedFile = getUserSelectedFile("save", "html", "HTML files");
        if (selectedFile != null) {
            PrintStream ps = null;
            try {
                ps = new PrintStream(new FileOutputStream(selectedFile));
                Iterator iter = steps.iterator();
                while (iter.hasNext()) {
                    String line = (String) iter.next();
                    ps.println(line);
                }
            } catch (FileNotFoundException e) {
            } finally {
                if (ps != null) {
                    ps.close();
                }
            }
        }
    }

    private void doViewManualSteps() {
        Iterator iter = steps.iterator();
        StringBuffer buf = new StringBuffer();
        while (iter.hasNext()) {
            String line = (String) iter.next();
            buf.append(line);
        }
        JOptionPane.showMessageDialog(this, buf.toString());
    }

    private File getUserSelectedFile(String chooserType, final String fileFilter, final String fileDescription) {
        JFileChooser fc = new JFileChooser(".");
        if (StringUtils.isNotEmpty(fileFilter) && StringUtils.isNotEmpty(fileDescription)) {
            fc.setFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    return  f.getName().toUpperCase().endsWith(fileFilter.toUpperCase());
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

    protected void updateView(HashMap settings) {
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
            messageText.setText("There were errors during configuration, see below for details");
        } else {
            messageText.setText("The configuration was successfully applied\n" +
                    "You must restart the SSG in order for the configuration to take effect.");
        }

        setupManualStepsPanel(wizard.getClusteringType());

    }

    private void setupManualStepsPanel(int clusteringType) {
        if (clusteringType == ClusteringConfigBean.CLUSTER_NONE) {
            manualStepsPanel.setVisible(false);
        } else {
            steps.add("<html>");
            steps.add("<body>");
            steps.add("<h3>The following <u>manual</u> steps are required to correctly configure the SSG cluster</h2>");
            steps.add("<br>");
            steps.add("<ul>");
            steps.add("<li>UPDATE HOSTS FILE: add a line which contains the IP address for this SSG, then the cluster host name, then the true hostname</li>");
            steps.add("        ex: 192.168.1.186      ssgcluster.domain.com realssgname");
            steps.add("");
            steps.add("<li>TIME SYNCHRONIZATION: Please ensure time is synchronized among all SSG nodes within the cluster</li>");
            steps.add("");

            if (clusteringType == ClusteringConfigBean.CLUSTER_JOIN) {
                steps.add("<li>RUN THE SSG CONFIGURATION WIZARD: run the wizard on each of the members of the cluster to generate the keystores</li>");
                steps.add("        Use the same password for the keystore on each of the members of the cluster");
                steps.add("");
                steps.add("<li>COPY THE KEYS: copy the contents of the keystore directory (SSG_ROOT/etc/keys) on the first node</li>");
                steps.add("        in the SSG cluster to the keystore directory (SSG_ROOT/etc/keys) on the other SSGs in the cluster");
            }
            steps.add("</ul>");
            steps.add("</body>");
            steps.add("</html>");
            manualStepsPanel.setVisible(true);
        }
    }

    public boolean canFinish() {
        return true;
    }
}

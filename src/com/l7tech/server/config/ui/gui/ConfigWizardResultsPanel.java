package com.l7tech.server.config.ui.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.ListHandler;
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
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

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

    private StringBuilder stepsBuffer;
    private String eol;

    public ConfigWizardResultsPanel(WizardStepPanel next) {
        super(next);
        init();
    }

    private void init() {
        stepsBuffer = new StringBuilder();
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
            String selectedFileName = selectedFile.getPath();
            if (!selectedFileName.endsWith(".html")) {
                selectedFileName = selectedFileName + ".html";
            }
            PrintStream ps = null;
            try {
                ps = new PrintStream(new FileOutputStream(selectedFileName));
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
            String selectedFileName = selectedFile.getPath();
            if (!selectedFileName.endsWith(".log")) {
                selectedFileName = selectedFileName + ".log";
            }
            PrintStream ps = null;
            try {
                ps = new PrintStream(new FileOutputStream(selectedFileName));

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

    protected void updateModel() {
    }

    private void doApplyConfig() {
        getParentWizard().applyConfiguration();
        List<String> logs = ListHandler.getLogList();
        ConfigurationWizard wizard = getParentWizard();
        if (wizard != null) {
            wizard.setCancelEnabled(false);
            wizard.setEnableBackButton(false);
        }
        if (logs != null && logs.size() > 0) {
            StringBuffer logBuffer = new StringBuffer();
            for (String log : logs) {
                logBuffer.append(log).append("\n");
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

        setupManualStepsPanel();
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

    protected void updateView() {
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

    private void setupManualStepsPanel() {
        StringBuilder allSteps = new StringBuilder();

        Map<String, List<String>> manualSteps = getParentWizard().getManualSteps();
        boolean showManualSteps = manualSteps != null && !manualSteps.isEmpty();
        if (showManualSteps) {
            Set<String> keys = manualSteps.keySet();
            for (String key : keys) {
                List<String> steps = manualSteps.get(key);
                for (String step : steps) {
                    allSteps.append(step);
                }
            }

            stepsBuffer.append("<html>").append(eol);
            stepsBuffer.append("<head><title></title></head>").append(eol);
            stepsBuffer.append("<body>").append(eol);
            stepsBuffer.append("<h3>The following <u>manual</u> steps are required to complete the configuration of the SSG</h3>").append(eol);

            stepsBuffer.append(allSteps).append(eol);
            stepsBuffer.append("</body>").append(eol);
            stepsBuffer.append("</html>").append(eol);
        }

        manualStepsPanel.setVisible(showManualSteps);
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
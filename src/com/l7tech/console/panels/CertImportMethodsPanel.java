package com.l7tech.console.panels;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */
public class CertImportMethodsPanel extends WizardStepPanel {

    private JPanel mainPanel;
    private JPanel certImportMethodsPane;
    private JRadioButton copyAndPasteRadioButton;
    private JRadioButton fileRadioButton;
    private JRadioButton urlConnRadioButton;
    private JButton browseButton;
    private JTextField certFileName;
    private JTextArea copyAndPasteTextArea;
    private JTextField urlConnTextField;


    public CertImportMethodsPanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                //Create a file chooser
                final JFileChooser fc = new JFileChooser();

                int returnVal = fc.showOpenDialog(CertImportMethodsPanel.this);

                File file = null;
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    file = fc.getSelectedFile();

                    certFileName.setText(file.getAbsolutePath());
                } else {
                    // cancelled by user
                }

            }
        });

        ButtonGroup bg = new ButtonGroup();
        bg.add(copyAndPasteRadioButton);
        bg.add(fileRadioButton);
        bg.add(urlConnRadioButton);

        // urlConnection as the default
        urlConnRadioButton.setSelected(true);
        browseButton.setEnabled(false);

        copyAndPasteRadioButton.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateEnableDisable();
            }
        });

        fileRadioButton.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateEnableDisable();
            }
        });

        urlConnRadioButton.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateEnableDisable();
            }
        });
    }

    private void updateEnableDisable() {
        if (copyAndPasteRadioButton.isSelected()) {
            browseButton.setEnabled(false);
            copyAndPasteTextArea.setEnabled(true);
            urlConnTextField.setEnabled(false);
            certFileName.setEnabled(false);

        } else if (fileRadioButton.isSelected()) {
            browseButton.setEnabled(true);
            copyAndPasteTextArea.setEnabled(false);
            urlConnTextField.setEnabled(false);
            certFileName.setEnabled(true);
        }

        if (urlConnRadioButton.isSelected()) {
            browseButton.setEnabled(false);
            copyAndPasteTextArea.setEnabled(false);
            urlConnTextField.setEnabled(true);
            certFileName.setEnabled(false);
        }
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Enter Certificate Info";
    }

    /**
     * Store the values of all fields on the panel to the wizard object which is a used for
     * keeping all the modified values. The wizard object will be used for providing the
     * updated values when updating the server.
     *
     * @param settings the object representing wizard panel state
     */
    public void storeSettings(Object settings) {

        if (settings != null) {

            if (settings instanceof CertInfo) {

                CertInfo ci =  (CertInfo) settings;

                if (copyAndPasteRadioButton.isSelected()) {
                    ci.setCertDataSource(new String(copyAndPasteTextArea.getText()));

                } else if (fileRadioButton.isSelected()) {
                    try {
                       ci.setCertDataSource(new File(certFileName.getText().trim()));
                    } finally {
                        //do nothing
                    }
                } else if (urlConnRadioButton.isSelected()) {
                    try {
                        ci.setCertDataSource(new URL(urlConnTextField.getText().trim()));

                    } catch (MalformedURLException e) {
                        //todo:
                    }

                } else {
                    //todo:
                }

            }
        }

    }

    /**
     * Test whether the step is finished and it is safe to finish the wizard.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canFinish() {
        return false;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        final JPanel _1;
        _1 = new JPanel();
        mainPanel = _1;
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        certImportMethodsPane = _2;
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(7, 3, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JRadioButton _3;
        _3 = new JRadioButton();
        urlConnRadioButton = _3;
        _3.setText("Retrieve via SSL Connection");
        _3.setSelected(false);
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JRadioButton _4;
        _4 = new JRadioButton();
        fileRadioButton = _4;
        _4.setText("Import from a File");
        _4.setEnabled(true);
        _4.setSelected(false);
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JRadioButton _5;
        _5 = new JRadioButton();
        copyAndPasteRadioButton = _5;
        _5.setText("Copy and Paste (Base64 PEM)");
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JTextField _6;
        _6 = new JTextField();
        urlConnTextField = _6;
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JTextField _7;
        _7 = new JTextField();
        certFileName = _7;
        _2.add(_7, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JButton _8;
        _8 = new JButton();
        browseButton = _8;
        _8.setText("Browse");
        _2.add(_8, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        final JScrollPane _9;
        _9 = new JScrollPane();
        _2.add(_9, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 5, 1, 0, 3, 7, 7, null, null, null));
        final JTextArea _10;
        _10 = new JTextArea();
        copyAndPasteTextArea = _10;
        _10.setRows(5);
        _9.setViewportView(_10);
        final com.intellij.uiDesigner.core.Spacer _11;
        _11 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_11, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 2, 1, 6, null, null, null));
        final JPanel _12;
        _12 = new JPanel();
        _12.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_12, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JLabel _13;
        _13 = new JLabel();
        _13.setText("Select the method of obtaining a certificate");
        _12.add(_13, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _14;
        _14 = new com.intellij.uiDesigner.core.Spacer();
        _12.add(_14, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 2, 1, 6, new Dimension(-1, 10), new Dimension(-1, 10), null));
        final com.intellij.uiDesigner.core.Spacer _15;
        _15 = new com.intellij.uiDesigner.core.Spacer();
        _12.add(_15, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 2, 1, 6, new Dimension(-1, 10), new Dimension(-1, 10), null));
        final com.intellij.uiDesigner.core.Spacer _16;
        _16 = new com.intellij.uiDesigner.core.Spacer();
        _1.add(_16, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 2, 1, 6, null, null, null));
    }

}

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.console.text.MaxLengthDocument;
import com.l7tech.console.util.Preferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PreferencesDialog
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PreferencesDialog extends JDialog {
    static Logger log = Logger.getLogger(PreferencesDialog.class.getName());

    private static final int DEFAULT_INACTIVITY_TIMEOUT = 30;

    /**
     * Resource bundle with default locale
     */
    private ResourceBundle resources = null;

    //private JTextField serverUrlTextField = null;
    private JComboBox lfComboBox = null;
    private JTextField inactivityTextField = null;
    private JCheckBox rememberLastIdCheckBox = null;


    /**
     * Command string for a cancel action (e.g., a button or menu item).
     */
    private String CMD_CANCEL = "cmd.cancel";

    /**
     * Command string for a help action (e.g., a button or menu item).
     */
    private String CMD_HELP = "cmd.help";

    /**
     * Command string for an "OK" action
     */
    private String CMD_OK = "cmd.ok";

    /** preferences instance */
    private Properties props = null;


    /**
     * Creates new form Preferences
     */
    public PreferencesDialog(Frame parent, boolean modal, boolean isConnected) {
        super(parent, modal);
        initResources();
        initComponents();
        loadPreferences();
        pack();
    }

    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources =
          ResourceBundle.getBundle("com.l7tech.console.resources.PreferencesDialog", locale);
    }


    /**
     *
     * Called from the constructor to initialize this window
     */
    private void initComponents() {

        // initialize the window
        setTitle(resources.getString("window.title"));
        Container contents = getContentPane();
        GridBagLayout grid = new GridBagLayout();
        contents.setLayout(grid);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                // user hit "close" button at top of window
                windowAction(CMD_CANCEL);
            }
        });
        Actions.setEscKeyStrokeDisposes(this);

        lfComboBox = new JComboBox();

        // look and feel label
        JLabel lfLabel = new JLabel();
        lfLabel.setDisplayedMnemonic(
          resources.getString("lookAndFeelComboBox.mnemonic").charAt(0));
        // setLabelFor() allows the label's mnemonic to work for the component
        lfLabel.setLabelFor(lfComboBox);
        lfLabel.setText(resources.getString("lookAndFeelComboBox.label"));
        GridBagConstraints constraints;
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(11, 12, 0, 0);

        contents.add(lfLabel, constraints);

        UIManager.LookAndFeelInfo[]
          lookAndFeels = UIManager.getInstalledLookAndFeels();
        for (int i = 0; lookAndFeels != null && i < lookAndFeels.length; i++) {
            try {
                LookAndFeel lookAndFeel =
                  (LookAndFeel) Class.forName(lookAndFeels[i].getClassName()).newInstance();

                if (lookAndFeel.isSupportedLookAndFeel()) {
                    lfComboBox.addItem(lookAndFeels[i].getName());
                    String lnf = getPreferences().getProperty(Preferences.LOOK_AND_FEEL);
                    if (lnf != null) {
                        if (lookAndFeels[i].getClassName().equals(lnf))
                            lfComboBox.setSelectedItem(lookAndFeels[i].getName());
                    }
                }

            } catch (Exception e) {
                log.log(Level.SEVERE, "getLookAndFeelMenu()", e);
            }
        }

        lfComboBox.setToolTipText(
          resources.
          getString("lookAndFeelComboBox.tooltip"));
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(11, 7, 0, 10);
        contents.add(lfComboBox, constraints);

        // inactivity timeout enable/disable
        JLabel inactivityLabel =
          new JLabel(resources.getString(("inactivityTimeout.label")));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 12;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(11, 12, 0, 0);
        contents.add(inactivityLabel, constraints);

        // inactivity timeout text field
        inactivityTextField = new JTextField(3);
        inactivityTextField.setDocument(new MaxLengthDocument(2));
        inactivityTextField.setToolTipText(resources.getString(("inactivityTimeout.tooltip")));
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 12;
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(11, 7, 0, 11);
        contents.add(inactivityTextField, constraints);

        // remember last login ID
        // inactivity timeout enable/disable
        JLabel rememberLastIdLabel =
          new JLabel(resources.getString(("rememberLastId.label")));
        rememberLastIdLabel.setToolTipText(resources.getString(("rememberLastId.tooltip")));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 14;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(11, 12, 0, 0);
        contents.add(rememberLastIdLabel, constraints);


        rememberLastIdCheckBox = new JCheckBox();
        rememberLastIdCheckBox.setToolTipText(resources.getString(("rememberLastId.tooltip")));
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 14;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(11, 7, 0, 11);
        contents.add(rememberLastIdCheckBox, constraints);
        try {
            String sb = getPreferences().getProperty(Preferences.SAVE_LAST_LOGIN_ID);
            boolean b = Boolean.valueOf(sb).booleanValue();

            rememberLastIdCheckBox.setSelected(b);
        } catch (IOException e) {
            log.log(Level.WARNING, "initComponents()", e);
        }

        rememberLastIdCheckBox.
          addActionListener(new ActionListener() {
              /**
               * Invoked when an action occurs.
               */
              public void actionPerformed(ActionEvent e) {
                  try {
                      getPreferences().
                        setProperty(Preferences.SAVE_LAST_LOGIN_ID,
                          (new Boolean(((JCheckBox) e.getSource()).isSelected())).toString());
                  } catch (IOException ex) {
                      ; // swallow
                  }
              }
          });

        // Button panel at the bottom of the window
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, 0));

        // ok button
        JButton okButton = new JButton();
        okButton.setText(resources.getString("okButton.label"));
        okButton.setActionCommand(CMD_OK);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });
        buttonPanel.add(okButton);

        // space
        buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));

        // cancel button
        JButton cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.label"));
        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });
        buttonPanel.add(cancelButton);

        // space
        buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));

        // help button
        JButton helpButton = new JButton();
        helpButton.setText(resources.getString("helpButton.label"));
        helpButton.setMnemonic(
          resources.getString("helpButton.mnemonic").charAt(0));
        helpButton.setActionCommand(CMD_HELP);
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                Actions.invokeHelp(PreferencesDialog.this);
            }
        });
        buttonPanel.add(helpButton);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 16;
        constraints.gridwidth = 7;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(17, 12, 11, 11);

        contents.add(buttonPanel, constraints);

        // equalize button sizes
        getRootPane().setDefaultButton(okButton);
        JButton buttons[] =
          new JButton[]{
              okButton, helpButton, cancelButton
          };
        Utilities.equalizeButtonSizes(buttons);

        JLabel[] jLabels =
          new JLabel[]{
              inactivityLabel,
              lfLabel
          };

        // equalize label sizes
        Utilities.equalizeLabelSizes(jLabels);

    }


    /**
     * The user has selected an option.
     * If actionCommand is an ActionEvent, getCommandString() is called,
     * otherwise toString() is used to get the action command.
     *
     * @param actionCommand may be null
     */
    private void windowAction(Object actionCommand) {
        boolean closeWindow = false;

        if (actionCommand == null) {
            // do nothing
        } else if (actionCommand.equals(CMD_CANCEL)) {
            closeWindow = true;
        } else if (actionCommand.equals(CMD_HELP)) {
            ;
        } else if (actionCommand.equals(CMD_OK)) {
            if (validatePreferences()) {
                savePreferences();
                closeWindow = true;
            } else {
            }
        } else {
        }
        if (closeWindow) {
            setVisible(false);
            dispose();
        }
    }

    /**
     * validate preferences
     *
     * @return true if validated ok, false otherwise
     */
    private boolean validatePreferences() {
        try {
            UIManager.LookAndFeelInfo[]
              lookAndFeels = UIManager.getInstalledLookAndFeels();
            for (int i = 0; lookAndFeels != null && i < lookAndFeels.length; i++) {
                if (lookAndFeels[i].getName().equals(lfComboBox.getSelectedItem())) {
                    getPreferences().setProperty(Preferences.LOOK_AND_FEEL,
                      lookAndFeels[i].getClassName());
                }
            }

            int timeout = -1;
            try {
                timeout = Integer.parseInt(inactivityTextField.getText());
            } catch (NumberFormatException e) {
                timeout = -1;
            }

            if (timeout < 0 || timeout > 60) {
                JOptionPane.showMessageDialog(null,
                  "The inactivity timeout should be a number of minutes\n" +
                  " between 0 and 60.  Use a value of 0 (zero)\n" +
                  "to disable the timeout.",
                  "Bad inactivity timeout",
                  JOptionPane.ERROR_MESSAGE);
                return false;
            }

            getPreferences().setProperty(Preferences.INACTIVITY_TIMEOUT, Integer.toString(timeout));

            return true;
        } catch (IOException e) {
            log.log(Level.SEVERE, "preferences", e);
        }

        return false;
    }

    /** save preferences */
    private void savePreferences() {
        try {
            // update & append
            Preferences prefs = Preferences.getPreferences();
            prefs.updateFromProperties(getPreferences(), true);
            prefs.store();
            prefs.updateSystemProperties();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error saving Preferences", e);
        }
    }

    /** load preferences into the form fields */
    private void loadPreferences() {
        try {
            String sTimeout = getPreferences().getProperty(Preferences.INACTIVITY_TIMEOUT);
            int timeout = DEFAULT_INACTIVITY_TIMEOUT;
            try {
                timeout = Integer.parseInt(sTimeout);
            } catch (NumberFormatException e) {
                ; // swallow; bad property value
            }
            inactivityTextField.setText(Integer.toString(timeout));
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error retrieving Preferences", e);
        }
    }

    /**
     * instantiate and returns the copy of preferences
     *
     * @return the <CODE>Properties</CODE> containing
     *         the current Prefernces state
     * @exception java.io.IOException
     */
    private Properties getPreferences() throws IOException {
        if (props == null) {
            props = Preferences.getPreferences().asProperties();
        }
        return props;
    }

    /**
     * This main() is provided for debugging purposes, to display a
     * sample dialog.
     */
    public static void main(String args[]) {
        JFrame frame = new JFrame() {
            public Dimension getPreferredSize() {
                return new Dimension(200, 100);
            }
        };
        frame.setTitle("Debugging frame");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(false);

        JDialog dialog = new PreferencesDialog(frame, false, false);
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }

            public void windowClosed(WindowEvent event) {
                System.exit(0);
            }
        });
        dialog.pack();
        dialog.setVisible(true);
    }
}




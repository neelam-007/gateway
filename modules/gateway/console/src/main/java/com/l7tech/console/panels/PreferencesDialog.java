package com.l7tech.console.panels;

import com.l7tech.console.MainWindow;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.History;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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
    private static final Logger log = Logger.getLogger(PreferencesDialog.class.getName());
    private static final int DEFAULT_INACTIVITY_TIMEOUT = 30; //this value MUST be the same as AbstractSsmPreferences.java
    private static final int DEFAULT_NUM_HOSTS_HISTORY = 5;

    /**
     * Resource bundle with default locale
     */
    private ResourceBundle resources;

    private JPanel mainPanel;
    private JLabel inactivityLabel;
    private JTextField inactivityTextField;
    private JLabel rememberLastIdLabel;
    private JCheckBox rememberLastIdCheckBox;
    private JCheckBox enableValidationCheckBox;
    private JLabel numHostsHistoryLabel;
    private JTextField numHostsHistoryTextField;
    private JTextField maxLeftCommentTextField;
    private JTextField maxRightCommentTextField;
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private JTextField fontScaleTextField;

    /** preferences instance */
    private Properties props;
    private int previousMaxLeft;
    private int previousMaxRight;
    private boolean commentSizeChanged;
    private boolean fontScaleChanged;
    private float prevFontScale;


    /**
     * Creates new form Preferences
     */
    public PreferencesDialog(Frame parent, boolean modal, boolean isApplet) {
        super(parent, modal);
        initResources();
        initComponents(isApplet);
        loadPreferences();
        DialogDisplayer.pack(this);
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
    private void initComponents( final boolean isApplet ) {
        // initialize the window
        setContentPane(mainPanel);
        setTitle(resources.getString("window.title"));
        getRootPane().setDefaultButton(okButton);
        Utilities.centerOnScreen(this);
        Utilities.setEscKeyStrokeDisposes(this);

        // inactivity timeout text field
        inactivityTextField.setDocument(new MaxLengthDocument(2));

        // remember last login ID
        try {
            String sb = getPreferences().getProperty(SsmPreferences.SAVE_LAST_LOGIN_ID);
            boolean b = Boolean.valueOf(sb);

            rememberLastIdCheckBox.setSelected(b);
        } catch (IOException e) {
            log.log(Level.WARNING, "initComponents()", e);
        }

        rememberLastIdCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    getPreferences().
                        setProperty(SsmPreferences.SAVE_LAST_LOGIN_ID,
                            (Boolean.valueOf(((JCheckBox) e.getSource()).isSelected())).toString());
                } catch (IOException ex) {
                    // swallow
                }
            }
        });

        // new in 4.0 checkbox to turn on/off validation
        try {
            String sb = getPreferences().getProperty(SsmPreferences.ENABLE_POLICY_VALIDATION_ID);
            boolean b = Boolean.valueOf(sb);
            if (sb == null || sb.length() < 1) {
                b = true;
            }
            enableValidationCheckBox.setSelected(b);
        } catch (IOException e) {
            log.log(Level.WARNING, "initComponents()", e);
        }

        enableValidationCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    getPreferences().
                        setProperty(SsmPreferences.ENABLE_POLICY_VALIDATION_ID,
                            (Boolean.valueOf(((JCheckBox) e.getSource()).isSelected())).toString());
                } catch (IOException ex) {
                    // swallow
                }
            }
        });

        //new in 5.0 allow configurable SSG history in Logon dialog
        numHostsHistoryTextField.setDocument(new MaxLengthDocument(2));

        // New in 5.3.1 allow to configure the displaying length of assertion comments
        maxLeftCommentTextField.setDocument(new MaxLengthDocument(3));
        maxRightCommentTextField.setDocument(new MaxLengthDocument(4));

        // ok button
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (validatePreferences()) {
                    savePreferences();
                    dispose();
                }
            }
        });

        // cancel button
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        // help button
        helpButton.setMnemonic(resources.getString("helpButton.mnemonic").charAt(0));
        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Actions.invokeHelp(PreferencesDialog.this);
            }
        });

        // Set F1 funcation for the help button
        KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
        String actionName = "showHelpTopics";
        AbstractAction helpAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                helpButton.doClick();
            }
        };
        helpAction.putValue(Action.NAME, actionName);
        helpAction.putValue(Action.ACCELERATOR_KEY, accel);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(accel, actionName);
        getRootPane().getActionMap().put(actionName, helpAction);
        getLayeredPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(accel, actionName);
        getLayeredPane().getActionMap().put(actionName, helpAction);
        ((JComponent)getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(accel, actionName);
        ((JComponent)getContentPane()).getActionMap().put(actionName, helpAction);

        if ( isApplet ) {
            // hide settings that do not apply
            inactivityLabel.setVisible( false );
            inactivityTextField.setVisible( false );
            rememberLastIdLabel.setVisible( false );
            rememberLastIdCheckBox.setVisible( false );
            numHostsHistoryLabel.setVisible( false );
            numHostsHistoryTextField.setVisible( false );
        }
    }

    /**
     * validate preferences
     *
     * @return true if validated ok, false otherwise
     */
    private boolean validatePreferences() {
        try {
            int timeout;
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

            getPreferences().setProperty(SsmPreferences.INACTIVITY_TIMEOUT, Integer.toString(timeout));
            TopComponents.getInstance().setInactivitiyTimeout(timeout);

            int numHostsHistory;
            try {
                numHostsHistory = Integer.parseInt(numHostsHistoryTextField.getText());
            } catch (NumberFormatException e) {
                numHostsHistory = -1;
            }

            if (numHostsHistory < 0 || numHostsHistory > 50) {
                JOptionPane.showMessageDialog(null,
                  "The Server URL history value should be a number\n" +
                  "between 0 and 50",
                  "Bad Server URL history",
                  JOptionPane.ERROR_MESSAGE);
                return false;
            }

            getPreferences().setProperty(SsmPreferences.NUM_SSG_HOSTS_HISTORY, Integer.toString(numHostsHistory));            
            //if the user sets the value to be 0, we want to be sure that this is reflected immediately in the
            //ssg.properties file and also on the logon dialog if they logout
            //This is achieved by setting the max size on the History
            SsmPreferences preferences = TopComponents.getInstance().getPreferences();
            History serverUrlHistory = preferences.getHistory(SsmPreferences.SERVICE_URL);
            String sMaxSize = preferences.getString(SsmPreferences.NUM_SSG_HOSTS_HISTORY, "5");
            if(sMaxSize != null && !sMaxSize.equals("")){
                try {
                    serverUrlHistory.setMaxSize((new Integer(sMaxSize)));
                    preferences.store();
                } catch(NumberFormatException nfe){
                    //Swallow - incorrectly set property
                    //don't need to set, it's has an internal default value
                }
            }

            final int maxLeftComment;
            final int maxLhsSize = SsmPreferences.MAX_LEFT_COMMENT_SIZE;
            try {
                maxLeftComment = Integer.parseInt(maxLeftCommentTextField.getText());
                if(maxLeftComment < 0 || maxLeftComment > maxLhsSize) throw new NumberFormatException("Invalid value");
                if (maxLeftComment != previousMaxLeft){
                    commentSizeChanged = true;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null,
                  "The value for Maximum Left Comment must be a number\n" +
                  "between 0 and " + maxLhsSize,
                  "Bad Left Comment Maximum",
                  JOptionPane.ERROR_MESSAGE);

                return false;
            }

            getPreferences().setProperty(SsmPreferences.NUM_SSG_MAX_LEFT_COMMENT, Integer.toString(maxLeftComment));

            final int maxRightComment;
            final int maxRhsSize = SsmPreferences.MAX_RIGHT_COMMENT_SIZE;
            try {
                maxRightComment = Integer.parseInt(maxRightCommentTextField.getText());
                if(maxRightComment < 0 || maxRightComment > maxRhsSize) throw new NumberFormatException("Invalid value");
                if(maxRightComment != previousMaxRight){
                    commentSizeChanged = true;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null,
                  "The value for Maximum Right Comment must be a number\n" +
                  "between 0 and " + maxRhsSize,
                  "Bad Right Comment Maximum",
                  JOptionPane.ERROR_MESSAGE);

                return false;
            }

            getPreferences().setProperty(SsmPreferences.NUM_SSG_MAX_RIGHT_COMMENT, Integer.toString(maxRightComment));

            final float fontScale;
            try {
                fontScale = Float.parseFloat(fontScaleTextField.getText());

                // Must be a positive number
                if (fontScale <= 0) {
                    throw new NumberFormatException();
                }

                fontScaleChanged = prevFontScale != fontScale;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null,
                    "The value for Policy Manager UI Font Size Scale must be a positive float number.",
                    "Bad Policy Manager UI Font Size Scale",
                    JOptionPane.ERROR_MESSAGE
                );
                return false;
            }
            getPreferences().setProperty(SsmPreferences.POLICY_MANAGER_UI_FONT_SIZE_SCALE, Float.toString(fontScale));

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
            SsmPreferences prefs = TopComponents.getInstance().getPreferences();
            prefs.updateFromProperties(getPreferences(), true);
            prefs.store();
            prefs.updateSystemProperties();

            if(commentSizeChanged){
                //update any policies being displayed
                final JTree tree = TopComponents.getInstance().getPolicyTree();
                if (tree != null && TopComponents.getInstance().isPolicyOpenForEditing()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            final PolicyEditorPanel panel = TopComponents.getInstance().getPolicyEditorPanel();
                            panel.updateNodesWithComments();
                        }
                    });
                } else {
                    log.log(Level.WARNING, "Unable to reach the palette tree.");
                }
            }

            // If the value of the font size scale is changed, then set new font size in the UI.
            if (fontScaleChanged) {
                float fontSizeScale = Float.parseFloat(fontScaleTextField.getText());
                ((MainWindow) TopComponents.getInstance().getComponent("mainWindow")).setFontSizeAndUpdateUI(fontSizeScale);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error saving Preferences", e);
        }
    }

    /** load preferences into the form fields */
    private void loadPreferences() {
        try {
            String sTimeout = getPreferences().getProperty(SsmPreferences.INACTIVITY_TIMEOUT);
            int timeout = DEFAULT_INACTIVITY_TIMEOUT;
            try {
                timeout = Integer.parseInt(sTimeout);
            } catch (NumberFormatException e) {
                // swallow; bad property value
            }
            inactivityTextField.setText(Integer.toString(timeout));

            String sNumHostsHistory = getPreferences().getProperty(SsmPreferences.NUM_SSG_HOSTS_HISTORY);
            int numHosts = DEFAULT_NUM_HOSTS_HISTORY;
            try {
                numHosts = Integer.parseInt(sNumHostsHistory);
            } catch (NumberFormatException e) {
                // swallow; bad property value
            }
            numHostsHistoryTextField.setText(Integer.toString(numHosts));

            String sMaxLeftComment = getPreferences().getProperty(SsmPreferences.NUM_SSG_MAX_LEFT_COMMENT);
            int maxLeftComment = SsmPreferences.DEFAULT_MAX_LEFT_COMMENT;
            try {
                maxLeftComment = Integer.parseInt(sMaxLeftComment);
            } catch (NumberFormatException e) {
                // swallow; bad property value
            }
            maxLeftCommentTextField.setText(Integer.toString(maxLeftComment));
            previousMaxLeft = maxLeftComment;

            String sMaxRightComment = getPreferences().getProperty(SsmPreferences.NUM_SSG_MAX_RIGHT_COMMENT);
            int maxRightComment = SsmPreferences.DEFAULT_MAX_RIGHT_COMMENT;
            try {
                maxRightComment = Integer.parseInt(sMaxRightComment);
            } catch (NumberFormatException e) {
                // swallow; bad property value
            }
            maxRightCommentTextField.setText(Integer.toString(maxRightComment));
            previousMaxRight = maxRightComment;

            String fontScaleStr = getPreferences().getProperty(SsmPreferences.POLICY_MANAGER_UI_FONT_SIZE_SCALE);
            float fontScale = SsmPreferences.DEFAULT_POLICY_MANAGER_FONT_SIZE_SCALE;
            try {
                if (fontScaleStr != null) fontScale = Float.parseFloat(fontScaleStr);
            } catch (NumberFormatException e) {
                // swallow; bad property value
            }
            fontScaleTextField.setText(Float.toString(fontScale));
            prevFontScale = fontScale;
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
            props = TopComponents.getInstance().getPreferences().asProperties();
        }
        return props;
    }
}
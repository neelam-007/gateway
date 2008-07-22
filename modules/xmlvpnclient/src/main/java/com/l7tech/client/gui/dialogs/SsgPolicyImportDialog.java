package com.l7tech.client.gui.dialogs;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import com.l7tech.proxy.Constants;

/**
 * Policy import dialog.
 *
 * <p>Dialog that allows the import of an SSG policy from a file or from a
 * predefined list of internal policies.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class SsgPolicyImportDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SsgPolicyImportDialog.class.getName());

    //- PUBLIC

    /**
     * Factory method for creating a dialog with a parent obtained from the given component.
     *
     * @param parent The parent component.
     * @return A dialog
     */
    public static SsgPolicyImportDialog createSsgPolicyImportDialog(Component parent) {
        SsgPolicyImportDialog dialog;

        Window window = SwingUtilities.getWindowAncestor(parent);

        if (window instanceof Frame) {
            dialog = new SsgPolicyImportDialog((Frame)window);
        } else {
            dialog = new SsgPolicyImportDialog((Dialog)window);
        }

        return dialog;
    }

    /**
     * Create an import dialog with the given parent.
     *
     * @param parent Parent window, may be null
     */
    public SsgPolicyImportDialog(Dialog parent) {
        super(parent, "Import Policy", true);
        init();
    }

    /**
     * Create an import dialog with the given parent.
     *
     * @param parent Parent window, may be null
     */
    public SsgPolicyImportDialog(Frame parent) {
        super(parent, "", true);
        init();
    }

    /**
     * Check if the dialog wan cancelled.
     *
     * @return true if the dialog was cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Did the user select file import.
     *
     * @return true if import from file was selected.
     */
    public boolean isFileImportSelected() {
        return radioButtonImportFile.isSelected();
    }

    /**
     * Did the user select wsdl import.
     *
     * @return true if import from wsdl was selected.
     */
    public boolean isWsdlImportSelected() {
        return radioButtonImportWsdl.isSelected();
    }

    /**
     * Did the user select a policy to import.
     *
     * @return true if import of selected policy.
     */
    public boolean isSelectedPolicyImport() {
        return radioButtonImportSelected.isSelected();
    }

    /**
     * Get the policy input stream or null if not a selected policy.
     *
     * <p>You should close the returned stream.</p>
     *
     * @return the stream or null
     */
    public InputStream getPolicyInputStream() {
        try {
            return policyUrl==null ? null : policyUrl.openStream();
        }
        catch(IOException ioe) {
            return null;
        }
    }

    //- PRIVATE

    private String TEMPLATE_DIRECTORY = "com/l7tech/proxy/resources/bridgePolicyTemplates";
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JRadioButton radioButtonImportFile;
    private JRadioButton radioButtonImportWsdl;
    private JRadioButton radioButtonImportSelected;
    private JList listOfPolicies;
    private URL resourceRoot;
    private URL policyUrl;
    private boolean cancelled;

    /**
     * Initialize components
     */
    private void init() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        final ButtonGroup radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(radioButtonImportFile);
        radioButtonGroup.add(radioButtonImportWsdl);
        radioButtonGroup.add(radioButtonImportSelected);
        radioButtonImportFile.setSelected(true);

        DefaultListModel listModel = new DefaultListModel();
        String[] policyNames = getPolicyNames();
        for (int i = 0; i < policyNames.length; i++) {
            String policyName = policyNames[i];
            listModel.addElement(policyName);
        }
        listOfPolicies.setModel(listModel);
        listOfPolicies.setEnabled(false);
        if (policyNames.length > 0)
            listOfPolicies.setSelectedIndex(0);
        else
            radioButtonImportSelected.setEnabled(false);

        radioButtonImportSelected.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                listOfPolicies.setEnabled(radioButtonImportSelected.isSelected());
            }
        });

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                checkForBypass(radioButtonGroup);
            }
        });
    }

    /** Close the dialog immediatley if only one radio button is available. */
    private void checkForBypass(ButtonGroup radioButtonGroup) {
        Enumeration buttons = radioButtonGroup.getElements();
        JRadioButton lastAvail = null;
        int numAvail = 0;
        while (buttons.hasMoreElements()) {
            JRadioButton button = (JRadioButton)buttons.nextElement();
            if (button.isVisible() && button.isEnabled()) {
                numAvail++;
                lastAvail = button;
            }
        }
        if (numAvail == 1) {
            // Only one option -- may as well take it immediately
            lastAvail.setSelected(true);
            onOK();
        }
    }

    /**
     * OK button handler
     */
    private void onOK() {
        if (radioButtonImportSelected.isSelected()) {
            String selectedPolicyName = (String) listOfPolicies.getSelectedValue();
            if (selectedPolicyName == null) {
                selectedPolicyName = (String) listOfPolicies.getModel().getElementAt(0);
            }
            try {
                policyUrl = new URL(resourceRoot.toString() + "/" +selectedPolicyName.replace(' ', '_') + ".xml");
            }
            catch(IOException ioe) {
                cancelled = true;
            }
        }
        dispose();
    }

    /**
     * Cancel button handler
     */
    private void onCancel() {
        cancelled = true;
        dispose();
    }

    /**
     * Load policy files from JAR or classpath directory
     */
    private String[] getPolicyNames() {
        Set policyNames = new TreeSet();

        URL resourceUrl = getClass().getClassLoader().getResource(TEMPLATE_DIRECTORY);
        if (resourceUrl == null) {
            logger.warning("No template directory configured -- no "+ Constants.APP_NAME +" policy templates available");
            return new String[0];
        }
        if ("file".equals(resourceUrl.getProtocol())) {
            try {
                File templateDirectory = new File(resourceUrl.toURI());
                String[] fileNames = templateDirectory.list();
                if (fileNames != null) {
                    for (int i = 0; i < fileNames.length; i++) {
                        String fileName = fileNames[i];
                        if (fileName.endsWith(".xml")) {
                            policyNames.add(fileName.substring(0, fileName.length()-4).replace('_', ' '));
                        }
                    }
                }
            }
            catch(URISyntaxException use) {
                // no policies for you
            }
        }
        else if ("jar".equals(resourceUrl.getProtocol())) {
            try {
                URLConnection urlConnection = resourceUrl.openConnection();
                if (urlConnection instanceof JarURLConnection) {
                    JarURLConnection jarUrlConnection = (JarURLConnection) urlConnection;
                    JarFile jarFile = jarUrlConnection.getJarFile();
                    List entries = Collections.list(jarFile.entries());
                    for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
                        JarEntry jarEntry = (JarEntry) iterator.next();
                        if (jarEntry.getName().startsWith(TEMPLATE_DIRECTORY+"/") && jarEntry.getName().endsWith(".xml")) {
                            String policyName = jarEntry.getName().substring(TEMPLATE_DIRECTORY.length()+1);
                            policyNames.add(policyName.substring(0, policyName.length()-4).replace('_', ' '));
                        }
                    }
                }
            }
            catch(IOException ioe) {
            }
        }
        resourceRoot = resourceUrl;

        return (String[]) policyNames.toArray(new String[policyNames.size()]);
    }
}

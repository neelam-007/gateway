package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.AssertionResourceType;
import com.l7tech.policy.AssertionResourceInfo;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.logging.Logger;


/**
 * A dialog to view / configure the properties of a xslt assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 6, 2004<br/>
 * $Id$<br/>
 */
public class XslTransformationPropertiesDialog extends JDialog {
    private static final Logger log = Logger.getLogger(XslTransformationPropertiesDialog.class.getName());

    private JButton okButton;
    private JButton cancelButton;
    private JComboBox directionCombo;
    private JPanel mainPanel;
    private JSpinner whichMimePartSpinner;
    private JLabel directionLabel;
    private JLabel whichMimePartLabel;
    private JPanel borderPanel;
    private JPanel innerPanel;
    private JComboBox cbXslLocation;

    private final boolean readOnly;
    private XslTransformation assertion;
    private final XslTransformationSpecifyPanel specifyPanel;
    private final XslTransformationFetchPanel fetchPanel;
    private final XslTransformationSpecifyUrlPanel specifyUrlPanel;
    private boolean wasoked = false;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.XslTransformationPropertiesDialog");

    // Combo box strings that also serve as mode identifiers
    private final String MODE_SPECIFY_XSL = resources.getString("specifyRadio.label");
    private final String MODE_SPECIFY_URL = resources.getString("fetchUrlRadio.label");
    private final String MODE_FETCH_PI_URL = resources.getString("fetchRadio.label");
    private final String[] MODES = new String[] {
            MODE_SPECIFY_XSL,
            MODE_SPECIFY_URL,
            MODE_FETCH_PI_URL,
    };

    private final String BORDER_TITLE_PREFIX = resources.getString("xslLocationPrefix.text");

    ResourceBundle getResources() {
        return resources;
    }

    private final String DIRECTION_REQUEST = resources.getString("directionCombo.requestValue");
    private final String DIRECTION_RESPONSE = resources.getString("directionCombo.responseValue");
    private final String[] DIRECTIONS = new String[]{DIRECTION_REQUEST, DIRECTION_RESPONSE};

    public XslTransformationPropertiesDialog(Frame owner, boolean modal, boolean readOnly, XslTransformation assertion) {
        super(owner, resources.getString("window.title"), modal);
        if (assertion == null) {
            throw new IllegalArgumentException("Xslt Transformation == null");
        }
        this.readOnly = readOnly;
        this.assertion = assertion;

        Utilities.setEscKeyStrokeDisposes(this);

        directionLabel.setLabelFor(directionCombo);
        whichMimePartLabel.setLabelFor(whichMimePartSpinner);

        specifyPanel = new XslTransformationSpecifyPanel(this, assertion);
        fetchPanel = new XslTransformationFetchPanel(this, assertion);
        specifyUrlPanel = new XslTransformationSpecifyUrlPanel(this, assertion);

        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.add(specifyPanel);
        innerPanel.add(fetchPanel);
        innerPanel.add(specifyUrlPanel);

        cbXslLocation.setModel(new DefaultComboBoxModel(MODES));
        cbXslLocation.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                updateModeComponents();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[] {
            fetchPanel.getAddButton(), fetchPanel.getEditButton(),
            fetchPanel.getRemoveButton(), specifyPanel.getFileButton(),
            specifyPanel.getUrlButton()
        });

        // create controls
        directionCombo.setModel(new DefaultComboBoxModel(DIRECTIONS));
        if (this.assertion.getDirection() == XslTransformation.APPLY_TO_REQUEST) {
            directionCombo.setSelectedItem(DIRECTION_REQUEST);
        } else {
            directionCombo.setSelectedItem(DIRECTION_RESPONSE);
        }

        whichMimePartSpinner.setModel(new SpinnerNumberModel(0, 0, 9999, 1));
        //noinspection UnnecessaryBoxing
        whichMimePartSpinner.setValue(new Integer(assertion.getWhichMimePart()));

        Utilities.equalizeComponentSizes(new JComponent[] {
                directionCombo,
                whichMimePartSpinner,
        });

        AssertionResourceInfo ri = assertion.getResourceInfo();
        AssertionResourceType rit = ri.getType();
        if (AssertionResourceType.MESSAGE_URL.equals(rit)) {
            cbXslLocation.setSelectedItem(MODE_FETCH_PI_URL);
        } else if (AssertionResourceType.SINGLE_URL.equals(rit)) {
            cbXslLocation.setSelectedItem(MODE_SPECIFY_URL);
        } else {
            if (!AssertionResourceType.STATIC.equals(rit)) log.warning("Unknown AssertionResourceType, assuming static: " + rit);
            cbXslLocation.setSelectedItem(MODE_SPECIFY_XSL);
        }
        updateModeComponents();

        // create callbacks
        okButton.setEnabled( !readOnly );
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
/*
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(XslTransformationPropertiesDialog.this);
            }
        });
*/
        add(mainPanel);

        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xmlpad
    }

    private String getCurrentFetchMode() {
        return (String)cbXslLocation.getSelectedItem();
    }

    private void updateModeComponents() {
        String mode = getCurrentFetchMode();
        if (MODE_FETCH_PI_URL.equals(mode)) {
            fetchPanel.setVisible(true);
            specifyPanel.setVisible(false);
            specifyUrlPanel.setVisible(false);
        } else if (MODE_SPECIFY_URL.equals(mode)) {
            specifyUrlPanel.setVisible(true);
            specifyPanel.setVisible(false);
            fetchPanel.setVisible(false);
        } else {
            // Assume specify XSL
            if (!MODE_SPECIFY_XSL.equals(mode)) log.warning("Unexpected fetch mode, assuming specify: " + mode);
            specifyPanel.setVisible(true);
            fetchPanel.setVisible(false);
            specifyUrlPanel.setVisible(false);
        }
        Border border = borderPanel.getBorder();
        if (border instanceof TitledBorder) {
            TitledBorder tb = (TitledBorder)border;
            tb.setTitle(BORDER_TITLE_PREFIX + " " + mode);
        }
        innerPanel.revalidate();
    }

    private void ok() {
        // validate the contents of the xml control
        final String err;

        String mode = getCurrentFetchMode();
        if (MODE_FETCH_PI_URL.equals(mode)) {
            err = fetchPanel.check();
            if (err == null) fetchPanel.updateModel(assertion);
        } else if (MODE_SPECIFY_URL.equals(mode)) {
            err = specifyUrlPanel.check();
            if (err == null) specifyUrlPanel.updateModel(assertion);
        } else {
            // Assume specify XSL
            if (!MODE_SPECIFY_XSL.equals(mode)) log.warning("Unexpected fetch mode, assuming specify: " + mode);
            err = specifyPanel.check();
            if (err == null) specifyPanel.updateModel(assertion);
        }

        if (err != null) {
            displayError(err, null);
            return;
        }

        if (directionCombo.getSelectedItem() == DIRECTION_REQUEST) {
            log.finest("selected request direction");
            assertion.setDirection(XslTransformation.APPLY_TO_REQUEST);
        } else if (directionCombo.getSelectedItem() == DIRECTION_RESPONSE) {
            log.finest("selected response direction");
            assertion.setDirection(XslTransformation.APPLY_TO_RESPONSE);
        } else {
            throw new IllegalStateException("Neither request nor response was selected");
        }

        assertion.setWhichMimePart(((Number)whichMimePartSpinner.getValue()).intValue());

        wasoked = true;
        dispose();
    }

    private void cancel() {
        wasoked = false;
        dispose();
    }

    void displayError(String msg, String title) {
        if (title == null) title = resources.getString("error.window.title");
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        XslTransformationPropertiesDialog dlg = new XslTransformationPropertiesDialog(null, true, false, new XslTransformation());
        dlg.pack();
        dlg.setVisible(true);
        System.exit(0);
    }

    public boolean wasOKed() {
        return wasoked;
    }
}

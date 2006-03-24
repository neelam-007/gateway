package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.console.action.Actions;

import javax.swing.*;
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

    private JRadioButton specifyRadio;
    private JRadioButton fetchRadio;
    private JButton okButton;
    private JButton cancelButton;
    private JComboBox directionCombo;
    private JPanel mainPanel;
    private JSpinner whichMimePartSpinner;
    private JLabel directionLabel;
    private JLabel whichMimePartLabel;

    private XslTransformation assertion;
    private final XslTransformationSpecifyPanel specifyPanel;
    private final XslTransformationFetchPanel fetchPanel;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.XslTransformationPropertiesDialog");
    private final JPanel innerPanel = new JPanel();

    ResourceBundle getResources() {
        return resources;
    }

    private final String DIRECTION_REQUEST = resources.getString("directionCombo.requestValue");
    private final String DIRECTION_RESPONSE = resources.getString("directionCombo.responseValue");
    private final String[] DIRECTIONS = new String[]{DIRECTION_REQUEST, DIRECTION_RESPONSE};

    public XslTransformationPropertiesDialog(Frame owner, boolean modal, XslTransformation assertion) {
        super(owner, resources.getString("window.title"), modal);
        if (assertion == null) {
            throw new IllegalArgumentException("Xslt Transformation == null");
        }
        this.assertion = assertion;

        Actions.setEscKeyStrokeDisposes(this);

        directionLabel.setLabelFor(directionCombo);
        whichMimePartLabel.setLabelFor(whichMimePartSpinner);

        specifyPanel = new XslTransformationSpecifyPanel(this, assertion);
        fetchPanel = new XslTransformationFetchPanel(this, assertion);

        mainPanel.add(innerPanel, new GridConstraints(7, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
        innerPanel.setLayout(new GridBagLayout());
        innerPanel.add(specifyPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        innerPanel.add(fetchPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(fetchRadio);
        radioGroup.add(specifyRadio);

        fetchRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setFetchMode();
            }
        });

        specifyRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSpecifyMode();
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
        whichMimePartSpinner.setValue(new Integer(assertion.getWhichMimePart()));

        if (assertion.isFetchXsltFromMessageUrls()) {
            fetchRadio.setSelected(true);
            setFetchMode();
        } else {
            specifyRadio.setSelected(true);
            setSpecifyMode();
        }

        // create callbacks
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
    }

    private void setSpecifyMode() {
        fetchPanel.setVisible(false);
        specifyPanel.setVisible(true);
        innerPanel.revalidate();
    }

    private void setFetchMode() {
        specifyPanel.setVisible(false);
        fetchPanel.setVisible(true);
        innerPanel.revalidate();
    }

    private void ok() {
        // validate the contents of the xml control
        String err;
        if (specifyRadio.isSelected()) {
            err = specifyPanel.check();
            if (err == null) specifyPanel.updateModel(assertion);
        } else if (fetchRadio.isSelected()) {
            err = fetchPanel.check();
            if (err == null) fetchPanel.updateModel(assertion);
        } else throw new IllegalStateException("Neither Specify nor Fetch mode selected");

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

        // exit
        XslTransformationPropertiesDialog.this.dispose();
    }

    private void cancel() {
        assertion = null;
        XslTransformationPropertiesDialog.this.dispose();
    }

    void displayError(String msg, String title) {
        if (title == null) title = resources.getString("error.window.title");
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        XslTransformationPropertiesDialog dlg = new XslTransformationPropertiesDialog(null, true, new XslTransformation());
        dlg.pack();
        dlg.setVisible(true);
        System.exit(0);
    }

    public XslTransformation getAssertion() {
        return assertion;
    }
}

package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.Ssg;
import org.apache.log4j.Category;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for editing properties of an SSG object.
 * User: mike
 * Date: May 26, 2003
 * Time: 11:14:36 AM
 * To change this template use Options | File Templates.
 */
public class SsgPropertyDialog extends PropertyDialog {
    private final Category log = Category.getInstance(SsgPropertyDialog.class);

    private Ssg ssg; // The real Ssg instnace, to which changes may be committed.
    private int gridY = 0; // Used for layout
    private JTextField fieldName;
    private JTextField fieldLocalEndpoint;
    private JTextField fieldServerUrl;
    private JPanel generalPane;

    /** Create an SsgPropertyDialog ready to edit an Ssg instance. */
    private SsgPropertyDialog(final Ssg ssg) {
        super("SSG Properties");
        tabbedPane.add("General", getGeneralPane());
        setSsg(ssg);
        pack();
    }

    /**
     * Attempt to build an "edit properties" dialog box for the given Ssg.
     * @param ssg The ssg whose properties we intend to edit
     * @return The property dialog that will edit said properties.  Call show() on it to run it.
     */
    public static PropertyDialog getPropertyDialogForObject(final Ssg ssg) {
        return new SsgPropertyDialog(ssg);
    }

    /** Make a GridBagConstraints for a control, and move to next row. */
    private GridBagConstraints gbc() {
        return new GridBagConstraints(1, gridY++, 1, 1, 0.0, 0.0,
                                      GridBagConstraints.WEST,
                                      GridBagConstraints.HORIZONTAL,
                                      new Insets(0, 0, 0, 0), 0, 0);
    }

    /** Make a GridBagConstraints for a label. */
    private GridBagConstraints gbcLabel() {
        return new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                      GridBagConstraints.EAST,
                                      GridBagConstraints.HORIZONTAL,
                                      new Insets(0, 0, 0, 0), 0, 0);
    }

    /** Create panel controls.  Should be called only from a constructor. */
    private JPanel getGeneralPane() {
        if (generalPane == null) {
            generalPane = new JPanel(new GridBagLayout());

            fieldName = new JTextField();
            fieldName.setPreferredSize(new Dimension(200, 20));
            generalPane.add(new JLabel("Name:"), gbcLabel());
            generalPane.add(fieldName, gbc());

            fieldLocalEndpoint = new JTextField();
            fieldLocalEndpoint.setPreferredSize(new Dimension(200, 20));
            generalPane.add(new JLabel("Endpoint:"), gbcLabel());
            generalPane.add(fieldLocalEndpoint, gbc());

            fieldServerUrl = new JTextField();
            fieldServerUrl.setPreferredSize(new Dimension(250, 20));
            generalPane.add(new JLabel("SSG URL:"), gbcLabel());
            generalPane.add(fieldServerUrl, gbc());

            // Have a spacer eat any leftover space
            generalPane.add(new JPanel(),
                            new GridBagConstraints(0, gridY++, 1, 1, 1.0, 1.0,
                                                   GridBagConstraints.CENTER,
                                                   GridBagConstraints.BOTH,
                                                   new Insets(0, 0, 0, 0), 0, 0));
        }

        return generalPane;
    }

    /** Set the Ssg object being edited by this panel. */
    public void setSsg(final Ssg ssg) {
        this.ssg = ssg;

        fieldName.setText(ssg.getName());
        fieldLocalEndpoint.setText(ssg.getLocalEndpoint());
        fieldServerUrl.setText(ssg.getServerUrl());
    }

    /**
     * Called when the Ok button is pressed.
     * Should copy any updated properties into the target object and return normally.
     * Caller is responsible for hiding and disposing of the property dialog.
     */
    protected void commitChanges() {
        ssg.setName(fieldName.getText());
        ssg.setLocalEndpoint(fieldLocalEndpoint.getText());
        ssg.setServerUrl(fieldServerUrl.getText());
        setSsg(ssg);
    }
}

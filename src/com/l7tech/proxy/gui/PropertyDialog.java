package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.console.panels.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Superclass for property dialog boxes.
 * User: mike
 * Date: May 29, 2003
 * Time: 2:48:03 PM
 * To change this template use Options | File Templates.
 */
public abstract class PropertyDialog extends JDialog {
    private static final String DFG = "defaultForeground";
    protected static Dimension MIN_SIZE = new Dimension(200, 150);
    protected JTabbedPane tabbedPane;
    protected JButton okButton;
    protected JButton cancelButton;
    protected boolean userClickedOk = false;

    /**
     * Creates a new PropertyDialog.
     * Sets up our tabbed pane, but doesn't put anything into it.
     * @param title
     * */
    protected PropertyDialog(final String title) {
        super(Gui.getInstance().getFrame(), title, true);

        // Enforce minimum size
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                Dimension size = PropertyDialog.this.getSize();
                if (size.width < MIN_SIZE.width && size.height < MIN_SIZE.height)
                    PropertyDialog.this.setSize(MIN_SIZE);
                else if (size.width < MIN_SIZE.width)
                    PropertyDialog.this.setSize(new Dimension(MIN_SIZE.width, size.height));
                else if (size.height < MIN_SIZE.height)
                    PropertyDialog.this.setSize(new Dimension(size.width, MIN_SIZE.height));
            }
        });

        getContentPane().setLayout(new GridBagLayout());

        tabbedPane = new JTabbedPane(JTabbedPane.NORTH, JTabbedPane.WRAP_TAB_LAYOUT);
        tabbedPane.setPreferredSize(new Dimension(400, 450));
        getContentPane().add(tabbedPane,
                             new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                                                    GridBagConstraints.CENTER,
                                                    GridBagConstraints.BOTH,
                                                    new Insets(5, 5, 0, 5),
                                                    4, 4));

        final JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new GridBagLayout());
        buttonPane.add(new JPanel(),
                       new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                                              GridBagConstraints.EAST,
                                              GridBagConstraints.HORIZONTAL,
                                              new Insets(0, 0, 0, 0),
                                              0, 0));

        buttonPane.add(getOkButton(),
                       new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                              GridBagConstraints.EAST,
                                              GridBagConstraints.NONE,
                                              new Insets(5, 0, 0, 0),
                                              0, 0));

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                userClickedOk = false;
                PropertyDialog.this.hide();
                PropertyDialog.this.dispose();
            }
        });
        buttonPane.add(cancelButton,
                       new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                                              GridBagConstraints.EAST,
                                              GridBagConstraints.NONE,
                                              new Insets(5, 5, 0, 0),
                                              0, 0));

        Utilities.equalizeButtonSizes(new AbstractButton[] { okButton, cancelButton });

        getContentPane().add(buttonPane,
                             new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                    GridBagConstraints.EAST,
                                                    GridBagConstraints.NONE,
                                                    new Insets(0, 5, 5, 5),
                                                    0, 0));
    }

    /** Get the OK button. */
    protected JButton getOkButton() {
        if (okButton == null) {
            okButton = new JButton("Ok");
            okButton.putClientProperty(DFG, okButton.getForeground());
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    commitChanges();
                    userClickedOk = true;
                    PropertyDialog.this.hide();
                    PropertyDialog.this.dispose();
                }
            });
        }
        return okButton;
    }

    /** Grey out the Ok button. */
    protected void disableOk() {
        getOkButton().setForeground(Color.GRAY);
        getOkButton().setEnabled(false);
    }

    /** Enable the Ok button. */
    protected void enableOk() {
        getOkButton().setForeground((Color)getOkButton().getClientProperty(DFG));
        getOkButton().setEnabled(true);
    }

    /**
     * Show the dialog modally and return the result.
     * @return True iff. the user closed the dialog by clicking the "Ok" button.
     */
    public boolean runDialog() {
        Utilities.centerOnScreen(this);
        show();
        return userClickedOk;
    }

    /**
     * Attempt to build an "edit properties" dialog box for the given object.
     * @param obj The object whose properties we intend to edit
     * @return The property dialog that will edit said properties.  Call show() on it to run it.
     * @throws ClassNotFoundException if no proerty dialog could be found for the given object.
     */
    public static PropertyDialog getPropertyDialogForObject(final Object obj) throws ClassNotFoundException {
        if (obj.getClass().equals(Ssg.class))
            return SsgPropertyDialog.getPropertyDialogForObject((Ssg)obj);
        throw new ClassNotFoundException("No property dialog for " + obj.getClass());
    }

    /**
     * Called when the Ok button is pressed.
     * Should copy any updated properties into the target object and return normally.
     * Caller is responsible for hiding and disposing of the property dialog.
     */
    protected abstract void commitChanges();
}

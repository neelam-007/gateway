package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.Ssg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Superclass for property dialog boxes.
 * User: mike
 * Date: May 29, 2003
 * Time: 2:48:03 PM
 * To change this template use Options | File Templates.
 */
public abstract class PropertyDialog extends JDialog {
    protected JTabbedPane tabbedPane;
    protected JButton okButton;
    protected JButton cancelButton;

    /**
     * Creates a new PropertyDialog.
     * Sets up our tabbed pane, but doesn't put anything into it.
     * @param title
     * */
    protected PropertyDialog(String title) {
        super(Gui.getInstance().getFrame(), title, true);
        getContentPane().setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane(JTabbedPane.NORTH, JTabbedPane.WRAP_TAB_LAYOUT);
        tabbedPane.setPreferredSize(new Dimension(400, 450));
        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new GridBagLayout());
        buttonPane.add(new JPanel(),
                       new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                                              GridBagConstraints.EAST,
                                              GridBagConstraints.HORIZONTAL,
                                              new Insets(0, 0, 0, 0),
                                              0, 0));

        okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                commitChanges();
                PropertyDialog.this.hide();
                PropertyDialog.this.dispose();
            }
        });
        buttonPane.add(okButton,
                       new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                              GridBagConstraints.EAST,
                                              GridBagConstraints.NONE,
                                              new Insets(0, 0, 0, 0),
                                              0, 0));

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PropertyDialog.this.hide();
                PropertyDialog.this.dispose();
            }
        });
        buttonPane.add(cancelButton,
                       new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                                              GridBagConstraints.EAST,
                                              GridBagConstraints.NONE,
                                              new Insets(0, 0, 0, 0),
                                              0, 0));

        getContentPane().add(buttonPane, BorderLayout.SOUTH);
    }

    /**
     * Attempt to build an "edit properties" dialog box for the given object.
     * @param obj The object whose properties we intend to edit
     * @return The property dialog that will edit said properties.  Call show() on it to run it.
     * @throws ClassNotFoundException if no proerty dialog could be found for the given object.
     */
    public static PropertyDialog getPropertyDialogForObject(Object obj) throws ClassNotFoundException {
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

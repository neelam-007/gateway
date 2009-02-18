package com.l7tech.client.gui.dialogs;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.client.gui.Gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Superclass for property dialog boxes.
 *
 * User: mike
 * Date: May 29, 2003
 * Time: 2:48:03 PM
 */
public abstract class PropertyDialog extends JDialog {
    protected static final Dimension MIN_SIZE = new Dimension(200, 150);
    private static final Dimension PREFERRED_SIZE = new Dimension(530, 560);
    protected JTabbedPane tabbedPane;
    protected JButton okButton;
    protected JButton cancelButton;
    protected boolean userClickedOk = false;
    protected final InputValidator validator = new InputValidator(this, getTitle());

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
        tabbedPane.setPreferredSize(PREFERRED_SIZE);
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

        buttonPane.add(getCancelButton(),
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
        getRootPane().setDefaultButton(getOkButton());
        Utilities.runActionOnEscapeKey(getRootPane(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) { doCancel(); }
        });
    }

    private void doCancel() {
        userClickedOk = false;
        PropertyDialog.this.setVisible(false);
        PropertyDialog.this.dispose();
    }

    private JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    doCancel();
                }
            });
        }
        return cancelButton;
    }

    /** Get the OK button.  To update the OK button's enable/disable state, call validator.validate(). */
    private JButton getOkButton() {
        if (okButton == null) {
            okButton = new JButton("OK");
            Utilities.enableGrayOnDisabled(okButton);
            validator.attachToButton(okButton, new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    commitChanges();
                    userClickedOk = true;
                    PropertyDialog.this.setVisible(false);
                    PropertyDialog.this.dispose();
                }
            });
        }
        return okButton;
    }

    /**
     * Show the dialog modally and return the result.
     * @return True iff. the user closed the dialog by clicking the "OK" button.
     */
    public boolean runDialog() {
        Utilities.centerOnScreen(this);
        setVisible(true);
        return userClickedOk;
    }

    /**
     * Called when the OK button is pressed.
     * Should copy any updated properties into the target object and return normally.
     * Caller is responsible for hiding and disposing of the property dialog.
     */
    protected abstract void commitChanges();
}

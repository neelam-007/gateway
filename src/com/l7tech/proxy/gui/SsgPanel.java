package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.Ssg;
import org.apache.log4j.Category;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;

/**
 * Panel for editing properties of an SSG object.
 * User: mike
 * Date: May 26, 2003
 * Time: 11:14:36 AM
 * To change this template use Options | File Templates.
 */
public class SsgPanel extends JPanel {
    private final Category log = Category.getInstance(SsgPanel.class);

    private Ssg ssg; // The real Ssg instnace, to which changes may be committed.
    private int gridY = 0; // Used for layout
    private JTextField fieldName;
    private JTextField fieldLocalEndpoint;
    private boolean dirty = false;
    private JButton applyButton;
    private JButton cancelButton;
    private JPanel buttonPanel;

    /** Make a GridBagConstraints for a control, and move to next row. */
    private GridBagConstraints gbc() {
        return new GridBagConstraints(1, gridY++, 1, 1, 1.0, 1.0,
                                      GridBagConstraints.WEST,
                                      GridBagConstraints.HORIZONTAL,
                                      new Insets(0, 0, 0, 0), 0, 0);
    }

    /** Make a GridBagConstraints for a label. */
    private GridBagConstraints gbcLabel() {
        return new GridBagConstraints(0, gridY, 1, 1, 1.0, 1.0,
                                      GridBagConstraints.EAST,
                                      GridBagConstraints.HORIZONTAL,
                                      new Insets(0, 0, 0, 0), 0, 0);
    }

    /** Create an SsgPanel ready to edit a new Ssg instance. */
    public SsgPanel() {
        init();
        setSsg(new Ssg());
    }

    /** Utility that watches for changes to editor widgets and calls setDirty(). */
    private class DirtyDocumentListener implements DocumentListener {
        private JTextField field;
        private Method accessorMethod;

        public DirtyDocumentListener(JTextField field, String accessorName) {
            this.field = field;
            try {
                this.accessorMethod = Ssg.class.getMethod(accessorName, null);
            } catch (Exception e) {
                log.error(e);
            }
        }

        private Object invokeAccessor() {
            try {
                return accessorMethod.invoke(ssg, null);
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }

        public void insertUpdate(DocumentEvent e) {
            if (!dirty && !field.getText().equals(invokeAccessor()))
                setDirty(true);
        }

        public void removeUpdate(DocumentEvent e) {
            if (!dirty && !field.getText().equals(invokeAccessor()))
                setDirty(true);
        }

        public void changedUpdate(DocumentEvent e) {
            if (!dirty && !field.getText().equals(invokeAccessor()))
                setDirty(true);
        }
    }

    /** Create panel controls.  Should be called only from a constructor. */
    private void init() {
        setLayout(new GridBagLayout());
        JPanel editPanel = new JPanel();
        add(editPanel,
            new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                                   GridBagConstraints.NORTH,
                                   GridBagConstraints.HORIZONTAL,
                                   new Insets(5, 5, 5, 5), 0, 0));
        add(makeButtonPanel(),
            new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                                   GridBagConstraints.SOUTHEAST,
                                   GridBagConstraints.HORIZONTAL,
                                   new Insets(0, 5, 5, 5), 0, 0));

        editPanel.setLayout(new GridBagLayout());

        fieldName = new JTextField();
        fieldName.setPreferredSize(new Dimension(200, 20));
        fieldName.getDocument().addDocumentListener(new DirtyDocumentListener(fieldName, "getName"));
        editPanel.add(new JLabel("Name:"), gbcLabel());
        editPanel.add(fieldName, gbc());

        fieldLocalEndpoint = new JTextField();
        fieldLocalEndpoint.setPreferredSize(new Dimension(200, 20));
        fieldLocalEndpoint.getDocument().addDocumentListener(new DirtyDocumentListener(fieldLocalEndpoint,
                                                                                       "getLocalEndpoint"));
        editPanel.add(new JLabel("Endpoint:"), gbcLabel());
        editPanel.add(fieldLocalEndpoint, gbc());
    }

    /** Create the button panel. */
    private JPanel makeButtonPanel() {
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        applyButton = new JButton("Apply");
        cancelButton = new JButton("Cancel");
        applyButton.putClientProperty("defaultforeground", applyButton.getForeground());
        cancelButton.putClientProperty("defaultforeground", applyButton.getForeground());

        applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                copyViewToModel();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSsg(ssg);
            }
        });

        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);

        return buttonPanel;
    }

    /** Set fields in ssg according to content of our GUI widgets. */
    private void copyViewToModel() {
        ssg.setName(fieldName.getText());
        ssg.setLocalEndpoint(fieldLocalEndpoint.getText());
        setSsg(ssg);
    }

    /** Create an SsgPanel ready to edit the given Ssg instance. */
    public SsgPanel(Ssg ssg) {
        init();
        setSsg(ssg);
    }

    /** Set the Ssg object being edited by this panel. */
    public void setSsg(Ssg ssg) {
        this.ssg = ssg;

        fieldName.setText(ssg.getName());
        fieldLocalEndpoint.setText(ssg.getLocalEndpoint());

        setDirty(false);
    }

    private void setDirty(boolean dirty) {
        this.dirty = dirty;
        applyButton.setEnabled(dirty);
        cancelButton.setEnabled(dirty);
        applyButton.setForeground(dirty ? (Color)applyButton.getClientProperty("defaultforeground") : Color.GRAY);
        cancelButton.setForeground(dirty ? (Color)cancelButton.getClientProperty("defaultforeground") : Color.GRAY);
    }
}

package com.l7tech.gui.widgets;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Utility class to display a ValidatedPanel with an Ok and Cancel button.
 *
 * Clicking on 'Ok' causes a call to {@link com.l7tech.gui.widgets.ValidatedPanel#doUpdateModel()} which if throws
 * will cause a warning dialog with the throwable's message to be displayed to the user and the dialog will remain visible,
 * otherwise the dialog will dismiss.
 *
 * Additionally after such an exception {@link #wasOKed()} will return false, until doUpdateModel() successfully returns.
 *
 * @param <V> the payload value type - the model for the Validated Panel.
 */
public class OkCancelDialog<V> extends JDialog {
    private V value;
    private boolean readOnly;
    private boolean wasoked = false;

    private JButton cancelButton;
    private JButton okButton;
    private JPanel innerPanel;
    private JPanel mainPanel;

    private final ValidatedPanel validatedPanel;

    public static <T> OkCancelDialog<T> createOKCancelDialog(Component owner, String title, boolean modal, ValidatedPanel<T> panel) {
        Window window = SwingUtilities.getWindowAncestor(owner);
        return new OkCancelDialog<T>(window, title, modal, panel);
    }

    public OkCancelDialog(Window owner, String title, boolean modal, ValidatedPanel<V> panel) {
        this( owner, title, modal, panel, false );
    }

    public OkCancelDialog(Window owner, String title, boolean modal, ValidatedPanel<V> panel, boolean readOnly) {
        super(owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS );
        this.validatedPanel = panel;
        this.readOnly = readOnly;
        initialize();
    }

    private void initialize() {
        Utilities.setEscAction(this, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        validatedPanel.addPropertyChangeListener("ok", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                okButton.setEnabled(!readOnly && evt.getNewValue() == Boolean.TRUE);
            }
        });

        validatedPanel.addPropertyChangeListener(validatedPanel.getPropertyName(), new PropertyChangeListener() {
            @SuppressWarnings({ "unchecked" })
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                value = (V) evt.getNewValue();
            }
        });

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    wasoked = true;
                    validatedPanel.updateModel();
                } catch (Throwable t) {
                    wasoked = false;
                    DialogDisplayer.showMessageDialog(OkCancelDialog.this, ExceptionUtils.getMessage(t), "Validation Warning", JOptionPane.WARNING_MESSAGE, null);
                    return;
                }

                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        okButton.setDefaultCapable(true);
        getRootPane().setDefaultButton(okButton);
        innerPanel.add(validatedPanel, BorderLayout.CENTER);

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        getContentPane().setLayout( new BorderLayout() );
        getContentPane().add( mainPanel, BorderLayout.CENTER );

        validatedPanel.checkSyntax();
        okButton.setEnabled(validatedPanel.isSyntaxOk() && !readOnly);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                validatedPanel.focusFirstComponent();
            }
        });
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        okButton.setEnabled(validatedPanel.isSyntaxOk() && !readOnly);
    }

    private void cancel() {
        value = null;
        dispose();
    }

    public V getValue() {
        return value;
    }

    public boolean wasOKed() {
        return wasoked;
    }
}

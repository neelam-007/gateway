package com.l7tech.console.panels;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Extends AssertionPropertiesEditorSupport to add an Ok and Cancel button and to manage the assertion
 * bean lifecycle.
 * <p/>
 * Subclasses must call {@link #initComponents()} before the dialog is displayed.
 */
public abstract class AssertionPropertiesOkCancelSupport<AT extends Assertion> extends AssertionPropertiesEditorSupport<AT> {
    private JButton okButton;
    private JButton cancelButton;
    private final Class<? extends AT> beanClass;
    private boolean confirmed = false;

    protected AssertionPropertiesOkCancelSupport(Class<? extends AT> beanClass, Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        this.beanClass = beanClass;
    }

    protected AssertionPropertiesOkCancelSupport(Class<? extends AT> beanClass) {
        this.beanClass = beanClass;
    }

    @Override
    protected void configureView() {
        updateOkButtonEnableState();
    }

    protected void updateOkButtonEnableState() {
        getOkButton().setEnabled( !isReadOnly() );
    }

    /**
     * Get the OK button.
     * <p/>
     * This method returns the existing Ok button, if any, or else calls {@link #createOkButton()} to create one
     * and then remembers the result for future calls.
     *
     * @return the Ok button.  Never null.
     */
    protected JButton getOkButton() {
        if (okButton == null)
            okButton = createOkButton();
        return okButton;
    }

    /**
     * Get the Cancel button.
     * <p/>
     * This method returns the existing Cancel button, if any, or else calls {@link #createCancelButton()} to create one
     * and then remembers the result for future calls.
     *
     * @return the Cancel button.  Never null.
     */
    protected JButton getCancelButton() {
        if (cancelButton == null)
            cancelButton = createCancelButton();
        return cancelButton;
    }

    /**
     * Create the cancel button for this dialog.
     * <p/>
     * This method creates a new JButton with the text "Cancel" and attaches the action listener
     * returned by {@link #createCancelAction()}.
     *
     * @return the cancel button.  Never null.
     */
    protected JButton createCancelButton() {
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(createCancelAction());
        return cancelButton;
    }

    /**
     * Create the action to attach to the cancel button created by {@link #createCancelButton}.
     * <p/>
     * This method creates an ActionListner that disposes this dialog.
     *
     * @return an ActionListener.  Never null.
     */
    protected ActionListener createCancelAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AssertionPropertiesOkCancelSupport.this.dispose();
            }
        };
    }

    /**
     * Create the action to attach to the Ok button created by {@link #createOkButton}.
     * <p/>
     * This method creates an ActionListener that calls {@link #getData} on a new instance of
     * beanClass, and displays an error dialog if it throws BadViewValueException.  Otherwise
     * it sets confirmed to true and disposes this dialog.
     *
     * @return the Ok action.  Never null.
     */
    protected ActionListener createOkAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    getData(createBean());
                } catch (ValidationException ve) {
                    showValidationErrorMessage(ve);
                    setConfirmed(false);
                    return;
                }
                setConfirmed(true);
                AssertionPropertiesOkCancelSupport.this.dispose();
            }
        };
    }

    /**
     * Show a dialog explaining the specified validation exception.
     * <p/>
     * This method displays a JOptionPane showing the message from the exception, if any, otherwise
     * a generic error message.
     *
     * @param ve a ValidationException, or null.
     */
    protected void showValidationErrorMessage(ValidationException ve) {
        String msg = ve == null ? "invalid data" : ExceptionUtils.getMessage(ve);
        DialogDisplayer.showMessageDialog(this, "Error", "Unable to save: " + msg, null);
    }

    /**
     * Create a new unfilled bean instance.
     * <P/>
     * This method just calls newInstance() on the result of {@link #getBeanClass}.
     *
     * @return a new bean instance.  Never null.
     */
    protected AT createBean() {
        try {
            return getBeanClass().newInstance();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the assertion bean class being edited.
     *
     * @return the assertion bean class.  Never null.
     */
    protected Class<? extends AT> getBeanClass() {
        return beanClass;
    }

    /**
     * Create the OK button.
     * <p/>
     * This method creates a new JButton with the text "Ok" and attaches the action listener
     * returned by {@link #createOkAction}.
     *
     * @return An Ok button.  Never null.
     */
    protected JButton createOkButton() {
        JButton okButton = new JButton("Ok");
        okButton.addActionListener(createOkAction());
        return okButton;
    }

    protected void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    /**
     * Copy the data out of the view into an assertion bean instance.
     * The provided bean should be filled and returned, if possible, but implementors may create and return
     * a new bean instead, if they must.
     *
     * @param assertion a bean to which the data from the view can be copied, if possible.  Must not be null.
     * @return a possibly-new assertion bean populated with data from the view.  Not necessarily the same bean that was passed in.
     *         Never null.
     * @throws ValidationException if the data cannot be collected because of a validation error.
     */
    public abstract AT getData(AT assertion) throws ValidationException;

    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Create the bottom panel that contains the Ok and Cancel buttons.
     * <p/>
     * This class just creates a panel with a horizontal BoxLayout and
     * populates it by calling {@link #createOkButton} and {@link #createCancelButton()}.
     *
     * @return a new panel.  Never null.
     */
    protected JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        final JButton okButton = getOkButton();
        final JButton cancelButton = getCancelButton();
        Utilities.equalizeButtonSizes(new JButton[] {okButton, cancelButton});
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(okButton);
        buttonPanel.add(Box.createHorizontalStrut(8));
        buttonPanel.add(cancelButton);
        return buttonPanel;
    }

    /**
     * Create the main content pane of this dialog.
     * <p/>
     * This method creates a panel with the main area filled with the return value from {@link #createPropertyPanel}
     * and a bar along the bottom filled with the return value from {@link #createButtonPanel}.
     *
     * @return the main content pane to use for this dialog.  Never null.
     */
    protected JPanel createContentPane() {
        JPanel main = new JPanel(new GridBagLayout());
        JPanel buttonPanel = createButtonPanel();
        JPanel propPanel = createPropertyPanel();

        GridBagConstraints maingc = new GridBagConstraints();
        maingc.fill = GridBagConstraints.BOTH;
        maingc.anchor = GridBagConstraints.NORTH;
        maingc.gridx = 0;
        maingc.gridy = 0;
        maingc.weightx = 100.0;
        maingc.weighty = 100.0;
        maingc.insets = new Insets(12, 2, 12, 2);
        main.add(propPanel, maingc);
        maingc.gridy = 1;
        maingc.insets = new Insets(2, 18, 8, 8);
        maingc.fill = GridBagConstraints.HORIZONTAL;
        maingc.anchor = GridBagConstraints.SOUTH;
        maingc.weightx = 0.0;
        maingc.weighty = 0.0;
        main.add(buttonPanel, maingc);
        return main;
    }

    /**
     * Create a panel to edit the properties of the assertion bean.  This panel does not include any
     * Ok or Cancel buttons.
     *
     * @return a panel that can be used to edit the assertion properties.  Never null.
     */
    protected abstract JPanel createPropertyPanel();

    /**
     * Prepare this dialog for display by creating the main content pane and wiring up the default button
     * and escape key action.
     * <p/>
     * This method sets the content pane to the return value from {@link #createContentPane}, sets the
     * default button to {@link #okButton}, configures the dialog to respond to the ESC key by disposing itself,
     * and packs the dialog.
     */
    protected void initComponents() {
        setContentPane(createContentPane());
        getRootPane().setDefaultButton(getOkButton());
        Utilities.setEscKeyStrokeDisposes(this);
        pack();
    }

    /**
     * Exception used to report invalid data.
     */
    public static class ValidationException extends RuntimeException {
        public ValidationException() {
        }

        public ValidationException(String message) {
            super(message);
        }

        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }

        public ValidationException(Throwable cause) {
            super(cause);
        }
    }
}

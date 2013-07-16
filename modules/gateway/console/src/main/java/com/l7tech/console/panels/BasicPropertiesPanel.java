package com.l7tech.console.panels;

import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.InputValidator;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * A panel which contains basic input fields that are commonly used and a dynamic validation message.
 */
public class BasicPropertiesPanel extends JPanel {
    private static final ImageIcon VALID = ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/DigitalSignatureStateValid16.png");
    private static final ImageIcon INVALID = ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/Warning16.png");
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private JPanel contentPanel;
    private JTextField nameField;
    private JTextArea descriptionTextArea;
    private JLabel validationLabel;
    private InputValidator inputValidator;

    public JTextField getNameField() {
        return nameField;
    }

    public String getNameText() {
        return nameField.getText().trim();
    }

    public JTextArea getDescriptionTextArea() {
        return descriptionTextArea;
    }

    public String getDescriptionText() {
        return descriptionTextArea.getText().trim();
    }

    public JLabel getValidationLabel() {
        return validationLabel;
    }

    /**
     * Configure validation behaviour for this panel.
     *
     * @param parent                the parent Component for error dialogs.
     * @param title                 the title for error dialogs.
     * @param okButton              the Button which should be attached to validation.
     * @param successActionListener ActionListener to call after the okButton is clicked and validation is successful.
     * @param maxNameChars          optional maximum number of characters to allow for the name.
     * @param maxDescChars          optional maximum number of characters to allow for the description.
     * @return the configured InputValidator which is attached to the fields of the BasicPropertiesPanel.
     */
    public InputValidator configureValidation(@NotNull final Component parent,
                                              @NotNull final String title,
                                              @NotNull final JButton okButton,
                                              @NotNull final ActionListener successActionListener,
                                              @Nullable final Integer maxNameChars,
                                              @Nullable final Integer maxDescChars) {
        inputValidator = new InputValidator(parent, title) {
            @Override
            public boolean isValid() {
                // dynamic error message - shows first error if there are errors
                final boolean valid = super.isValid();
                if (!valid) {
                    final String[] errors = getAllValidationErrors();
                    validationLabel.setText(errors.length > 0 ? errors[0] : StringUtils.EMPTY);
                    validationLabel.setIcon(INVALID);
                } else {
                    validationLabel.setText(StringUtils.EMPTY);
                    validationLabel.setIcon(VALID);
                }
                return valid;
            }
        };
        inputValidator.disableButtonWhenInvalid(okButton);
        inputValidator.constrainTextFieldToBeNonEmpty(NAME, nameField, null);
        if (maxNameChars != null) {
            inputValidator.constrainTextFieldToMaxChars(NAME, nameField, maxNameChars, null);
        }
        if (maxDescChars != null) {
            inputValidator.constrainTextFieldToMaxChars(DESCRIPTION, descriptionTextArea, maxDescChars, null);
        }
        inputValidator.attachToButton(okButton, successActionListener);
        return inputValidator;
    }
}

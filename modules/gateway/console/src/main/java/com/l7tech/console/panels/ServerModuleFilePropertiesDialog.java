package com.l7tech.console.panels;

import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.DocumentSizeFilter;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.WrappingLabel;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Represents {@link ServerModuleFile} properties dialog.
 */
public class ServerModuleFilePropertiesDialog extends JDialog {
    private static final long serialVersionUID = 3134981092231010304L;
    private static final ResourceBundle resources = ResourceBundle.getBundle(ServerModuleFilePropertiesDialog.class.getName());

    private static final String SPLIT_ASSERTIONS_REGEX = "\\s*,\\s*";
    private static final int NAME_FIELD_MAX_LENGTH = 128;

    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JList<String> contentsList;
    private JLabel sizeLabel;
    private JLabel filenameLabel;
    private JTextField moduleNameText;
    private JLabel typeLabel;
    private JTextField sha256TextField;
    private JScrollPane stateScrollPane;
    private WrappingLabel stateLabel;

    private boolean confirmed = false;

    public ServerModuleFilePropertiesDialog(final Window owner, final ServerModuleFile module, final String state, final boolean readOnly) {
        super(owner, resources.getString("dialog.title"), JDialog.DEFAULT_MODALITY_TYPE);

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        final InputValidator inputValidator = new InputValidator(this, resources.getString("dialog.title"));
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                ServerModuleFilePropertiesDialog.this.confirmed = true;
                dispose();
            }
        });
        inputValidator.disableButtonWhenInvalid(okButton);

        // Name field must not be empty and must not be longer than 128 characters
        ((AbstractDocument)moduleNameText.getDocument()).setDocumentFilter(new DocumentSizeFilter(NAME_FIELD_MAX_LENGTH));
        inputValidator.constrainTextFieldToBeNonEmpty(
                resources.getString("field.name"),
                moduleNameText,
                new InputValidator.ComponentValidationRule(moduleNameText) {
                    @Override
                    public String getValidationError() {
                        final int nameLength = getModuleName().length();
                        if (nameLength == 0) {
                            return resources.getString("error.validation.blank.name");
                        } else if (nameLength > NAME_FIELD_MAX_LENGTH) {
                            return MessageFormat.format(resources.getString("error.validation.name.max.length"), NAME_FIELD_MAX_LENGTH);
                        }
                        return null;
                    }
                }
        );
        inputValidator.validateWhenDocumentChanges(moduleNameText);
        inputValidator.isValid();

        Utilities.attachDefaultContextMenu(sha256TextField);
        Utilities.setEscAction(this, cancelButton);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(final WindowEvent e) {
                if (!readOnly) {
                    moduleNameText.selectAll();
                    moduleNameText.requestFocus();
                }
            }
        });

        initStateComponents();

        setData(module, state);

        if (readOnly) {
            moduleNameText.setEnabled(false);
            okButton.setEnabled(false);
        }
    }

    /**
     * Initialize State component using {@code WrappingLabel}.
     */
    final void initStateComponents() {
        stateScrollPane.getHorizontalScrollBar().setUnitIncrement(8);
        stateScrollPane.getVerticalScrollBar().setUnitIncrement(10);

        stateLabel.setContextMenuAutoSelectAll(false);
        stateLabel.setContextMenuEnabled(true);
    }

    /**
     * Set the GUI elements associated with the specified {@link ServerModuleFile module} properties.
     */
    private void setData( final ServerModuleFile module, final String state ) {

        moduleNameText.setText(module != null ? module.getName() : StringUtils.EMPTY);
        sha256TextField.setText(module != null ? module.getModuleSha256() : StringUtils.EMPTY);
        filenameLabel.setText(module != null ? module.getProperty(ServerModuleFile.PROP_FILE_NAME) : StringUtils.EMPTY);

        stateLabel.setText(module != null && StringUtils.isNotBlank(state) ? state : resources.getString("text.state.empty"));
        stateLabel.setCaretPosition(0);

        final ModuleType moduleType = module != null ? module.getModuleType() : null;
        typeLabel.setText(moduleType != null ? moduleType.toString() : StringUtils.EMPTY);

        sizeLabel.setText(module != null ? module.getHumanReadableFileSize() : StringUtils.EMPTY);

        final String assertions = module != null ? module.getProperty(ServerModuleFile.PROP_ASSERTIONS) : null;
        if (assertions != null) {
            final String[] listData = assertions.split(SPLIT_ASSERTIONS_REGEX);
            //noinspection serial
            contentsList.setModel(
                    new AbstractListModel<String>() {
                        @Override
                        public int getSize() { return listData.length; }
                        @Override
                        public String getElementAt(int i) { return listData[i]; }
                    }
            );
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Returns trimmed text of the module name TextField.  Never {@code null}.
     */
    @NotNull
    public String getModuleName() {
        final String text = moduleNameText.getText();
        return text != null ? text.trim() : StringUtils.EMPTY;
    }

    /**
     * Convenient method without specifying the exception.
     *
     * @see #showError(String, Throwable)
     */
    @SuppressWarnings("UnusedDeclaration")
    private void showError(@NotNull String message) {
        showError(message, null);
    }

    /**
     * Utility method for displaying an error that happen.
     *
     * @param message     The error message to display.
     * @param e           The exception that occurred.
     */
    private void showError(@NotNull String message, @Nullable final Throwable e) {
        if (e != null) {
            message = message + ": " + ExceptionUtils.getMessage(e);
        }
        DialogDisplayer.showMessageDialog(this, message, resources.getString("error.title"), JOptionPane.ERROR_MESSAGE, null);
    }
}

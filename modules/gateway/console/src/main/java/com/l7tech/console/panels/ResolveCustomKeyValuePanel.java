package com.l7tech.console.panels;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.ext.store.KeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreException;
import com.l7tech.policy.exporter.CustomKeyValueReference;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This panel allows an administrator to take the appropriate action when a policy
 * being imported contains custom assertion external references.
 */
public class ResolveCustomKeyValuePanel extends WizardStepPanel<Object> {
    private static final Logger logger = Logger.getLogger(ResolveCustomKeyValuePanel.class.getName());
    protected static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.ResolveCustomKeyValuePanel");
    protected static final String EMPTY_STRING = resources.getString("empty.string");

    protected JButton createCustomKeyValueEntityButton;
    protected JRadioButton changeRadioButton;
    protected JRadioButton removeRadioButton;
    protected JRadioButton ignoreRadioButton;
    // available entities for this external reference
    protected JComboBox<Object> customKeyValueComboBox; // items custom-key-value id's (prefix+id)
    protected JPanel mainPanel;
    protected JTextField entityIdTextField;
    protected JTextField entityPrefixTextField;
    protected JLabel titleLabel;
    protected JPanel externalReferencePanel;

    @NotNull
    protected final CustomKeyValueReference externalReference;
    @NotNull
    protected String externalEntityTypeName;

    /**
     * Construct the panel object.
     *
     * @param next                 {@code null} for now.
     * @param externalReference    object containing the custom external reference
     */
    public ResolveCustomKeyValuePanel(
            @Nullable final WizardStepPanel<Object> next,
            @NotNull final CustomKeyValueReference externalReference
    ) throws IOException {
        super(next);
        this.externalReference = externalReference;
        this.externalEntityTypeName = safeString(resources.getString("generic.entity"));

        // some sanity check
        if (externalReference.getEntityKey() == null || externalReference.getEntityKey().trim().isEmpty()) {
            throw new IOException("Missing Entity Key cannot be null or empty");
        }

        if (externalReference.getEntityKeyPrefix() == null || externalReference.getEntityKeyPrefix().trim().isEmpty()) {
            throw new IOException("Missing Entity KeyPrefix cannot be null or empty");
        }

        if (externalReference.getEntityBase64Value() == null || externalReference.getEntityBase64Value().trim().isEmpty()) {
            throw new IOException("Missing Entity Base64Value cannot be null or empty");
        }
    }

    /**
     * Initialize the panel elements.
     */
    public void initialize() throws IOException {
        setLayout(new BorderLayout());
        add(mainPanel);

        // set text to components
        titleLabel.setText(
                MessageFormat.format(
                        resources.getString("title.missing"),
                        externalEntityTypeName
                )
        );
        changeRadioButton.setText(
                MessageFormat.format(
                        resources.getString("action.change"),
                        externalEntityTypeName
                )
        );
        removeRadioButton.setText(
                MessageFormat.format(
                        resources.getString("action.remove"),
                        externalEntityTypeName
                )
        );
        createCustomKeyValueEntityButton.setText(
                MessageFormat.format(
                        resources.getString("button.create"),
                        externalEntityTypeName
                )
        );
        entityIdTextField.setText(safeString(extractEntityNameFromKey(externalReference.getEntityKey(), externalReference.getEntityKeyPrefix())));
        entityPrefixTextField.setText(safeString(externalReference.getEntityKeyPrefix()));

        // default is delete
        removeRadioButton.setSelected(true);
        customKeyValueComboBox.setEnabled(false);

        // enable/disable provider selector as per action type selected
        changeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                customKeyValueComboBox.setEnabled(true);
            }
        });
        removeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                customKeyValueComboBox.setEnabled(false);
            }
        });
        ignoreRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                customKeyValueComboBox.setEnabled(false);
            }
        });

        // set the title accordingly
        final Border border = externalReferencePanel.getBorder();
        if (border instanceof TitledBorder) {
            ((TitledBorder)border).setTitle(
                    MessageFormat.format(
                            resources.getString("title.details"),
                            externalEntityTypeName
                    )
            );
        }

        // create custom combo-box renderer
        customKeyValueComboBox.setRenderer(
                new TextListCellRenderer<>(
                        new Functions.Unary<String, Object>() {
                            @Override
                            public String call(@NotNull final Object obj) {
                                if (!(obj instanceof String)) {
                                    throw new IllegalArgumentException("ComboBox item must be of type String");
                                }
                                return safeString(extractEntityNameFromKey((String) obj, externalReference.getEntityKeyPrefix()));
                            }
                        }
                )
        );

        // if no create editor is provided then use generic approach
        // add action for creating external references button
        createCustomKeyValueEntityButton.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        doCreateCustomKeyValue();
                    }
                }
        );

        // populate external reference entity combo
        notifyActive();
    }

    /**
     * Generic logic for creating the missing CustomKeyValue entity.
     */
    protected void doCreateCustomKeyValue() {
        final String customKeyValueIdToAdd;
        try {
            final KeyValueStore keyValueStore = externalReference.getKeyValueStore();
            customKeyValueIdToAdd = externalReference.getEntityKey();
            if (keyValueStore.contains(customKeyValueIdToAdd)) {
                throw new KeyValueStoreException(
                        MessageFormat.format(
                                resources.getString("errors.saveFailed.exists"),
                                externalEntityTypeName,
                                safeString(extractEntityNameFromKey(customKeyValueIdToAdd, externalReference.getEntityKeyPrefix()))
                        )
                );
            }
            keyValueStore.save(customKeyValueIdToAdd, HexUtils.decodeBase64(externalReference.getEntityBase64Value()));
        } catch (final Exception e) {
            showErrorMessage(
                    resources.getString("errors.saveFailed.title"),
                    MessageFormat.format(
                            resources.getString("errors.saveFailed.message"),
                            externalEntityTypeName
                    ) + "\n" + ExceptionUtils.getMessage(e),
                    e
            );
            return;
        }

        // inform that the process was successful
        DialogDisplayer.showMessageDialog(
                this,
                MessageFormat.format(
                        resources.getString("save.success.message"),
                        externalEntityTypeName,
                        safeString(extractEntityNameFromKey(customKeyValueIdToAdd, externalReference.getEntityKeyPrefix()))
                ),
                resources.getString("save.success.title"),
                JOptionPane.INFORMATION_MESSAGE,
                null
        );

        // populate external reference entity combo
        notifyActive();

        // select newly added entity
        selectNewlyAddedEntity(customKeyValueIdToAdd);
    }

    @Override
    public String getDescription() {
       return getStepLabel();
    }

    @Override
    public boolean canFinish() {
        return !hasNextPanel();
    }

    @Override
    public String getStepLabel() {
        return MessageFormat.format(
                resources.getString("stepLabel.unresolved.stored.password"),
                externalEntityTypeName,
                safeString(
                        extractEntityNameFromKey(
                                externalReference.getEntityKey(),
                                externalReference.getEntityKeyPrefix()
                        )
                )
        );
    }

    @Override
    public void notifyActive() {
        populateExternalReferenceEntitySelectorComboBox();
        enableAndDisableComponents();
    }

    @Override
    public boolean onNextButton() {
        if (changeRadioButton.isSelected()) {
            if (customKeyValueComboBox.getSelectedIndex() < 0) return false;
            externalReference.setLocalizeReplace((String)customKeyValueComboBox.getSelectedItem());
        } else if (removeRadioButton.isSelected()) {
            externalReference.setLocalizeDelete();
        } else if (ignoreRadioButton.isSelected()) {
            externalReference.setLocalizeIgnore();
        }
        return true;
    }

    /**
     * Utility function for selecting the newly added entity with key {@code newEntityKey}
     */
    protected void selectNewlyAddedEntity(@NotNull final String newEntityKey) {
        // loop through combo model-items
        final ComboBoxModel<Object> model = customKeyValueComboBox.getModel();
        for (int i = 0; i < model.getSize(); ++i) {
            // extract the entity descriptor from element-id
            final String entityKey = (String)model.getElementAt(i);
            if (newEntityKey.equals(entityKey)) {
                model.setSelectedItem(entityKey);
                changeRadioButton.setEnabled(true);
                changeRadioButton.setSelected(true);
                customKeyValueComboBox.setEnabled(true);
                break;
            }
        }
    }

    /**
     * Utility function for populating the available entities for this external reference
     */
    protected void populateExternalReferenceEntitySelectorComboBox() {
        final DefaultComboBoxModel<Object> agentComboBoxModel = new DefaultComboBoxModel<>();
        final KeyValueStore keyValueStore = externalReference.getKeyValueStore();
        final Map<String, byte[]> keyValuePairs = keyValueStore.findAllWithKeyPrefix(externalReference.getEntityKeyPrefix());
        for (final String key: keyValuePairs.keySet()) {
            agentComboBoxModel.addElement(key);
        }
        customKeyValueComboBox.setModel(agentComboBoxModel);
    }

    /**
     * Enable or Disable change combo-box, depending whether or not there are available entities for this external reference.
     */
    private void enableAndDisableComponents() {
        final boolean enableSelection = customKeyValueComboBox.getModel().getSize() > 0;
        changeRadioButton.setEnabled( enableSelection );
        if ( !changeRadioButton.isEnabled() && changeRadioButton.isSelected() ) {
            removeRadioButton.setSelected( true );
        }
    }

    /**
     * Utility function for extracting entity name from the specified <tt>key</tt>, using the <tt>refKeyPrefix</tt>.
     *
     * @param key             the entity key in format "prefix.name"
     * @param refKeyPrefix    the entity prefix.
     * @return a {@code String} containing the entity name, or {@code null} if the key doesn't contain the specified prefix.
     */
    @Nullable
    protected static String extractEntityNameFromKey(
            @NotNull final String key,
            @NotNull String refKeyPrefix
    ) {
        return (key.length() >= refKeyPrefix.length()) ? key.substring(refKeyPrefix.length()) : null;
    }

    /**
     * Utility function for displaying a error message dialog with a specified {@code title} and {@code msg}
     * and log the exception that was thrown.
     *
     * @param title    the error dialog title
     * @param msg      the message
     * @param e        the exception that was thrown
     */
    private void showErrorMessage(final String title, final String msg, final Throwable e) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, null);
    }

    /**
     * Utility function for providing predefined value ({@link #EMPTY_STRING}) for empty {@code String}'s.
     *
     * @param value    the specified <code>String</code> object.
     * @return passed <code>String</code> <tt>value</tt> if non-null, {@link #EMPTY_STRING} otherwise.
     */
    @NotNull
    protected static String safeString(@Nullable final String value) {
        return value == null ? EMPTY_STRING : value;
    }
}

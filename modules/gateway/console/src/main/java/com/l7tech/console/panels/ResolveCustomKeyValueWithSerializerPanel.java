package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.ext.entity.CustomEntityDescriptor;
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;
import com.l7tech.policy.assertion.ext.entity.panels.CustomEntityCreateUiPanel;
import com.l7tech.policy.assertion.ext.store.KeyValueStore;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This panel is used when the Custom Assertion developer decides to modify the missing entity before creating it.
 */
public class ResolveCustomKeyValueWithSerializerPanel extends ResolveCustomKeyValuePanel {
    private static final long serialVersionUID = 2466226866548531086L;
    private static final Logger logger = Logger.getLogger(ResolveCustomKeyValueWithSerializerPanel.class.getName());

    /**
     * CustomKeyValueStore entity serializer
     */
    @NotNull
    private final CustomEntitySerializer entitySerializer;

    /**
     * CustomKeyValueStore entity
     */
    @NotNull
    private final CustomEntityDescriptor externalEntity;

    /**
     * Maps entity name with key
     */
    private Map<
            String, // entity name
            String  // entity key
            > entityNameToKeyMap = new HashMap<>();

    /**
     * Construct the panel object.
     *
     * @param next              {@code null} for now.
     * @param externalReference object containing the custom external reference
     */
    public ResolveCustomKeyValueWithSerializerPanel(
            @Nullable WizardStepPanel<Object> next,
            @NotNull CustomKeyValueReference externalReference
    ) throws IOException {
        super(next, externalReference);

        final CustomEntitySerializer entitySerializer = getExternalEntitySerializer(externalReference.getEntitySerializer());
        if (entitySerializer == null) {
            // also log as warning since the exception below is going to be swallowed by the import/export wizard
            // therefore the user will not see the reason why import failed
            logger.warning("Failed to instantiate exported entity serializer with class-name: \"" + externalReference.getEntitySerializer() + "\"");
            throw new IOException("Failed to instantiate exported entity serializer with class-name: \"" + externalReference.getEntitySerializer() + "\"");
        }
        this.entitySerializer = entitySerializer;

        final Object entityObj = entitySerializer.deserialize(HexUtils.decodeBase64(externalReference.getEntityBase64Value()));
        if (!(entityObj instanceof CustomEntityDescriptor)) {
            // also log as warning since the exception below is going to be swallowed by the import/export wizard
            // therefore the user will not see the reason why import failed
            logger.warning("Failed to resolve entity bytes into " + CustomEntityDescriptor.class.getName());
            throw new IOException("Failed to resolve entity bytes into " + CustomEntityDescriptor.class.getName());
        }
        this.externalEntity = (CustomEntityDescriptor)entityObj;

        this.externalEntityTypeName =  safeString(externalEntity.getProperty(CustomEntityDescriptor.TYPE, String.class));

        // initialize the panel
        this.initializeWithSerializer();
    }

    /**
     * Override and do nothing.<br/>
     * This method will be called from the base class constructor and this panel initialization cannot start until this
     * class constructor is complete, to be more precise, not until {@link #externalEntity} and {@link #externalEntityTypeName}
     * fields are properly initialized.<br/>
     * Therefore {@link #initializeWithSerializer()} method is introduced and will be called accordingly,
     * from this class constructor.
     */
    @Override
    protected void initialize() throws IOException {
        // do nothing here
    }

    /**
     * Initialize the panel elements.
     */
    private void initializeWithSerializer() throws IOException {
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

        // setup details panel
        final JPanel entityDetailsPanel = externalEntity.getUiObject(CustomEntityDescriptor.MISSING_DETAIL_UI_OBJECT, JPanel.class);
        if (entityDetailsPanel != null) {
            externalReferencePanel.removeAll();
            externalReferencePanel.setLayout(new BorderLayout());
            externalReferencePanel.add(entityDetailsPanel, BorderLayout.PAGE_START);
        } else {
            // if no entityDetailsPanel provided create a default key-id, key-prefix and name layout
            createDefaultExternalReferenceSummeryPanel();

            // add entity name
            //noinspection serial
            externalReferencePanel.add(
                    new JLabel(safeString(resources.getString("label.name"))),
                    new GridBagConstraints() {{
                        gridx = 0;
                        gridy = 2;
                        anchor = FIRST_LINE_START;
                        fill = HORIZONTAL;
                        insets = new Insets(0, 1, 2, 0);
                    }}
            );
            //noinspection serial
            externalReferencePanel.add(
                    new JTextField(safeString(externalEntity.getProperty(CustomEntityDescriptor.NAME, String.class))) {{
                        setEditable(false);
                    }},
                    new GridBagConstraints() {{
                        gridx = 1;
                        gridy = 2;
                        weightx = 1.0;
                        fill = HORIZONTAL;
                        insets = new Insets(0, 2, 2, 1);
                    }}
            );
        }

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

        // set the missing panel title accordingly
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
                                if (!(obj instanceof CustomEntityDescriptor)) {
                                    throw new IllegalArgumentException("ComboBox item must be of type CustomEntityDescriptor");
                                }
                                return safeString(((CustomEntityDescriptor)obj).getProperty(CustomEntityDescriptor.SUMMARY, String.class));
                            }
                        }
                )
        );

        // if the custom assertion supports CREATE_UI_OBJECT, then
        // attach the panel to our CreateMissingCustomKeyValueEntityDialog
        final CustomEntityCreateUiPanel editorCreatePanel = externalEntity.getUiObject(CustomEntityDescriptor.CREATE_UI_OBJECT, CustomEntityCreateUiPanel.class);
        //noinspection StatementWithEmptyBody
        if (editorCreatePanel == null) {
            // default to the generic functionality
            createCustomKeyValueEntityButton.addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(@NotNull final ActionEvent actionEvent) {
                            doCreateCustomKeyValue();
                        }
                    }
            );
        } else {
            // create editor is provided so use specific approach
            // add action for creating external references button
            createCustomKeyValueEntityButton.addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(@NotNull final ActionEvent actionEvent) {
                            doCreateSpecificCustomKeyValue(editorCreatePanel);
                        }
                    }
            );
        }
    }

    @Override
    public String getStepLabel() {
        return MessageFormat.format(
                resources.getString("stepLabel.unresolved.stored.password"),
                externalEntityTypeName,
                safeString(
                        externalEntity.getProperty(
                                CustomEntityDescriptor.NAME,
                                String.class
                        )
                )
        );
    }

    @Override
    public boolean onNextButton() {
        if (changeRadioButton.isSelected()) {
            if (customKeyValueComboBox.getSelectedIndex() < 0) return false;
            final CustomEntityDescriptor entity = (CustomEntityDescriptor)customKeyValueComboBox.getSelectedItem();
            final String replaceEntityKey = entityNameToKeyMap.get(
                    entity.getProperty(
                            CustomEntityDescriptor.NAME,
                            String.class
                    )
            );
            if (replaceEntityKey == null || replaceEntityKey.trim().isEmpty()) {
                // also log as warning since the exception below is going to be swallowed by the import/export wizard
                // therefore the user will not see the reason why import failed
                logger.warning("Failed to get entity key from name: \"" + replaceEntityKey + "\"");
                throw new RuntimeException("Failed to get entity key from name: \"" + replaceEntityKey + "\"");
            }
            externalReference.setLocalizeReplace(replaceEntityKey);
        } else if (removeRadioButton.isSelected()) {
            externalReference.setLocalizeDelete();
        } else if (ignoreRadioButton.isSelected()) {
            externalReference.setLocalizeIgnore();
        }
        return true;
    }

    @Override
    protected void populateExternalReferenceEntitySelectorComboBox() {
        entityNameToKeyMap.clear();

        final CustomEntityDescriptor currentSelectedItem = (customKeyValueComboBox.getSelectedIndex() < 0) ? null : (CustomEntityDescriptor)customKeyValueComboBox.getSelectedItem();
        final DefaultComboBoxModel<Object> agentComboBoxModel = new DefaultComboBoxModel<>();
        final KeyValueStore keyValueStore = externalReference.getKeyValueStore();
        final Map<String, byte[]> keyValuePairs = keyValueStore.findAllWithKeyPrefix(externalReference.getEntityKeyPrefix());

        for (final Map.Entry<String, byte[]> entry : keyValuePairs.entrySet()) {
            final Object entityObj = entitySerializer.deserialize(entry.getValue());
            if (entityObj instanceof CustomEntityDescriptor) { // ignore invalid entity objects
                entityNameToKeyMap.put(
                        ((CustomEntityDescriptor) entityObj).getProperty(
                                CustomEntityDescriptor.NAME,
                                String.class
                        ),
                        entry.getKey()
                );
                agentComboBoxModel.addElement(entityObj);
            } else {
                logger.log(
                        Level.WARNING,
                        "Failed to resolve entity bytes into \"" + CustomEntityDescriptor.class.getName() +
                                "\" for key \"" + entry.getKey() + "\". Ignoring this entry!"
                );
            }
        }

        customKeyValueComboBox.setModel(agentComboBoxModel);
        if (currentSelectedItem != null) {
            final String selectedEntityName = currentSelectedItem.getProperty(CustomEntityDescriptor.NAME, String.class);
            if (selectedEntityName != null) {
                for (int i = 0; i < agentComboBoxModel.getSize(); ++i) {
                    // extract the entity descriptor from element-id
                    final CustomEntityDescriptor entity = (CustomEntityDescriptor)agentComboBoxModel.getElementAt(i);
                    if (selectedEntityName.equals(entity.getProperty(CustomEntityDescriptor.NAME, String.class))) {
                        agentComboBoxModel.setSelectedItem(entity);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Specific logic for creating the missing CustomKeyValue entity.
     * @param editorCreatePanel    the custom assertion specific editor panel.
     */
    private void doCreateSpecificCustomKeyValue(@NotNull final CustomEntityCreateUiPanel editorCreatePanel) {
        final CreateMissingCustomKeyValueEntityDialog dlg = new CreateMissingCustomKeyValueEntityDialog(
                TopComponents.getInstance().getTopParent(),
                editorCreatePanel,
                externalEntity
        );

        DialogDisplayer.pack(dlg);
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(
                dlg,
                new Runnable() {
                    @Override
                    public void run() {
                        if (dlg.isConfirmed()) {
                            final Runnable reedit = new Runnable() {
                                public void run() {
                                    doCreateSpecificCustomKeyValue(editorCreatePanel);
                                }
                            };

                            final CustomEntityDescriptor newEntity = dlg.getNewEntity();
                            if (newEntity == null) {
                                return; // user choose to skip
                            }

                            final String customKeyValueIdToAdd;
                            try {
                                final String newlyCreatedEntityName = newEntity.getProperty(CustomEntityDescriptor.NAME, String.class);
                                // first check whether the newly entity name is valid i.e. non-empty
                                if (newlyCreatedEntityName == null || newlyCreatedEntityName.isEmpty()) {
                                    throw new Exception(resources.getString("errors.saveFailed.emptyEntityName"));
                                }

                                final KeyValueStore keyValueStore = externalReference.getKeyValueStore();

                                // check if the newly entered entity name is unique.
                                // just in case other process somehow add the same entity name, re-enumerate the
                                // key-value-store names for this prefix.
                                final Map<String, byte[]> existingEntityNames = keyValueStore.findAllWithKeyPrefix(externalReference.getEntityKeyPrefix());
                                final java.util.List<String> existingConnNames = new ArrayList<>();
                                for (final Map.Entry<String, byte[]> entry: existingEntityNames.entrySet()) {
                                    final Object entityObj = entitySerializer.deserialize(entry.getValue());
                                    if (entityObj instanceof CustomEntityDescriptor) { // ignore invalid entity objects
                                        existingConnNames.add(((CustomEntityDescriptor) entityObj).getProperty(CustomEntityDescriptor.NAME, String.class));
                                    } else {
                                        logger.log(
                                                Level.WARNING,
                                                "Failed to resolve entity bytes into \"" + CustomEntityDescriptor.class.getName() +
                                                        "\" for key \"" + entry.getKey() + "\". Ignoring this entry!"
                                        );
                                    }
                                }
                                // check if the newly entered entity name is unique.
                                if (existingConnNames.contains(newlyCreatedEntityName)) {
                                    throw new Exception(
                                            MessageFormat.format(
                                                    resources.getString("errors.saveFailed.entity.name.exists"),
                                                    externalEntityTypeName,
                                                    newlyCreatedEntityName
                                            )
                                    );
                                }

                                // finally add our entity into key-value-store using the missing key
                                customKeyValueIdToAdd = externalReference.getEntityKey();
                                if (keyValueStore.contains(customKeyValueIdToAdd)) {
                                    // for some reason our missing entity key exists in the system.
                                    // most probable cause is that some other process added our key while this import wizard was running.
                                    // we cannot continue with the creation, so let the user know and return
                                    DialogDisplayer.showMessageDialog(
                                            ResolveCustomKeyValueWithSerializerPanel.this,
                                            MessageFormat.format(
                                                    resources.getString("errors.saveFailed.exists.cannot.continue"),
                                                    externalEntityTypeName,
                                                    safeString(extractEntityNameFromKey(customKeyValueIdToAdd, externalReference.getEntityKeyPrefix()))
                                            ),
                                            resources.getString("errors.saveFailed.title"),
                                            JOptionPane.ERROR_MESSAGE,
                                            null
                                    );
                                    return;
                                }
                                keyValueStore.save(customKeyValueIdToAdd, entitySerializer.serialize(newEntity));
                            } catch (final Exception ex) {
                                // if an error happen log it, let the user know and rerun the creation dialog
                                showErrorMessage(
                                        resources.getString("errors.saveFailed.title"),
                                        MessageFormat.format(
                                                resources.getString("errors.saveFailed.message"),
                                                externalEntityTypeName
                                        ) + " " + ExceptionUtils.getMessage(ex),
                                        ex,
                                        reedit
                                );
                                return;
                            }

                            // populate external reference entity combo
                            notifyActive();

                            // select the newly added entity
                            selectNewlyAddedEntity(customKeyValueIdToAdd);
                        }
                    }
                }
        );
    }

    /**
     * Utility function for selecting the newly added entity with key {@code newEntityKey}
     */
    @Override
    protected void selectNewlyAddedEntity(@NotNull final String newEntityKey) {
        String newEntityName = null;
        for (final Map.Entry<String, String> entity : entityNameToKeyMap.entrySet()) {
            if (newEntityKey.equals(entity.getValue())) {
                newEntityName = entity.getKey();
                break;
            }
        }
        if (newEntityName == null) {
            throw new RuntimeException("Failed to locate entity with key: \"" + newEntityKey + "\"");
        }

        final ComboBoxModel<Object> model = customKeyValueComboBox.getModel();
        for (int i = 0; i < model.getSize(); ++i) {
            // extract the entity descriptor from element-id
            final CustomEntityDescriptor entity = (CustomEntityDescriptor)model.getElementAt(i);
            if (newEntityName.equals(entity.getProperty(CustomEntityDescriptor.NAME, String.class))) {
                model.setSelectedItem(entity);
                changeRadioButton.setEnabled(true);
                changeRadioButton.setSelected(true);
                customKeyValueComboBox.setEnabled(true);
                break;
            }
        }
    }

    /**
     * Utility function for displaying a error message dialog with a specified {@code title} and {@code msg},
     * log the exception that was thrown, and optionally invoke the specified <tt>continuation</tt> callback.
     *
     * @param title           the error dialog title
     * @param msg             the message
     * @param e               the exception that was thrown
     * @param continuation    callback to invoke when dialog is dismissed.  optional, so it can be null.
     */
    private void showErrorMessage(final String title, final String msg, final Throwable e, final Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }

    /**
     * Utility method for getting {@link CustomEntitySerializer} object from it's class name.
     *
     * @param extEntitySerializerClassName    the Serializer class name.
     * @return {@link CustomEntitySerializer} object if the class name is registered, {@code null} otherwise.
     */
    private static CustomEntitySerializer getExternalEntitySerializer(final String extEntitySerializerClassName) {
        final Registry registry = Registry.getDefault();
        if (!registry.isAdminContextPresent()) {
            logger.warning("Cannot get Policy Exporter and Importer Admin due to no Admin Context present.");
            return null;
        } else {
            return registry.getCustomAssertionsRegistrar().getExternalEntitySerializer(extEntitySerializerClassName);
        }
    }
}

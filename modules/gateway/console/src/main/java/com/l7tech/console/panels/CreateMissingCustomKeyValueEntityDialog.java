package com.l7tech.console.panels;

import com.l7tech.policy.assertion.ext.entity.CustomEntityDescriptor;
import com.l7tech.policy.assertion.ext.entity.panels.CustomEntityCreateUiPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static com.l7tech.console.api.CustomConsoleContext.*;

/**
 * Represents a dialog object responsible for creating the missing custom-key-value-store entity.
 * <p/>
 * This is a simple dialog with Ok and Cancel buttons, passing the action to the entity properties panel.
 * The properties panel must extend {@link CustomEntityCreateUiPanel}.
 *
 * @see CustomEntityCreateUiPanel
 */
public class CreateMissingCustomKeyValueEntityDialog extends JDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.CreateMissingCustomKeyValueEntityDialog");

    private JButton okButton;
    private JButton cancelButton;
    private JPanel propertiesPanelHolder;
    private JPanel mainPanel;

    /**
     * custom-key-value-store entity properties panel.
     */
    @NotNull
    private final CustomEntityCreateUiPanel propertiesPanel;

    /**
     * Final entity object when the user clicked Ok button.
     */
    @Nullable
    private CustomEntityDescriptor newEntity = null;

    /**
     * A flag indicating whether the entity wrapped with this dialog has been validated, when the user clicked the Ok button.
     */
    private boolean confirmed = false;

    /**
     * Construct the dialog using the custom-key-value-store entity properties panel.
     *
     * @param owner              owner window.
     * @param propertiesPanel    custom-key-value-store entity properties panel.
     * @param entity             custom-key-value-store entity.
     */
    public CreateMissingCustomKeyValueEntityDialog(
            @NotNull final Window owner,
            @NotNull final CustomEntityCreateUiPanel propertiesPanel,
            @NotNull final CustomEntityDescriptor entity
    ) {
        super(
                owner,
                MessageFormat.format(
                        resources.getString("title"),
                        ResolveCustomKeyValuePanel.safeString(
                                entity.getProperty(CustomEntityDescriptor.TYPE, String.class)
                        )
                )
        );
        setModal(true);

        this.propertiesPanel = propertiesPanel;

        final Map<String, Object> consoleContext = new HashMap<>(4);
        addCustomExtensionInterfaceFinder(consoleContext);
        addCommonUIServices(consoleContext, null, null);
        addKeyValueStoreServices(consoleContext);
        addVariableServices(consoleContext, null, null);
        this.propertiesPanel.setConsoleContextUsed(consoleContext);
        this.propertiesPanel.initialize(entity);

        this.initialize();
    }

    /**
     * Getter for {@link #confirmed}
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Setter for {@link #confirmed}
     */
    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    /**
     * Initialize GUI components
     */
    private void initialize() {
        propertiesPanelHolder.setLayout(new BorderLayout());
        propertiesPanelHolder.add(propertiesPanel, BorderLayout.CENTER);
        propertiesPanelHolder.setBorder(BorderFactory.createEmptyBorder());

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        mainPanel.registerKeyboardAction(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        onCancel();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        setContentPane(mainPanel);
        getRootPane().setDefaultButton(okButton);

        pack();
    }

    /**
     * Ok button action listener
     */
    private void onOk() {
        newEntity = propertiesPanel.validateEntity();
        if (null != newEntity) {
            confirmed = true;
            dispose();
        }
    }

    /**
     * Cancel button action listener
     */
    private void onCancel() {
        newEntity = null;
        dispose();
    }

    /**
     * Getter for {@link #newEntity}
     * @return the final entity object or null if the user clicked cancel.
     */
    @Nullable
    public CustomEntityDescriptor getNewEntity() {
        return newEntity;
    }
}
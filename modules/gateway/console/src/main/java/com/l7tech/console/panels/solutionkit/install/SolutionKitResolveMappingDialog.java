package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.*;
import com.l7tech.console.panels.encass.EncapsulatedAssertionManagerWindow;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.solutionkit.SolutionKitUtils;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.util.GoidUpgradeMapper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog used to resolve entity mapping errors.
 */
public class SolutionKitResolveMappingDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SolutionKitResolveMappingDialog.class.getName());

    private JPanel mainPanel;
    private JLabel nameFieldLabel;
    private JLabel idFieldLabel;
    private JLabel entityTypeFieldLabel;
    private JLabel errorTypeFieldLabel;
    private JComboBox entityComboBox;
    private JButton manageButton;
    private JButton okButton;
    private JButton cancelButton;

    private final Mapping mapping;
    private final Item item;
    private boolean isConfirmed = false;

    // Casted ComboBox of entityComboBox. Only one of these is not null.
    //
    private SecurePasswordComboBox securePasswordComboBox;
    private PrivateKeysComboBox privateKeysComboBox;
    private JdbcConnectionComboBox jdbcConnectionComboBox;
    private EncapsulatedAssertionConfigComboBox encapsulatedAssertionConfigComboBox;
    private CassandraConnectionComboBox cassandraConnectionComboBox;

    /**
     * Create dialog. Currently store passwords, private keys, and JDBC connections are supported.
     *
     * @param owner owner of the dialog
     * @param mapping mapping the entity mapping
     * @param item the entity item. Can be null.
     */
    public SolutionKitResolveMappingDialog(@NotNull Dialog owner, @NotNull Mapping mapping, Item item) {
        super(owner, "Resolve Entity Conflict", true);
        this.mapping = mapping;
        this.item = item;

        initialize();
        populateFields();
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public String getResolvedId() {
        if (entityComboBox.getSelectedIndex() == -1) {
            return null;
        }

        if (securePasswordComboBox != null) {
            return securePasswordComboBox.getSelectedSecurePassword().getGoid().toString();
        } else if (privateKeysComboBox != null) {
            Goid keyStoreId = privateKeysComboBox.getSelectedKeystoreId();
            String keyAlias = privateKeysComboBox.getSelectedKeyAlias();
            return keyStoreId.toString() + ":" + keyAlias;
        } else if (jdbcConnectionComboBox != null) {
            return jdbcConnectionComboBox.getSelectedJdbcConnection().toString();
        } else if (encapsulatedAssertionConfigComboBox != null) {
            return encapsulatedAssertionConfigComboBox.getSelectedEncapsulatedAssertion().getGoid().toString();
        } else if (cassandraConnectionComboBox != null) {
            return cassandraConnectionComboBox.getSelectedCassandraConnection().getGoid().toString();
        } else {
            return null;
        }
    }

    private void initialize() {
        manageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onMangeEntities();
            }
        });

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(mainPanel);
    }

    private void populateFields() {
        if (item != null) {
            nameFieldLabel.setText(item.getName());
        }
        idFieldLabel.setText(mapping.getSrcId());
        entityTypeFieldLabel.setText(mapping.getType());
        errorTypeFieldLabel.setText(mapping.getErrorType().toString());

        if (securePasswordComboBox != null) {
            securePasswordComboBox.reloadPasswordList(null);
        } else if (privateKeysComboBox != null) {
            privateKeysComboBox.repopulate();
        } else if (jdbcConnectionComboBox != null) {
            jdbcConnectionComboBox.reload();
        } else if (encapsulatedAssertionConfigComboBox != null) {
            encapsulatedAssertionConfigComboBox.reload();
        } else if (cassandraConnectionComboBox != null) {
            cassandraConnectionComboBox.reload();
        }
        // else, unsupported entity type. Do nothing.
    }

    private void onMangeEntities() {
        if (securePasswordComboBox != null) {
            manageSecurePasswords();
        } else if (privateKeysComboBox != null) {
            managePrivateKeys();
        } else if (jdbcConnectionComboBox != null) {
            manageJdbcConnections();
        } else if (encapsulatedAssertionConfigComboBox != null) {
            manageEncapsulatedAssertionConfigComboBox();
        } else if (cassandraConnectionComboBox != null) {
            manageCassandraConnectionComboBox();
        }
        // else, unsupported entity type. Do nothing.
    }

    private void manageSecurePasswords() {
        SecurePasswordManagerWindow dlg = new SecurePasswordManagerWindow(this.getOwner());
        if (item != null) {
            StoredPasswordMO resource = (StoredPasswordMO) item.getContent();
            SecurePassword securePassword = SolutionKitUtils.fromMangedObject(resource);
            dlg.setDefaultSecurePassword(securePassword);
        }

        DialogDisplayer.pack(dlg);
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                DialogDisplayer.pack(SolutionKitResolveMappingDialog.this);
            }
        });
        securePasswordComboBox.reloadPasswordList(null);
    }

    private void manageJdbcConnections() {
        JdbcConnectionManagerWindow dlg = new JdbcConnectionManagerWindow(this);
        if (item != null) {
            JDBCConnectionMO resource = (JDBCConnectionMO) item.getContent();
            final JdbcConnection jdbcConnection = SolutionKitUtils.fromMangedObject(resource);
            dlg.setDefaultJdbcConnection(jdbcConnection);
        }

        DialogDisplayer.pack(dlg);
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                DialogDisplayer.pack(SolutionKitResolveMappingDialog.this);
            }
        });
        jdbcConnectionComboBox.reload();
    }

    private void managePrivateKeys() {
        PrivateKeyManagerWindow dlg = new PrivateKeyManagerWindow(this.getOwner());
        DialogDisplayer.pack(dlg);
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                DialogDisplayer.pack(SolutionKitResolveMappingDialog.this);
            }
        });
        privateKeysComboBox.repopulate();
    }

    private void manageEncapsulatedAssertionConfigComboBox() {
        final EncapsulatedAssertionManagerWindow dlg = new EncapsulatedAssertionManagerWindow(this);
        if (item != null) {
            final EncapsulatedAssertionMO resource = (EncapsulatedAssertionMO) item.getContent();
            final EncapsulatedAssertionConfig encapsulatedAssertionConfig = SolutionKitUtils.fromMangedObject(resource);

            // Reset guid as null, since a new encapsulated assertion before saved must have a null guid.  Otherwise,
            // Validate.isTrue(config.getGuid() == null, "The EncapsulatedAssertionConfig has already been persisted.")
            // in CreateEncapsulatedAssertionAction will be failed.
            encapsulatedAssertionConfig.setGuid(null);

            // Retried the policy fragment associated with this encapsulated assertion
            encapsulatedAssertionConfig.setPolicy(getPolicyByGoid(GoidUpgradeMapper.mapId(EntityType.POLICY, resource.getPolicyReference().getId())));

            dlg.setDefaultConfig(encapsulatedAssertionConfig);
        }

        DialogDisplayer.pack(dlg);
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                DialogDisplayer.pack(SolutionKitResolveMappingDialog.this);
            }
        });
        encapsulatedAssertionConfigComboBox.reload();
    }

    private Policy getPolicyByGoid(final Goid policyGoid) {
        final PolicyAdmin policyAdmin = Registry.getDefault().getPolicyAdmin();
        if (policyAdmin == null) {
            logger.warning("Cannot get Policy Admin since the AdminContext is not available.");
            return null;
        }
        try {
            return policyAdmin.findPolicyByPrimaryKey(policyGoid);
        } catch (FindException e) {
            logger.warning("Cannot find a policy with GOID = " + policyGoid.toString());
            return null;
        }
    }

    private void manageCassandraConnectionComboBox() {
        final CassandraConnectionManagerDialog dlg = new CassandraConnectionManagerDialog(this);
        if (item != null) {
            final CassandraConnectionMO resource = (CassandraConnectionMO) item.getContent();
            final CassandraConnection cassandraConnection = SolutionKitUtils.fromMangedObject(resource);
            dlg.setDefaultConnection(cassandraConnection);
        }

        DialogDisplayer.pack(dlg);
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                DialogDisplayer.pack(SolutionKitResolveMappingDialog.this);
            }
        });
        cassandraConnectionComboBox.reload();
    }

    private void onOk() {
        if (entityComboBox.getSelectedIndex() == -1) {
            JOptionPane.showMessageDialog(this,
                "An entity must be selected.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        } else {
            isConfirmed = true;
            dispose();
        }
    }

    private void onCancel() {
        dispose();
    }

    private void createUIComponents() {
        EntityType entityType = EntityType.valueOf(mapping.getType());
        switch (entityType) {
            case SECURE_PASSWORD:
                securePasswordComboBox = new SecurePasswordComboBox();
                entityComboBox = securePasswordComboBox;
                break;

            case SSG_KEY_ENTRY:
                privateKeysComboBox = new PrivateKeysComboBox();
                // Hide default SSL key. Cannot update mapping to default SSL key.
                privateKeysComboBox.setIncludeDefaultSslKey(false);
                entityComboBox = privateKeysComboBox;
                break;

            case JDBC_CONNECTION:
                jdbcConnectionComboBox = new JdbcConnectionComboBox();
                entityComboBox = jdbcConnectionComboBox;
                break;

            case ENCAPSULATED_ASSERTION:
                encapsulatedAssertionConfigComboBox = new EncapsulatedAssertionConfigComboBox();
                entityComboBox = encapsulatedAssertionConfigComboBox;
                break;

            case CASSANDRA_CONFIGURATION:
                cassandraConnectionComboBox = new CassandraConnectionComboBox();
                entityComboBox = cassandraConnectionComboBox;
                break;

            default:
                logger.log(Level.WARNING, "Unsupported entity type: " + mapping.getType());
                // create default JComboBox.
                entityComboBox = new JComboBox();
                break;
        }
    }
}
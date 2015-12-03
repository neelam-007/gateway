package com.l7tech.external.assertions.remotecacheassertion.console;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheTypes;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

import static com.l7tech.external.assertions.remotecacheassertion.RemoteCacheTypes.*;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 10/11/11
 * Time: 3:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class CacheServerDialog extends JDialog {
    private JPanel mainPanel;
    private JTextField nameField;
    private JComboBox typeComboBox;
    private JSpinner timeoutField;
    private JPanel cacheTypeConfigPanelHolder;
    private JButton okButton;
    private JButton cancelButton;
    private JCheckBox enabledCheckBox;

    private static final String TITLE = "Remote Cache Configuration";

    /**
     * @noinspection ThisEscapedInObjectConstruction
     */
    private final InputValidator validator = new InputValidator(this, TITLE);
    private boolean confirmed = false;

    private RemoteCacheConfigPanel cacheConfigPanel = null;

    public CacheServerDialog(Dialog parent, RemoteCacheEntity remoteCache) {
        super(parent, TITLE, true);

        initComponents();
        setData(remoteCache);
    }

    private void initComponents() {
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        validator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        validator.constrainTextFieldToBeNonEmpty("name", nameField, null);

        for (RemoteCacheTypes cacheType : RemoteCacheTypes.values()) {
            typeComboBox.addItem(cacheType.name());
        }

        typeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (cacheConfigPanel == null ||
                        (cacheConfigPanel instanceof MemcachedConfigPanel && Memcached.name().equals(typeComboBox.getSelectedItem())) ||
                        (cacheConfigPanel instanceof TerracottaConfigPanel && Terracotta.name().equals(typeComboBox.getSelectedItem())) ||
                        (cacheConfigPanel instanceof CoherenceConfigPanel && Coherence.name().equals(typeComboBox.getSelectedItem())) ||
                        (cacheConfigPanel instanceof GemfireConfigPanel && GemFire.name().equals(typeComboBox.getSelectedItem())) ||
                        (cacheConfigPanel instanceof RedisConfigPanel && Redis.name().equals(typeComboBox.getSelectedItem()))) {
                    return;
                }

                cacheConfigPanel.removeValidators();
                cacheTypeConfigPanelHolder.removeAll();

                RemoteCacheConfigPanel configPanel;
                String selectedType = (String) typeComboBox.getSelectedItem();

                switch (RemoteCacheTypes.valueOf(selectedType)) {
                    case Memcached:
                        configPanel = new MemcachedConfigPanel(CacheServerDialog.this, new HashMap<String, String>(), validator);
                        cacheTypeConfigPanelHolder.add(configPanel.getMainPanel());
                        cacheConfigPanel = configPanel;
                        break;
                    case Terracotta:
                        configPanel = new TerracottaConfigPanel(CacheServerDialog.this, new HashMap<String, String>(), validator);
                        cacheTypeConfigPanelHolder.add(configPanel.getMainPanel());
                        cacheConfigPanel = configPanel;
                        break;
                    case Coherence:
                        configPanel = new CoherenceConfigPanel(CacheServerDialog.this, new HashMap<String, String>(), validator);
                        cacheTypeConfigPanelHolder.add(configPanel.getMainPanel());
                        cacheConfigPanel = configPanel;
                        break;
                    case GemFire:
                        configPanel = new GemfireConfigPanel(CacheServerDialog.this, new HashMap<String, String>(), validator);
                        cacheTypeConfigPanelHolder.add(configPanel.getMainPanel());
                        cacheConfigPanel = configPanel;
                        break;
                    case Redis:
                        configPanel = new RedisConfigPanel(CacheServerDialog.this, new HashMap<String, String>(), validator);
                        cacheTypeConfigPanelHolder.add(configPanel.getMainPanel());
                        cacheConfigPanel = configPanel;
                        break;
                    default:
                        break;
                }
                pack();
            }
        });

        timeoutField.setModel(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));

        Utilities.setEscKeyStrokeDisposes(this);

        pack();
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void setData(RemoteCacheEntity remoteCache) {
        if (remoteCache == null) {
            return;
        }

        nameField.setText(remoteCache.getName());
        timeoutField.setValue(remoteCache.getTimeout());
        enabledCheckBox.setSelected(remoteCache.isEnabled());

        RemoteCacheTypes cacheType = RemoteCacheTypes.getEntityEnumType(remoteCache.getType());
        RemoteCacheConfigPanel configPanel;
        switch (cacheType) {
            case Memcached:
                configPanel = new MemcachedConfigPanel(this, remoteCache.getProperties(), validator);
                typeComboBox.setSelectedItem(Memcached.name());
                cacheTypeConfigPanelHolder.removeAll();
                cacheTypeConfigPanelHolder.add(configPanel.getMainPanel());
                cacheConfigPanel = configPanel;
                break;
            case Terracotta:
                configPanel = new TerracottaConfigPanel(this, remoteCache.getProperties(), validator);
                typeComboBox.setSelectedItem(RemoteCacheTypes.Terracotta.name());
                cacheTypeConfigPanelHolder.removeAll();
                cacheTypeConfigPanelHolder.add(configPanel.getMainPanel());
                cacheConfigPanel = configPanel;
                break;
            case Coherence:
                configPanel = new CoherenceConfigPanel(this, remoteCache.getProperties(), validator);
                typeComboBox.setSelectedItem(RemoteCacheTypes.Coherence.name());
                cacheTypeConfigPanelHolder.removeAll();
                cacheTypeConfigPanelHolder.add(configPanel.getMainPanel());
                cacheConfigPanel = configPanel;
                break;
            case GemFire:
                configPanel = new GemfireConfigPanel(this, remoteCache.getProperties(), validator);
                typeComboBox.setSelectedItem(RemoteCacheTypes.GemFire.name());
                cacheTypeConfigPanelHolder.removeAll();
                cacheTypeConfigPanelHolder.add(configPanel.getMainPanel());
                cacheConfigPanel = configPanel;
                break;
            case Redis:
                configPanel = new RedisConfigPanel(this, remoteCache.getProperties(), validator);
                typeComboBox.setSelectedItem(RemoteCacheTypes.Redis.name());
                cacheTypeConfigPanelHolder.removeAll();
                cacheTypeConfigPanelHolder.add(configPanel.getMainPanel());
                cacheConfigPanel = configPanel;
                break;
            default:
                break;
        }
        pack();
    }

    public RemoteCacheEntity getData(RemoteCacheEntity remoteCache) {
        remoteCache.setName(nameField.getText().trim());
        remoteCache.setTimeout((Integer) timeoutField.getValue());
        remoteCache.setEnabled(enabledCheckBox.isSelected());

        if (cacheConfigPanel instanceof MemcachedConfigPanel) {
            remoteCache.setType(RemoteCacheTypes.Memcached.getEntityType());
            remoteCache.setProperties(cacheConfigPanel.getData());
        } else if (cacheConfigPanel instanceof TerracottaConfigPanel) {
            remoteCache.setType(RemoteCacheTypes.Terracotta.getEntityType());
            remoteCache.setProperties(cacheConfigPanel.getData());
        } else if (cacheConfigPanel instanceof CoherenceConfigPanel) {
            remoteCache.setType(RemoteCacheTypes.Coherence.getEntityType());
            remoteCache.setProperties(cacheConfigPanel.getData());
        } else if (cacheConfigPanel instanceof GemfireConfigPanel) {
            remoteCache.setType(RemoteCacheTypes.GemFire.getEntityType());
            remoteCache.setProperties(cacheConfigPanel.getData());
        } else if (cacheConfigPanel instanceof RedisConfigPanel) {
            remoteCache.setType(RemoteCacheTypes.Redis.getEntityType());
            remoteCache.setProperties(cacheConfigPanel.getData());
        }
        return remoteCache;
    }
}

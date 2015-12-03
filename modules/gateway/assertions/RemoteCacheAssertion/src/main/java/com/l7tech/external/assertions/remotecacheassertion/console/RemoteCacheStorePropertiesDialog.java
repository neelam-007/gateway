package com.l7tech.external.assertions.remotecacheassertion.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntityAdmin;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheStoreAssertion;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheTypes;
import com.l7tech.external.assertions.remotecacheassertion.server.CachedMessageData;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 02/11/11
 * Time: 2:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteCacheStorePropertiesDialog extends AssertionPropertiesEditorSupport<RemoteCacheStoreAssertion> {
    private JPanel contentPane;
    private JComboBox remoteCacheComboBox;
    private JTextField cacheEntryKeyField;
    private JTextField maxEntryAgeField;
    private JTextField maxEntrySizeField;
    private JCheckBox dontCacheSoapFaults;
    private JButton okButton;
    private JButton cancelButton;
    private JLabel maxEntryAgeLabel;
    private JLabel maxEntrySizeLable;
    private JComboBox<CachedMessageData.ValueType> valueTypesComboBox;
    private JLabel valueObjectTypeLabel;

    public static final String TITLE = "Remote Cache Storage Properties";
    public static final long kMAX_ENTRY_AGE = 100000000L;
    public static final long kMAX_ENTRY_SIZE = 1000000000L;

    /**
     * @noinspection ThisEscapedInObjectConstruction
     */
    private final InputValidator validator = new InputValidator(this, TITLE);
    private boolean confirmed = false;

    private static class RemoteCacheEntry {
        private Goid goid;
        private String name;
        private String type;

        public RemoteCacheEntry(Goid goid, String name, String type) {
            this.goid = goid;
            this.name = name;
            this.type = type;
        }

        public Goid getGoid() {
            return goid;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String toString() {
            return name;
        }

    }

    public RemoteCacheStorePropertiesDialog(Window owner) {
        super(owner, TITLE);
        init();
    }

    private void init() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        loadRemoteCacheList();

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

        validator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                RemoteCacheEntry entry = (RemoteCacheEntry) remoteCacheComboBox.getSelectedItem();

                if (entry == null) {
                    return "No servers were specified.";
                }

                return null;
            }
        });

        validator.constrainTextFieldToBeNonEmpty("Cache entry key", cacheEntryKeyField, new InputValidator.ComponentValidationRule(cacheEntryKeyField) {
            @Override
            public String getValidationError() {
                String[] refs = Syntax.getReferencedNames(cacheEntryKeyField.getText());
                if (refs == null || refs.length < 1)
                    return "The cache entry key must contain at least one interpolated context variable.";
                return null;
            }
        });
        //We do not need to add Textfield validation because this will support context variables.
        //validator.constrainTextFieldToNumberRange("Maximum entry age", maxEntryAgeField, 0, kMAX_ENTRY_AGE);
        //validator.constrainTextFieldToNumberRange("Maximum entry size", maxEntrySizeField, 0, kMAX_ENTRY_SIZE);
        Utilities.setEscKeyStrokeDisposes(this);

        remoteCacheComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideComponents();
            }
        });

        valueTypesComboBox.setModel(new DefaultComboBoxModel<CachedMessageData.ValueType>(CachedMessageData.ValueType.values()));

        valueTypesComboBox.setVisible(false);
        valueObjectTypeLabel.setVisible(false);
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }


    private void onCancel() {
        confirmed = false;
        dispose();
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(RemoteCacheStoreAssertion ass) {

        for (int i = 0; i < remoteCacheComboBox.getItemCount(); i++) {
            RemoteCacheEntry entry = (RemoteCacheEntry) remoteCacheComboBox.getItemAt(i);
            if (entry != null && entry.getGoid().equals(ass.getRemoteCacheGoid())) {
                remoteCacheComboBox.setSelectedIndex(i);
                break;
            }
        }

        cacheEntryKeyField.setText(ass.getCacheEntryKey() == null ? "" : ass.getCacheEntryKey());
        maxEntryAgeField.setText((ass.getMaxEntryAge()));
        maxEntrySizeField.setText((ass.getMaxEntrySizeBytes()));
        dontCacheSoapFaults.setSelected(!ass.isStoreSoapFaults());
        if (ass.getValueType() != null)
            valueTypesComboBox.setSelectedItem(CachedMessageData.ValueType.valueOf(ass.getValueType()));
    }

    @Override
    public RemoteCacheStoreAssertion getData(RemoteCacheStoreAssertion ass) {
        RemoteCacheEntry remoteCacheEntry = (RemoteCacheEntry) remoteCacheComboBox.getSelectedItem();

        ass.setRemoteCacheGoid(remoteCacheEntry.getGoid());
        ass.setCacheEntryKey(cacheEntryKeyField.getText());
        ass.setMaxEntryAge((maxEntryAgeField.getText()));
        ass.setMaxEntrySizeBytes((maxEntrySizeField.getText()));
        ass.setStoreSoapFaults(!dontCacheSoapFaults.isSelected());
        ass.setValueType(valueTypesComboBox.getSelectedItem().toString());

        return ass;
    }

    private void loadRemoteCacheList() {
        try {
            DefaultComboBoxModel model = new DefaultComboBoxModel();
            RemoteCacheEntityAdmin remoteCacheAdmin = Registry.getDefault().getExtensionInterface(RemoteCacheEntityAdmin.class, null);

            //add empty first element to combo box
            model.addElement(null);

            for (RemoteCacheEntity remoteCache : remoteCacheAdmin.findAll()) {
                model.addElement(new RemoteCacheEntry(remoteCache.getGoid(), remoteCache.getName(), remoteCache.getType()));
            }

            remoteCacheComboBox.setModel(model);
        } catch (FindException fe) {
        }
    }

    private void hideComponents() {

        RemoteCacheEntry entry = (RemoteCacheEntry) remoteCacheComboBox.getSelectedItem();
        valueTypesComboBox.setVisible(false);
        valueObjectTypeLabel.setVisible(false);

        if (entry != null && entry.getType().equals(RemoteCacheTypes.GemFire.getEntityType())) {
            maxEntryAgeField.setVisible(false);
            maxEntrySizeField.setVisible(false);
            maxEntryAgeLabel.setVisible(false);
            maxEntrySizeLable.setVisible(false);
        } else {
            maxEntryAgeField.setVisible(true);
            maxEntrySizeField.setVisible(true);
            maxEntryAgeLabel.setVisible(true);
            maxEntrySizeLable.setVisible(true);
        }

        if (entry != null && entry.getType().equals(RemoteCacheTypes.Memcached.getEntityType())) {
            valueTypesComboBox.setVisible(true);
            valueObjectTypeLabel.setVisible(true);
        }
    }
}

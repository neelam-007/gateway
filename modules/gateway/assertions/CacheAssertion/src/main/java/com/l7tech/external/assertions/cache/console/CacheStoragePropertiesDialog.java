package com.l7tech.external.assertions.cache.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.external.assertions.cache.CacheStorageAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class CacheStoragePropertiesDialog extends AssertionPropertiesEditorSupport<CacheStorageAssertion> {
    public static final String TITLE = "Cache Storage Properties";

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox storeFromComboBox;
    private SquigglyTextField cacheIdField;
    private SquigglyTextField maxEntriesField;
    private SquigglyTextField maxEntryAgeField;
    private SquigglyTextField maxEntrySizeField;
    private SquigglyTextField cacheKeyField;

    /** @noinspection ThisEscapedInObjectConstruction*/
    private final InputValidator validator = new InputValidator(this, TITLE);
    private boolean confirmed = false;

    public CacheStoragePropertiesDialog(Frame owner) {
        super(owner, TITLE, true);
        init();
    }

    public CacheStoragePropertiesDialog(Dialog owner) {
        super(owner, TITLE, true);
        init();
    }

    private void init() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        validator.attachToButton(buttonOK, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
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

        validator.constrainTextFieldToBeNonEmpty("Cache ID", cacheIdField, null);
        validator.constrainTextFieldToBeNonEmpty("Cache Entry Key", cacheKeyField, new InputValidator.ComponentValidationRule(cacheKeyField) {
            public String getValidationError() {
                String[] refs = Syntax.getReferencedNames(cacheKeyField.getText());
                if (refs == null || refs.length < 1)
                    return "The cache entry key must contain at least one interpolated context variable.";
                return null;
            }
        });
        validator.constrainTextFieldToNumberRange("Max Entries", maxEntriesField, 0, 1000000L);
        validator.constrainTextFieldToNumberRange("Max Entry Age", maxEntryAgeField, 0, 100000000L);
        validator.constrainTextFieldToNumberRange("Max Entry Size", maxEntrySizeField, 0, 1000000000L);
        validator.addRule(new InputValidator.ComponentValidationRule(storeFromComboBox) {
            public String getValidationError() {
                return storeFromComboBox.getSelectedItem() == null ? "Please select something to store" : null;
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public static void main(String[] args) {
        CacheStoragePropertiesDialog dialog = new CacheStoragePropertiesDialog((Frame)null);
        dialog.setData(new CacheStorageAssertion());
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private static class StoreFromEntry {
        private final boolean isRequest;
        private final String varname;
        private final String label;

        private StoreFromEntry(boolean request, String varname, String label) {
            isRequest = request;
            this.varname = varname;
            this.label = label;
        }

        public boolean isRequest() {
            return isRequest;
        }

        public String getVarname() {
            return varname;
        }

        public String toString() {
            return label;
        }
    }

    private void repopulateStoreFromComboBox(CacheStorageAssertion ass) {
        storeFromComboBox.removeAllItems();
        final StoreFromEntry reqItem = new StoreFromEntry(true, null, "Default Request");
        storeFromComboBox.addItem(reqItem);
        final StoreFromEntry resItem = new StoreFromEntry(false, null, "Default Response");
        storeFromComboBox.addItem(resItem);
        final MessageFormat displayFormat = new MessageFormat("Context Variable: {0}{1}{2}");
        final Map<String, VariableMetadata> predecessorVariables = PolicyVariableUtils.getVariablesSetByPredecessors(ass);
        final SortedSet<String> predecessorVariableNames = new TreeSet<String>(predecessorVariables.keySet());
        for (String variableName: predecessorVariableNames) {
            if (predecessorVariables.get(variableName).getType() == DataType.MESSAGE) {
                final StoreFromEntry item = new StoreFromEntry(false, variableName,
                                                               displayFormat.format(new Object[]{Syntax.SYNTAX_PREFIX, variableName, Syntax.SYNTAX_SUFFIX}));
                storeFromComboBox.addItem(item);
                if (variableName.equals(ass.getSourceVariableName()))
                    storeFromComboBox.setSelectedItem(item);
            }
        }

        if (ass.getSourceVariableName() == null)
            storeFromComboBox.setSelectedItem(ass.isUseRequest() ? reqItem : resItem);
    }

    public void setData(CacheStorageAssertion ass) {
        repopulateStoreFromComboBox(ass);
        cacheIdField.setText(ass.getCacheId());
        cacheKeyField.setText(ass.getCacheEntryKey());
        maxEntryAgeField.setText(Long.toString(ass.getMaxEntryAgeMillis() / 1000L));
        maxEntriesField.setText(Integer.toString(ass.getMaxEntries()));
        maxEntrySizeField.setText(Long.toString(ass.getMaxEntrySizeBytes()));
    }

    public CacheStorageAssertion getData(CacheStorageAssertion ass) {
        StoreFromEntry entry = (StoreFromEntry)storeFromComboBox.getSelectedItem();
        ass.setSourceVariableName(entry.getVarname());
        ass.setUseRequest(entry.isRequest());
        ass.setMaxEntries(Integer.parseInt(maxEntriesField.getText()));
        ass.setMaxEntryAgeMillis(Long.parseLong(maxEntryAgeField.getText()) * 1000L);
        ass.setMaxEntrySizeBytes(Long.parseLong(maxEntrySizeField.getText()));
        ass.setCacheId(cacheIdField.getText());
        ass.setCacheEntryKey(cacheKeyField.getText());
        return ass;
    }
}

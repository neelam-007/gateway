package com.l7tech.external.assertions.cache.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.external.assertions.cache.CacheStorageAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;

public class CacheStoragePropertiesDialog extends AssertionPropertiesEditorSupport<CacheStorageAssertion> {
    private static ResourceBundle resourceBundle = ResourceBundle.getBundle(CacheLookupPropertiesDialog.class.getName());

    public static final String TITLE = resourceBundle.getString("cache.storage.properties.title");
    public static final long kMAX_ENTIRES = 1000000L;
    public static final long kMAX_ENTRY_AGE = 100000000L;
    public static final long kMAX_ENTRY_SIZE = 1000000000L;


    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private SquigglyTextField cacheIdField;
    private SquigglyTextField maxEntriesField;
    private SquigglyTextField maxEntryAgeField;
    private SquigglyTextField maxEntrySizeField;
    private SquigglyTextField cacheKeyField;
    private JCheckBox dontCacheFaults;

    /** @noinspection ThisEscapedInObjectConstruction*/
    private final InputValidator validator = new InputValidator(this, TITLE);
    private boolean confirmed = false;

    public CacheStoragePropertiesDialog(Window owner) {
        super(owner, TITLE);
        init();
    }

    private void init() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        validator.attachToButton(buttonOK, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
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

        validator.constrainTextFieldToBeNonEmpty("Cache ID", cacheIdField, null);
        validator.constrainTextFieldToBeNonEmpty("Cache Entry Key", cacheKeyField, new InputValidator.ComponentValidationRule(cacheKeyField) {
            @Override
            public String getValidationError() {
                String[] refs = Syntax.getReferencedNames(cacheKeyField.getText());
                if (refs == null || refs.length < 1)
                    return resourceBundle.getString("the.cache.entry.key.error");
                return null;
            }
        });
        validator.constrainTextFieldToNumberRange("Max Entries", maxEntriesField, 0, kMAX_ENTIRES);
        validator.constrainTextFieldToNumberRange("Max Entry Age", maxEntryAgeField, 0, kMAX_ENTRY_AGE);
        validator.constrainTextFieldToNumberRange("Max Entry Size", maxEntrySizeField, 0, kMAX_ENTRY_SIZE);
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

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(CacheStorageAssertion ass) {
        cacheIdField.setText(ass.getCacheId());
        cacheKeyField.setText(ass.getCacheEntryKey());
        maxEntryAgeField.setText(Long.toString(ass.getMaxEntryAgeMillis() / 1000L));
        maxEntriesField.setText(Integer.toString(ass.getMaxEntries()));
        maxEntrySizeField.setText(Long.toString(ass.getMaxEntrySizeBytes()));
        dontCacheFaults.setSelected(! ass.isStoreSoapFaults());
    }

    @Override
    public CacheStorageAssertion getData(CacheStorageAssertion ass) {
        ass.setMaxEntries(Integer.parseInt(maxEntriesField.getText()));
        ass.setMaxEntryAgeMillis(Long.parseLong(maxEntryAgeField.getText()) * 1000L);
        ass.setMaxEntrySizeBytes(Long.parseLong(maxEntrySizeField.getText()));
        ass.setCacheId(cacheIdField.getText());
        ass.setCacheEntryKey(cacheKeyField.getText());
        ass.setStoreSoapFaults(! dontCacheFaults.isSelected());
        return ass;
    }
}

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

public class CacheStoragePropertiesDialog extends AssertionPropertiesEditorSupport<CacheStorageAssertion> {
    public static final String TITLE = "Cache Storage Properties";

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private SquigglyTextField cacheIdField;
    private SquigglyTextField maxEntriesField;
    private SquigglyTextField maxEntryAgeField;
    private SquigglyTextField maxEntrySizeField;
    private SquigglyTextField cacheKeyField;

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
                    return "The cache entry key must contain at least one interpolated context variable.";
                return null;
            }
        });
        validator.constrainTextFieldToNumberRange("Max Entries", maxEntriesField, 0, 1000000L);
        validator.constrainTextFieldToNumberRange("Max Entry Age", maxEntryAgeField, 0, 100000000L);
        validator.constrainTextFieldToNumberRange("Max Entry Size", maxEntrySizeField, 0, 1000000000L);

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
    }

    @Override
    public CacheStorageAssertion getData(CacheStorageAssertion ass) {
        ass.setMaxEntries(Integer.parseInt(maxEntriesField.getText()));
        ass.setMaxEntryAgeMillis(Long.parseLong(maxEntryAgeField.getText()) * 1000L);
        ass.setMaxEntrySizeBytes(Long.parseLong(maxEntrySizeField.getText()));
        ass.setCacheId(cacheIdField.getText());
        ass.setCacheEntryKey(cacheKeyField.getText());
        return ass;
    }
}

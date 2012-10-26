package com.l7tech.external.assertions.cache.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.external.assertions.cache.CacheLookupAssertion;
import com.l7tech.external.assertions.cache.CacheStorageAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;

public class CacheStoragePropertiesDialog extends AssertionPropertiesEditorSupport<CacheStorageAssertion> {

    private static ResourceBundle resourceBundle = ResourceBundle.getBundle(CacheStoragePropertiesDialog.class.getName());
    public static final String TITLE = resourceBundle.getString("cache.storage.properties.title");

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

        validator.constrainTextFieldToBeNonEmpty(resourceBundle.getString("cache.id.field"), cacheIdField, null);

        validator.constrainTextFieldToBeNonEmpty(resourceBundle.getString("cache.key.field"), cacheKeyField, new InputValidator.ComponentValidationRule(cacheKeyField) {
            @Override
            public String getValidationError() {
                String[] refs = Syntax.getReferencedNames(cacheKeyField.getText());
                if (refs == null || refs.length < 1)
                    return resourceBundle.getString("the.cache.entry.key.error");
                return null;
            }
        });

        validator.constrainTextFieldToBeNonEmpty(resourceBundle.getString("max.entries.field"), maxEntriesField, new InputValidator.ComponentValidationRule(maxEntriesField) {
            @Override
            public String getValidationError() {
                if (ValidationUtils.isValidInteger(maxEntriesField.getText(), false, 0, CacheStorageAssertion.kMAX_ENTRIES)
                        || Syntax.isOnlyASingleVariableReferenced(maxEntriesField.getText())) {
                    return null;
                } else {
                    return "The Max Entries field must be a number between 0 and " + CacheStorageAssertion.kMAX_ENTRIES + ".";
                }
            }
        });

        validator.constrainTextFieldToBeNonEmpty(resourceBundle.getString("max.entry.age.field"), maxEntryAgeField, new InputValidator.ComponentValidationRule(maxEntryAgeField) {
            @Override
            public String getValidationError() {
                if (ValidationUtils.isValidLong(maxEntryAgeField.getText(), false, 0, CacheStorageAssertion.kMAX_ENTRY_AGE_SECONDS)
                        || Syntax.isOnlyASingleVariableReferenced(maxEntryAgeField.getText())) {
                    return null;
                } else {
                    return "The Max Entry Age field must be a number between 0 and " + CacheStorageAssertion.kMAX_ENTRY_AGE_SECONDS + ".";
                }
            }
        });

        validator.constrainTextFieldToBeNonEmpty(resourceBundle.getString("max.entry.size.field"), maxEntrySizeField, new InputValidator.ComponentValidationRule(maxEntrySizeField) {
            @Override
            public String getValidationError() {
                if (ValidationUtils.isValidLong(maxEntrySizeField.getText(), false, 0, CacheStorageAssertion.kMAX_ENTRY_SIZE)
                        || Syntax.isOnlyASingleVariableReferenced(maxEntrySizeField.getText())) {
                    return null;
                } else {
                    return "The Max Entry Size field must be a number between 0 and " + CacheStorageAssertion.kMAX_ENTRY_SIZE + ".";
                }
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

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(CacheStorageAssertion ass) {
        cacheIdField.setText(ass.getCacheId());
        cacheKeyField.setText(ass.getCacheEntryKey());
        maxEntriesField.setText(ass.getMaxEntries());
        maxEntrySizeField.setText(ass.getMaxEntrySizeBytes());
        dontCacheFaults.setSelected(! ass.isStoreSoapFaults());

        if (CacheLookupAssertion.isLong(ass.getMaxEntryAgeMillis())) {
            final long seconds = Long.parseLong(ass.getMaxEntryAgeMillis()) / 1000L;
            maxEntryAgeField.setText(String.valueOf(seconds));
        } else {
            // It's a context variable reference.
            maxEntryAgeField.setText(ass.getMaxEntryAgeMillis());
        }
    }

    @Override
    public CacheStorageAssertion getData(CacheStorageAssertion ass) {
        ass.setMaxEntries(maxEntriesField.getText());
        ass.setMaxEntrySizeBytes(maxEntrySizeField.getText());
        ass.setCacheId(cacheIdField.getText());
        ass.setCacheEntryKey(cacheKeyField.getText());
        ass.setStoreSoapFaults(! dontCacheFaults.isSelected());

        if (CacheLookupAssertion.isLong(maxEntryAgeField.getText())) {
            final long milliseconds = Long.parseLong(maxEntryAgeField.getText()) * 1000L;
            ass.setMaxEntryAgeMillis(String.valueOf(milliseconds));
        } else {
            // It's a context variable reference.
            ass.setMaxEntryAgeMillis(maxEntryAgeField.getText());
        }

        return ass;
    }
}

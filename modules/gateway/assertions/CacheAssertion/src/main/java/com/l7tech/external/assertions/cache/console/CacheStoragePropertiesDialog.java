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
import java.text.MessageFormat;
import java.util.ResourceBundle;

import static com.l7tech.external.assertions.cache.CacheStorageAssertion.kMAX_ENTRIES;
import static com.l7tech.external.assertions.cache.CacheStorageAssertion.kMAX_ENTRY_AGE_SECONDS;
import static com.l7tech.external.assertions.cache.CacheStorageAssertion.kMAX_ENTRY_SIZE;
import static com.l7tech.util.ValidationUtils.isValidLong;

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

        validator.constrainTextFieldToBeNonEmpty(resourceBundle.getString("cache.key.field"), cacheKeyField,
                new InputValidator.ComponentValidationRule(cacheKeyField) {
            @Override
            public String getValidationError() {
                String[] refs = Syntax.getReferencedNames(cacheKeyField.getText());
                if (refs.length < 1)
                    return resourceBundle.getString("the.cache.entry.key.error");
                return null;
            }
        });

        validator.constrainTextField(maxEntriesField, new InputValidator.ComponentValidationRule(maxEntriesField) {
            @Override
            public String getValidationError() {
                return validateField(maxEntriesField.getText(), "max.entries.variable.error", "max.entries.range.error", kMAX_ENTRIES);
            }
        });

        validator.constrainTextField(maxEntryAgeField, new InputValidator.ComponentValidationRule(maxEntryAgeField) {
            @Override
            public String getValidationError() {
                return validateField(maxEntryAgeField.getText(), "max.entry.age.variable.error", "max.entry.age.range.error", kMAX_ENTRY_AGE_SECONDS);
            }
        });

        validator.constrainTextField(maxEntrySizeField, new InputValidator.ComponentValidationRule(maxEntrySizeField) {
            @Override
            public String getValidationError() {
                return validateField(maxEntrySizeField.getText(), "max.entry.size.variable.error", "max.entry.size.range.error", kMAX_ENTRY_SIZE);
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
    }

    private String validateField(String expression,
                                 String resourceKeyForVarRefError,
                                 String resourceKeyForRangeError,
                                 long maxValue){
        if (Syntax.isOnlyASingleVariableReferenced(expression)) {
            // no validation of a context variable reference.
            return null;
        }

        final String[] refs = Syntax.getReferencedNames(expression);
        if (refs.length > 0) {
            return resourceBundle.getString(resourceKeyForVarRefError);
        } else {
            final boolean isWithinRange = isValidLong(expression, false, 0, maxValue);
            if (!isWithinRange) {
                return MessageFormat.format(resourceBundle.getString(resourceKeyForRangeError),
                        String.valueOf(maxValue));
            }
        }

        return null;

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
        maxEntryAgeField.setText(ass.getMaxEntryAgeSeconds());
    }

    @Override
    public CacheStorageAssertion getData(CacheStorageAssertion ass) {
        ass.setMaxEntries(maxEntriesField.getText());
        ass.setMaxEntrySizeBytes(maxEntrySizeField.getText());
        ass.setCacheId(cacheIdField.getText());
        ass.setCacheEntryKey(cacheKeyField.getText());
        ass.setStoreSoapFaults(! dontCacheFaults.isSelected());
        ass.setMaxEntryAgeSeconds(maxEntryAgeField.getText().trim());

        return ass;
    }
}

package com.l7tech.external.assertions.cache.console;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.external.assertions.cache.CacheLookupAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.MessageFormat;import java.util.ResourceBundle;

public class CacheLookupPropertiesDialog extends AssertionPropertiesEditorSupport<CacheLookupAssertion> {
    private static ResourceBundle resourceBundle = ResourceBundle.getBundle(CacheLookupPropertiesDialog.class.getName());
    public static final String TITLE = resourceBundle.getString("cache.lookup.properties.title");

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private SquigglyTextField cacheIdField;
    private SquigglyTextField cacheKeyField;
    private SquigglyTextField maxAgeField;
    private JTextField contentTypeOverride;

    /** @noinspection ThisEscapedInObjectConstruction*/
    private final InputValidator validator = new InputValidator(this, TITLE);
    private boolean confirmed = false;

    public CacheLookupPropertiesDialog(Window owner) {
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
                onOk();
            }
        });

        buttonCancel.addActionListener(new AbstractAction("cancel") {
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
                if (refs == null || refs.length < 1)
                    return resourceBundle.getString("the.cache.entry.key.error");
                return null;
            }
        });

        validator.constrainTextFieldToNumberRange(resourceBundle.getString("max.entry.age.field"), maxAgeField, 0,
                CacheLookupAssertion.MAX_SECONDS_FOR_MAX_ENTRY_AGE);

        validator.constrainTextFieldToBeNonEmpty(resourceBundle.getString("max.entry.age.field"), maxAgeField,
                new InputValidator.ComponentValidationRule(maxAgeField) {
                    @Override
                    public String getValidationError() {
                        if (Syntax.isOnlyASingleVariableReferenced(maxAgeField.getText()) ||
                                ValidationUtils.isValidLong(maxAgeField.getText(), false,
                                        CacheLookupAssertion.MIN_SECONDS_FOR_MAX_ENTRY_AGE,
                                        CacheLookupAssertion.MAX_SECONDS_FOR_MAX_ENTRY_AGE)) {
                            return null;
                        } else {
                            return MessageFormat.format(resourceBundle.getString("max.entry.age.range.error"),
                                    String.valueOf(CacheLookupAssertion.MAX_SECONDS_FOR_MAX_ENTRY_AGE));
                        }
                    }
                });

        validator.constrainTextField(contentTypeOverride, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                try {
                    String contentType = contentTypeOverride.getText();
                    if (contentType == null || contentType.isEmpty())
                        return null;
                    ContentTypeHeader.parseValue(contentType);
                    return null;
                } catch (IOException e) {
                    return MessageFormat.format(resourceBundle.getString("invalid.content.type.invalid.configured"),
                            ExceptionUtils.getMessage(e));
                }
            }
        });

        maxAgeField.setText("86400");

        Utilities.setEscKeyStrokeDisposes(this);
    }

    private void onOk() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public static void main(String[] args) {
        CacheLookupPropertiesDialog dialog = new CacheLookupPropertiesDialog(null);
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(CacheLookupAssertion ass) {
        cacheIdField.setText(ass.getCacheId());
        cacheKeyField.setText(ass.getCacheEntryKey());
        contentTypeOverride.setText(ass.getContentTypeOverride());

        if (ValidationUtils.isValidLong(ass.getMaxEntryAgeMillis(), false, 0L, Long.MAX_VALUE)) {
            final long seconds = Long.parseLong(ass.getMaxEntryAgeMillis()) / 1000L;
            maxAgeField.setText(String.valueOf(seconds));
        } else {
            maxAgeField.setText(ass.getMaxEntryAgeMillis());
        }
    }

    @Override
    public CacheLookupAssertion getData(CacheLookupAssertion ass) {
        ass.setCacheId(cacheIdField.getText());
        ass.setCacheEntryKey(cacheKeyField.getText());
        ass.setContentTypeOverride(contentTypeOverride.getText());

        if (ValidationUtils.isValidLong(maxAgeField.getText(), false, 0L, Long.MAX_VALUE)) {
            final long milliseconds = Long.parseLong(maxAgeField.getText()) * 1000L;
            ass.setMaxEntryAgeMillis(String.valueOf(milliseconds));
        } else {
            ass.setMaxEntryAgeMillis(maxAgeField.getText());
        }

        return ass;
    }
}

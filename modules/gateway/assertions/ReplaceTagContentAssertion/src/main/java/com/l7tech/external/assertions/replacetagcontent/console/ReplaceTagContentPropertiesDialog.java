package com.l7tech.external.assertions.replacetagcontent.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.replacetagcontent.ReplaceTagContentAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import java.awt.*;

public class ReplaceTagContentPropertiesDialog extends AssertionPropertiesOkCancelSupport<ReplaceTagContentAssertion> {
    private JPanel contentPanel;
    private JTextField searchTextField;
    private JTextField replaceTextField;
    private JTextField tagsTextField;
    private JCheckBox ignoreCaseCheckBox;
    private InputValidator validators;

    public ReplaceTagContentPropertiesDialog(final Frame parent, final ReplaceTagContentAssertion assertion) {
        super(assertion.getClass(), parent, (String) assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();
    }

    @Override
    public void setData(final ReplaceTagContentAssertion assertion) {
        searchTextField.setText(assertion.getSearchFor());
        replaceTextField.setText(assertion.getReplaceWith());
        tagsTextField.setText(assertion.getTagsToSearch());
        ignoreCaseCheckBox.setSelected(!assertion.isCaseSensitive());
    }

    @Override
    public ReplaceTagContentAssertion getData(final ReplaceTagContentAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if (error != null) {
            throw new ValidationException(error);
        }
        // do not trim search or replacement
        assertion.setSearchFor(searchTextField.getText());
        assertion.setReplaceWith(replaceTextField.getText());
        assertion.setTagsToSearch(tagsTextField.getText().trim());
        assertion.setCaseSensitive(!ignoreCaseCheckBox.isSelected());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPanel;
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        validators = new InputValidator(this, getTitle());
        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                String error = null;
                // completely empty is not valid but some whitespace is valid
                if (searchTextField.getText().isEmpty()) {
                    error = "The SearchFor field must not be empty.";
                }
                return error;
            }
        });
        validators.constrainTextFieldToBeNonEmpty("Search Within Tags", tagsTextField, null);
    }
}

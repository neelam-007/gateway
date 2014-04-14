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
        assertion.setSearchFor(searchTextField.getText().trim());
        assertion.setReplaceWith(replaceTextField.getText().trim());
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
        validators.constrainTextFieldToBeNonEmpty("Search For", searchTextField, null);
        validators.constrainTextFieldToBeNonEmpty("Search Within Tags", tagsTextField, null);
        validators.constrainTextFieldToBeNonEmpty("Replace With", replaceTextField, null);
    }
}

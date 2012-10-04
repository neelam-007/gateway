package com.l7tech.external.assertions.ahttp.console;

import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.ahttp.AsyncHttpRoutingAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AsyncHttpRoutingAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<AsyncHttpRoutingAssertion> {
    private static final Logger logger = Logger.getLogger(AsyncHttpRoutingAssertionPropertiesDialog.class.getName());

    private JPanel contentPane;
    private JComboBox policyComboBox;
    private JTextField urlField;
    private JComboBox httpVersionComboBox;
    private JComboBox httpMethodComboBox;
    private JCheckBox useKeepAlivesCheckBox;
    private JCheckBox overrideHttpMethodCheckBox;
    private JCheckBox overrideHttpVersionCheckBox;

    private Collection<PolicyHeader> policyHeaders;

    public AsyncHttpRoutingAssertionPropertiesDialog(Window owner, AsyncHttpRoutingAssertion assertion) {
        super(AsyncHttpRoutingAssertion.class, owner, assertion, true);
        initComponents();
    }

    @Override
    public void setData(AsyncHttpRoutingAssertion assertion) {
        urlField.setText(assertion.getProtectedServiceUrl());

        setSelectedPolicy(assertion.getPolicyGuid());

        final HttpMethod method = assertion.getHttpMethod();
        httpMethodComboBox.setSelectedItem(method);
        overrideHttpMethodCheckBox.setSelected(method != null);

        final GenericHttpRequestParams.HttpVersion version = assertion.getHttpVersion();
        httpVersionComboBox.setSelectedItem(version);
        overrideHttpVersionCheckBox.setSelected(version != null);

        useKeepAlivesCheckBox.setSelected(assertion.isUseKeepAlives());
    }

    @Override
    public AsyncHttpRoutingAssertion getData(AsyncHttpRoutingAssertion assertion) throws ValidationException {
        assertion.setProtectedServiceUrl(urlField.getText());

        assertion.setPolicyGuid(getSelectedPolicyGuid());

        assertion.setHttpMethod(overrideHttpMethodCheckBox.isSelected() ? (HttpMethod)httpMethodComboBox.getSelectedItem() : null);

        assertion.setHttpVersion(overrideHttpVersionCheckBox.isSelected() ? (GenericHttpRequestParams.HttpVersion)httpVersionComboBox.getSelectedItem() : null);

        assertion.setUseKeepAlives(useKeepAlivesCheckBox.isSelected());

        if (assertion.getProtectedServiceUrl() == null)
            throw new ValidationException("A URL must be provided.");

        if (assertion.getPolicyGuid() == null)
            throw new ValidationException("A response policy must be selected.");

        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        httpMethodComboBox.setModel(new DefaultComboBoxModel(HttpMethod.values()));
        httpVersionComboBox.setModel(new DefaultComboBoxModel(GenericHttpRequestParams.HttpVersion.values()));
        return contentPane;
    }

    private void loadPolicyHeaders() {
        try {
            policyHeaders = Registry.getDefault().getPolicyAdmin().findPolicyHeadersWithTypes(EnumSet.of(PolicyType.INCLUDE_FRAGMENT), false);
            policyComboBox.setModel(new DefaultComboBoxModel(policyHeaders.toArray(new PolicyHeader[policyHeaders.size()])));
        } catch (FindException e) {
            final String msg = "Unable to read policy headers: " + ExceptionUtils.getMessage(e);
            logger.log(Level.INFO, msg, ExceptionUtils.getDebugException(e));
            showError(msg);
        }
    }

    private void showError(final String msg) {
        DialogDisplayer.showMessageDialog(AsyncHttpRoutingAssertionPropertiesDialog.this, msg, "Error", JOptionPane.ERROR_MESSAGE, null);
    }

    private void setSelectedPolicy(String policyGuid) {
        if (policyGuid == null) {
            policyComboBox.setSelectedItem(null);
            return;
        }
        if (policyHeaders == null) {
            loadPolicyHeaders();
        }
        for (PolicyHeader policyHeader : policyHeaders) {
            if (policyGuid.equals(policyHeader.getGuid())) {
                policyComboBox.setSelectedItem(policyHeader);
                return;
            }
        }

        PolicyHeader header = null;
        try {
            Policy policy = Registry.getDefault().getPolicyAdmin().findPolicyByGuid(policyGuid);
            header = policy == null ? null : new PolicyHeader(policy);
        } catch (FindException e) {
            final String msg = "Unable to read policy header for policy with GUID " + policyGuid + ": " + ExceptionUtils.getMessage(e);
            logger.log(Level.INFO, msg, ExceptionUtils.getDebugException(e));
        }

        if (header == null) {
            header = new PolicyHeader(-1, false, PolicyType.INCLUDE_FRAGMENT, "Policy GUID " + policyGuid, null, policyGuid, null, null, 0, 0, false);
        }

        final PolicyHeader[] headerArray = policyHeaders.toArray(new PolicyHeader[policyHeaders.size() + 1]);
        headerArray[policyHeaders.size()] = header;
        policyComboBox.setModel(new DefaultComboBoxModel(headerArray));
        policyComboBox.setSelectedItem(header);
    }

    public String getSelectedPolicyGuid() {
        PolicyHeader header = (PolicyHeader)policyComboBox.getSelectedItem();
        return header == null ? null : header.getGuid();
    }
}

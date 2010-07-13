package com.l7tech.external.assertions.rawtcp.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion;

import javax.swing.*;
import java.awt.*;

public class SimpleRawTransportAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<SimpleRawTransportAssertion> {
    private JPanel contentPane;
    private JComboBox comboBox1;
    private JComboBox comboBox2;

    public SimpleRawTransportAssertionPropertiesDialog(Window owner, SimpleRawTransportAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    public void setData(SimpleRawTransportAssertion assertion) {
        // TODO
    }

    @Override
    public SimpleRawTransportAssertion getData(SimpleRawTransportAssertion assertion) throws ValidationException {
        // TODO
        return assertion;
    }
}

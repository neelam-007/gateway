package com.l7tech.console.panels;

import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.HttpRoutingServiceParameter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class HttpRoutingAssertionTestParametersDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField paramNameField;
    private JTextField paramValueField;
    private JLabel paramNameLabel;
    private JLabel paramValueLabel;
    private JRadioButton header;
    private JRadioButton query;

    private boolean confirmed;

    private SimpleTableModel<HttpRoutingServiceParameter> serviceParamTableModel;

    private HttpRoutingServiceParameter previousParameter;
    private HttpRoutingServiceParameter parameter;

    public HttpRoutingAssertionTestParametersDialog(final Window owner, final String title, final SimpleTableModel<HttpRoutingServiceParameter> serviceParamTableModel) {
        super(owner, title);
        initializeComponents();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle(title);
        confirmed = false;
        this.serviceParamTableModel = serviceParamTableModel;
    }

    private void initializeComponents() {
        Utilities.setEscKeyStrokeDisposes(this);
        buttonOK.addActionListener(new ActionListener() {
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
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
    }

    private boolean serviceParamExists(@NotNull final HttpRoutingServiceParameter parameter){
        for (int i = 0; i < serviceParamTableModel.getRowCount(); ++i) {
            final HttpRoutingServiceParameter p = serviceParamTableModel.getRowObject(i);
            if (p.equals(parameter)) {
                return true;
            }
        }
        return false;
    }

    private void onOK() {
        if (paramNameField.getText().trim().isEmpty()) {
            DialogDisplayer.showMessageDialog(this,
                    "Please enter valid parameter name.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE, null);
            return;
        }
        HttpRoutingServiceParameter parameter = new HttpRoutingServiceParameter(
                paramNameField.getText().trim(),
                paramValueField.getText().trim(),
                header.isSelected() ? HttpRoutingServiceParameter.HEADER: HttpRoutingServiceParameter.QUERY
        );

        if (serviceParamExists(parameter)) {
            if(previousParameter == null || !previousParameter.equals(parameter)){
                DialogDisplayer.showMessageDialog(this,
                        "The parameter '" + paramNameField.getText().trim() + "' already exists.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE, null);
                return;
            }
        }
        this.parameter = parameter;
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    /**
     * @return true if the user clicked OK, false otherwise.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    public HttpRoutingServiceParameter getParameter(){
        return parameter;
    }

    public void setParameter(final HttpRoutingServiceParameter parameter){
        this.previousParameter = new HttpRoutingServiceParameter(parameter.getName(), parameter.getValue(), parameter.getType());
        this.parameter = parameter;
        paramNameField.setText(parameter.getName());
        paramValueField.setText(parameter.getValue());
        boolean isHeader = parameter.getType().equals(HttpRoutingServiceParameter.HEADER);
        header.setSelected(isHeader);
        query.setSelected(!isHeader);
    }

}

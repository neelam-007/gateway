package com.l7tech.external.assertions.cassandra.console;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolOptions;
import com.l7tech.external.assertions.cassandra.CassandraNamedParameter;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 1/30/14
 * Time: 5:03 PM
 *
 */
public class CassandraNamedParameterDialog extends JDialog {
    private JPanel mainPanel;
    private JTextField parameterNameTextField;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField parameterValueTextField;
    private JComboBox parameterTypeComboBox;

    private static final String TITLE = "Cassandra Named Parameter";

    /** @noinspection ThisEscapedInObjectConstruction*/
    private final InputValidator validator = new InputValidator(this, TITLE);
    private boolean confirmed = false;

    public CassandraNamedParameterDialog(Dialog parent, CassandraNamedParameter cassandraNamedParameter) {
        super(parent, TITLE, true);

        initComponents(cassandraNamedParameter);
        setData(cassandraNamedParameter);
    }

    public CassandraNamedParameterDialog(Dialog parent, CassandraNamedParameter cassandraNamedParameter, boolean edit) {
            super(parent, TITLE, true);

            initComponents(cassandraNamedParameter);
            setData(cassandraNamedParameter);
            if (edit){
                parameterNameTextField.setEditable(false);
            }
        }

    private void initComponents(CassandraNamedParameter cassandraNamedParameter) {
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        validator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
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

        validator.constrainTextFieldToBeNonEmpty("name", parameterNameTextField, null);

        Utilities.setEscKeyStrokeDisposes(this);

        pack();
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void setData(CassandraNamedParameter cassandraNamedParameter) {
        if(cassandraNamedParameter == null) {
            return;
        }

        parameterNameTextField.setText(cassandraNamedParameter.getParameterName());
        parameterValueTextField.setText(cassandraNamedParameter.getParameterValue());

        populateDataTypeComboBox(cassandraNamedParameter);

        pack();
    }

    public CassandraNamedParameter getData(CassandraNamedParameter cassandraNamedParameter) {
        cassandraNamedParameter.setParameterName(parameterNameTextField.getText().trim());
        cassandraNamedParameter.setParameterDataType(((DataType.Name)parameterTypeComboBox.getSelectedItem()).name());
        cassandraNamedParameter.setParameterValue(parameterValueTextField.getText());

        return cassandraNamedParameter;
    }

    private void populateDataTypeComboBox(CassandraNamedParameter cassandraNamedParameter){

        Set<DataType.Name> unsupportedTypes = new HashSet<>();

        unsupportedTypes.add(DataType.Name.LIST);
        unsupportedTypes.add(DataType.Name.MAP);
        unsupportedTypes.add(DataType.Name.SET);

        DefaultComboBoxModel model = new DefaultComboBoxModel();

        for(DataType.Name name: DataType.Name.values()){
            if (unsupportedTypes.contains(name)){
                continue;
            }

            model.addElement(name);
        }

        parameterTypeComboBox.setModel(model);
        parameterTypeComboBox.setSelectedItem(DataType.Name.valueOf(
                cassandraNamedParameter.getParameterDataType()!= null ? cassandraNamedParameter.getParameterDataType(): "TEXT" ));
    }

}

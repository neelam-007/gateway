package com.l7tech.external.assertions.bulkjdbcinsert.console;

import com.l7tech.external.assertions.bulkjdbcinsert.BulkJdbcInsertAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

/**
 * Created by moiyu01 on 15-09-29.
 */
public class BulkJdbcInsertTableMapperDialog extends JDialog{
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.bulkjdbcinsert.console.resources.BulkJdbcInsertPropertiesDialog");

    private JTextField columnNameTextField;
    private JPanel mainPanel;
    private JTextField orderTextField;
    private JLabel transformationLabel;
    private JLabel orderLabel;
    private JLabel columnNameLabel;
    private JTextField paramTextField;
    private JLabel paramLabel;
    private JButton OKButton;
    private JButton cancelButton;
    private JComboBox transformationComboBox;
    private BulkJdbcInsertAssertion.ColumnMapper mapper;
    private boolean confirmed;


    public BulkJdbcInsertTableMapperDialog(final Window owner, final String title, final BulkJdbcInsertAssertion.ColumnMapper mapper) {
        super(owner, title);
        initialize(mapper, title);

    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void initialize(final BulkJdbcInsertAssertion.ColumnMapper mapper, final String title) {
        confirmed = false;
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(cancelButton);
        pack();
        Utilities.centerOnScreen(this);
        Utilities.setEscKeyStrokeDisposes(this);

        this.mapper = mapper;

        final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisableOkButton();
            }
        });
        // todo: test field length verification
        columnNameTextField.getDocument().addDocumentListener(changeListener);
        orderTextField.getDocument().addDocumentListener(changeListener);
        transformationComboBox.setModel(new DefaultComboBoxModel(BulkJdbcInsertAssertion.TRANSFORMATIONS));

        final InputValidator inputValidator = new InputValidator(this, title);
        inputValidator.constrainTextFieldToBeNonEmpty(resources.getString("column.label.name"), columnNameTextField, null);
        inputValidator.constrainTextField(orderTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if(StringUtils.isBlank(orderTextField.getText())) return resources.getString("mapper.order.error.empty");

                try{
                    Integer val = new Integer(orderTextField.getText());
                    if(val == null || val < 0 ) return resources.getString("mapper.order.error.invalid");
                } catch(NumberFormatException e) {
                    return resources.getString("mapper.order.error.invalid");
                }
                return null;
            }
        });

        inputValidator.ensureComboBoxSelection(transformationLabel.getText(),transformationComboBox);

        inputValidator.constrainTextField(paramTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if("Subtract".equals(transformationComboBox.getSelectedItem())) {
                    if(StringUtils.isBlank(paramTextField.getText())) {
                        return resources.getString("mapper.param.error.empty");
                    }
                    else {
                        try {
                            Integer.parseInt(paramTextField.getText());
                        } catch(NumberFormatException e) {
                            return resources.getString("mapper.param.error.invalid");
                        }
                    }
                }

                return null;
            }
        });

        inputValidator.attachToButton(OKButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doOk();
            }
        });

        transformationComboBox.addActionListener(changeListener);

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        modelToView();
        enableOrDisableOkButton();
    }

    private void doCancel() {
        confirmed = false;
        dispose();
    }

    private void doOk() {
        confirmed = true;
        viewToModel();
        dispose();
    }

    private void viewToModel() {
        mapper.setName(columnNameTextField.getText());
        mapper.setOrder(Integer.parseInt(orderTextField.getText()));
        mapper.setTransformation((String)transformationComboBox.getSelectedItem());
        mapper.setTransformParam(paramTextField.getText());
    }

    private void modelToView() {
        columnNameTextField.setText(mapper.getName() != null ? mapper.getName() : "");
        orderTextField.setText(Integer.toString(mapper.getOrder()));
        if(StringUtils.isNotBlank(mapper.getTransformation())) {
            transformationComboBox.setSelectedItem(mapper.getTransformation());
        }
        paramTextField.setText(mapper.getTransformParam() != null ? mapper.getTransformParam() : "");
    }

    private void enableOrDisableOkButton() {
        boolean selected = transformationComboBox.getSelectedIndex() != -1;
        paramTextField.setEnabled(selected && !transformationComboBox.getSelectedItem().equals("String"));
    }




}

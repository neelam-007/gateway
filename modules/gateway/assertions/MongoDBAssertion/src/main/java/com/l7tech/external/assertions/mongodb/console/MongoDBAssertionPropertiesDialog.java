package com.l7tech.external.assertions.mongodb.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.mongodb.MongoDBAssertion;
import com.l7tech.external.assertions.mongodb.MongoDBOperation;
import com.l7tech.external.assertions.mongodb.MongoDBWriteConcern;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntity;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntityAdmin;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.*;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Properties dialog for the JDBC Query Assertion.
 */
public class MongoDBAssertionPropertiesDialog extends AssertionPropertiesEditorSupport<MongoDBAssertion> {

    private static final Logger logger = Logger.getLogger(MongoDBAssertionPropertiesDialog.class.getName());

    private JPanel mainPanel;
    /**
     * Should never have a null selection as a default is always set in @{link #modelToView}
     */
    private JComboBox<String> connectionComboBox;
    private JTextArea sqlQueryTextArea;
    private JButton cancelButton;
    private JButton okButton;
    private JTextField collectionNameTextField;
    private JComboBox operationComboBox;
    private JTextArea updateQueryTextArea;
    private JCheckBox failIfNoResultsCheckBox;
    private JTextField prefixTextField;
    private JTextArea projectionDocumentTextArea;
    private JCheckBox enableMultiCheckBox;
    private JCheckBox enableUpsertCheckBox;
    private JComboBox writeConcernComboBox;
    private MongoDBAssertion assertion;
    private boolean confirmed;



    public MongoDBAssertionPropertiesDialog(Window owner, MongoDBAssertion assertion) {
        super(owner, assertion);
        this.assertion = assertion;
        initialize();
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(MongoDBAssertion assertion) {
        this.assertion = assertion;
        modelToView();
        configureView();
    }

    @Override
    public MongoDBAssertion getData(final MongoDBAssertion assertion) {
        viewToModel(assertion);
        return assertion;
    }

    @Override
    protected void configureView() {
       enableOrDisableOkButton();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.centerOnScreen(this);
        Utilities.setEscKeyStrokeDisposes(this);

        sqlQueryTextArea.setDocument(new MaxLengthDocument(JdbcAdmin.MAX_QUERY_LENGTH));

        final RunOnChangeListener connectionListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisableOkButton();
            }
        });
        ((JTextField)connectionComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(connectionListener);
        connectionComboBox.addItemListener(connectionListener);


        final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableOkButton();
            }
        });

        sqlQueryTextArea.getDocument().addDocumentListener(changeListener);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        populateConnectionCombobox();
        populateOperationComboBox();
        populateWriteConcernComboBox();

        operationComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(operationComboBox.getSelectedItem().equals(MongoDBOperation.UPDATE)){
                    updateQueryTextArea.setEnabled(true);
                    enableMultiCheckBox.setEnabled(true);
                    enableUpsertCheckBox.setEnabled(true);
                } else {
                    updateQueryTextArea.setEnabled(false);
                    enableMultiCheckBox.setEnabled(false);
                    enableUpsertCheckBox.setEnabled(false);
                }

                if(operationComboBox.getSelectedItem().equals(MongoDBOperation.FIND)){
                    projectionDocumentTextArea.setEnabled(true);
                } else {
                    projectionDocumentTextArea.setEnabled(false);
                }
            }
        });
    }

    private void ok(){
        confirmed = true;
        dispose();
    }

    private void modelToView() {
        populateConnectionCombobox();
        populateOperationComboBox();
        populateWriteConcernComboBox();
        collectionNameTextField.setText(assertion.getCollectionName());
        sqlQueryTextArea.setText(assertion.getQueryDocument());
        sqlQueryTextArea.setCaretPosition(0);
        projectionDocumentTextArea.setText(assertion.getProjectionDocument());
        projectionDocumentTextArea.setCaretPosition(0);
        updateQueryTextArea.setText(assertion.getUpdateDocument());
        updateQueryTextArea.setCaretPosition(0);
        failIfNoResultsCheckBox.setSelected(assertion.isFailIfNoResults());
        prefixTextField.setText(assertion.getPrefix());
        enableMultiCheckBox.setSelected(assertion.isEnableMulti());
        enableUpsertCheckBox.setSelected(assertion.isEnableUpsert());
    }

    private void viewToModel(final MongoDBAssertion assertion) {
        assertion.setCollectionName(collectionNameTextField.getText());
        assertion.setConnectionGoid(((MongoDBConnectionEntity)connectionComboBox.getSelectedItem()).getGoid());
        assertion.setOperation(((MongoDBOperation)operationComboBox.getSelectedItem()).name());
        assertion.setWriteConcern(((MongoDBWriteConcern)writeConcernComboBox.getSelectedItem()).name());
        assertion.setQueryDocument(sqlQueryTextArea.getText());
        assertion.setProjectionDocument(projectionDocumentTextArea.getText());
        assertion.setUpdateDocument(updateQueryTextArea.getText());
        assertion.setFailIfNoResults(failIfNoResultsCheckBox.isSelected());
        assertion.setPrefix(prefixTextField.getText());
        assertion.setEnableMulti(enableMultiCheckBox.isSelected());
        assertion.setEnableUpsert(enableUpsertCheckBox.isSelected());
    }

    private void populateConnectionCombobox() {
        MongoDBConnectionEntity selectedConnection = null;

        try {
           DefaultComboBoxModel model = new DefaultComboBoxModel();
           MongoDBConnectionEntityAdmin mongoDBConnectionEntityAdmin = Registry.getDefault().getExtensionInterface(MongoDBConnectionEntityAdmin.class, null);

           for(MongoDBConnectionEntity connection :mongoDBConnectionEntityAdmin.findByType()) {
               model.addElement(connection);
               if (connection.getGoid().equals(assertion.getConnectionGoid())) {
                   selectedConnection = connection;
               }
           }

           connectionComboBox.setModel(model);
           connectionComboBox.setSelectedItem(selectedConnection);
       } catch(FindException fe) {
            logger.log(Level.WARNING, "Calling populateConnectionCombobox().  Cannot find MongoDBConnectionEntity connections by type. ", fe);
       }

    }

    private void populateOperationComboBox(){
        MongoDBOperation selectedOperation = null;

        DefaultComboBoxModel model = new DefaultComboBoxModel();

        for(MongoDBOperation operation: MongoDBOperation.values()){
            model.addElement(operation);
            if(operation.name().equals(assertion.getOperation())){
                selectedOperation = operation;
            }
        }

        operationComboBox.setModel(model);
        operationComboBox.setSelectedItem(selectedOperation);

    }

    private void populateWriteConcernComboBox(){
        MongoDBWriteConcern selectedOperation = null;

        DefaultComboBoxModel model = new DefaultComboBoxModel();

        for(MongoDBWriteConcern writeConcern: MongoDBWriteConcern.values()){
            model.addElement(writeConcern);
            if(writeConcern.name().equals(assertion.getWriteConcern())){
                selectedOperation = writeConcern;
            }
        }

        writeConcernComboBox.setModel(model);
        writeConcernComboBox.setSelectedItem(selectedOperation);

        }

    private void enableOrDisableOkButton() {
        boolean enabled = isNonEmptyRequiredTextField(sqlQueryTextArea.getText()) &&
                isNonEmptyRequiredTextField(collectionNameTextField.getText()) &&
                isNonEmptyRequiredTextField(prefixTextField.getText());

        okButton.setEnabled(enabled);
    }

    private boolean isNonEmptyRequiredTextField(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private void doCancel() {
        MongoDBAssertionPropertiesDialog.this.dispose();
    }

}
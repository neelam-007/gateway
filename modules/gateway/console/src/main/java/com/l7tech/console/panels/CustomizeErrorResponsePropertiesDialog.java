package com.l7tech.console.panels;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.CustomizeErrorResponseAssertion;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Functions;
import com.l7tech.util.NameValuePair;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;

/**
 * Properties dialog for customize error response assertion.
 */
public class CustomizeErrorResponsePropertiesDialog extends AssertionPropertiesOkCancelSupport<CustomizeErrorResponseAssertion>{

    //- PUBLIC

    public CustomizeErrorResponsePropertiesDialog( final Window parent,
                                                            final CustomizeErrorResponseAssertion assertion ) {
        super(CustomizeErrorResponseAssertion.class, parent, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public CustomizeErrorResponseAssertion getData( final CustomizeErrorResponseAssertion assertion ) throws ValidationException {
        assertion.setErrorLevel( (CustomizeErrorResponseAssertion.ErrorLevel) errorLevelComboBox.getSelectedItem() );
        assertion.setHttpStatus( httpStatusTextField.getText() );
        assertion.setContentType( contentTypeTextField.getText() );
        assertion.setContent( contentTextPane.getText() );
        assertion.setIncludePolicyDownloadURL( policyDownloadUrlCheckBox.isSelected() );
        //noinspection unchecked
        assertion.setExtraHeaders(extraHeadersTableModel.getRows().toArray(new NameValuePair[extraHeadersTableModel.getRows().size()]));

        return assertion;
    }

    @Override
    public void setData( final CustomizeErrorResponseAssertion assertion ) {
        if ( assertion.getErrorLevel() != null ) {
            errorLevelComboBox.setSelectedItem( assertion.getErrorLevel() );
        }
        setValue( httpStatusTextField, assertion.getHttpStatus() );
        setValue( contentTypeTextField, assertion.getContentType() );
        setValue( contentTextPane, assertion.getContent() );
        policyDownloadUrlCheckBox.setSelected( assertion.isIncludePolicyDownloadURL() );
        final NameValuePair[] heads = assertion.getExtraHeaders();
        extraHeadersTableModel.setRows(heads == null ? Collections.<NameValuePair>emptyList() : Arrays.asList(heads));

        updateState();
    }

    //- PROTECTED

    @Override
    protected void initComponents() {
        super.initComponents();

        RunOnChangeListener stateUpdateListener = new RunOnChangeListener(){
            @Override
            public void run() {
                updateState();
            }
        };

        extraHeadersTableModel = TableUtil.configureTable(extraHeadersTable,
                TableUtil.column("Name", 10, 100, 99999, Functions.<String, NameValuePair>propertyTransform(NameValuePair.class, "key"), String.class),
                TableUtil.column("Value", 10, 100, 999999, Functions.<String, NameValuePair>propertyTransform(NameValuePair.class, "value"), String.class));
        extraHeadersTable.getSelectionModel().addListSelectionListener(stateUpdateListener);

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showEditHeaderDialog("New Header", new NameValuePair("", ""), new Functions.UnaryVoid<NameValuePair>() {
                    @Override
                    public void call(NameValuePair value) {
                        extraHeadersTableModel.addRow(value);
                    }
                });
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int row = extraHeadersTable.getSelectedRow();
                NameValuePair value = extraHeadersTableModel.getRowObject(row);
                if (value != null) showEditHeaderDialog("Edit Header", value, new Functions.UnaryVoid<NameValuePair>() {
                    @Override
                    public void call(NameValuePair value) {
                        extraHeadersTableModel.setRowObject(row, value);
                    }
                });
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                extraHeadersTableModel.removeRowAt(extraHeadersTable.getSelectedRow());
            }
        });

        errorLevelComboBox.setModel( new DefaultComboBoxModel( CustomizeErrorResponseAssertion.ErrorLevel.values() ) );
        errorLevelComboBox.addActionListener( stateUpdateListener );
        errorLevelComboBox.setRenderer( new TextListCellRenderer<CustomizeErrorResponseAssertion.ErrorLevel>(
                new Functions.Unary<String,CustomizeErrorResponseAssertion.ErrorLevel>(){
                    @Override
                    public String call( final CustomizeErrorResponseAssertion.ErrorLevel errorLevel ) {
                        String text = "";
                        switch ( errorLevel ) {
                            case DROP_CONNECTION:
                                text = "Drop Connection";
                                break;
                            case TEMPLATE_RESPONSE:
                                text = "Template Response";
                                break;
                        }
                        return text;
                    }
                } ) );

        httpStatusTextField.getDocument().addDocumentListener( stateUpdateListener );
        contentTypeTextField.getDocument().addDocumentListener( stateUpdateListener );

        Utilities.setMaxLength( httpStatusTextField.getDocument(), 10000 );
        Utilities.setMaxLength( contentTypeTextField.getDocument(), 10000 );
        Utilities.setMaxLength( contentTextPane.getDocument(), 1000000 );

    }

    private void showEditHeaderDialog(final String title, NameValuePair initialValue, final Functions.UnaryVoid<NameValuePair> actionIfConfirmed) {
        final OkCancelDialog<NameValuePair> dlg = new OkCancelDialog<NameValuePair>(this, title, true, new NameValuePanel(initialValue));
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.wasOKed()) {
                    NameValuePair value = dlg.getValue();
                    if (value != null)
                        actionIfConfirmed.call(value);
                }
            }
        });
    }

    @Override
    protected void configureView() {
        super.configureView();
        updateState();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    //- PRIVATE

    private JPanel mainPanel;
    private JCheckBox policyDownloadUrlCheckBox;
    private JTextPane contentTextPane;
    private JTextField httpStatusTextField;
    private JTextField contentTypeTextField;
    private JComboBox errorLevelComboBox;
    private JLabel errorLevelLabel;
    private JTable extraHeadersTable;
    private JButton addButton;
    private JButton removeButton;
    private JButton editButton;

    private SimpleTableModel<NameValuePair> extraHeadersTableModel;

    private void setValue( final JTextComponent component, final String text ) {
        if ( text != null ) {
            component.setText( text );
            component.setCaretPosition( 0 );
        }
    }

    private void updateState() {
        boolean canOk = true;

        boolean enableResponseControls =
                errorLevelComboBox.getSelectedItem() == CustomizeErrorResponseAssertion.ErrorLevel.TEMPLATE_RESPONSE;

        Utilities.setEnabled( mainPanel, enableResponseControls );
        if ( !enableResponseControls ) {
            mainPanel.setEnabled( true );
            errorLevelLabel.setEnabled( true );
            errorLevelComboBox.setEnabled( true );
        } else {
            if ( !httpStatusTextField.getText().contains( Syntax.SYNTAX_PREFIX )  ) {
                canOk = ValidationUtils.isValidInteger( httpStatusTextField.getText(), false, 100, 599 );
            }
            if ( canOk && !contentTypeTextField.getText().contains( Syntax.SYNTAX_PREFIX )) {
                try {
                    final ContentTypeHeader cth = ContentTypeHeader.parseValue( contentTypeTextField.getText() );
                    final String charsetName = cth.getParam("charset");
                    if ( charsetName != null ) {
                        try {
                            final Charset charset = Charset.forName(charsetName);
                            canOk = charset.canEncode();
                        } catch ( IllegalArgumentException iae ) {
                            canOk = false;
                        }
                    }
                } catch ( IOException e ) {
                    canOk = false;
                }
            }
        }

        boolean headerSelected = extraHeadersTable.getSelectedRow() >= 0;
        editButton.setEnabled(headerSelected);
        removeButton.setEnabled(headerSelected);

        getOkButton().setEnabled( !isReadOnly() && canOk );
    }
}

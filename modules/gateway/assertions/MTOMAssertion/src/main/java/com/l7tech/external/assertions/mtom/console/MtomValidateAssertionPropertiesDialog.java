package com.l7tech.external.assertions.mtom.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.mtom.MtomValidateAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.util.Functions;
import com.l7tech.xml.xpath.XpathExpression;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import java.util.Arrays;
import java.util.Collections;

/**
 *
 */
public class MtomValidateAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<MtomValidateAssertion> {

    //- PUBLIC

    public MtomValidateAssertionPropertiesDialog( final Window parent,
                                                  final MtomValidateAssertion assertion ) {
        super(MtomValidateAssertion.class, parent, bundle.getString("dialog.title"), true);
        initComponents();
        setData(assertion);
    }

    @Override
    public MtomValidateAssertion getData( final MtomValidateAssertion assertion ) throws ValidationException {
        assertion.setRequireEncoded( requireEncodedCheckBox.isSelected() );
        assertion.setValidationRules( validationRuleTableModel.getRows().toArray(
                new MtomValidateAssertion.ValidationRule[validationRuleTableModel.getRowCount()]));
        return assertion;
    }

    @Override
    public void setData( final MtomValidateAssertion assertion ) {
        requireEncodedCheckBox.setSelected( assertion.isRequireEncoded() );
        if ( assertion.getValidationRules()==null ) {
            validationRuleTableModel.setRows( Collections.<MtomValidateAssertion.ValidationRule>emptyList() );
        } else {
            validationRuleTableModel.setRows( Arrays.asList( assertion.getValidationRules() ) );
        }
        enableAndDisableControls();
    }

    //- PROTECTED

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        addButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                addValidationRule();
            }
        } );

        editButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                editValidationRule();
            }
        } );

        removeButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                removeValidationRule();
            }
        } );

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                enableAndDisableControls();
            }
        } );

        validationRuleTableModel = TableUtil.configureTable(
                validationRulesTable,
                TableUtil.column(bundle.getString( "table.xpath" ), 40, 240, 100000, new Functions.Unary<Object,MtomValidateAssertion.ValidationRule>() {
                    @Override
                    public Object call(MtomValidateAssertion.ValidationRule rule) {
                        return rule.getXpathExpression()==null ? "" : rule.getXpathExpression().getExpression();
                    }
                }),
                TableUtil.column(bundle.getString( "table.itemCount" ), 40, 80, 180, property("count"), Integer.class),
                TableUtil.column(bundle.getString( "table.itemSize" ), 40, 80, 180, new Functions.Unary<Long,MtomValidateAssertion.ValidationRule>() {
                    @Override
                    public Long call(MtomValidateAssertion.ValidationRule rule) {
                        return rule.getSize() / 1024L;
                    }
                }, Long.class)
        );
        validationRulesTable.setModel( validationRuleTableModel );
        validationRulesTable.getTableHeader().setReorderingAllowed( false );
        validationRulesTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        validationRulesTable.getSelectionModel().addListSelectionListener( enableDisableListener );
        Utilities.setDoubleClickAction( validationRulesTable, editButton );
    }

    //- PRIVATE

    private static final ResourceBundle bundle = ResourceBundle.getBundle( MtomValidateAssertionPropertiesDialog.class.getName());

    private JPanel mainPanel;
    private JCheckBox requireEncodedCheckBox;
    private JTable validationRulesTable;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private SimpleTableModel<MtomValidateAssertion.ValidationRule> validationRuleTableModel;

    private void enableAndDisableControls() {
        getOkButton().setEnabled( !isReadOnly() );

        boolean itemSelected = validationRulesTable.getSelectedRow()>-1;
        addButton.setEnabled( !isReadOnly() );
        editButton.setEnabled( itemSelected );
        removeButton.setEnabled( !isReadOnly() && itemSelected );
    }

    private void addValidationRule() {
        final MtomValidateAssertion.ValidationRule rule = new MtomValidateAssertion.ValidationRule();
        rule.setXpathExpression( new XpathExpression("//*[xop:Include]", Collections.singletonMap("xop", "http://www.w3.org/2004/08/xop/include")));
        final OkCancelDialog<MtomValidateAssertion.ValidationRule> dialog =
                buildDialog( rule );
        DialogDisplayer.display( dialog, new Runnable(){
            @Override
            public void run() {
                if ( dialog.wasOKed() ) {
                    validationRuleTableModel.addRow( rule );
                }
            }
        } );
    }

    private void editValidationRule() {
        final int viewRow = validationRulesTable.getSelectedRow();
        if ( viewRow > -1 ) {
            final int modelRow = validationRulesTable.convertRowIndexToModel( viewRow );
            final OkCancelDialog<MtomValidateAssertion.ValidationRule> dialog =
                    buildDialog( validationRuleTableModel.getRowObject(modelRow) );
            DialogDisplayer.display( dialog, new Runnable(){
                @Override
                public void run() {
                    if ( dialog.wasOKed() ) {
                        validationRuleTableModel.fireTableRowsUpdated(modelRow,modelRow);
                    }
                }
            } );
        }
    }

    private OkCancelDialog<MtomValidateAssertion.ValidationRule> buildDialog( final MtomValidateAssertion.ValidationRule rule ) {
        OkCancelDialog<MtomValidateAssertion.ValidationRule> dialog =
                OkCancelDialog.createOKCancelDialog(
                        this,
                        getTitle() + " - " + bundle.getString( "dialog.rule.label" ),
                        true,
                        new ValidationRulePanel( rule ) );

        dialog.pack();
        Utilities.centerOnParentWindow( dialog );

        return dialog;
    }

    private void removeValidationRule() {
        final int viewRow = validationRulesTable.getSelectedRow();
        if ( viewRow > -1 ) {
            final int modelRow = validationRulesTable.convertRowIndexToModel( viewRow );
            validationRuleTableModel.removeRowAt( modelRow );
        }
    }

    private static Functions.Unary<Integer,MtomValidateAssertion.ValidationRule> property(String propName) {
        return Functions.propertyTransform(MtomValidateAssertion.ValidationRule.class, propName);
    }


}

package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.util.ValidationUtils;
import com.l7tech.util.ClassUtils;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.util.Map;
import java.util.ResourceBundle;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 2-Apr-2009
 * Time: 6:06:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class XacmlRequestBuilderXmlContentPanel extends JPanel implements XacmlRequestBuilderNodePanel {
    private static final ResourceBundle resources = ResourceBundle.getBundle( XacmlRequestBuilderDialog.class.getName() );

    private JTable xmlAttributesTable;
    private JButton addButton;
    private JButton modifyButton;
    private JButton removeButton;
    private JTextArea contentField;
    private JCheckBox repeatCheckBox;
    private JPanel settingsPanel;
    private JPanel mainPanel;
    private JPanel attributesPanel;
    private JPanel contentPanel;

    private XacmlRequestBuilderAssertion.GenericXmlElementHolder genericXmlElementHolder;
    private DefaultTableModel tableModel;
    private JDialog window;

    /**
     *
     * @param genericXmlElementHolder
     * @param xacmlVersion of the request being generated
     * @param window
     */
    public XacmlRequestBuilderXmlContentPanel(
            XacmlRequestBuilderAssertion.GenericXmlElementHolder genericXmlElementHolder,
            XacmlAssertionEnums.XacmlVersionType xacmlVersion,
            JDialog window) {
        this.genericXmlElementHolder = genericXmlElementHolder;
        this.window = window;

        tableModel = new DefaultTableModel(new Object[] {"Name", "Value"}, 0){
            @Override
            public boolean isCellEditable( int row, int column ) {
                return false;
            }
        };
        for(Map.Entry<String, String> entry : genericXmlElementHolder.getAttributes().entrySet()) {
            tableModel.addRow(new Object[] {entry.getKey(), entry.getValue()});
        }
        xmlAttributesTable.setModel(tableModel);
        xmlAttributesTable.getTableHeader().setReorderingAllowed( false );

        contentField.setText(genericXmlElementHolder.getContent());
        contentField.setCaretPosition( 0 );

        boolean instanceOfRepeatTag =
                genericXmlElementHolder instanceof XacmlRequestBuilderAssertion.XmlElementCanRepeatTag;
        if(instanceOfRepeatTag){
            XacmlRequestBuilderAssertion.XmlElementCanRepeatTag repeatTag =
                    (XacmlRequestBuilderAssertion.XmlElementCanRepeatTag) genericXmlElementHolder;
            repeatCheckBox.setSelected(repeatTag.isCanElementHaveSameTypeSibilings());
        }else{
            repeatCheckBox.setSelected(false);    
        }
        init(xacmlVersion == XacmlAssertionEnums.XacmlVersionType.V2_0 && instanceOfRepeatTag,
                ClassUtils.getInnerClassName(genericXmlElementHolder.getClass()).toLowerCase());
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void init( final boolean showSettings,
                      final String resourcePrefix ) {
        ((TitledBorder)attributesPanel.getBorder()).setTitle( resources.getString( resourcePrefix+".attributes" ));
        ((TitledBorder)contentPanel.getBorder()).setTitle( resources.getString( resourcePrefix+".content" ));

        xmlAttributesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        xmlAttributesTable.getSelectionModel().addListSelectionListener( new ListSelectionListener(){
            @Override
            public void valueChanged( ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        } );

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                boolean done = false;
                String name = "";
                String value = "";
                while ( !done ) {
                    XacmlRequestBuilderXmlAttrPanel dialog = new XacmlRequestBuilderXmlAttrPanel(window, name, value);
                    dialog.setVisible(true);

                    if( dialog.isConfirmed() ) {
                        name = dialog.getName();
                        value = dialog.getValue();

                        if ( validateAttributeName( null, name ) ) {
                            done = true;
                            tableModel.addRow(new Object[] {name, value});
                            genericXmlElementHolder.getAttributes().put(name, value);
                        }
                    } else {
                        done = true;
                    }
                }
            }
        });

        modifyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                int selectedRow = xmlAttributesTable.getSelectedRow();
                if(selectedRow == -1) {
                    return;
                }

                boolean done = false;
                final String originalName = (String)xmlAttributesTable.getValueAt(selectedRow, 0);
                String name = originalName;
                String value = (String)xmlAttributesTable.getValueAt(selectedRow, 1);
                while ( !done ) {
                    XacmlRequestBuilderXmlAttrPanel dialog = new XacmlRequestBuilderXmlAttrPanel(window, name, value);
                    dialog.setVisible(true);

                    if ( dialog.isConfirmed() ) {
                        name = dialog.getName();
                        value = dialog.getValue();

                        if ( validateAttributeName( originalName, name ) ) {
                            done = true;
                            xmlAttributesTable.setValueAt(name, selectedRow, 0);
                            xmlAttributesTable.setValueAt(value, selectedRow, 1);

                            genericXmlElementHolder.getAttributes().remove(originalName);
                            genericXmlElementHolder.getAttributes().put(name, value);
                        }
                    } else {
                        done = true;
                    }
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                int selectedRow = xmlAttributesTable.getSelectedRow();
                if(selectedRow == -1) {
                    return;
                }

                String name = (String)xmlAttributesTable.getValueAt(selectedRow, 0);
                tableModel.removeRow(selectedRow);
                genericXmlElementHolder.getAttributes().remove(name);
            }
        });

        contentField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                genericXmlElementHolder.setContent(contentField.getText().trim());
            }
        }));

        repeatCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if(genericXmlElementHolder instanceof XacmlRequestBuilderAssertion.XmlElementCanRepeatTag){
                    XacmlRequestBuilderAssertion.XmlElementCanRepeatTag repeatTag =
                            (XacmlRequestBuilderAssertion.XmlElementCanRepeatTag) genericXmlElementHolder;
                    repeatTag.setCanElementHaveSameTypeSibilings(repeatCheckBox.isSelected());
                }
            }
        });

        settingsPanel.setVisible(showSettings);

        Utilities.setDoubleClickAction( xmlAttributesTable, modifyButton );
        enableOrDisableButtons();
    }

    @Override
    public boolean handleDispose() {
        return true;
    }

    private void enableOrDisableButtons() {
        boolean enable = xmlAttributesTable.getSelectedRow() > -1;
        modifyButton.setEnabled( enable );
        removeButton.setEnabled( enable );
    }

    private boolean validateAttributeName( final String originalName, final String name ) {
        boolean valid = false;

        if ( (originalName==null || !originalName.equals(name)) && isDuplicateAttributeName(name)) {
            JOptionPane.showMessageDialog(window,
                "The attribute name '" + name + "' already exists.  Please use a new attribute name and try again.",
                "Duplicate Attribute Name", JOptionPane.ERROR_MESSAGE, null );
        } else if ( !isProbablyValidXmlNameOrVariable( name )) {
            JOptionPane.showMessageDialog(window,
                "The attribute name '" + name + "' is not valid.  Please modify the attribute name and try again.",
                "Invalid Attribute Name", JOptionPane.ERROR_MESSAGE, null );
        } else {
            valid = true;
        }

        return valid;
    }

    private boolean isProbablyValidXmlNameOrVariable( final String name ) {
        // Replace variables with a valid name character so we can validate
        // assuming that the runtime value will be good.
        String nameWithoutVariables = Syntax.regexPattern.matcher( name ).replaceAll( "a" );        
        return  ValidationUtils.isProbablyValidXmlName( nameWithoutVariables );
    }

    private boolean isDuplicateAttributeName(String attrName) {
        return genericXmlElementHolder.getAttributes().containsKey(attrName);
    }
}

package com.l7tech.console.panels;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.PauseListenerAdapter;
import com.l7tech.gui.util.TextComponentPauseListenerManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Logger;

/**
 * This class is similar with {@link com.l7tech.console.panels.AssertionKeyAliasEditor}
 *
 * User: ghuang
 */
public class ResolveContextVariablesPanel extends JDialog {
    private final Logger logger = Logger.getLogger(ResolveContextVariablesPanel.class.getName());
    private static final String TITLE = "Resolve Context Variables";

    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JTable table;
    private JTextPane textPane;

    private InputValidator validators;
    private String[] values= null;
    private String [] varsUsed = null;
    private boolean wasOked;
    private int currentSelection = -1;

    public ResolveContextVariablesPanel(Window owner, String[] varsUsed) throws HeadlessException {
        super(owner, TITLE, SampleMessageDialog.DEFAULT_MODALITY_TYPE);
        this.varsUsed = varsUsed;

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        validators = new InputValidator( this, getTitle() );

        values = new String[varsUsed.length];
        table.setModel(new ContextVarsTableModel());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = table.getSelectedRow();
                if(currentSelection >= 0){
                    values[currentSelection] = textPane.getText().trim();
                }
                if(row <0)
                    textPane.setEnabled(false);
                else {
                    textPane.setEnabled(true);
                    textPane.setText(values[row]);
                }
                currentSelection = row;
            }
        });

        TextComponentPauseListenerManager.registerPauseListener(
            textPane,
            new PauseListenerAdapter() {
                @Override
                public void textEntryPaused(JTextComponent component, long msecs) {
                    int row = table.getSelectedRow();
                    if(row >= 0){
                        values[row] = textPane.getText();
                        table.tableChanged(new TableModelEvent(table.getModel(),row));
                    }
                }
            },
            300);

        validators.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                for(int rowIndex = 0; rowIndex < table.getRowCount(); ++rowIndex){
                    if( (values[rowIndex]).isEmpty()){
                        return "Please enter value for "+table.getValueAt(rowIndex,1);
                    }
                }
                return null;
            }
        });
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(currentSelection >= 0){
                    values[currentSelection] = textPane.getText();
                }
                String err = validators.validate();
                if (err !=null){
                    JOptionPane.showMessageDialog(getOwner(), err, getTitle(), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                wasOked = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                wasOked = false;
                dispose();
            }
        });

        pack();
        int row = table.getSelectedRow();
        if(row <0)
            textPane.setEnabled(false);

    }

    public Map<String, Object> getValues(){
        Map<String, Object> values = new HashMap<String, Object>();
        for(int rowIndex = 0; rowIndex < table.getRowCount(); ++rowIndex){
            values.put(this.varsUsed[rowIndex],this.values[rowIndex]);
        }
        return values;
    }

    public boolean getWasOked() {
        return wasOked;
    }

    private class ContextVarsTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return varsUsed.length;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return varsUsed[rowIndex];
                case 1:
                    return values[rowIndex];
                default:
                    throw new IllegalArgumentException("No Column" + columnIndex);
            }
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
                case 0:
                    return "Context Variable";
                case 1:
                    return "Value";
                default:
                    throw new IllegalArgumentException("No Column" + column);
            }
        }
    }
}

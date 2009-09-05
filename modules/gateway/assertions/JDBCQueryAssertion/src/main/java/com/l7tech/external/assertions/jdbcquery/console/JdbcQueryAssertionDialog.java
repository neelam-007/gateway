package com.l7tech.external.assertions.jdbcquery.console;

import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.external.assertions.jdbcquery.JDBCQueryAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Properties dialog for the JDBC Query Assertion.
 */
public class JdbcQueryAssertionDialog extends AssertionPropertiesEditorSupport<JDBCQueryAssertion> {

    protected static final Logger logger = Logger.getLogger(JdbcQueryAssertionDialog.class.getName());
    private JDBCQueryAssertion assertion;
    private boolean wasOkButtonPressed = false;
    private EventListenerList listenerList = new EventListenerList();

    private JTextField userTextField;
    private JPasswordField passwordTextField;
    private JTextField driverTextField;
    private JTextField connectionUrlTextField;
    private JTextArea sqlTextArea;

    private JButton testButton;
    private JButton cancelButton;
    private JButton okButton;

    private JPanel mainPanel;
    private JTable variableTable;
    private VariableMapTableModel model;
    private JTextField variablePrefixTextField;
    private JButton generateButton;
    private JButton newButton;
    private JButton editButton;
    private JButton deleteButton;



    private final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
        public void run() {
            configureView();
        }
    });

    public JdbcQueryAssertionDialog(Frame owner, JDBCQueryAssertion assertion) {
        super(owner, "JDBC Query Properties", true);
        this.initComponents();
        this.setData(assertion);
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(mainPanel);
        setMinimumSize(new Dimension(600, 400));
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);

        userTextField.getDocument().addDocumentListener(changeListener);
        passwordTextField.getDocument().addDocumentListener(changeListener);
        driverTextField.getDocument().addDocumentListener(changeListener);
        sqlTextArea.getDocument().addDocumentListener(changeListener);
        variablePrefixTextField.getDocument().addDocumentListener(changeListener);

        variableTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        variableTable.setModel(getTableModel());

        testButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onTest();
            }
        });

        testButton.setVisible(false);//TODO disabled for now until a new SSG Admin method is added

        generateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onGenerate();
            }
        });

        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onNew();
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onEdit();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onDelete();
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        enableDisableComponents();
    }

    public VariableMapTableModel getTableModel(){
        if(model == null){
            model = new VariableMapTableModel();
        }
        return model;
    }

    private void fireEventAssertionChanged(final Assertion a) {
        final CompositeAssertion parent = a.getParent();
        if (parent == null)
            return;

        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        int[] indices = new int[parent.getChildren().indexOf(a)];
                        PolicyEvent event = new PolicyEvent(this, new AssertionPath(a.getPath()), indices, new Assertion[]{a});
                        EventListener[] listeners = listenerList.getListeners(PolicyListener.class);
                        for (EventListener listener : listeners) {
                            ((PolicyListener) listener).assertionsChanged(event);
                        }
                    }
                });
    }

    //initialize form from data
    private void initFormData(final JDBCQueryAssertion assertion) {
        connectionUrlTextField.setText(assertion.getConnectionUrl());
        driverTextField.setText(assertion.getDriver());
        userTextField.setText(assertion.getUser());
        passwordTextField.setText(assertion.getPass());
        sqlTextArea.setText(assertion.getSql());
        variablePrefixTextField.setText(assertion.getVariablePrefix());

        //populate the context variables table from the variablesToSet
        setVariableTable(assertion);
    }

    //update data from form
    private void saveFormData(final JDBCQueryAssertion assertion) {
        assertion.setConnectionUrl(connectionUrlTextField.getText());
        assertion.setDriver(driverTextField.getText());
        assertion.setUser(userTextField.getText());
        assertion.setPass(new String(passwordTextField.getPassword()));
        assertion.setSql(sqlTextArea.getText());
        assertion.setVariablePrefix(variablePrefixTextField.getText());
        assertion.setVariableMap(buildVariableMap());
    }

    @Override
    protected void configureView() {
        boolean enableOkButton = userTextField.getText().length() > 0 &&
                driverTextField.getText().length() > 0 &&
                connectionUrlTextField.getText().length() > 0 &&
                sqlTextArea != null && sqlTextArea.getText().length() > 0 &&
                variablePrefixTextField != null && variablePrefixTextField.getText().length() > 0;

        enableDisableComponents();

        enableOkButton &= !isReadOnly();
        okButton.setEnabled(enableOkButton);
    }

    private void enableDisableComponents() {
        String user = userTextField.getText();
        String pass = new String(passwordTextField.getPassword());
        String driver = driverTextField.getText();
        String connectionUrl = connectionUrlTextField.getText();

        if (Syntax.getReferencedNames(user).length > 0 ||
                Syntax.getReferencedNames(pass).length > 0 ||
                Syntax.getReferencedNames(driver).length > 0 ||
                Syntax.getReferencedNames(connectionUrl).length > 0) {

            testButton.setEnabled(false);
        } else {
            testButton.setEnabled(true);
        }

    }

    private void setVariableTable(final JDBCQueryAssertion assertion) {
        Map<String, String> variableMap = assertion.getVariableMap();
        model.clearMappings();
        
        Set<Map.Entry<String, String>> entries = variableMap.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            model.addMapping(new Pair<String, String>(entry.getKey(), entry.getValue()));
        }
        model.update(variablePrefixTextField.getText());
    }

    /* map of variables set by this assertion
    * key = table column name
    * value = context variable name
    */
    private Map<String, String> buildVariableMap() {
        Map<String, String> variableMap = new HashMap<String, String>();
        java.util.List<Pair<String, String>> mappings = model.getMappings();

        for(Pair<String, String> tuple : mappings){
            variableMap.put(tuple.getKey(), tuple.getValue());
        }

        return variableMap;
    }


    public boolean isConfirmed() {
        return wasOkButtonPressed;
    }

    public void setData(JDBCQueryAssertion assertion) {
        this.assertion = assertion;
        initFormData(assertion);
    }

    public JDBCQueryAssertion getData(final JDBCQueryAssertion assertion) {
        saveFormData(assertion);
        return assertion;
    }

    private void onTest() {
        //attempt to create a connection
        Connection conn = null;
        String msg = "Connection information is valid.";

        try {
            Class.forName(driverTextField.getText());
            conn = DriverManager.getConnection(connectionUrlTextField.getText(), userTextField.getText(), new String(passwordTextField.getPassword()));

            //TODO validate the SQL

            JOptionPane.showMessageDialog(this, msg, "Connection Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (ClassNotFoundException cnfe) {
            msg = ExceptionUtils.getMessage(cnfe, cnfe.getMessage());
            logger.log(Level.WARNING, msg, cnfe);
            JOptionPane.showMessageDialog(this, msg, "Driver Error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException sqle) {
            msg = ExceptionUtils.getMessage(sqle, sqle.getMessage());
            logger.log(Level.WARNING, msg, sqle);
            JOptionPane.showMessageDialog(this, msg, "Connection Error", JOptionPane.WARNING_MESSAGE);
        } finally {
            ResourceUtils.closeQuietly(conn);
        }
    }

    public static final Pattern GREP_COLUMNS_PATTERN = Pattern.compile("^select\\s+(.*?)\\s+from", Pattern.CASE_INSENSITIVE);
    public static final Pattern GREP_ALIAS_PATTERN = Pattern.compile("^(.*?)\\s+as\\s+(.*?)$", Pattern.CASE_INSENSITIVE);

    private void onGenerate() {
        model.clearMappings();
        //populate the context variables table based on SELECT queries only
        String sql = sqlTextArea.getText();
        if (sql == null || sql.length() <= 0 || !JDBCQueryAssertion.SELECT_PATTERN.matcher(sql).find()) {
            return;
        }

        Matcher columnMatcher = GREP_COLUMNS_PATTERN.matcher(sql);
        if (!columnMatcher.find()) {
            //show a dialog explaining that the user should create their column to variable mapping manually
            JOptionPane.showMessageDialog(this, "Unable to generate variable names. Verify your SQL syntax and/or enter the variable names manually.");
            return;
        }

        //create an array of all the columns in the query
        String[] columns = columnMatcher.group(1).split(",\\s*");

        //evaluate any aliases
        String columnName;
        for (String column : columns) {
            Matcher aliasMatcher = GREP_ALIAS_PATTERN.matcher(column);
            if (aliasMatcher.matches()) {
                columnName = aliasMatcher.group(2);
            } else {
                columnName = column;
            }
            model.addMapping(new Pair<String, String>(columnName, columnName));
        }
        model.update(variablePrefixTextField.getText());
    }

    private void onNew() {
        VariableMappingDialog vmd = new VariableMappingDialog(this, "New Variable Mapping");
        vmd.setVisible(true);

        if(vmd.isConfirmed()){
            model.addMapping(new Pair<String, String>(vmd.getColumnName(), vmd.getVariableSuffixName()));
            model.update(variablePrefixTextField.getText());
        }

        vmd.dispose();
    }

    private void onEdit() {
        int row = variableTable.getSelectedRow();
        if(row == -1) return;

        Pair<String, String> selectedTuple = model.getMappings().get(row);
        String colName = selectedTuple.getKey();
        String varName = selectedTuple.getValue();

        VariableMappingDialog vmd = new VariableMappingDialog(this, "Edit Variable Mapping", colName, varName);
        vmd.setVisible(true);

        if(vmd.isConfirmed()){
            model.updateMapping(row, new Pair<String, String>(vmd.getColumnName(), vmd.getVariableSuffixName()));
            model.update(variablePrefixTextField.getText());
        }

        vmd.dispose();
    }

    private void onDelete() {
        int row = variableTable.getSelectedRow();
        if(row == -1) return;

        model.removeMapping(row);
        model.update(variablePrefixTextField.getText());
    }

    private void onOK() {
        getData(assertion);
        fireEventAssertionChanged(assertion);
        wasOkButtonPressed = true;
        JdbcQueryAssertionDialog.this.dispose();
    }

    private void onCancel() {
        JdbcQueryAssertionDialog.this.dispose();
    }
}

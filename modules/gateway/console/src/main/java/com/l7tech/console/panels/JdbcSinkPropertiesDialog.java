package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.audit.JdbcAuditSink;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;


/**
 *
 */
public class JdbcSinkPropertiesDialog extends JDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.JdbcSinkPropertiesDialog");

    private JButton OKButton;
    private JButton cancelButton;
    private JComboBox jdbcConnectionComboBox;
    private JButton manageJDBCConnectionsButton;
    private JButton testButton;
    private JButton createSchemaButton;
    private JButton exportSchemaButton;
    private JCheckBox fallbackToInternalConnectionCheckBox;
    private JCheckBox allAuditsCheckBox;
    private JCheckBox metricsCheckBox;
    private JCheckBox messageProcessingAuditsOnlyCheckBox;
    private JPanel mainPanel;
    private JCheckBox enableCheckbox;
    private JCheckBox otherAuditsCheckBox;
    private JTextField nameField;

    private boolean confirmed = false;
    private JdbcAuditSink auditSink;

    public JdbcSinkPropertiesDialog( final Window parent, JdbcAuditSink auditSink) {
        super( parent, resources.getString("title"), JDialog.DEFAULT_MODALITY_TYPE );

        initalize();
        modelToView(auditSink);
        this.auditSink = auditSink;
    }

    private void initalize(){
        setContentPane(mainPanel);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        RunOnChangeListener enableDisableListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisableComponents();
            }
        });
        allAuditsCheckBox.addActionListener(enableDisableListener);
        metricsCheckBox.addActionListener(enableDisableListener);
        messageProcessingAuditsOnlyCheckBox.addActionListener(enableDisableListener);
        otherAuditsCheckBox.addActionListener(enableDisableListener);
        jdbcConnectionComboBox.addActionListener(enableDisableListener);


        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                dispose();
            }
        });

        OKButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        testButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onTest();
            }
        });

         createSchemaButton.addActionListener( new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 onCreateSchema();
             }
         });

        exportSchemaButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onExportSchema();
            }
        });

        manageJDBCConnectionsButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JdbcConnectionManagerWindow dialog = new JdbcConnectionManagerWindow(TopComponents.getInstance().getTopParent());
                dialog.pack();
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog);
                loadJdbcConnections();
                pack();
            }
        });

        loadJdbcConnections();

        pack();
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.setMinimumSize(this);
        Utilities.centerOnParentWindow( this );
    }


    private void onCreateSchema() {
        // todo
    }
    private void onExportSchema()  {
        // todo
    }
    private void onTest(){
        final AuditAdmin admin = getAuditAdmin();
        if (admin == null) return;

        // Assign data to a tested JDBC sink
        final JdbcAuditSink sink = new JdbcAuditSink();
        viewToModel(sink);
        String result = null;
        try {
            result = doAsyncAdmin(
                    admin,
                    JdbcSinkPropertiesDialog.this,
                    resources.getString("message.testing.progress"),
                    resources.getString("message.testing"),
                    admin.testJdbcAuditSink(sink)).right();

            String message = result.isEmpty() ?
                    resources.getString("message.testing.jdbc.conn.passed") : MessageFormat.format(resources.getString("message.testing.jdbc.conn.failed"), result);

            DialogDisplayer.showMessageDialog(this, message, resources.getString("dialog.title.jdbc.conn.test"),
                    result.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE, null);


        } catch (InterruptedException e) {
            // do nothing, user cancelled

        } catch (InvocationTargetException e) {
            result = resources.getString("message.testing.jdbc.conn.failed");
            DialogDisplayer.showMessageDialog(this, MessageFormat.format(result, e.getMessage()),
                    resources.getString("dialog.title.jdbc.conn.test"),
                    JOptionPane.WARNING_MESSAGE, null);
        } catch (RuntimeException e) {
            result = resources.getString("message.testing.jdbc.conn.failed");
            DialogDisplayer.showMessageDialog(this, MessageFormat.format(result, e.getMessage()),
                    resources.getString("dialog.title.jdbc.conn.test"),
                    JOptionPane.WARNING_MESSAGE, null);
        }
        
        if(result==null){
            // if successful
            createSchemaButton.setEnabled(true);
            exportSchemaButton.setEnabled(true);
        }
    }

    private AuditAdmin getAuditAdmin(){
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getAuditAdmin();
    }

    private void onOk() {
        String err = validateInputs();
        if(err!=null)
        {
            DialogDisplayer.showMessageDialog(this,resources.getString("error.title"),err,null);
        }
        viewToModel(auditSink);
        confirmed = true;
        dispose();
    }

    private String validateInputs() {
        // todo
        // The user can create a maximum of one (enabled) sink for each type of data to be output
        // (so a maximum of 4) and each sink must be for a different JDBC connection
        // (or for the special "<internal>" connection to use the Gateways own database)
        return null;
    }

    private void viewToModel(JdbcAuditSink sink) {
        List<String> outputs = new ArrayList<String>();
        if(allAuditsCheckBox.isSelected()){
            if (messageProcessingAuditsOnlyCheckBox.isSelected())
                outputs.add(JdbcAuditSink.outputOptionMessageProcessingAudits);
            else
                outputs.add(JdbcAuditSink.outputOptionAllAudits);
        }
        if(metricsCheckBox.isSelected())
            outputs.add(JdbcAuditSink.outputOptionMetrics);
        if(otherAuditsCheckBox.isSelected())
            outputs.add(JdbcAuditSink.outputOtherAudits);

        sink.setOutputList(outputs);
        sink.setFallbackToInternal(fallbackToInternalConnectionCheckBox.isSelected());
        sink.setEnabled(enableCheckbox.isSelected());
        sink.setConnectionName((String) jdbcConnectionComboBox.getSelectedItem());
        sink.setName(nameField.getText());
        
        
    }

    private void enableDisableComponents() {
        messageProcessingAuditsOnlyCheckBox.setEnabled(allAuditsCheckBox.isSelected());
        createSchemaButton.setEnabled(false);
        exportSchemaButton.setEnabled(false);
    }

    private void loadJdbcConnections() {
        List<String> jdbcConnections = new ArrayList<String>();
        jdbcConnections.add("<internal>");

        try {
            jdbcConnections.addAll(Registry.getDefault().getJdbcConnectionAdmin().getAllJdbcConnectionNames());
        } catch (FindException e) {
            // todo
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        jdbcConnectionComboBox.setModel(new DefaultComboBoxModel(jdbcConnections.toArray()));
    }

    private void modelToView(JdbcAuditSink auditSink) {
        nameField.setText(auditSink.getName());
        enableCheckbox.setSelected(auditSink.isEnabled());
        fallbackToInternalConnectionCheckBox.setSelected(auditSink.isFallbackToInternal());
        jdbcConnectionComboBox.setSelectedItem(auditSink.getConnectionName());

        metricsCheckBox.setSelected(auditSink.isOutputOptionEnabled(JdbcAuditSink.outputOptionMetrics));
        allAuditsCheckBox.setSelected(auditSink.isOutputOptionEnabled(JdbcAuditSink.outputOptionAllAudits));
        otherAuditsCheckBox.setSelected(auditSink.isOutputOptionEnabled(JdbcAuditSink.outputOtherAudits));
        messageProcessingAuditsOnlyCheckBox.setSelected(auditSink.isOutputOptionEnabled(JdbcAuditSink.outputOptionMessageProcessingAudits));
    }

    public void selectNameField(){
        nameField.requestFocus();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public JdbcAuditSink getSinkConfiguration(){
        return auditSink;
    }
}

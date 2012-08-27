package com.l7tech.console.panels;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.transport.http.HttpAdmin;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.HttpRoutingServiceParameter;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;

import static com.l7tech.util.Functions.propertyTransform;

public class HttpRoutingAssertionTestDialog extends JDialog {
    public HttpRoutingAssertionTestDialog(final Window owner, final HttpRoutingAssertion assertion, final String[] ipListToTest) {
        super(owner, DIALOG_TITLE);
        this.assertion = assertion;
        this.ipListToTest = ipListToTest;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        Set<HttpMethod> methods = EnumSet.allOf(HttpMethod.class);
        methods.removeAll(Arrays.asList(HttpMethod.OTHER)); // Omit methods not supports by Commons HTTP client
        requestMethodComboBox.setModel(new DefaultComboBoxModel(methods.toArray()));

        listContentType.setModel(new DefaultComboBoxModel(contentTypes));

        intializeServiceParametersSection(owner);
        //get the needed data from the assertion
        setData(assertion);
        //initially set enabled flags
        updateTestBodyState();

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        final ActionListener requestMethodListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTestBodyState();
            }
        };
        forceIncludeRequestBodyCheckBox.addActionListener(requestMethodListener);
        requestMethodComboBox.addActionListener(requestMethodListener);

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public boolean isClosedOnOk() {
        return closedOnOk;
    }

    public String getTestContentType() {
        return listContentType.getSelectedItem().toString();
    }

    public String getTestBodyMessage() {
        return textBody.getText();
    }

    public HttpMethod getHttpMethod() {
        return (HttpMethod) requestMethodComboBox.getSelectedItem();
    }

    public boolean isForceIncludeRequestBody() {
        return forceIncludeRequestBodyCheckBox.isSelected();
    }

    public List<HttpRoutingServiceParameter> getParameters() {
        return serviceParamTableModel.getRows();
    }

    private void onOK() {
        closedOnOk = true;
        final JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        final CancelableOperationDialog cancelDialog =
                new CancelableOperationDialog(null, resources.getString("dialog.test.title"), resources.getString("dialog.test.progress"), progressBar);
        cancelDialog.pack();
        cancelDialog.setModal(true);
        Utilities.centerOnScreen(cancelDialog);

        final HttpRoutingAssertion assertionTest = assertion.clone();

        assertionTest.setTestBodyMessage(getTestBodyMessage());
        assertionTest.setHttpMethod(getHttpMethod());
        assertionTest.setForceIncludeRequestBody(isForceIncludeRequestBody());

        List<HttpPassthroughRule> headerRules = new ArrayList<HttpPassthroughRule>();
        List<HttpPassthroughRule> paramRules = new ArrayList<HttpPassthroughRule>();
        boolean contentTypeOverride=false;
        List<HttpRoutingServiceParameter> parameters = getParameters();
        if (parameters != null && parameters.size() > 0) {
            for (HttpRoutingServiceParameter param : parameters) {
                HttpPassthroughRule rule = new HttpPassthroughRule(param.getName(), true, param.getValue());
                if("content-type".equalsIgnoreCase(param.getName())){//although we have a dropdown of content-type which the system know, we will let the user override it if they want to, if they added content-type header
                    contentTypeOverride=true;
                }
                if (param.getValue() == null) {//we can't pass a null value for the rule, make this use use the original value w/c is really nothing since we are testing
                    rule.setUsesCustomizedValue(false);
                }
                if (HttpRoutingServiceParameter.HEADER.equals(param.getType())) {
                    headerRules.add(rule);
                } else {
                    paramRules.add(rule);
                }
            }
        }
        if (!contentTypeOverride && getTestContentType() != null) {
            HttpPassthroughRule rule = new HttpPassthroughRule("Content-Type", true, getTestContentType());
            headerRules.add(rule);
        }
        //disable forwardAll for header & param rules, this will allow us to easily use the customize parameters for the test using the rules
        assertionTest.getRequestHeaderRules().setForwardAll(false);//allows us to set the headers
        assertionTest.getRequestParamRules().setForwardAll(true);//allows us to pass all the parameters set
        //set the rules for the test, if any was set
        assertionTest.getRequestHeaderRules().setRules(headerRules.toArray(new HttpPassthroughRule[headerRules.size()]));

        StringBuilder queryParam = new StringBuilder();
        String key = null;
        String value;
        try {
            for (HttpRoutingServiceParameter p : parameters) {
                if (p.getType().equals(HttpRoutingServiceParameter.QUERY)) {
                    key = p.getName();
                    value = p.getValue();
                    queryParam.append(URLEncoder.encode(key, URL_ENCODING)).append("=").append(URLEncoder.encode(value, URL_ENCODING)).append("&");
                }
            }
            if (queryParam.length() > 0) {
                queryParam = queryParam.delete(queryParam.length() - 1, queryParam.length());
            }
        } catch (UnsupportedEncodingException e) {
            JOptionPane.showMessageDialog(
                    HttpRoutingAssertionTestDialog.this,
                    "Parameter value of  " + key + " contains an Unsupported Encoding ",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        final String[] ipListToTestWithQuery = ipListToTest.clone();
        if(queryParam.length()>0){
            int counter=0;
            for(String url: ipListToTest){            
                if(url.indexOf('?')>0){
                    url=url+"&"+queryParam.toString();
                } else {
                    url=url+"?"+queryParam.toString();
                }
                ipListToTestWithQuery[counter++]=url;
            }
        }
        
        final StringBuffer responseStr = new StringBuffer();
        final Callable<Boolean> callable = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                HttpAdmin admin = getHttpAdmin();
                responseStr.append(admin.testConnection(ipListToTestWithQuery, textBody.getText(), assertionTest));
                return Boolean.TRUE;
            }
        };

        boolean testOkay = false;
        try {
            final Boolean result = Utilities.doWithDelayedCancelDialog(callable, cancelDialog, 500L);
            if (result == Boolean.TRUE) {
                Object[] options = {"Ok",
                        "View Response"};
                int selection = JOptionPane.showOptionDialog(
                        HttpRoutingAssertionTestDialog.this,
                        resources.getString("dialog.test.result.gateway.success") + (ipListToTest.length > 1 ? "s." : "."),
                        resources.getString("dialog.test.result.success"),
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null,options, options[0]);
                testOkay = true;
                if(selection==1){
                    JPanel responsePanel = new JPanel();
                    responsePanel.setLayout(new BoxLayout(responsePanel, BoxLayout.Y_AXIS));
                    if (responseStr.toString() != null && responseStr.toString().length() != 0) {
                        responsePanel.add(Box.createVerticalStrut(10));
                        JTextArea responseLog = new JTextArea(responseStr.toString());
                        responseLog.setAlignmentX(Component.LEFT_ALIGNMENT);
                        responseLog.setBorder(BorderFactory.createEtchedBorder());
                        responseLog.setEditable(false);
                        responseLog.setEnabled(true);
                        responseLog.setColumns(80);
                        responseLog.setRows(20);
                        responseLog.setLineWrap(true);
                        responseLog.setFont(new Font(null, Font.PLAIN, 11));
                        JScrollPane scrollPane = new JScrollPane(responseLog);
                        setPreferredSize(new Dimension(450, 110));
                        responsePanel.add(scrollPane);
                    }
                    JOptionPane.showMessageDialog(
                            HttpRoutingAssertionTestDialog.this,
                            responsePanel,
                            resources.getString("dialog.test.response.log"),
                            JOptionPane.INFORMATION_MESSAGE);
                }//end of if(selection==1)
            }
        } catch (InterruptedException e) {
            // Swing thread interrupted.
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause != null) {
                if (cause instanceof HttpAdmin.HttpAdminException) {
                    final HttpAdmin.HttpAdminException fte = (HttpAdmin.HttpAdminException) cause;
                    JPanel panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                    panel.add(new JLabel(resources.getString("dialog.test.result.gateway.failure") + (ipListToTest.length > 1 ? "s." : ".")));
                    panel.add(new JLabel(fte.getMessage()));
                    if (fte.getSessionLog() != null && fte.getSessionLog().length() != 0) {
                        panel.add(Box.createVerticalStrut(10));
                        panel.add(new JLabel(resources.getString("dialog.test.detail.log")));
                        JTextArea sessionLog = new JTextArea(fte.getSessionLog());
                        sessionLog.setAlignmentX(Component.LEFT_ALIGNMENT);
                        sessionLog.setBorder(BorderFactory.createEtchedBorder());
                        sessionLog.setEditable(false);
                        sessionLog.setEnabled(true);
                        sessionLog.setFont(new Font(null, Font.PLAIN, 11));
                        panel.add(sessionLog);
                    }
                    JOptionPane.showMessageDialog(
                            HttpRoutingAssertionTestDialog.this,
                            panel,
                            resources.getString("dialog.test.result.failure"),
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    throw ExceptionUtils.wrap(cause);
                }
            }
        }

        //if test was okay, we can safely close & save the values, if not allow the user to change the values
        if (testOkay) {
            dispose();
        }
    }

    private HttpAdmin getHttpAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent()) {
            return null;
        }
        return reg.getHttpAdmin();
    }

    private void onCancel() {
        closedOnOk = false;
        dispose();
    }

    private void updateTestBodyState() {
        if (forceIncludeRequestBodyCheckBox.isSelected()) {
            textBody.setEnabled(true);
        } else if (requestMethodComboBox.getSelectedItem() == HttpMethod.POST || requestMethodComboBox.getSelectedItem() == HttpMethod.PUT) {
            textBody.setEnabled(true);
        } else {
            textBody.setEnabled(false);
        }
    }

    private void intializeServiceParametersSection(final Window owner) {
        serviceParams.getTableHeader().setReorderingAllowed(false);
        removeParam.setEnabled(false);
        editParam.setEnabled(false);

        serviceParams.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        serviceParams.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        serviceParams.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                boolean enabled = serviceParams.getSelectedRow() >= 0;
                removeParam.setEnabled(enabled);
                editParam.setEnabled(enabled);
            }
        });

        addParam.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                HttpRoutingAssertionTestParametersDialog dspd = new HttpRoutingAssertionTestParametersDialog(owner, DIALOG_TITLE_NEW_PARAMETER, serviceParamTableModel);
                dspd.pack();
                Utilities.centerOnScreen(dspd);
                dspd.setVisible(true);
                if (dspd.isConfirmed()) {
                    serviceParamTableModel.addRow(dspd.getParameter());
                }
            }
        });

        editParam.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                editServerParameter(owner);
            }
        });

        Utilities.setDoubleClickAction(serviceParams, editParam);
        removeParam.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                int selectedIndex = serviceParams.getSelectedRow();
                if (selectedIndex >= 0) {
                    int index = serviceParams.getRowSorter().convertRowIndexToModel(selectedIndex);
                    serviceParamTableModel.removeRowAt(index);
                }
            }
        });
    }

    private void editServerParameter(final Window owner) {
        final int selectedIndex = serviceParams.getSelectedRow();
        if (selectedIndex >= 0) {
            int index = serviceParams.getRowSorter().convertRowIndexToModel(selectedIndex);
            HttpRoutingAssertionTestParametersDialog dspd = new HttpRoutingAssertionTestParametersDialog(owner, DIALOG_TITLE_EDIT_PARAMETER, serviceParamTableModel);
            dspd.pack();
            Utilities.centerOnScreen(dspd);

            HttpRoutingServiceParameter param = serviceParamTableModel.getRowObject(index);
            dspd.setParameter(param);
            dspd.setVisible(true);
            if (dspd.isConfirmed()) {
                serviceParamTableModel.setRowObject(index, dspd.getParameter());
            }
        }
    }

    public void setData(HttpRoutingAssertion assertion) {
        serviceParamTableModel = TableUtil.configureTable(
                serviceParams,
                // Update column indexes above if changing or reordering (COLUMN_FILE_NAME, etc)
                TableUtil.column(PARAMETER_NAME, 40, 80, 999999, stringProperty("name")),
                TableUtil.column(PARAMETER_VALUE, 40, 80, 999999, stringProperty("value")),
                TableUtil.column(PARAMETER_TYPE, 40, 80, 999999, stringProperty("type"))
        );


        TableRowSorter sorter = new TableRowSorter<TableModel>(serviceParamTableModel);
        sorter.setSortsOnUpdates(true);
        sorter.setComparator(0, new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                //should we consider the Locale?
                return o1.toLowerCase().compareTo(o2.toLowerCase());
            }
        });


        serviceParams.setModel(serviceParamTableModel);
        sorter.setSortable(0, true);
        sorter.toggleSortOrder(0);
        serviceParams.setRowSorter(sorter);

        //we will assume in first instance of this assertion on creation, testContentType is empty. we will use it to trigger pre-load of defaults parameters by copying in from the assertion rules property.
        if (assertion.getTestContentType() == null) {
            if (assertion.getRequestHeaderRules().getRules().length > 0) {
                for (int i = 0; i < assertion.getRequestHeaderRules().getRules().length; i++) {
                    HttpPassthroughRule rule = assertion.getRequestHeaderRules().getRules()[i];
                    HttpRoutingServiceParameter param = new HttpRoutingServiceParameter();
                    param.setName(rule.getName());
                    param.setType(HttpRoutingServiceParameter.HEADER);
                    param.setValue(rule.getCustomizeValue());
                    serviceParamTableModel.addRow(param);
                }
                for (int i = 0; i < assertion.getRequestParamRules().getRules().length; i++) {
                    HttpPassthroughRule rule = assertion.getRequestParamRules().getRules()[i];
                    HttpRoutingServiceParameter param = new HttpRoutingServiceParameter();
                    param.setName(rule.getName());
                    param.setType(HttpRoutingServiceParameter.QUERY);
                    param.setValue(rule.getCustomizeValue());
                    serviceParamTableModel.addRow(param);
                }
            }
            forceIncludeRequestBodyCheckBox.setSelected(assertion.isForceIncludeRequestBody());
            if (assertion.getHttpMethod() == null) {
                requestMethodComboBox.setSelectedItem(HttpMethod.POST);
            } else {
                requestMethodComboBox.setSelectedItem(assertion.getHttpMethod());
            }
        } else {
            forceIncludeRequestBodyCheckBox.setSelected(assertion.isTestForceIncludeRequestBody());
            listContentType.setSelectedItem(assertion.getTestContentType());
            requestMethodComboBox.setSelectedItem(assertion.getTestHttpMethod());
        }
        textBody.setText(new String(assertion.getTestBodyMessage()));

        for (HttpRoutingServiceParameter row : assertion.getTestParameters()) {
            serviceParamTableModel.addRow(row);
        }
    }

    private static Functions.Unary<String, HttpRoutingServiceParameter> stringProperty(final String propName) {
        return propertyTransform(HttpRoutingServiceParameter.class, propName);
    }

    private boolean closedOnOk = false;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton removeParam;
    private JButton addParam;
    private JButton editParam;
    private JTable serviceParams;
    private JComboBox requestMethodComboBox;
    private JCheckBox forceIncludeRequestBodyCheckBox;
    private JTextArea textBody;
    private JComboBox listContentType;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.HttpRoutingAssertionDialog");

    private static final String DIALOG_TITLE = "HTTP Assertion Test Properties";

    private static final String DIALOG_TITLE_NEW_PARAMETER = "Add New Service Parameter";
    private static final String DIALOG_TITLE_EDIT_PARAMETER = "Edit Service Parameter";

    private static final String PARAMETER_NAME = "Parameter Name";
    private static final String PARAMETER_VALUE = "Parameter Value";
    private static final String PARAMETER_TYPE = "Parameter Type";

    private static final String URL_ENCODING = "UTF-8";

    private static final String[] contentTypes = {"text/xml; charset=utf-8", "text/plain; charset=utf-8", "application/x-www-form-urlencoded", "application/soap+xml; charset=utf-8", "application/json; charset=utf-8", "application/octet-stream"};

    private SimpleTableModel<HttpRoutingServiceParameter> serviceParamTableModel;
    private HttpRoutingAssertion assertion;
    private String[] ipListToTest;
}

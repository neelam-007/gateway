package com.l7tech.external.assertions.siteminder.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;

import com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion;
import com.l7tech.external.assertions.siteminder.SiteMinderCheckProtectedAssertion;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 7/12/13
 */
public class SiteMinderCheckProtectedPropertiesDialog extends AssertionPropertiesOkCancelSupport<SiteMinderCheckProtectedAssertion> {

    private static final Pattern AGENTID_PATTERN = Pattern.compile("^\\s*([a-zA-Z0-9]+).name\\s*=",Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);

    private JPanel propertyPanel;
    private JComboBox agentComboBox;
    private JTextField resourceTextField;
    private JComboBox actionComboBox;
    private TargetVariablePanel prefixTargetVariablePanel;
    private final InputValidator inputValidator;
    private ClusterStatusAdmin clusterStatusAdmin;

    public SiteMinderCheckProtectedPropertiesDialog(final Frame owner, final SiteMinderCheckProtectedAssertion assertion) {
        super(SiteMinderCheckProtectedAssertion.class, owner, assertion, true);
        inputValidator = new InputValidator(this, getTitle());
        initComponents();

    }

    @Override
    protected void initComponents() {
        super.initComponents();
        initAdminConnection();
        prefixTargetVariablePanel.setVariable(SiteMinderAuthenticateAssertion.DEFAULT_PREFIX);
        prefixTargetVariablePanel.setDefaultVariableOrPrefix(SiteMinderAuthenticateAssertion.DEFAULT_PREFIX);
        DefaultComboBoxModel<String> agentComboBoxModel = new DefaultComboBoxModel<>();
        populateAgentComboBoxModel(agentComboBoxModel);
        agentComboBox.setModel(agentComboBoxModel);

        DefaultComboBoxModel<String> actionComboBoxModel = new DefaultComboBoxModel<>(new String[]{"GET","POST","PUT"});
        actionComboBox.setModel(actionComboBoxModel);

        prefixTargetVariablePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                enableDisableComponents();
            }
        });

        actionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });

        inputValidator.constrainTextFieldToBeNonEmpty("Protected Resource", resourceTextField, null);
        inputValidator.ensureComboBoxSelection("Action", actionComboBox);
        inputValidator.attachToButton(getOkButton(), super.createOkAction());

    }

    /**
     * populates agent combo box with agent IDs
     * @param agentComboBoxModel
     */
    private void populateAgentComboBoxModel(DefaultComboBoxModel<String> agentComboBoxModel) {

        try {
            java.util.List<String> agents = getSiteMinderAdmin().getAllSiteMinderConfigurationNames();
            for (String agent: agents) {
                agentComboBoxModel.addElement(agent);
            }

        } catch (FindException e) {
            //do not throw any exceptions at this point. leave agent combo box empty
        }
    }

    private void enableDisableComponents() {
        getOkButton().setEnabled(prefixTargetVariablePanel.isEntryValid() && actionComboBox.getSelectedIndex() > -1 && !resourceTextField.getText().isEmpty());
    }
    /**
     * Configure the view with the data from the specified assertion bean.
     * This call should immediately configure all the editor widgets, before returning.
     *
     * @param assertion the assertion bean that provides that data that should be copied into the view.  Must not be null.
     */
    @Override
    public void setData(SiteMinderCheckProtectedAssertion assertion) {
        agentComboBox.setSelectedItem(assertion.getAgentID());
        resourceTextField.setText(assertion.getProtectedResource());
        actionComboBox.getModel().setSelectedItem(assertion.getAction());

        if(assertion.getPrefix() != null && !assertion.getPrefix().isEmpty()) {
            prefixTargetVariablePanel.setVariable(assertion.getPrefix());
        }
        else {
            prefixTargetVariablePanel.setVariable(SiteMinderAuthenticateAssertion.DEFAULT_PREFIX);
        }
    }

    /**
     * Copy the data out of the view into an assertion bean instance.
     * The provided bean should be filled and returned, if possible, but implementors may create and return
     * a new bean instead, if they must.
     *
     * @param assertion a bean to which the data from the view can be copied, if possible.  Must not be null.
     * @return a possibly-new assertion bean populated with data from the view.  Not necessarily the same bean that was passed in.
     *         Never null.
     * @throws com.l7tech.console.panels.AssertionPropertiesOkCancelSupport.ValidationException
     *          if the data cannot be collected because of a validation error.
     */
    @Override
    public SiteMinderCheckProtectedAssertion getData(SiteMinderCheckProtectedAssertion assertion) throws ValidationException {
        assertion.setAgentID((String)agentComboBox.getSelectedItem());
        assertion.setProtectedResource(resourceTextField.getText());
        assertion.setAction((String) actionComboBox.getSelectedItem());
        assertion.setPrefix(prefixTargetVariablePanel.getVariable());
        return assertion;
    }

    /**
     * Create a panel to edit the properties of the assertion bean.  This panel does not include any
     * Ok or Cancel buttons.
     *
     * @return a panel that can be used to edit the assertion properties.  Never null.
     */
    @Override
    protected JPanel createPropertyPanel() {
        return propertyPanel;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected ActionListener createOkAction() {
        // returns a no-op action so we can add our own Ok listener
        return new RunOnChangeListener();
    }

    private void initAdminConnection() {
        clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();
    }

    private SiteMinderAdmin getSiteMinderAdmin(){
        return Registry.getDefault().getSiteMinderConfigurationAdmin();
    }


}

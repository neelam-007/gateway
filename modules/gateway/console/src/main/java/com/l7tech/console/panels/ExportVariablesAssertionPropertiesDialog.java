package com.l7tech.console.panels;

import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.gui.widgets.JCheckBoxListModel;
import com.l7tech.policy.assertion.ExportVariablesAssertion;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ExportVariablesAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<ExportVariablesAssertion> {
    private JPanel mainPanel;
    private JList variablesList;

    private JCheckBoxListModel variablesListModel = new JCheckBoxListModel(Collections.<JCheckBox>emptyList());

    public ExportVariablesAssertionPropertiesDialog(Window parent, ExportVariablesAssertion assertion) {
        super(ExportVariablesAssertion.class, parent, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData(ExportVariablesAssertion assertion) {
        Set<String> setVars = SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion).keySet();
        Set<String> usedVars = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        usedVars.addAll(Arrays.asList(assertion.getExportedVars()));

        Set<String> allVars = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        allVars.addAll(setVars);
        allVars.addAll(usedVars);

        List<JCheckBox> boxes = new ArrayList<JCheckBox>();
        for (String varName : allVars)
            boxes.add(new JCheckBox(varName, usedVars.contains(varName)));

        variablesListModel = new JCheckBoxListModel(boxes);
        variablesListModel.attachToJList(variablesList);
    }

    @Override
    public ExportVariablesAssertion getData(ExportVariablesAssertion assertion) throws ValidationException {
        List<JCheckBox> boxes = variablesListModel.getAllCheckedEntries();
        List<String> labels = Functions.map(boxes, Functions.<String,JCheckBox>propertyTransform(JCheckBox.class, "label"));
        assertion.setExportedVars(labels.toArray(new String[labels.size()]));
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }
}

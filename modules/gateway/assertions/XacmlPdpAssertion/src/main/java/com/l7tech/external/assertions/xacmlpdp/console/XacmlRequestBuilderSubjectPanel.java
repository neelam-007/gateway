package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.gui.util.RunOnChangeListener;

import javax.swing.*;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 2-Apr-2009
 * Time: 5:01:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class XacmlRequestBuilderSubjectPanel extends JPanel implements XacmlRequestBuilderNodePanel {
    private JPanel mainPanel;
    private JComboBox subjectCategoryComboBox;

    private XacmlRequestBuilderAssertion.Subject subject;

    public XacmlRequestBuilderSubjectPanel( final XacmlRequestBuilderAssertion.Subject subject ) {
        this.subject = subject;
        
        init();

        subjectCategoryComboBox.setSelectedItem( subject.getSubjectCategory() );
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void init() {
        subjectCategoryComboBox.setModel( new DefaultComboBoxModel( XacmlConstants.XACML_10_SUBJECTCATEGORIES.toArray() ) );
        subjectCategoryComboBox.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                subject.setSubjectCategory(((String)subjectCategoryComboBox.getSelectedItem()).trim());
            }
        }));
    }

    @Override
    public boolean handleDispose(final XacmlRequestBuilderDialog builderDialog) {
        // Access editor directly to get the current text
        subject.setSubjectCategory(((String)subjectCategoryComboBox.getEditor().getItem()).trim());

        return true;
    }
}

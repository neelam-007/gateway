package com.l7tech.external.assertions.apiportalintegration.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.apiportalintegration.ApiPortalEncassIntegrationAssertion;
import com.l7tech.gateway.common.admin.EncapsulatedAssertionAdmin;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

/**
 * This will list the encasses that use the containing policy. This is just informative. There are no properties to set
 * here.
 */
public class ApiPortalEncassIntegrationPropertiesDialog extends AssertionPropertiesEditorSupport<ApiPortalEncassIntegrationAssertion> {
    private JList<String> encassList;
    private JPanel mainPane;
    private JButton closeButton;

    public ApiPortalEncassIntegrationPropertiesDialog(final Frame parent, final ApiPortalEncassIntegrationAssertion assertion) {
        super(parent, (String) assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME));
        initComponents(assertion);
    }

    @Override
    public boolean isConfirmed() {
        return true;
    }

    @Override
    public void setData(ApiPortalEncassIntegrationAssertion assertion) {
    }

    @Override
    public ApiPortalEncassIntegrationAssertion getData(ApiPortalEncassIntegrationAssertion assertion) {
        return assertion;
    }

    protected void initComponents(final ApiPortalEncassIntegrationAssertion assertion) {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(mainPane, BorderLayout.CENTER);
        Utilities.setEscKeyStrokeDisposes(this);

        //load the list of encapsulated assertions that use this policy
        try {
            final Goid policyGoid = assertion.ownerPolicyGoid();
            if (policyGoid != null) {
                EncapsulatedAssertionAdmin encapsulatedAssertionAdmin = getEncapsulatedAssertionAdmin();
                final Collection<EncapsulatedAssertionConfig> encapsulatedAssertionConfigs = encapsulatedAssertionAdmin.findByPolicyGoid(policyGoid);
                encassList.setModel(new DefaultListModel<String>() {{
                    if (encapsulatedAssertionConfigs != null && !encapsulatedAssertionConfigs.isEmpty()) {
                        for (EncapsulatedAssertionConfig encapsulatedAssertionConfig : encapsulatedAssertionConfigs) {
                            addElement(encapsulatedAssertionConfig.getName() + " : " + encapsulatedAssertionConfig.getGuid());
                        }
                    }
                }});
            }
        } catch (final FindException e) {
            //Show an error message, it's not the end of the world if we can't show the list of Encapsulated Assertions
            encassList.setModel(new DefaultListModel<String>() {{
                addElement("Error loading list of Encapsulated assertions using this policy: " + e.getMessage());
            }});
        }
        encassList.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                //This makes the list un-selectable
            }
        });
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                ApiPortalEncassIntegrationPropertiesDialog.this.dispose();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
        pack();
    }

    private static EncapsulatedAssertionAdmin getEncapsulatedAssertionAdmin() {
        return Registry.getDefault().getEncapsulatedAssertionAdmin();
    }
}

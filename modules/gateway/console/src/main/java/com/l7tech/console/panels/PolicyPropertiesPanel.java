package com.l7tech.console.panels;

import com.l7tech.console.action.EditServiceProperties;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gui.util.DocumentSizeFilter;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;

/**
 * @author alex
 */
public class PolicyPropertiesPanel extends ValidatedPanel {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.PolicyPropertiesPanel");

    private JPanel mainPanel;
    private JTextField nameField;
    private JTextField guidField;
    private JCheckBox soapCheckbox;
    private JComboBox typeCombo;
    private JComboBox tagCombo;
    private JLabel unsavedWarningLabel;
    private JTextField oidField;
    private SecurityZoneWidget zoneControl;
    // TODO include a policy panel

    private final Policy policy;
    private final boolean canUpdate;
    private final Map<PolicyType,Map<String, String>> policyTagsByType = new HashMap<PolicyType,Map<String, String>>();
    
    private RunOnChangeListener syntaxListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            checkSyntax();
        }
    });

    public static OkCancelDialog<Policy> makeDialog(Frame owner, Policy policy, boolean canUpdate) {
        return new OkCancelDialog<Policy>(owner, resources.getString("dialog.title"), true, new PolicyPropertiesPanel(policy, canUpdate));
    }

    public PolicyPropertiesPanel(Policy policy, boolean canUpdate) {
        super("policy");
        this.policy = policy;

        if (areUnsavedChangesToThisPolicy(policy)) {
            this.canUpdate = false;
            unsavedWarningLabel.setText(resources.getString("unsaved.warning"));
        } else if (looksLikeAuditSinkPolicy(policy)) {
            this.canUpdate = false;
            unsavedWarningLabel.setText(resources.getString("auditsink.warning"));
        } else if (looksLikeAuditLookupPolicy(policy)) {
            this.canUpdate = false;
            unsavedWarningLabel.setText(resources.getString("auditlookup.warning"));
        } else if (looksLikeDebugTracePolicy(policy)) {
            this.canUpdate = false;
            unsavedWarningLabel.setText(resources.getString("debugtrace.warning"));
        } else {
            unsavedWarningLabel.setVisible(false);
            this.canUpdate = canUpdate;
        }

        init();
    }

    private boolean looksLikeAuditSinkPolicy(Policy policy) {
        return policy != null &&
               PolicyType.INTERNAL.equals(policy.getType()) &&
                ExternalAuditStoreConfigWizard.INTERNAL_TAG_AUDIT_SINK.equals(policy.getInternalTag());
    }

    private boolean looksLikeAuditLookupPolicy(Policy policy) {
        return policy != null &&
                PolicyType.INTERNAL.equals(policy.getType()) &&
                ExternalAuditStoreConfigWizard.INTERNAL_TAG_AUDIT_LOOKUP.equals(policy.getInternalTag());
    }

    private boolean looksLikeDebugTracePolicy(Policy policy) {
        return policy != null &&
               PolicyType.INTERNAL.equals(policy.getType()) &&
               EditServiceProperties.INTERNAL_TAG_TRACE_POLICY.equals(policy.getInternalTag());
    }

    private static boolean areUnsavedChangesToThisPolicy(Policy policy) {
        PolicyEditorPanel pep = TopComponents.getInstance().getPolicyEditorPanel();
        return pep != null && Goid.equals(policy.getGoid(), pep.getPolicyGoid()) && pep.isUnsavedChanges();
    }

    @Override
    protected Object getModel() {
        return policy;
    }

    @Override
    protected void initComponents() {
        PolicyType selectedType = policy.getType();
        java.util.List<PolicyType> types = new ArrayList<PolicyType>();
        for ( PolicyType type : PolicyType.values()) {
            if (type.isShownInGui() || selectedType==type) types.add(type);
        }
        Collections.sort( types, new ResolvingComparator<PolicyType,String>(new Resolver<PolicyType,String>(){
            @Override
            public String resolve( final PolicyType key ) {
                return key.getName().toLowerCase();
            }
        }, false) );

        typeCombo.setModel(new DefaultComboBoxModel(types.toArray(new PolicyType[types.size()])));

        final String policyInternalTag = policy.getInternalTag();
        for ( final PolicyType type : PolicyType.values() ) {
            final Map<String,String> policyTags = new LinkedHashMap<String, String>();
            this.policyTagsByType.put( type, policyTags );

            if ( type == PolicyType.INTERNAL ) {
                ServiceAdmin svcManager = Registry.getDefault().getServiceManager();
                Set<ServiceTemplate> templates = svcManager.findAllTemplates();
                for (ServiceTemplate template : templates) {
                    Map<String, String> templateTags = template.getPolicyTags();
                    if (templateTags != null) {
                        policyTags.putAll(templateTags);
                    }
                }

                if (looksLikeAuditSinkPolicy(policy) && policyInternalTag != null)
                    policyTags.put(policyInternalTag, null);

                if (looksLikeAuditLookupPolicy(policy) && policyInternalTag != null)
                    policyTags.put(policyInternalTag, null);

            }

            for ( final String tag : type.getGuiTags() ) {
                policyTags.put( tag, null );
            }
        }

        final List<String> tagList = new ArrayList<String>();
        tagList.addAll( policyTagsByType.get(selectedType).keySet());
        tagCombo.setModel(new DefaultComboBoxModel(tagList.toArray(new String[tagList.size()])));

        if (policyInternalTag != null)
            tagCombo.setSelectedItem(policyInternalTag);
        else
            tagCombo.setSelectedIndex(-1);
        
        // The max length of a policy name is 255. 
        ((AbstractDocument)nameField.getDocument()).setDocumentFilter(new DocumentSizeFilter(255));

        guidField.setText(policy.getGuid() == null ? "" : policy.getGuid());
        guidField.putClientProperty(Utilities.PROPERTY_CONTEXT_MENU_AUTO_SELECT_ALL, "true");
        Utilities.attachDefaultContextMenu(guidField);

        oidField.setText(Goid.isDefault(policy.getGoid()) ? "" : policy.getId());
        oidField.putClientProperty(Utilities.PROPERTY_CONTEXT_MENU_AUTO_SELECT_ALL, "true");
        Utilities.attachDefaultContextMenu(oidField);

        nameField.setEditable(nameField.isEditable() && canUpdate);
        soapCheckbox.setEnabled(soapCheckbox.isEnabled() && canUpdate);
        typeCombo.setEnabled(typeCombo.isEnabled() && canUpdate);

        nameField.setText(policy.getName());
        soapCheckbox.setSelected(policy.isSoap());
        typeCombo.setSelectedItem(policy.getType());

        soapCheckbox.addChangeListener(syntaxListener);
        typeCombo.addItemListener(syntaxListener);
        tagCombo.addItemListener(syntaxListener);
        nameField.getDocument().addDocumentListener(syntaxListener);

        zoneControl.configure(Goid.isDefault(policy.getGoid()) ? OperationType.CREATE : canUpdate ? OperationType.UPDATE : OperationType.READ, policy);

        typeCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    enableDisable();
                }
            }
        });
        enableDisable();

        // select name field for create
        if(Goid.isDefault(policy.getGoid())){
            nameField.selectAll();
        }

        checkSyntax();

        add(mainPanel, BorderLayout.CENTER);
    }

    private void enableDisable() {
        PolicyType type = (PolicyType) typeCombo.getSelectedItem();
        zoneControl.setEnabled(type != null && type.isSecurityZoneable());
        zoneControl.setToolTipText(zoneControl.isEnabled() ? null : "Policy type not zoneable");
        enableTagChooser();
    }

    private void enableTagChooser() {
        final PolicyType policyType = (PolicyType) typeCombo.getSelectedItem();
        final boolean enableTags = !policyTagsByType.get( policyType ).isEmpty() && canUpdate;
        if ( enableTags ) {
            final List<String> tagList = new ArrayList<String>();
            tagList.addAll( policyTagsByType.get(policyType).keySet());
            tagCombo.setModel(new DefaultComboBoxModel(tagList.toArray(new String[tagList.size()])));
        }
        if ( policy.getInternalTag() != null ) {
            tagCombo.setSelectedItem( policy.getInternalTag() );                
        }
        tagCombo.setEnabled(enableTags);
    }

    @Override
    protected String getSyntaxError(Object model) {
        if (nameField.getText().trim().length() > 0) return null;
        PolicyType type = (PolicyType) typeCombo.getSelectedItem();
        if (type == null) return resources.getString("typeRequiredError");
        if (type.getSupertype() == PolicyType.Supertype.FRAGMENT) return resources.getString("nameRequiredError");
        return null;
    }

    @Override
    public void focusFirstComponent() {
        nameField.requestFocus();
    }

    @Override
    protected void doUpdateModel() {
        policy.setName(nameField.getText().trim());
        policy.setSoap(soapCheckbox.isSelected());
        policy.setType((PolicyType)typeCombo.getSelectedItem());
        policy.setSecurityZone(zoneControl.isEnabled() ? zoneControl.getSelectedZone() : null);

        if (policy.getType() == PolicyType.INTERNAL || policy.getType().getGuiTags() != null) {
            String tag = (String) tagCombo.getSelectedItem();
            policy.setInternalTag(tag);
            final Map<String,String> policyTags = this.policyTagsByType.get( policy.getType() );
            if (policyTags.get(tag) != null) {
                if (StringUtils.isEmpty(policy.getXml())) //only update the policy the tag specific policy if there aren't any policy contents already
                    policy.setXml(policyTags.get(tag));
            }
        }
    }
}

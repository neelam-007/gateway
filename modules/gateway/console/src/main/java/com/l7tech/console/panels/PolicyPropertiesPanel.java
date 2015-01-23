package com.l7tech.console.panels;

import com.l7tech.console.action.EditServiceProperties;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.DocumentSizeFilter;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.Functions;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

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
public class PolicyPropertiesPanel extends ValidatedPanel<Policy> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.PolicyPropertiesPanel");

    private JPanel mainPanel;
    private JTextField nameField;
    private JTextField guidField;
    private JCheckBox soapCheckbox;
    private JComboBox<PolicyType> typeCombo;
    private JComboBox<String> tagCombo;
    private JLabel unsavedWarningLabel;
    private JTextField oidField;
    private SecurityZoneWidget zoneControl;
    private JLabel policyTagLabel;
    private JLabel policySubTagLabel;
    private JComboBox<String> subTagCombo;
    // TODO include a policy panel

    private final Policy policy;
    private final boolean canUpdate;

    private final Map<PolicyType,Collection<PolicyAdmin.PolicyTagInfo>> policyTagsByType = new HashMap<>();

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
    protected Policy getModel() {
        return policy;
    }

    @Override
    protected void initComponents() {
        PolicyType selectedType = policy.getType();

        final String policyInternalTag = policy.getInternalTag();
        for ( final PolicyType type : PolicyType.values() ) {
            Collection<PolicyAdmin.PolicyTagInfo> tagInfos = new ArrayList<>();

            if ( type.isDynamicTags() ) {
                Collection<PolicyAdmin.PolicyTagInfo> moreTags = Registry.getDefault().getPolicyAdmin().getPolicyTags( type );
                tagInfos.addAll( moreTags );
            }

            if ( type == PolicyType.INTERNAL ) {
                if (looksLikeAuditSinkPolicy(policy) && policyInternalTag != null) {
                    PolicyAdmin.PolicyTagInfo info = new PolicyAdmin.PolicyTagInfo( policyInternalTag, null, null );
                    tagInfos.add( info );
                }

                if (looksLikeAuditLookupPolicy(policy) && policyInternalTag != null) {
                    PolicyAdmin.PolicyTagInfo info = new PolicyAdmin.PolicyTagInfo( policyInternalTag, null, null );
                    tagInfos.add( info );
                }

                if (looksLikeDebugTracePolicy(policy) && policyInternalTag != null) {
                    PolicyAdmin.PolicyTagInfo info = new PolicyAdmin.PolicyTagInfo( policyInternalTag, null, null );
                    tagInfos.add( info );
                }

            }

            for ( final String tag : type.getGuiTags() ) {
                PolicyAdmin.PolicyTagInfo info = new PolicyAdmin.PolicyTagInfo( tag, null, null );
                tagInfos.add( info );
            }

            policyTagsByType.put( type, tagInfos );
        }

        java.util.List<PolicyType> types = new ArrayList<>();
        for ( PolicyType type : PolicyType.values()) {
            if (type.isShownInGui() || selectedType==type) types.add(type);
        }
        Collections.sort( types, new ResolvingComparator<>( new Resolver<PolicyType, String>() {
            @Override
            public String resolve( final PolicyType key ) {
                return key.getName().toLowerCase();
            }
        }, false ) );

        // Don't offer subtag controls unless at least one subtag is known to exist,
        // or the current policy is already using a subtag
        boolean subtagsPolicy = policy.getInternalSubTag() != null && policy.getInternalSubTag().length() > 0;
        boolean haveAtLeastOneSubTag = hasSubtags( null, policyTagsByType );
        boolean showSubTagControls = subtagsPolicy || haveAtLeastOneSubTag;

        // Don't offer POLICY_BACKED_OPERATION PolicyType unless an interface is registered (or this policy is already of that type)
        boolean pbsPolicy = PolicyType.POLICY_BACKED_OPERATION.equals( policy.getType() );
        boolean haveAtLeastOnePolicyBackedMethod = hasSubtags( PolicyType.POLICY_BACKED_OPERATION, policyTagsByType );
        if ( !pbsPolicy && !haveAtLeastOnePolicyBackedMethod ) {
            policyTagsByType.remove( PolicyType.POLICY_BACKED_OPERATION );
            types.remove( PolicyType.POLICY_BACKED_OPERATION );
        }

        typeCombo.setModel(new DefaultComboBoxModel<>(types.toArray(new PolicyType[types.size()])));

        policySubTagLabel.setVisible( showSubTagControls );
        subTagCombo.setVisible( showSubTagControls );

        populateTagComboBox( selectedType );
        tagCombo.setSelectedItem( policyInternalTag );

        populateSubTagComboBox( selectedType, policyInternalTag );
        subTagCombo.setSelectedItem( policy.getInternalSubTag() );

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
        typeCombo.addItemListener( syntaxListener );
        tagCombo.addItemListener(syntaxListener);
        subTagCombo.addItemListener( syntaxListener );
        nameField.getDocument().addDocumentListener( syntaxListener );

        zoneControl.configure( Goid.isDefault( policy.getGoid() ) ? OperationType.CREATE : canUpdate ? OperationType.UPDATE : OperationType.READ, policy );

        typeCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    final PolicyType policyType = (PolicyType) e.getItem();
                    populateTagComboBox( policyType );
                    populateSubTagComboBox( policyType, (String) tagCombo.getSelectedItem() );
                    enableDisable();
                }
            }
        });
        tagCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    final PolicyType policyType = (PolicyType) typeCombo.getSelectedItem();
                    final String policyTag = (String) tagCombo.getSelectedItem();
                    populateSubTagComboBox( policyType, policyTag );
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

    private boolean hasSubtags( @Nullable PolicyType requiredType, Map<PolicyType, Collection<PolicyAdmin.PolicyTagInfo>> policyTagsByType ) {
        if ( requiredType != null ) {
            Collection<PolicyAdmin.PolicyTagInfo> tagInfos = policyTagsByType.get( requiredType );
            return tagInfos != null && hasSubtags( tagInfos );
        }

        for ( Collection<PolicyAdmin.PolicyTagInfo> tagInfos : policyTagsByType.values() ) {
            boolean hasSubtag = hasSubtags( tagInfos );
            if ( hasSubtag )
                return true;
        }

        return false;
    }

    private static boolean hasSubtags( Collection<PolicyAdmin.PolicyTagInfo> tagInfos ) {
        boolean ret = false;

        for ( PolicyAdmin.PolicyTagInfo tagInfo : tagInfos ) {
            Set<String> subTags = tagInfo.policySubTags;
            if ( subTags.size() > 0 ) {
                ret = true;
                break;
            }
        }

        return ret;
    }

    private Functions.Unary<String, PolicyAdmin.PolicyTagInfo> transformPolicyTagName() {
        return new Functions.Unary<String, PolicyAdmin.PolicyTagInfo>() {
            @Override
            public String call( PolicyAdmin.PolicyTagInfo policyTagInfo ) {
                return policyTagInfo.policyTag;
            }
        };
    }

    private static Functions.Unary<Boolean, PolicyAdmin.PolicyTagInfo> predicatePolicyTagNameEquals( final String tag ) {
        return new Functions.Unary<Boolean, PolicyAdmin.PolicyTagInfo>() {
            @Override
            public Boolean call( PolicyAdmin.PolicyTagInfo policyTagInfo ) {
                return tag.equals( policyTagInfo.policyTag );
            }
        };
    }

    private void enableDisable() {
        PolicyType type = (PolicyType) typeCombo.getSelectedItem();
        zoneControl.setEnabled(type != null && type.isSecurityZoneable());
        zoneControl.setToolTipText( zoneControl.isEnabled() ? null : "Policy type not zoneable" );

        tagCombo.setEnabled( tagCombo.getModel().getSize() > 0 && canUpdate );
        subTagCombo.setEnabled( tagCombo.isEnabled() && subTagCombo.getModel().getSize() > 0 && canUpdate );

        // If policy has already been created, its type and tag and subtag may no longer be changed.  (SSM-4318)
        // This is to prevent eg. changing the backing policy of an encapsulated assertion into a global policy.
        if ( !policy.isUnsaved() ) {
            typeCombo.setEnabled( false );
            tagCombo.setEnabled( false );
            subTagCombo.setEnabled( false );
        }
    }

    private void populateTagComboBox( PolicyType policyType ) {
        final List<String> tagList = new ArrayList<>();

        final Collection<PolicyAdmin.PolicyTagInfo> tagInfos = policyTagsByType.get( policyType );
        tagList.addAll( Functions.map( tagInfos, transformPolicyTagName() ) );

        tagCombo.setModel( new DefaultComboBoxModel<>( tagList.toArray( new String[tagList.size()] ) ) );
    }

    private void populateSubTagComboBox( PolicyType policyType, String policyTag ) {
        final List<String> subTagList = new ArrayList<>();

        if ( policyTag != null ) {
            Collection<PolicyAdmin.PolicyTagInfo> tagInfos = policyTagsByType.get( policyType );
            if ( tagInfos != null ) {
                PolicyAdmin.PolicyTagInfo tagInfo = Functions.grepFirst( tagInfos, predicatePolicyTagNameEquals( policyTag ) );

                if ( tagInfo != null ) {
                    Set<String> subTags = tagInfo.policySubTags;
                    if ( !subTags.isEmpty() ) {
                        subTagList.addAll( subTags );
                    }
                }
            }
        }

        subTagCombo.setModel( new DefaultComboBoxModel<>( subTagList.toArray( new String[subTagList.size()] ) ) );
    }

    @Override
    protected String getSyntaxError(Policy model) {
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

            String subTag = (String) subTagCombo.getSelectedItem();
            policy.setInternalSubTag( subTag );

            final Collection<PolicyAdmin.PolicyTagInfo> tagInfos = this.policyTagsByType.get( policy.getType() );

            PolicyAdmin.PolicyTagInfo tagInfo = tagInfos == null
                    ? null
                    : Functions.grepFirst( tagInfos, predicatePolicyTagNameEquals( tag ) );
            if ( tagInfo != null ) {
                //only update the policy the tag specific policy if there aren't any policy contents already
                if ( StringUtils.isEmpty( policy.getXml() ) && tagInfo.defaultPolicyXml != null ) {
                    policy.setXml( tagInfo.defaultPolicyXml );
                }
            }
        }
    }
}

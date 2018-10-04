package com.l7tech.console.security.rbac;

import com.l7tech.console.util.EntityNameResolver;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.Identity;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Read-only properties dialog for a PermissionGroup.
 */
public class PermissionGroupPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(PermissionGroupPropertiesDialog.class.getName());
    private static final String PERMISSION_GROUP_PROPERTIES = "Permission Group Properties";
    private static final String ALL = "<ALL>";
    private static final String SEPARATOR = ", ";
    private JPanel contentPanel;
    private JTextArea scopeTextArea;
    private JButton closeButton;
    private JLabel typeLabel;
    private JLabel opsLabel;

    public PermissionGroupPropertiesDialog(@NotNull final Window owner, @NotNull final PermissionGroup group) {
        super(owner, PERMISSION_GROUP_PROPERTIES, DEFAULT_MODALITY_TYPE);
        setContentPane(contentPanel);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                dispose();
            }
        });
        getRootPane().setDefaultButton(closeButton);
        Utilities.setEscAction(this, closeButton);
        typeLabel.setText(group.getEntityType() == EntityType.ANY ? ALL : group.getEntityType().getName());
        scopeTextArea.setText(getScopeDescription(group));
        final Set<String> operations = new TreeSet<>();
        boolean containsOther = false;
        for (final OperationType operationType : group.getOperations()) {
            if (operationType != OperationType.OTHER) {
                operations.add(operationType.getName().toLowerCase());
            } else {
                containsOther = true;
            }
        }
        if (containsOther) {
            operations.addAll(group.getOtherOperations());
        }
        opsLabel.setText(StringUtils.join(operations, SEPARATOR));
    }

    /**
     * @param group the PermissionGroup for which to get a description of its scope.
     * @return a comma-separated description of each scope predicate in the PermissionGroup.
     */
    public static String getScopeDescription(@NotNull final PermissionGroup group) {
        String scopeDescription = null;
        final Set<ScopePredicate> scope = group.getScope();
        if (!scope.isEmpty()) {
            final EntityNameResolver nameResolver = Registry.getDefault().getEntityNameResolver();
            scopeDescription = tryGetAttributeSpecificDescriptions(group);
            if (scopeDescription == null) {
                final Set<String> predicateDescriptions = new TreeSet<>();
                for (final ScopePredicate predicate : scope) {
                    try {
                        predicateDescriptions.add(nameResolver.getNameForEntity(predicate, true));
                    } catch (final FindException e) {
                        logger.log(Level.WARNING, "Unable to determine name for predicate " + predicate + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    }
                }
                scopeDescription = StringUtils.join(predicateDescriptions.toArray(), SEPARATOR);
            }
        } else {
            scopeDescription = ALL;
        }
        return scopeDescription;
    }

    /**
     * Look for attribute predicate combinations which may identify a specific entity and return a scope description if possible.
     */
    private static String tryGetAttributeSpecificDescriptions(final PermissionGroup group) {
        String scopeDescription = null;
        final Set<ScopePredicate> scope = group.getScope();
        ObjectIdentityPredicate objectIdentityPredicate = null;
        final Identity identity = tryGetIdentity(group.getEntityType(), scope);
        if (identity != null) {
            objectIdentityPredicate = new ObjectIdentityPredicate(null, identity.getId());
            objectIdentityPredicate.setHeader(new IdentityHeader(identity.getProviderId(), identity.getId(), group.getEntityType(), identity.getName(), null, null, null));
        } else {
            final ServiceUsageHeader serviceUsageHeader = tryGetServiceUsage(group.getEntityType(), scope);
            if (serviceUsageHeader != null) {
                objectIdentityPredicate = new ObjectIdentityPredicate(null, null);
                objectIdentityPredicate.setHeader(serviceUsageHeader);
            }
        }
        if (objectIdentityPredicate != null) {
            try {
                scopeDescription = Registry.getDefault().getEntityNameResolver().getNameForEntity(objectIdentityPredicate, true);
            } catch (final FindException | PermissionDeniedException e) {
                logger.log(Level.WARNING, "Unable to resolve name for predicate " + objectIdentityPredicate + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
        return scopeDescription;
    }

    private static ServiceUsageHeader tryGetServiceUsage(final EntityType type, final Set<ScopePredicate> scope) {
        ServiceUsageHeader serviceUsageHeader = null;
        if (type == EntityType.SERVICE_USAGE && scope.size() == 2) {
            final Map<String, String> equalsAttributes = getEqualsAttributes(scope);
            if (equalsAttributes.containsKey(PermissionSummaryPanel.SERVICE_ID) && equalsAttributes.containsKey(PermissionSummaryPanel.NODE_ID)) {
                try {
                    final Goid serviceGoid = Goid.parseGoid(equalsAttributes.get(PermissionSummaryPanel.SERVICE_ID));
                    serviceUsageHeader = new ServiceUsageHeader(serviceGoid, equalsAttributes.get(PermissionSummaryPanel.NODE_ID));
                } catch (final IllegalArgumentException e) {
                    logger.log(Level.WARNING, "Unable to parse goid from attribute value: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }
        return serviceUsageHeader;
    }

    private static Identity tryGetIdentity(final EntityType type, final Set<ScopePredicate> scope) {
        Identity identity = null;
        if ((type == EntityType.USER || type == EntityType.GROUP) && scope.size() == 2) {
            final Map<String, String> equalsAttributes = getEqualsAttributes(scope);
            if (equalsAttributes.containsKey(PermissionSummaryPanel.PROVIDER_ID) && equalsAttributes.containsKey(PermissionSummaryPanel.ID)) {
                try {
                    final Goid providerGoid = Goid.parseGoid(equalsAttributes.get(PermissionSummaryPanel.PROVIDER_ID));
                    final String identityId = equalsAttributes.get(PermissionSummaryPanel.ID);
                    if (type == EntityType.USER) {
                        identity = Registry.getDefault().getIdentityAdmin().findUserByID(providerGoid, identityId);
                    } else if (type == EntityType.GROUP) {
                        identity = Registry.getDefault().getIdentityAdmin().findGroupByID(providerGoid, identityId);
                    }
                } catch (final IllegalArgumentException | FindException | PermissionDeniedException e) {
                    logger.log(Level.WARNING, "Unable to parse goid from attribute value: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }
        return identity;
    }

    private static Map<String, String> getEqualsAttributes(final Set<ScopePredicate> scope) {
        final Map<String, String> equalsAttributes = new HashMap<>();
        for (final ScopePredicate predicate : scope) {
            if (predicate instanceof AttributePredicate) {
                final AttributePredicate attributePredicate = (AttributePredicate) predicate;
                if (attributePredicate.getMode() == null || attributePredicate.getMode().equals(AttributePredicate.EQUALS)) {
                    equalsAttributes.put(attributePredicate.getAttribute(), attributePredicate.getValue());
                }
            }
        }
        return equalsAttributes;
    }
}

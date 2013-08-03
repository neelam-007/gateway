package com.l7tech.gateway.common.security.rbac;

import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.util.ConfigFactory;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: megery
 * Date: Jul 31, 2006
 * Time: 4:55:46 PM
 */
public class RbacUtilities {
    public static final String SYSTEM_PROP_ENABLEROLEEDIT = "com.l7tech.rbac.allowEditRoles";
    public static final String SYSTEM_PROP_ENABLE_BUILTIN_ROLEEDIT = "com.l7tech.rbac.allowEditBuiltinRoles";
    public static final Pattern removeOidPattern = Pattern.compile("^(\\w{1,128} .*?) \\(#-?[\\da-f][\\da-f, ]*\\)$");
    public static final String REGEX_PATTERN = "^\\w'{'1,128'}' (.*?) {0}$";

    /**
     * A map of {@link Pattern}s for extracting the entity name from a role name
     */
    private static final Map<EntityType, Pattern> patternMap = new HashMap<EntityType, Pattern>();
    static {
        patternMap.put(EntityType.SERVICE, Pattern.compile(MessageFormat.format(REGEX_PATTERN, ServiceAdmin.ROLE_NAME_TYPE_SUFFIX)));
        patternMap.put(EntityType.ID_PROVIDER_CONFIG, Pattern.compile(MessageFormat.format(REGEX_PATTERN, IdentityAdmin.ROLE_NAME_TYPE_SUFFIX)));
        patternMap.put(EntityType.POLICY, Pattern.compile(MessageFormat.format(REGEX_PATTERN, PolicyAdmin.ROLE_NAME_TYPE_SUFFIX)));
        patternMap.put(EntityType.FOLDER, Pattern.compile(MessageFormat.format(REGEX_PATTERN, FolderAdmin.ROLE_NAME_TYPE_SUFFIX)));
        patternMap.put(EntityType.ESM_SSG_CLUSTER, Pattern.compile(MessageFormat.format(REGEX_PATTERN, "Gateway Cluster Nodes")));
        patternMap.put(EntityType.ESM_STANDARD_REPORT, Pattern.compile(MessageFormat.format(REGEX_PATTERN, "Report")));
        patternMap.put(EntityType.LOG_SINK, Pattern.compile(MessageFormat.format(REGEX_PATTERN, LogSinkAdmin.ROLE_NAME_TYPE_SUFFIX)));
        patternMap.put(EntityType.SECURITY_ZONE, Pattern.compile(MessageFormat.format(REGEX_PATTERN, RbacAdmin.ZONE_ROLE_NAME_TYPE_SUFFIX)));
    }

    public static boolean isEnableRoleEditing() {
        return ConfigFactory.getBooleanProperty( SYSTEM_PROP_ENABLEROLEEDIT, true );
    }

    public static boolean isEnableBuiltInRoleEditing() {
        return ConfigFactory.getBooleanProperty( SYSTEM_PROP_ENABLE_BUILTIN_ROLEEDIT, false );
    }

    public static String getDescriptionString(Role role, boolean asHtml) {
        String name = role.getName();
        Matcher mat = removeOidPattern.matcher(name);
        if (mat.matches()) name = mat.group(1);

        String description = role.getDescription();
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(description))
            return "";

        String entityName = null;
        EntityType type = role.getEntityType();
        if (type != null) {
            if (patternMap.containsKey(type)) {
                Pattern pat = patternMap.get(type);
                mat = pat.matcher(name);
                if (mat.matches()) {
                    entityName = mat.group(1);
                    Entity entity = role.getCachedSpecificEntity();
                    if (entity instanceof PublishedService) {
                        String uri = ((PublishedService) entity).getRoutingUri();
                        if (uri != null) entityName += " [" + uri + "]";
                    }
                }
            }
        }

        String[] args;
        String nameMaybeHtml = asHtml ? "<strong>" + name + "</strong>" : name;
        if (entityName == null) {
            args = new String[] { nameMaybeHtml };
        } else {
            args = new String[] { nameMaybeHtml, entityName };
        }

        return MessageFormat.format(description, args);
    }

    /**
     * Get a copy of the given scope, detaching each predicate from its permission as well as setting its oid to default oid.
     *
     * @param   in the scope to copy (optional).
     * @return  a copy of the given scope with its predicates detached and oids set to default or an empty set if the given scope is null.
     */
    public static Set<ScopePredicate> getAnonymousNoOidsCopyOfScope(@Nullable final Set<ScopePredicate> in) {
       final Set<ScopePredicate> out = new HashSet<>();
       if (in != null) {
           for (final ScopePredicate scopePredicate : in) {
               final ScopePredicate clone = scopePredicate.createAnonymousClone();
               clone.setOid(ScopePredicate.DEFAULT_OID);
               out.add(clone);
           }
       }
       return out;
   }
}

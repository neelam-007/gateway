package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.rbac.RbacAttribute;
import com.l7tech.util.SyspropUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Collects attributes which are available to be used for an AttributePredicate by looking for the RbacAttribute annotation.
 * <p/>
 * Attribute display names can be specified in the RbacAttributes.properties file.
 *
 * @see RbacAttribute
 */
public class RbacAttributeCollector {
    static final String ALLOW_ALL_PROP = "com.l7tech.gateway.common.security.rbac.RbacAttributeCollector.allowAll";
    private static final Logger logger = Logger.getLogger(RbacAttributeCollector.class.getName());
    private static final ResourceBundle RESOURCE = ResourceBundle.getBundle("com.l7tech.gateway.common.security.rbac.RbacAttributes");
    private static Collection<Class> supportedReturnTypes;
    private static final String NAME = "name";

    static {
        supportedReturnTypes = new HashSet<>();
        supportedReturnTypes.add(Number.class);
        supportedReturnTypes.add(Long.TYPE);
        supportedReturnTypes.add(Integer.TYPE);
        supportedReturnTypes.add(Byte.TYPE);
        supportedReturnTypes.add(Short.TYPE);
        supportedReturnTypes.add(Boolean.TYPE);
        supportedReturnTypes.add(Character.TYPE);
        supportedReturnTypes.add(CharSequence.class);
        supportedReturnTypes.add(Boolean.class);
        supportedReturnTypes.add(Enum.class);
        supportedReturnTypes.add(Goid.class);
    }

    /**
     * Collect attributes for the given EntityType which are available to be used in an AttributePredicate.
     *
     * @param entityType the EntityType for which to collect available attributes.
     * @return a map where key=attribute and value=display name for the attribute. May be empty but not null.
     */
    @NotNull
    public static Map<String, String> collectAttributes(@NotNull final EntityType entityType) {
        final Map<String, String> attributes = new TreeMap<>();
        final Class eClazz = entityType.getEntityClass();
        if (eClazz != null) {
            attributes.putAll(collectAttributes(eClazz));
        }
        // Allow attempts to use Name for ANY entity, since in practice most will be NamedEntity subclasses
        if (EntityType.ANY.equals(entityType)) {
            attributes.put(NAME, NAME);
        }
        return attributes;
    }

    protected static Map<String, String> collectAttributes(@NotNull final Class clazz) {
        final Map<String, String> attributes = new TreeMap<>();
        try {
            final BeanInfo info = Introspector.getBeanInfo(clazz);
            final PropertyDescriptor[] props = info.getPropertyDescriptors();
            for (final PropertyDescriptor propertyDescriptor : props) {
                final Method getter = propertyDescriptor.getReadMethod();
                if (getter != null && isSupportedReturnType(getter.getReturnType())) {
                    final String propName = propertyDescriptor.getName();
                    if (SyspropUtil.getBoolean(ALLOW_ALL_PROP, false)) {
                        attributes.put(propName, getDisplayName(propName, null));
                    } else {
                        final RbacAttribute methodAttribute = getter.getAnnotation(RbacAttribute.class);
                        if ((methodAttribute != null)) {
                            final String attributeIdentifier = methodAttribute.displayNameIdentifier();
                            attributes.put(propName, getDisplayName(propName, attributeIdentifier));
                        }
                    }
                } else {
                    logger.log(Level.FINEST, "No getter or invalid return type for property descriptor: " + propertyDescriptor.getName());
                }
            }
            final Set<Class> interfaces = new HashSet<>();
            collectInterfaces(clazz, interfaces);
            for (final Class inter : interfaces) {
                attributes.putAll(collectAttributes(inter));
            }
        } catch (final IntrospectionException e) {
            logger.log(Level.WARNING, "Unable to introspect " + clazz, e);
        }
        return attributes;
    }

    private static String getDisplayName(@NotNull final String propName, @Nullable final String attributeIdentifier) {
        // default is property name
        String displayName = propName;
        if (StringUtils.isNotBlank(attributeIdentifier) && RESOURCE.containsKey(attributeIdentifier)) {
            // look up by RbacAttribute display name identifier
            displayName = RESOURCE.getString(attributeIdentifier);
        } else if (RESOURCE.containsKey(propName)) {
            // look up by property name
            displayName = RESOURCE.getString(propName);
        }
        return displayName;
    }

    private static boolean isSupportedReturnType(final Class returnType) {
        for (final Class supportedReturnType : supportedReturnTypes) {
            if (supportedReturnType.isAssignableFrom(returnType)) {
                return true;
            }
        }
        return false;
    }

    private static void collectInterfaces(final Class clazz, final Set<Class> interfaces) {
        final Class[] directInterfaces = clazz.getInterfaces();
        for (final Class directInterface : directInterfaces) {
            interfaces.add(directInterface);
            collectInterfaces(directInterface, interfaces);
        }
    }
}

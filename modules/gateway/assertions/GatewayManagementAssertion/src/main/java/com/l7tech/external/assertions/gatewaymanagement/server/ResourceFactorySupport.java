package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.BeanUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 */
abstract class ResourceFactorySupport<R> implements ResourceFactory<R> {

    //- PUBLIC

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public Map<String, String> createResource( final Object resource ) throws InvalidResourceException {
        throw new IllegalStateException("Read only");
    }

    @Override
    public R putResource( final Map<String, String> selectorMap, final Object resource ) throws ResourceNotFoundException, InvalidResourceException {
        throw new IllegalStateException("Read only");
    }

    @Override
    public String deleteResource( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        throw new IllegalStateException("Read only");
    }

    //- PROTECTED

    /**
     * Validate the beans properties.
     *
     * @param bean The bean to validate
     * @param groups The validation groups
     */
    protected final void validate( final Object bean, final Class<?>... groups ) throws InvalidResourceException {
        if ( !VALIDATION_ENABLED ) return;

        final Set<ConstraintViolation<Object>> violations = validator.validate( bean, groups );
        if ( !violations.isEmpty() ) {
            final StringBuilder validationReport = new StringBuilder();
            boolean first = true;
            for ( final ConstraintViolation<Object> violation : violations ) {
                if ( !first ) validationReport.append( '\n' );
                first = false;
                validationReport.append( violation.getPropertyPath().toString() );
                validationReport.append( ' ' );
                validationReport.append( violation.getMessage() );
            }
            throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, validationReport.toString() );
        }
    }

    /**
     * Get the properties map for the given object.
     *
     * @param bean The bean to extract properties from (required)
     * @param beanClass The class of the bean (required)
     * @return The map of property names to values (not null)
     * @throws ResourceAccessException if the type of a property is not supported or an unknown property is found.
     */
    protected final <B extends PersistentEntity> Map<String,Object> getProperties( final B bean, final Class<B> beanClass ) {
        final Map<String,Object> propertyMap = new HashMap<String,Object>();

        try {
            final Collection<String> ignoredProperties = propertiesHelper.getIgnoredProperties(beanClass);
            final Collection<String> writeOnlyProperties = propertiesHelper.getWriteOnlyProperties(beanClass);
            final Map<String,String> propertyMapping = propertiesHelper.getPropertiesMap(beanClass);
            final Set<PropertyDescriptor> properties = BeanUtils.omitProperties(
                    BeanUtils.getProperties( beanClass ),
                    ignoredProperties.toArray(new String[ignoredProperties.size()]));

            for ( final PropertyDescriptor prop : properties ) {
                if ( writeOnlyProperties.contains( prop.getName() ) ) {
                    continue;
                }
                if ( !propertyMapping.containsKey(prop.getName()) ) {
                    throw new ResourceAccessException( "Unknown entity property '"+prop.getName()+"'." );
                }

                final String mappedName = propertyMapping.get(prop.getName()) != null ? propertyMapping.get(prop.getName()) : prop.getName();
                final Object value = prop.getReadMethod().invoke(bean);
                if ( value == null ) {
                    // skip null property    
                } else if ( value instanceof Boolean ||
                            value instanceof Number ||
                            value instanceof String ) {
                    propertyMap.put( mappedName, value );
                } else if ( value instanceof Enum ) {
                    propertyMap.put( mappedName, propertiesHelper.getEnumText( (Enum) value ) );
                } else {
                    throw new ResourceAccessException("Unsupported property type '" + value.getClass() + "'." );
                }
            }
        } catch ( InvocationTargetException e ) {
            throw new ResourceAccessException(e.getCause());
        } catch ( IllegalAccessException e ) {
            throw new ResourceAccessException(e);
        } catch ( RuntimeException re ) {
            if ( ExceptionUtils.causedBy( re, IntrospectionException.class )) {
                throw new ResourceAccessException(re);
            } else {
                throw re;
            }
        }

        return propertyMap;
    }

    /**
     * Set the properties for the given object from the given properties map.
     *
     * @param bean The bean to set the properties of (required)
     * @param propertiesMap The map of properties to set (may be null)
     * @param beanClass The class of the bean (required)
     */
    @SuppressWarnings({ "unchecked" })
    protected final <B extends PersistentEntity> void setProperties( final B bean,
                                                                     final Map<String,Object> propertiesMap,
                                                                     final Class<B> beanClass ) {
        try {
            final Collection<String> ignoredProperties = propertiesHelper.getIgnoredProperties(beanClass);
            final Map<String,String> propertyMapping = propertiesHelper.getPropertiesMap(beanClass);
            final Map<String,Object> propertyDefaults = propertiesHelper.getPropertyDefaultsMap(beanClass);
            final Set<PropertyDescriptor> properties = BeanUtils.omitProperties(
                    BeanUtils.getProperties( beanClass ),
                    ignoredProperties.toArray(new String[ignoredProperties.size()]));

            for ( final PropertyDescriptor prop : properties ) {
                final String mappedName;
                if ( propertyMapping.containsKey( prop.getName() ) ) {
                    mappedName = propertyMapping.get(prop.getName()) != null ? propertyMapping.get(prop.getName()) : prop.getName();
                } else {
                    throw new ResourceAccessException( "Unknown entity property '"+prop.getName()+"'." );
                }

                Object value = propertiesMap != null ? propertiesMap.get( mappedName ) : null;
                if ( value == null ) {
                    value = propertyDefaults.get( prop.getName() );
                }
                final Method setter = prop.getWriteMethod();

                final Class<?> parameterType = setter.getParameterTypes()[0];
                if ( value != null && !parameterType.isPrimitive() && !parameterType.isAssignableFrom( value.getClass() ) ) {
                    if ( value instanceof String && Enum.class.isAssignableFrom( parameterType ) ) {
                        value = propertiesHelper.getEnumValue( (Class<? extends Enum>) parameterType, (String) value );
                    } else {
                        throw new ResourceAccessException("Unsupported property type '" + value.getClass() + "' (requires '"+parameterType+"')" );
                    }
                } else if ( value == null && parameterType.isPrimitive() ) {
                    throw new ResourceAccessException("Missing required property '" + mappedName + "'" );
                }

                setter.invoke( bean, value );
            }
        } catch ( RuntimeException re ) {
            if ( ExceptionUtils.causedBy( re, IntrospectionException.class )) {
                throw new ResourceAccessException(re);
            } else {
                throw re;
            }
        } catch ( InvocationTargetException e ) {
            throw new ResourceAccessException(e.getCause());
        } catch ( IllegalAccessException e ) {
            throw new ResourceAccessException(e);
        }
    }

    /**
     * Process a name value.
     *
     * @param name The name to process
     * @return The processed name.
     */
    protected final String asName( final String name ) {
        return trim( name );
    }

    /**
     * Trim text value.
     */
    protected final String trim( final String text ) {
        String processedText = text;

        if ( processedText != null ) {
            processedText = processedText.trim();
        }

        return processedText;
    }

    /**
     * Get the id of the parent folder for the given item.
     *
     * @param hasFolder The item in the folder
     * @return The parent folder id or null
     */
    protected final String getFolderId( final HasFolder hasFolder ) {
        String id = null;

        if ( hasFolder.getFolder() != null && hasFolder.getFolder().getOid() != -5002 ) {
            id = hasFolder.getFolder().getId();
        }

        return id;
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    protected final void checkPermitted( final OperationType operationType,
                                         final String otherOperationName,
                                         final Entity entity ) {
        if (entity == null) return;

        final User user = JaasUtils.getCurrentUser();
        final EntityType entityType = EntityType.findTypeByEntity(entity.getClass());
        try {
            if ( user==null || !rbacServices.isPermittedForEntity( user, entity, operationType, otherOperationName ) ) {
                if ( otherOperationName != null ) {
                    throw new PermissionDeniedException( operationType, entity, otherOperationName );
                } else {
                    throw new PermissionDeniedException( operationType, entityType );
                }
            }
        } catch ( FindException fe ) {
            throw (PermissionDeniedException) new PermissionDeniedException( operationType, entityType, "Error in permission check.").initCause(fe);
        }
    }

    /**
     * Filter a collection of entities or entity types.
     */
    final protected <ET> Collection<ET> accessFilter( final Collection<ET> entities,
                                                      final EntityType entityType,
                                                      final OperationType operationType,
                                                      final String otherOperationName ) throws FindException {
        final Collection<ET> filteredEntities;
        final User user = JaasUtils.getCurrentUser();

        if ( rbacServices.isPermittedForAnyEntityOfType(user, operationType, entityType) ) {
            filteredEntities = entities;
        } else {
            filteredEntities = securityFilter.filter( entities, user, operationType, otherOperationName );
        }

        return filteredEntities;
    }

    protected void handleObjectModelException( final ObjectModelException ome ) {
        throw new ResourceAccessException(ome);
    }

    @SuppressWarnings({"unchecked"})
    protected <R> R transactional( final TransactionalCallback<R,?> callback,
                                 final boolean readOnly,
                                 final Class<? extends Exception>... checkedExceptionTypes )  {
        try {
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setReadOnly( readOnly );
            return (R) tt.execute( new TransactionCallback(){
                @Override
                public Object doInTransaction( final TransactionStatus transactionStatus ) {
                    try {
                        return callback.execute();
                    } catch (ObjectModelException e) {
                        transactionStatus.setRollbackOnly();
                        handleObjectModelException(e);
                        return null; // not reached
                    } catch ( Throwable t ) {
                        throw new TransactionalException(t);
                    }
                }
            });
        } catch ( TransactionalException te ) {
            te.throwException( checkedExceptionTypes );
            return null; // not reached
        } catch ( TransactionException te ) {
            throw new ResourceAccessException(te);
        } catch ( DataAccessException dae ) {
            throw new ResourceAccessException(dae);
        }
    }

    protected interface TransactionalCallback<R,E extends Exception> {
        R execute() throws ObjectModelException, E;
    }

    //- PACKAGE

    ResourceFactorySupport( final RbacServices rbacServices,
                            final SecurityFilter securityFilter,
                            final PlatformTransactionManager transactionManager ) {
        this.securityFilter = securityFilter;
        this.rbacServices = rbacServices;
        this.transactionManager = transactionManager;
    }

    //- PRIVATE

    private static final boolean VALIDATION_ENABLED = SyspropUtil.getBoolean( "com.l7tech.external.assertions.gatewaymanagement.validationEnabled", true );

    private final RbacServices rbacServices;
    private final SecurityFilter securityFilter;
    private final PlatformTransactionManager transactionManager;
    private final EntityPropertiesHelper propertiesHelper = new EntityPropertiesHelper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @SuppressWarnings({"serial"})
    private static class TransactionalException extends RuntimeException {
        public TransactionalException( final Throwable cause ) {
            super(cause);
        }

        public void throwException( final Class<? extends Exception>... checkedExceptionTypes ) {
            final Throwable cause = getCause();

            for ( Class<? extends Exception> exceptionClass : checkedExceptionTypes ) {
                if ( exceptionClass.isInstance(cause) ) {
                    this.<RuntimeException>throwAsType( cause );
                }
            }

            throw this;
        }

        @SuppressWarnings({"unchecked"})
        private <T extends Throwable> void throwAsType( final Throwable throwable ) throws T {
            throw (T) throwable;
        }
    }
}

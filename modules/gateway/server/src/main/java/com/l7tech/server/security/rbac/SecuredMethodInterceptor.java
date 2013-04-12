/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.util.Injector;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.CollectionUpdate;
import com.l7tech.util.CollectionUpdateFilterer;
import com.l7tech.util.ExceptionUtils;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.security.rbac.OperationType.*;

/**
 * Runtime RBAC enforcement kernel
 * 
 * @author alex
 */
public class SecuredMethodInterceptor implements MethodInterceptor, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(SecuredMethodInterceptor.class.getName());
    private final RbacServices rbacServices;
    private final EntityFinder entityFinder;
    private static final String DEFAULT_ID = Long.toString(PersistentEntity.DEFAULT_OID);

    private ApplicationContext applicationContext;

    public SecuredMethodInterceptor(RbacServices rbacServices, EntityFinder entityFinder) {
        this.rbacServices = rbacServices;
        this.entityFinder = entityFinder;
    }

    public SecurityFilter getSecurityFilter() {
        return new SecurityFilter() {
            @Override
            public <T> Collection<T> filter(Collection<T> entityCollection, User user, OperationType type, @Nullable String operationName) throws FindException {
                return SecuredMethodInterceptor.this.filter( entityCollection, user, type, operationName, "internalFilter", null );
            }
        };
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private <CT extends Iterable<T>, T> CT filter(CT iter, User user, OperationType type, @Nullable String operationName, String methodName, @Nullable CustomEntityTranslator entityTranslator) throws FindException {
        List<T> removals = new ArrayList<T>();
        for (T element : iter) {
            Entity testEntity;
            if (element instanceof Entity) {
                testEntity = (Entity)element;
            } else if (element instanceof EntityHeader) {
                EntityHeader header = (EntityHeader) element;
                testEntity = entityFinder.find(header);
            } else if (entityTranslator != null && entityFinder != null) {
                testEntity = entityTranslator.locateEntity(element, entityFinder);
                if (testEntity == null && element != null) {
                    removals.add(element);
                }
            } else {
                throw new IllegalArgumentException("Element of collection was neither Entity nor EntityHeader (and no entityTranslator and/or entityFinder was available)");
            }

            if (testEntity != null && !rbacServices.isPermittedForEntity(user, testEntity, type, operationName)) {
                removals.add(element);
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST,
                            "Omitting {0} #{1} from return value of {2}",
                            new Object[] { testEntity.getClass().getSimpleName(), testEntity.getId(), methodName }
                    );
                }
            }
        }
        return removeFiltered(iter, removals);
    }

    private <CT extends Iterable<T>, T> CT removeFiltered(CT iter, List<T> removals) {
        if (iter instanceof Collection) {
            Collection<T> out;
            if (iter instanceof EntityHeaderSet) {
                out = new EntityHeaderSet((Set)iter);
            } else if (iter instanceof Set) {
                out = new HashSet<T>((Collection<? extends T>)iter);
            } else {
                out = new ArrayList<T>((Collection<? extends T>)iter);
            }

            for (T removal : removals)
                out.remove(removal);

            return (CT)out;
        } else if (iter instanceof CollectionUpdate) {
            CollectionUpdate collectionUpdate = (CollectionUpdate)iter;
            return (CT)CollectionUpdateFilterer.filter(collectionUpdate, removals);
        } else {
            throw new IllegalStateException("Unable to filter " + iter.getClass());
        }
    }

    private static void collectAnnotations(Class clazz, List<Secured> annotations) {
        //noinspection unchecked
        Secured secured = (Secured) clazz.getAnnotation(Secured.class);
        if (secured != null) annotations.add(secured);
        for (Class intf : clazz.getInterfaces()) {
            collectAnnotations(intf, annotations);
        }
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        final Object target = methodInvocation.getThis();
        final Object[] args = methodInvocation.getArguments();
        final Method method = methodInvocation.getMethod();
        final Class<?> rtype = method.getReturnType();

        final String mname = target.getClass().getName() + "." + method.getName();
        logger.log(Level.FINE, "Intercepted invocation of {0}", mname);

        final List<Secured> annotations = new ArrayList<Secured>();
        final CheckInfo check;
        String customInterceptorClassName = null;
        String customEntityTranslatorClassName = null;
        {
            Secured methodSecured = method.getAnnotation(Secured.class);
            if (methodSecured != null) annotations.add(methodSecured);

            collectAnnotations(target.getClass(), annotations);
            EntityType[] checkTypes = null;
            OperationType checkOperation = null;
            MethodStereotype checkStereotype = null;
            String checkOtherOperationName = null;
            int checkRelevantArg = -1;
            for (Secured secured : annotations) {
                if (secured.types() != null && checkTypes == null || (checkTypes.length == 1 && checkTypes[0] == EntityType.ANY)) {
                    checkTypes = secured.types();
                }

                if (secured.stereotype() != null && checkStereotype == null)
                    checkStereotype = secured.stereotype();

                if (secured.relevantArg() >= 0 && checkRelevantArg < 0)
                    checkRelevantArg = secured.relevantArg();

                if(checkOtherOperationName == null && !secured.otherOperation().trim().equals("")){  //first in wins
                    checkOtherOperationName = secured.otherOperation();    
                }

                if (secured.customInterceptor() != null && !secured.customInterceptor().trim().equals("")) {
                    if (customInterceptorClassName != null)
                        throw new IllegalStateException("More than one declared @Secured customInterceptorClassName applies to this method");
                    customInterceptorClassName = secured.customInterceptor();
                }

                if (secured.customEntityTranslatorClassName() != null && !secured.customEntityTranslatorClassName().trim().equals("")) {
                    if (customInterceptorClassName != null)
                        throw new IllegalStateException("More than one declared @Secured customEntityTranslatorClassName applies to this method");
                    customEntityTranslatorClassName = secured.customEntityTranslatorClassName();
                }
            }

            if (checkOperation == null && checkStereotype == null) throw new IllegalStateException("Security declaration for " + mname + " does not specify either an operation or a stereotype");
            if (checkTypes == null) throw new IllegalStateException("Security declaration for " + mname + " does not specify an entity type");

            check = new CheckInfo(mname, checkTypes, checkOperation, checkStereotype, checkRelevantArg, checkOtherOperationName);
        }

        final User user = JaasUtils.getCurrentUser();
        if (user == null) throw new IllegalStateException("Secured method " + mname + " invoked with Subject containing no User principal");

        if (customInterceptorClassName != null) {
            return invokeWithCustomInterceptor(methodInvocation, user, customInterceptorClassName, target.getClass().getClassLoader());
        }

        switch (check.stereotype) {
            case SAVE_OR_UPDATE:
                Entity entity = getEntityArg(check, args);
                if (entity != null) {
                    String id = entity.getId();
                    checkEntityBefore(check, args, id == null || DEFAULT_ID.equals(id) ? CREATE : UPDATE);
                    break;
                } else {
                    // this is incredibly ugly: Must have permission to update AND create ANY entity of all specified types; if so, we BYPASS any remaining checks
                    check.setBefore(CheckBefore.NONE);
                    check.setAfter(CheckAfter.NONE);
                    for (EntityType type : check.types) {
                        if (!rbacServices.isPermittedForAnyEntityOfType(user, UPDATE, type)) {
                            throw new PermissionDeniedException(UPDATE, type);
                        }
                        if (!rbacServices.isPermittedForAnyEntityOfType(user, CREATE, type)) {
                            throw new PermissionDeniedException(CREATE, type);
                        }
                    }
                    /* BYPASS further checks */
                    return methodInvocation.proceed();
                }
            case UPDATE:
                // Like SAVE_OR_UPDATE, but CREATE permission will not help you here -- only UPDATE permission will do, even if an entity is located and its OID is -1
                entity = getEntityArg(check, args);
                if (entity != null) {
                    // Must have permission to UPDATE this entity
                    checkEntityBefore(check, args, UPDATE);
                    break;
                } else {
                    // Must have permission to update ANY entity of all specified types; if so, we BYPASS any remaining checks
                    check.setBefore(CheckBefore.NONE);
                    check.setAfter(CheckAfter.NONE);
                    for (EntityType type : check.types) {
                        if (!rbacServices.isPermittedForAnyEntityOfType(user, UPDATE, type)) {
                            throw new PermissionDeniedException(UPDATE, type);
                        }
                    }
                    /* BYPASS further checks */
                    return methodInvocation.proceed();
                }
            case FIND_ENTITIES:
                check.operation = READ;
                if (Iterable.class.isAssignableFrom(rtype)) {
                    check.setBefore(CheckBefore.NONE);
                    check.setAfter(CheckAfter.COLLECTION);
                } else if (rtype.isArray() && (customEntityTranslatorClassName != null || Entity.class.isAssignableFrom(rtype.getComponentType()) || EntityHeader.class.isAssignableFrom(rtype.getComponentType()))) {
                    check.setBefore(CheckBefore.NONE);
                    check.setAfter(CheckAfter.COLLECTION);
                } else {
                    // Unsupported return value type; must be able to read all
                    check.setBefore(CheckBefore.ALL);
                    check.setAfter(CheckAfter.NONE);
                }
                break;
            case FIND_HEADERS:
                check.setBefore(CheckBefore.NONE);
                check.operation = READ;
                if (Iterable.class.isAssignableFrom(rtype) || rtype.isArray()) {
                    check.setAfter(CheckAfter.COLLECTION);
                } else if (EntityHeader.class.isAssignableFrom(rtype)) {
                    check.setAfter(CheckAfter.HEADER);
                } else {
                    // Unsupported return value type; must be able to read all
                    check.setBefore(CheckBefore.ALL);
                    check.setAfter(CheckAfter.NONE);
                }
                break;
            case FIND_ENTITY:
                // Check after; need to read entity before evaluating attribute predicates
                checkEntityAfter(check);
                break;
            case DELETE_ENTITY:
                // Check before to prevent illegal deletion
                checkEntityBefore(check, args, DELETE);
                break;
            case DELETE_BY_ID:
                // Check before to prevent illegal deletion
                checkEntityFromId(check, args, DELETE);
                break;
            case DELETE_IDENTITY_BY_ID:
                // Check before to prevent illegal deletion
                checkIdentityFromId(args, check, DELETE);
                break;
            case GET_IDENTITY_PROPERTY_BY_ID:
                checkIdentityFromId(args, check, READ);
                break;
            case SET_PROPERTY_BY_ID:
                checkEntityFromId(check, args, UPDATE);
                break;
            case GET_PROPERTY_BY_ID:
                checkEntityFromId(check, args, READ);
                break;
            case GET_PROPERTY_OF_ENTITY:
                checkEntityBefore(check, args, READ);
                break;
            case SET_PROPERTY_OF_ENTITY:
                checkEntityBefore(check, args, UPDATE);
                break;
            case SET_PROPERTY_BY_UNIQUE_ATTRIBUTE:
                check.setBefore(CheckBefore.ALL);
                check.setAfter(CheckAfter.NONE);
                check.operation = UPDATE;
                break;
            case DELETE_BY_UNIQUE_ATTRIBUTE:
            case DELETE_MULTI:
                check.setBefore(CheckBefore.ALL);
                check.setAfter(CheckAfter.NONE);
                check.operation = DELETE;
                break;
            case ENTITY_OPERATION:
                checkEntityFromId(check, args, OperationType.OTHER);
                break;
            case SAVE:
                entity = getEntityArg(check, args);
                if (entity != null) {
                    String id = entity.getId();
                    if (!DEFAULT_ID.equals(id))
                        throw new PermissionDeniedException(OTHER, entity, "re-create existing");
                    checkEntityBefore(check, args, CREATE);
                    break;
                } else {
                    // Must have permission to create any new entity of all required types
                    check.setBefore(CheckBefore.NONE);
                    check.setAfter(CheckAfter.NONE);
                    for (EntityType type : check.types) {
                        if (!rbacServices.isPermittedForAnyEntityOfType(user, CREATE, type)) {
                            throw new PermissionDeniedException(CREATE, type);
                        }
                    }
                    /* BYPASS further checks */
                    return methodInvocation.proceed();
                }
            default:
                throw new UnsupportedOperationException("Security declaration for method " + mname + " specifies unsupported stereotype " + check.stereotype.name());
        }

        if (check.getBefore() == null) throw new NullPointerException("check.before");
        if (check.getAfter() == null) throw new NullPointerException("check.after");
        if (check.operation == null || check.operation == OperationType.NONE)
            throw new NullPointerException("check.operation");

        switch (check.getBefore()) {
            case ENTITY:
                if (check.entity == null) throw new NullPointerException("check.entity");
                if (!rbacServices.isPermittedForEntity(user, check.entity, check.operation, check.otherOperationName)) {
                    throw new PermissionDeniedException(check.operation, check.entity, check.otherOperationName);
                }
                break;
            case ID:
                if (check.id == null) throw new NullPointerException("check.id");
                if (check.types.length > 1)
                    throw new IllegalStateException("Security declaration for method " + mname + " needs to check ID, but multiple EntityTypes specified");
                Entity entity = entityFinder.find(check.types[0].getEntityClass(), check.id);
                if (entity == null)
                    throw new IllegalStateException("Unable to locate " + check.types[0] + " #" + check.id);
                if (!rbacServices.isPermittedForEntity(user, entity, check.operation, check.otherOperationName)) {
                    throw new PermissionDeniedException(check.operation, entity, check.otherOperationName);
                }
                break;
            case ALL:
                for (EntityType checkType : check.types) {
                    if (!rbacServices.isPermittedForAnyEntityOfType(user, check.operation, checkType)) {
                        throw new PermissionDeniedException(check.operation, checkType);
                    }
                }
                break;
            case NONE:
                break;
        }

        Object rv = methodInvocation.proceed();
        if (rv == null) return null;

        switch(check.getAfter()) {
            case HEADER:
                if (rv instanceof EntityHeader) {
                    EntityHeader header = (EntityHeader)rv;
                    rv = new EntityHeader[] { header };
                    // fallthrough
                } else if (rv != null) {
                    throw new IllegalStateException("check.after was HEADER but return value is not an EntityHeader");
                }
            //noinspection fallthrough
            case COLLECTION:
                boolean skip = true;
                for (EntityType type : check.types) {
                    // Avoid needlessly querying permissions on every member of the collection if this user has
                    // blanket permissions
                    if (!rbacServices.isPermittedForAnyEntityOfType(user, check.operation, type)) {
                        skip = false;
                        continue;
                    }

                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST,
                                "User is permitted to {0} any {1}; may be able to skip individual permission checks",
                                new Object[] { check.operation.name(), type.name() }
                                );
                    }
                }

                if (skip) {
                    return unwrapHeader(rv, rv, check);
                } else if (rv instanceof EntityHeader[]) {
                    EntityHeader[] array = (EntityHeader[]) rv;
                    List<EntityHeader> headers = filter(Arrays.asList(array), user, check.operation, check.otherOperationName, check.methodName, null);
                    Object[] a0 = (Object[]) Array.newInstance(array.getClass().getComponentType(), 0);
                    return unwrapHeader(headers.toArray(a0), rv, check);
                } else if (rv instanceof Entity[]) {
                    Entity[] array = (Entity[]) rv;
                    List<Entity> entities = filter(Arrays.asList(array), user, check.operation, check.otherOperationName, check.methodName, null);
                    Object[] a0 = (Object[]) Array.newInstance(array.getClass().getComponentType(), 0);
                    return entities.toArray(a0);
                } else if (rv instanceof Iterable) {
                    CustomEntityTranslator customEntityTranslator = customEntityTranslatorClassName == null
                        ? null
                        : findCustomEntityTranslator(customEntityTranslatorClassName, target.getClass().getClassLoader());
                    return filter((Iterable) rv, user, check.operation, check.otherOperationName, check.methodName, customEntityTranslator);
                } else if (rv instanceof Object[] && customEntityTranslatorClassName != null) {
                    CustomEntityTranslator customEntityTranslator = findCustomEntityTranslator(customEntityTranslatorClassName, target.getClass().getClassLoader());
                    Object[] array = (Object[])rv;
                    List<Object> elements = filter(Arrays.asList(array), user, check.operation, check.otherOperationName, check.methodName, customEntityTranslator);
                    Object[] a0 = (Object[]) Array.newInstance(array.getClass().getComponentType(), 0);
                    return elements.toArray(a0);
                } else {
                    throw new IllegalStateException("Return value of " + mname + " was not Entity[], EntityHeader[] or Iterable and no custom translator is specified");
                }
            case ENTITY:
                if (!(rv instanceof Entity)) {
                    throw new IllegalStateException("Return value of " + mname + " was not an Entity");
                }

                Entity entity = (Entity)rv;

                if (!rbacServices.isPermittedForEntity(user, entity, check.operation, null))
                    throw new PermissionDeniedException(check.operation, entity, check.otherOperationName);
                return rv;
            case NONE:
                logger.log(Level.FINE, "Permitted {0} on {1} for {2}",
                        new Object[]{check.operation.name(),
                                getEntityName(check.entity),
                                mname});
                return rv;
            default:
                String ename = getEntityName(check.entity);
                Level level = (check.entity == null) ? Level.INFO : Level.FINE;
                logger.log(level, "Permitted {0} on {1} for {2}", 
                        new Object[]{check.operation.name(), ename, mname});
                return rv;
        }
    }

    private Object invokeWithCustomInterceptor(@NotNull MethodInvocation methodInvocation, @NotNull User user, @NotNull String customInterceptorClassName, @NotNull ClassLoader classLoader) throws Throwable {
        final CustomRbacInterceptor ci;
        try {
            ci = findCustomInterceptor(customInterceptorClassName, classLoader);
            injector().inject(ci);
            ci.setUser(user);
        } catch (Exception e) {
            final String msg = "Unable to create custom RBAC interceptor " + customInterceptorClassName + ": " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            throw new IllegalStateException(msg); // avoid chaining back to the client in this case -- the caught exception likely includes internal information and/or object references
        }
        return ci.invoke(methodInvocation);
    }

    private CustomRbacInterceptor findCustomInterceptor(String customInterceptorClassName, ClassLoader classLoader) {
        // TODO pool custom interceptor instances to avoid paying for autowiring on every invocation
        try {
            final Class<?> interceptorClass = classLoader.loadClass(customInterceptorClassName);
            if (!CustomRbacInterceptor.class.isAssignableFrom(interceptorClass))
                throw new ClassCastException("Custom RBAC interceptor is not an instance of CustomRbacInterceptor");
            return (CustomRbacInterceptor) interceptorClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Custom RBAC interceptor class not found: " + ExceptionUtils.getMessage(e), e);
        } catch (InstantiationException e) {
            throw new IllegalStateException("Custom RBAC interceptor class not instantiable: " + ExceptionUtils.getMessage(e), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Custom RBAC interceptor class not accessible: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private CustomEntityTranslator findCustomEntityTranslator(String customEntityTranslatorClassName, ClassLoader classLoader) {
        try {
            final Class<?> translatorClass = classLoader.loadClass(customEntityTranslatorClassName);
            if (!CustomEntityTranslator.class.isAssignableFrom(translatorClass))
                throw new ClassCastException("Custom entity translator is not an instance of CustomEntityTranslator");
            return (CustomEntityTranslator) translatorClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Custom entity translator class not found: " + ExceptionUtils.getMessage(e), e);
        } catch (InstantiationException e) {
            throw new IllegalStateException("Custom entity translator class not instantiable: " + ExceptionUtils.getMessage(e), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Custom entity translator class not accessible: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private Injector injector() {
        return applicationContext.getBean("injector", Injector.class);
    }

    private Object unwrapHeader( final Object rv, final Object preFilter, final CheckInfo check ) {
        Object result = rv;

        // unwrap in case of HEADER
        if ( CheckAfter.HEADER == check.getAfter() && rv instanceof EntityHeader[] && preFilter instanceof EntityHeader[] ) {
            final EntityHeader[] header = (EntityHeader[]) rv;
            final EntityHeader[] originalHeader = (EntityHeader[]) preFilter;
            if ( header.length == 1 ) {
                result = header[0];
            } else if ( header.length == 0 && originalHeader.length == 1 ) { // not permitted, so throw
                throw new PermissionDeniedException(check.operation, originalHeader[0].getType());
            } else { // something went wrong
                throw new IllegalStateException("Unable to unwrap header result.");
            }
        }

        return result;
    }

    private void checkEntityAfter(CheckInfo check) {
        check.setBefore(CheckBefore.NONE);
        check.operation = READ;
        check.setAfter(CheckAfter.ENTITY);
    }

    private void checkIdentityFromId(Object[] args, CheckInfo check, OperationType operation) throws FindException {
        if (args[0] instanceof Long && args[1] instanceof String) {
            IdentityHeader header = new IdentityHeader((Long)args[0], (String)args[1], check.types[0], null, null, null, null);
            Entity ent = entityFinder.find(header);
            check.setBefore(CheckBefore.ENTITY);
            check.operation = operation;
            check.entity = ent;
            check.setAfter(CheckAfter.NONE);
        } else {
            throwNoId(check);
        }
    }

    private void checkEntityFromId(CheckInfo check, Object[] args, OperationType operation) throws FindException {

        Serializable id = getIdArg(check, args);
        if (id == null) throwNoId(check);

        check.setBefore(CheckBefore.ENTITY);
        check.operation = operation;
        FindException findException = null;
        for(EntityType type: check.types){
            try {
                check.entity = entityFinder.find(type.getEntityClass(), id);
                if(check.entity != null) break;
            } catch (FindException e) {
                //we are only going to remember the last FindException, if one is thrown
                findException = e;
            } 
        }
        
        if(check.entity == null){
            if(findException != null){
                throw findException;
            }else{
                throw new ObjectNotFoundException("Entity not found for any declared type");
            }
        }
        check.setAfter(CheckAfter.NONE);
    }

    private String getEntityName(Entity entity) {
        if (entity instanceof NamedEntity) {
            return ((NamedEntity)entity).getName();
        } else if (entity != null) {
            return entity.getClass().getSimpleName() + " #" + entity.getId();
        } else {
            return "<unknown>";
        }
    }

    private void checkEntityBefore(CheckInfo check, Object[] args, OperationType operation) {
        getEntityArgOrThrow(check, args);
        logger.log(Level.FINER, "Will check Entity before invocation");
        check.setBefore(CheckBefore.ENTITY);
        check.operation = operation;
        check.setAfter(CheckAfter.NONE);
    }

    private Entity getEntityArgOrThrow(CheckInfo info, Object[] args) {
        Entity ent = getEntityArg(info, args);
        if (ent != null) return ent;
        throw new IllegalStateException("Security declaration for method " +
                info.methodName + " specifies " + info.stereotype +
                ", but can't find Entity argument");

    }

    private Entity getEntityArg(CheckInfo info, Object[] args) {
        Object arg = getSingleOrRelevantArg(args, info.relevantArg);
        if (arg == null) return null;

        if (arg instanceof Entity) {
            info.entity = (Entity)arg;
            return info.entity;
        }

        return null;
    }

    private void throwNoId(CheckInfo info) {
        throw new IllegalStateException("Security declaration for method " +
                info.methodName + " specifies " + info.stereotype +
                ", but can't find appropriate argument(s)");
    }

    private Serializable getIdArg(CheckInfo info, Object[] args) {
        Object arg = getSingleOrRelevantArg(args, info.relevantArg);
        if (arg == null) return null;

        if (arg instanceof Long || arg instanceof String) {
            info.id = arg.toString();
            return arg.toString();
        }

        return null;
    }

    private Object getSingleOrRelevantArg(Object[] args, int relevantArg) {
        if (args.length == 1) {
            return args[0];
        } else if (args.length > 1 && relevantArg >= 0 && relevantArg < args.length) {
            return args[relevantArg];
        } else {
            //Unable to determine relevant argument from method invocation. Hint: Was relevantArg specified in the @secured annotation?
            return null;
        }
    }
}

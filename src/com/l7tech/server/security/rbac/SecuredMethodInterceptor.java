/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.*;
import static com.l7tech.common.security.rbac.OperationType.*;
import com.l7tech.identity.AnonymousIdentityReference;
import com.l7tech.identity.Identity;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.event.EntityInvalidationEvent;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.collections.iterators.ArrayIterator;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.security.auth.Subject;
import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.security.AccessController;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class SecuredMethodInterceptor implements MethodInterceptor, ApplicationListener {
    private static final Logger logger = Logger.getLogger(SecuredMethodInterceptor.class.getName());
    private final RoleManager roleManager;

    public SecuredMethodInterceptor(RoleManager roleManager) {
        this.roleManager = roleManager;
        logger.log(Level.INFO, this.getClass().getSimpleName() + " initialized");
    }

    private List filter(Iterator iter, User user, CheckInfo check) throws FindException {
        List newlist = new ArrayList();
        while (iter.hasNext()) {
            Object element = iter.next();
            Entity testEntity;
            if (element instanceof Entity) {
                testEntity = (Entity) element;
            } else if (element instanceof EntityHeader) {
                EntityHeader header = (EntityHeader) element;
                testEntity = AnonymousEntityReference.fromHeader(header);
            } else {
                throw new IllegalArgumentException("Element of collection was neither Entity nor EntityHeader");
            }

            if (roleManager.isPermittedForEntity(user, testEntity, check.operation, check.otherOperationName)) {
                newlist.add(element);
            } else {
                logger.info("Omitting " + testEntity.getClass().getSimpleName() + " #" + testEntity.getOid() + " from return value of " + check.mname);
            }
        }
        return newlist;
    }

    private static void collectAnnotations(Class clazz, List<Secured> annotations) {
        //noinspection unchecked
        Secured secured = (Secured) clazz.getAnnotation(Secured.class);
        if (secured != null) annotations.add(secured);
        for (Class intf : clazz.getInterfaces()) {
            collectAnnotations(intf, annotations);
        }
    }

    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Object target = methodInvocation.getThis();
        Object[] args = methodInvocation.getArguments();
        Method method = methodInvocation.getMethod();

        String mname = target.getClass().getName() + "." + method.getName();
        logger.log(Level.FINE, "Intercepted invocation of {0}", mname);

        List<Secured> annotations = new ArrayList<Secured>();
        CheckInfo check;
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

                if (secured.operation() != null && checkOperation == null)
                    checkOperation = secured.operation();

                if (secured.stereotype() != null && checkStereotype == null)
                    checkStereotype = secured.stereotype();

                if (secured.relevantArg() >= 0 && checkRelevantArg < 0)
                    checkRelevantArg = secured.relevantArg();

                if (secured.otherOperationName() != null && checkOtherOperationName == null)
                    checkOtherOperationName = secured.otherOperationName();
            }

            if (checkOperation == null && checkStereotype == null) throw new IllegalStateException("Security declaration for " + mname + " does not specify either an operation or a stereotype");
            if (checkTypes == null) throw new IllegalStateException("Security declaration for " + mname + " does not specify an entity type");

            check = new CheckInfo(mname, checkTypes, checkOperation, checkStereotype, checkRelevantArg, checkOtherOperationName);
        }

        Subject subject = Subject.getSubject(AccessController.getContext());
        if (subject == null) throw new IllegalStateException("Secured method " + mname + " invoked with no Subject active");
        Set<User> users = subject.getPrincipals(User.class);
        if (users == null || users.isEmpty()) throw new IllegalStateException("Secured method " + mname + " invoked with Subject containing no User principal");
        User user = users.iterator().next();

        switch (check.operation) {
            case CREATE:
            case UPDATE:
                checkEntityBefore(check, args, UPDATE);
                break;
            case READ:
                checkOidBefore(check, args, READ);
                check.after = CheckAfter.ENTITY;
                break;
            case DELETE:
                if (getEntityArg(check, args) != null) {
                    checkEntityBefore(check, args, DELETE);
                } else {
                    checkOidBefore(check, args, DELETE);
                }
                break;
            case OTHER:
                if (check.otherOperationName == null || check.otherOperationName.length() == 0)
                    throw new IllegalStateException("Security declaration for method " +
                            mname + " specifies " + check.operation +
                            ", but does not specify otherOperationName");

                if (getEntityArg(check, args) != null) {
                    check.before = CheckBefore.ENTITY;
                } else {
                    getOidArgOrThrow(check, args);
                    check.before = CheckBefore.OID;
                }

                check.after = CheckAfter.NONE;
                break;
            default:
                methodSwitch:
                switch (check.stereotype) {
                    case SAVE_OR_UPDATE:
                        Entity entity = getEntityArg(check, args);
                        if (entity != null) {
                            checkEntityBefore(check, args, entity.getOid() == Entity.DEFAULT_OID ? CREATE : UPDATE);
                            break;
                        } else {
                            // TODO this is incredibly ugly
                            check.before = CheckBefore.NONE;
                            check.after = CheckAfter.NONE;
                            for (EntityType type : check.types) {
                                if (!roleManager.isPermittedForAllEntities(user, type, UPDATE)) {
                                    throw new PermissionDeniedException(UPDATE, type);
                                }
                                if (!roleManager.isPermittedForAllEntities(user, type, CREATE)) {
                                    throw new PermissionDeniedException(CREATE, type);
                                }
                            }
                            return methodInvocation.proceed();
                        }
                    case FIND_ENTITIES:
                        check.before = CheckBefore.NONE;
                        check.operation = READ;
                        if (Collection.class.isAssignableFrom(method.getReturnType()) ||
                                method.getReturnType().isArray()) {
                            check.after = CheckAfter.COLLECTION;
                        } else {
                            // Unsupported return value type; must be able to read all
                            check.before = CheckBefore.ALL;
                        }
                        break;
                    case FIND_HEADERS:
                        check.before = CheckBefore.NONE;
                        check.operation = READ;
                        check.after = CheckAfter.COLLECTION;
                        break;
                    case FIND_BY_PRIMARY_KEY:
                        OidArgResult result = getOidArg(check, args);
                        check.operation = READ;
                        switch (result) {
                            case FOUND:
                                logger.log(Level.FINER, "Will check OID before invocation");
                                check.before = CheckBefore.OID;
                                check.after = CheckAfter.NONE;
                                break methodSwitch;
                            case INVALID:
                                check.before = CheckBefore.NONE;
                                check.after = CheckAfter.ENTITY;
                                logger.log(Level.FINER, "Will check Entity after invocation");
                                break methodSwitch;
                            case NOT_FOUND:
                                throwNoOid(check);
                                break methodSwitch;
                        }
                    case FIND_ENTITY_BY_ATTRIBUTE:
                        check.before = CheckBefore.NONE;
                        check.operation = READ;
                        check.after = CheckAfter.ENTITY;
                        break;
                    case DELETE_ENTITY:
                        checkEntityBefore(check, args, DELETE);
                        break;
                    case DELETE_BY_OID:
                        if (check.types.length != 1)
                            throw new IllegalStateException("Security declaration for method " + mname + " specifies DELETE_BY_OID, but has multiple types");
                        checkOidBefore(check, args, DELETE);
                        break;
                    case GET_PROPERTY_BY_OID:
                        checkOidBefore(check, args, READ);
                        break;
                    case SET_PROPERTY_BY_OID:
                        checkOidBefore(check, args, UPDATE);
                        break;
                    case GET_PROPERTY_OF_ENTITY:
                        checkEntityBefore(check, args, READ);
                        break;
                    case SET_PROPERTY_OF_ENTITY:
                        checkEntityBefore(check, args, UPDATE);
                        break;
                    default:
                        throw new UnsupportedOperationException("Security declaration for method " + mname + " specifies unsupported stereotype " + check.stereotype.name());
                }
        }

        if (check.before == null) throw new NullPointerException("check.before");
        if (check.after == null) throw new NullPointerException("check.after");
        if (check.operation == null || check.operation == OperationType.NONE)
            throw new NullPointerException("check.operation");

        switch(check.before) {
            case ENTITY:
                if (check.entity == null) throw new NullPointerException("check.entity");
                if (!roleManager.isPermittedForEntity(user, check.entity, check.operation, null)) {
                    throw new PermissionDeniedException(check.operation, check.entity);
                }
                break;
            case OID:
                if (check.oid == Entity.DEFAULT_OID) throw new NullPointerException("check.oid");
                if (check.types.length > 1) throw new IllegalStateException("Security declaration for method " + mname + " needs to check OID, but multiple EntityTypes specified");
                final AnonymousEntityReference entity = new AnonymousEntityReference(check.types[0].getEntityClass(), check.oid);
                if (!roleManager.isPermittedForEntity(user, entity, check.operation, check.otherOperationName)) {
                    throw new PermissionDeniedException(check.operation, entity);
                }
                break;
            case ALL:
                for (EntityType checkType : check.types) {
                    if (!roleManager.isPermittedForAllEntities(user, checkType, check.operation)) {
                        throw new PermissionDeniedException(check.operation, checkType);
                    }
                }
                break;
        }

        Object rv = methodInvocation.proceed();

        if (rv == null) return null;

        switch(check.after) {
            case COLLECTION:
                if (rv instanceof EntityHeader[]) {
                    EntityHeader[] array = (EntityHeader[]) rv;
                    List headers = filter(new ArrayIterator(array), user, check);
                    Object[] a0 = (Object[]) Array.newInstance(array.getClass().getComponentType(), 0);
                    return headers.toArray(a0);
                } else if (rv instanceof Entity[]) {
                    Entity[] array = (Entity[]) rv;
                    List entities = filter(new ArrayIterator(array), user, check);
                    Object[] a0 = (Object[]) Array.newInstance(array.getClass().getComponentType(), 0);
                    return entities.toArray(a0);
                } else if (rv instanceof Collection) {
                    // TODO check generic type?
                    Collection coll = (Collection) rv;
                    return filter(coll.iterator(), user, check);
                } else {
                    throw new IllegalStateException("Return value of " + mname + " was not Entity[] or Collection<Entity>");
                }
            case ENTITY:
                Entity entity;
                if (rv instanceof Entity) {
                    entity = (Entity) rv;
                } else if (rv instanceof Identity) {
                    // Replace LDAP users and groups with a reasonable stand-in
                    Identity id = (Identity) rv;
                    entity = new AnonymousIdentityReference(id.getClass(), id.getUniqueIdentifier(), id.getProviderId(), id.getName());
                } else {
                    throw new IllegalStateException("Return value of " + mname + " was not an Entity");
                }
                    
                if (!roleManager.isPermittedForEntity(user, entity, check.operation, null))
                    throw new PermissionDeniedException(check.operation, (Entity)rv);
                return entity;
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

    private String getEntityName(Entity entity) {
        if (entity instanceof NamedEntity) {
            return ((NamedEntity)entity).getName();
        } else if (entity != null) {
            return entity.getClass().getSimpleName() + " #" + entity.getOid();
        } else {
            return "<unknown>";
        }
    }

    private void checkOidBefore(CheckInfo check, Object[] args, OperationType operation) {
        getOidArgOrThrow(check, args);
        logger.log(Level.FINER, "Will check OID before invocation");
        check.before = CheckBefore.OID;
        check.operation = operation;
        check.after = CheckAfter.NONE;
    }

    private void checkEntityBefore(CheckInfo check, Object[] args, OperationType operation) {
        getEntityArgOrThrow(check, args);
        logger.log(Level.FINER, "Will check Entity before invocation");
        check.before = CheckBefore.ENTITY;
        check.operation = operation;
        check.after = CheckAfter.NONE;
    }

    private Entity getEntityArgOrThrow(CheckInfo info, Object[] args) {
        Entity ent = getEntityArg(info, args);
        if (ent != null) return ent;
        throw new IllegalStateException("Security declaration for method " +
                info.mname + " specifies " + info.stereotype +
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

    private void getOidArgOrThrow(CheckInfo info, Object[] args) {
        if (getOidArg(info, args) != OidArgResult.FOUND)
            throwNoOid(info);
    }

    private void throwNoOid(CheckInfo info) {
        throw new IllegalStateException("Security declaration for method " +
                info.mname + " specifies " + info.stereotype +
                ", but can't find long or String argument");
    }

    private OidArgResult getOidArg(CheckInfo info, Object[] args) {
        Object arg = getSingleOrRelevantArg(args, info.relevantArg);
        if (arg == null) return OidArgResult.NOT_FOUND;

        if (arg instanceof Long) {
            info.oid = (Long)arg;
            return OidArgResult.FOUND;
        } else if (arg instanceof String) {
            try {
                info.oid = Long.valueOf((String)arg);
            } catch (NumberFormatException e) {
                logger.fine("Argument is not a valid long; must check entity after invocation");
                return OidArgResult.INVALID;
            }
            return OidArgResult.FOUND;
        }

        return OidArgResult.NOT_FOUND;
    }

    private static enum OidArgResult {
        FOUND, NOT_FOUND, INVALID
    }

    private Object getSingleOrRelevantArg(Object[] args, int relevantArg) {
        if (args.length == 1) {
            return args[0];
        } else if (args.length > 1 && relevantArg >= 0 && relevantArg < args.length) {
            return args[relevantArg];
        } else {
            return null;
        }
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent eie = (EntityInvalidationEvent) event;
            if (Role.class.isAssignableFrom(eie.getEntityClass())) {
                for (long oid : eie.getEntityIds()) {
                    try {
                        // Refresh cached roles, if they still exist
                        roleManager.getCachedEntity(oid, 0);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Couldn't refresh cached Role", e);
                    }
                }
            }
        }
    }
}

/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.common.security.rbac.*;
import static com.l7tech.common.security.rbac.OperationType.*;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.AnonymousEntityReference;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.server.event.EntityInvalidationEvent;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.security.auth.Subject;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.util.Collection;
import java.util.Set;
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
    }

    private static class CheckInfo {
        private CheckInfo(String mname) {
            this.mname = mname;
        }

        private final String mname;
        private CheckBefore before;
        private CheckAfter after;
        private OperationType operation;
        private String otherOperationName = null;
        private long oid = Entity.DEFAULT_OID;
        private Entity entity = null;
    }

    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Object target = methodInvocation.getThis();
        Object[] args = methodInvocation.getArguments();
        Method method = methodInvocation.getMethod();

        String mname = target.getClass().getName() + "." + method.getName();

        Secured beanSecured = target.getClass().getAnnotation(Secured.class);
        if (beanSecured == null) {
            for (Class intf : target.getClass().getInterfaces()) {
                //noinspection unchecked
                beanSecured = (Secured)intf.getAnnotation((Class) Secured.class);
                if (beanSecured == null) break;
            }
        }
        if (beanSecured == null) throw new IllegalArgumentException("Class " + target.getClass().getName() + " has no security declaration");

        Secured methodSecured = method.getAnnotation(Secured.class);
        if (methodSecured == null) throw new IllegalArgumentException("Method " + mname + " has no security declaration");

        Subject subject = Subject.getSubject(AccessController.getContext());
        if (subject == null) throw new IllegalStateException("Secured method " + mname + " invoked with no Subject active");
        Set<User> users = subject.getPrincipals(User.class);
        if (users == null || users.isEmpty()) throw new IllegalStateException("Secured method " + mname + " invoked with Subject containing no User principal");
        User user = users.iterator().next();

        EntityType[] checkTypes = methodSecured.types();
        if (checkTypes == null) checkTypes = beanSecured.types();
        if (checkTypes == null) throw new IllegalStateException("Secured method " + mname + " does not specify an entity type");

        CheckInfo check = new CheckInfo(mname);

        switch(methodSecured.operation()) {
            case CREATE:
            case UPDATE:
                checkEntityBefore(check, methodSecured, args, methodSecured.operation());
                break;
            case READ:
                checkOidBefore(check, methodSecured, args, READ);
                check.after = CheckAfter.ENTITY;
                break;
            case DELETE:
                if (getEntityArg(check, methodSecured, args)) {
                    checkEntityBefore(check, methodSecured, args, DELETE);
                } else {
                    checkOidBefore(check, methodSecured, args, DELETE);
                }
                break;
            case OTHER:
                if (methodSecured.otherOperationName() == null || methodSecured.otherOperationName().length() == 0)
                    throw new IllegalStateException("Security declaration for method " +
                            mname + " specifies " + methodSecured.operation() +
                            ", but does not specify otherOperationName");

                if (getEntityArg(check, methodSecured, args)) {
                    check.before = CheckBefore.ENTITY;
                } else {
                    getOidArgOrThrow(check, methodSecured, args);
                    check.before = CheckBefore.OID;
                }

                check.operation = OTHER;
                check.otherOperationName = methodSecured.otherOperationName();
                check.after = CheckAfter.NONE;
                break;
            default:
                switch(methodSecured.stereotype()) {
                    case SAVE_OR_UPDATE:
                        if (getEntityArg(check, methodSecured, args)) {
                            checkEntityBefore(check, methodSecured, args, createOrWrite(check.entity));
                            break;
                        } else {
                            // TODO this is incredibly ugly
                            check.before = CheckBefore.NONE;
                            check.after = CheckAfter.NONE;
                            for (EntityType type : checkTypes) {
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
                            method.getReturnType().isArray())
                        {
                            check.after = CheckAfter.COLLECTION;
                        } else {
                            // Unsupported return value type; must be able to read all
                            check.before = CheckBefore.ALL;
                        }
                        break;
                    case FIND_HEADERS:
                        // Anyone can list currently
                        check.before = CheckBefore.NONE;
                        check.operation = READ;
                        check.after = CheckAfter.NONE;
                        break;
                    case FIND_BY_PRIMARY_KEY:
                        checkOidBefore(check, methodSecured, args, READ);
                        check.after = CheckAfter.ENTITY;
                        break;
                    case FIND_ENTITY_BY_ATTRIBUTE:
                        check.before = CheckBefore.NONE;
                        check.operation = READ;
                        check.after = CheckAfter.ENTITY;
                        break;
                    case DELETE_ENTITY:
                        checkEntityBefore(check, methodSecured, args, DELETE);
                        break;
                    case DELETE_BY_OID:
                        if (checkTypes.length != 1) throw new IllegalStateException("Security declaration for method " + mname + " specifies DELETE_BY_OID, but has multiple types");
                        checkOidBefore(check, methodSecured, args, DELETE);
                        break;
                    case GET_PROPERTY_BY_OID:
                        checkOidBefore(check, methodSecured, args, READ);
                        break;
                    case SET_PROPERTY_BY_OID:
                        checkOidBefore(check, methodSecured, args, UPDATE);
                        break;
                    case GET_PROPERTY_OF_ENTITY:
                        checkEntityBefore(check, methodSecured, args, READ);
                        break;
                    case SET_PROPERTY_OF_ENTITY:
                        checkEntityBefore(check, methodSecured, args, UPDATE);
                    default:
                        throw new UnsupportedOperationException("Security declaration for method " + mname + " specifies unsupported stereotype " + methodSecured.stereotype().name());
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
                if (checkTypes.length > 1) throw new IllegalStateException("Security declaration for method " + mname + " needs to check OID, but multiple EntityTypes specified");
                final AnonymousEntityReference entity = new AnonymousEntityReference(checkTypes[0].getEntityClass(), check.oid);
                if (!roleManager.isPermittedForEntity(user, entity, check.operation, check.otherOperationName)) {
                    throw new PermissionDeniedException(check.operation, entity);
                }
                break;
            case ALL:
                for (EntityType checkType : checkTypes) {
                    if (!roleManager.isPermittedForAllEntities(user, checkType, check.operation)) {
                        throw new PermissionDeniedException(check.operation, checkType);
                    }
                }
                break;
        }

        Object rv = methodInvocation.proceed();

        switch(check.after) {
            case COLLECTION:
                logger.log(Level.INFO, "Not yet implemented: invoking {0}; should check {1} after invocation", new Object[] { mname, check.after.name() });
                return rv;
            case ENTITY:
                if (rv instanceof Entity) {
                    Entity entity = (Entity) rv;
                    if (!roleManager.isPermittedForEntity(user, entity, check.operation, null))
                        throw new PermissionDeniedException(check.operation, (Entity)rv);
                    return entity;
                } else {
                    throw new IllegalStateException("Return value of " + mname + " was not an Entity");
                }
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

    private void checkOidBefore(CheckInfo check, Secured methodSecured, Object[] args, OperationType operation) {
        getOidArgOrThrow(check, methodSecured, args);
        check.before = CheckBefore.OID;
        check.operation = operation;
        check.after = CheckAfter.NONE;
    }

    private void checkEntityBefore(CheckInfo check, Secured methodSecured, Object[] args, OperationType operation) {
        getEntityArgOrThrow(check, methodSecured, args);
        check.before = CheckBefore.ENTITY;
        check.operation = operation;
        check.after = CheckAfter.NONE;
    }

    private void getEntityArgOrThrow(CheckInfo info, Secured methodSecured, Object[] args) {
        if (!getEntityArg(info, methodSecured, args))
            throw new IllegalStateException("Security declaration for method " +
                    info.mname + " specifies " + methodSecured.stereotype() +
                    ", but can't find Entity argument");

    }

    private boolean getEntityArg(CheckInfo info, Secured methodSecured, Object[] args) {
        Object arg = getSingleOrRelevantArg(methodSecured, args);
        if (arg == null) return false;

        if (arg instanceof Entity) {
            info.entity = (Entity)arg;
            return true;
        }

        return false;
    }

    private void getOidArgOrThrow(CheckInfo info, Secured methodSecured, Object[] args) {
        if (!getOidArg(info, methodSecured, args))
            throw new IllegalStateException("Security declaration for method " +
                    info.mname + " specifies " + methodSecured.stereotype() +
                    ", but can't find long or String argument");
    }

    private boolean getOidArg(CheckInfo info, Secured methodSecured, Object[] args) {
        Object arg = getSingleOrRelevantArg(methodSecured, args);
        if (arg == null) return false;

        if (arg instanceof Long) {
            info.oid = (Long)arg;
            return true;
        } else if (arg instanceof String) {
            info.oid = Long.valueOf((String)arg);
            return true;
        }

        return false;
    }

    private Object getSingleOrRelevantArg(Secured methodSecured, Object[] args) {
        int relevant = methodSecured.relevantArg();
        if (args.length == 1) {
            return args[0];
        } else if (args.length > 1 && relevant >= 0 && relevant < args.length) {
            return args[relevant];
        } else {
            return null;
        }
    }

    private OperationType createOrWrite(Entity checkEntity) {
        return checkEntity.getOid() == Entity.DEFAULT_OID ? CREATE : UPDATE;
    }

    private static enum CheckBefore {
        /** Nothing to check before invocation */
        NONE,

        /** Check permission to execute operation against entity with specified OID */
        OID,

        /** Check permission to execute operation against specified Entity */
        ENTITY,

        /** Check permission to execute operation against all entities of type */
        ALL
    }

    private static enum CheckAfter {
        /** Nothing to check after invocation */
        NONE,

        /** Return value must be Entity; check permission to execute operation against it */
        ENTITY,

        /**
         * Return value is a Collection&lt;Entity&gt;; check permission to execute operation
         * against all entities in collection
         */
        COLLECTION
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

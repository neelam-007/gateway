/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.common.security.rbac.*;
import static com.l7tech.common.security.rbac.EntityType.GROUP;
import static com.l7tech.common.security.rbac.EntityType.USER;
import static com.l7tech.common.security.rbac.OperationType.*;
import com.l7tech.identity.PersistentGroup;
import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.DeletedEntity;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
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

    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Object target = methodInvocation.getThis();
        Object[] args = methodInvocation.getArguments();
        Method method = methodInvocation.getMethod();

        String mname = target.getClass().getName() + "." + method.getName();

        Secured beanSecured = target.getClass().getAnnotation(Secured.class);
        if (beanSecured == null) {
            for (Class intf : target.getClass().getInterfaces()) {
                //noinspection unchecked
                beanSecured = (Secured)intf.getAnnotation(Secured.class);
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
        EnumSet types = EnumSet.copyOf(Arrays.asList(checkTypes));

        Entity checkEntity = null;
        long checkOid = Entity.DEFAULT_OID;
        OperationType checkOperation;
        CheckAfterType checkAfter = CheckAfterType.NONE;

        switch(methodSecured.stereotype()) {
            case SAVE_OR_UPDATE:
                if (args.length == 1 && args[0] instanceof Entity) {
                    checkEntity = (Entity) args[0];
                    checkOperation = createOrWrite(checkEntity);
                    break;
                } else if (args.length >= 2 && args[0] instanceof Long &&
                        args[1] instanceof PersistentUser &&
                        checkTypes.length == 1 && checkTypes[0] == USER)
                {
                    checkEntity = (PersistentUser)args[1];
                    checkOperation = createOrWrite(checkEntity);
                    break;
                } else if (args.length >= 2 && args[0] instanceof Long &&
                        args[1] instanceof PersistentGroup &&
                        checkTypes.length == 1 && checkTypes[0] == GROUP)
                {
                    checkEntity = (PersistentGroup)args[1];
                    checkOperation = createOrWrite(checkEntity);
                    break;
                } else {
                    // TODO this is incredibly ugly
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
                checkOperation = READ;
                if (Collection.class.isAssignableFrom(method.getReturnType())) {
                    checkAfter = CheckAfterType.COLLECTION;
                }
                break;
            case FIND_HEADERS:
                // Anyone can list currently
                checkOperation = READ;
                break;
            case FIND_BY_PRIMARY_KEY:
                if (args.length == 2 && (types.contains(USER) || types.contains(GROUP))) {
                    checkOperation = READ;
                    checkAfter = CheckAfterType.RETURN;
                    break;
                } else if (args.length == 1 && (args[0] instanceof Long || args[0] instanceof CharSequence)) {
                    checkOperation = READ;
                    checkAfter = CheckAfterType.RETURN;
                    break;
                } else {
                    throw new IllegalStateException("Security declaration for method " + mname + " specifies FIND_BY_PRIMARY_KEY, but args are not suitable");
                }
            case FIND_ENTITY_BY_ATTRIBUTE:
                checkOperation = READ;
                checkAfter = CheckAfterType.RETURN;
                break;
            case DELETE_ENTITY:
                if (checkTypes.length != 1) throw new IllegalStateException("Security declaration for method " + mname + " specifies DELETE_ENTITY, but has multiple types");
                if (args.length == 1 && args[0] instanceof Entity) {
                    checkEntity = (Entity) args[0];
                    checkOperation = DELETE;
                    break;
                } else {
                    throw new IllegalStateException("Security declaration for method " + mname + " specifies DELETE_ENTITY, but args not (Entity)");
                }
            case DELETE_BY_OID:
                if (checkTypes.length != 1) throw new IllegalStateException("Security declaration for method " + mname + " specifies DELETE_BY_OID, but has multiple types");
                if (args.length == 1 && args[0] instanceof Long) {
                    checkOperation = DELETE;
                    checkEntity = new DeletedEntity(checkTypes[0].getEntityClass(), (Long)args[0]);
                    break;
                } else {
                    throw new IllegalStateException("Security declaration for method " + mname + " specifies DELETE_BY_OID, but args not (long)");
                }
            case GET_PROPERTY_BY_OID:
                if (args.length >= 2 && types.contains(USER) || types.contains(GROUP)) {
                    if (args[0] instanceof Long && args[1] instanceof String) {
                        checkOperation = READ;
                        checkOid = (Long)args[0];
                        break;
                    } else {
                        throw new IllegalStateException("Security declaration for method " + mname + " specifies GET_PROPERTY_BY_OID and has types=(USER|GROUP), but args do not start with (long, String)");
                    }
                } else if (args.length == 1 && args[0] instanceof Long) {
                    checkOperation = READ;
                    checkOid = (Long)args[0];
                    break;
                } else {
                    throw new IllegalStateException("Security declaration for method " + mname + " specifies GET_PROPERTY_BY_OID but args not (long)");
                }
            case GET_PROPERTY_OF_ENTITY:
                if (args[0] instanceof Entity) {
                    checkEntity = (Entity) args[0];
                } else if (args.length > 1 && args[1] instanceof Entity) {
                    checkEntity = (Entity) args[1];
                } else {
                    throw new IllegalStateException("Security declaration for method " + mname + " specifies GET_PROPERTY_OF_ENTITY but args do not contain an Entity");
                }

                checkOperation = READ;
                break;
            case SET_PROPERTY_OF_ENTITY:
                if (args[0] instanceof Entity) {
                    checkEntity = (Entity) args[0];
                } else if (args.length > 1 && args[1] instanceof Entity) {
                    checkEntity = (Entity) args[1];
                } else {
                    throw new IllegalStateException("Security declaration for method " + mname + " specifies SET_PROPERTY_OF_ENTITY but args do not contain an Entity");
                }
                checkOperation = UPDATE;
                break;
            default:
                throw new UnsupportedOperationException("Security declaration for method " + mname + " specifies unsupported stereotype " + methodSecured.stereotype().name());
        }

        if (checkEntity != null && !roleManager.isPermittedForEntity(user, checkEntity, checkOperation, null)) {
            throw new PermissionDeniedException(checkOperation, checkEntity);
        }

        Object rv = methodInvocation.proceed();
        if (checkAfter != CheckAfterType.NONE) {
            logger.log(Level.WARNING, "Not yet implemented: invoking {0}; should check {1} after invocation", new Object[] { mname, checkAfter.name() });
            return rv;
        } else {
            String ename;
            Level level;
            if (checkEntity == null) {
                ename = "<unknown>";
                level = Level.WARNING;
            } else {
                ename = checkEntity instanceof NamedEntity ? ((NamedEntity) checkEntity).getName() : checkEntity.getClass().getSimpleName() + " #" + checkEntity.getOid();
                level = Level.FINE;
            }
            logger.log(level, "Permitted {0} on {1} for {2}", new Object[]{checkOperation.name(), ename, mname});
            return rv;
        }

    }

    private OperationType createOrWrite(Entity checkEntity) {
        return checkEntity.getOid() == Entity.DEFAULT_OID ? CREATE : UPDATE;
    }

    private static enum CheckAfterType {
        NONE, RETURN, COLLECTION
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

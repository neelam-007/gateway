package com.l7tech.gateway.common.solutionkit;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Remote admin interface for managing {@link com.l7tech.gateway.common.solutionkit.SolutionKit}.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types= EntityType.SOLUTION_KIT)
@Administrative
public interface SolutionKitAdmin extends AsyncAdminMethods {

    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_HEADERS)
    Collection<SolutionKitHeader> findSolutionKits() throws FindException;

    @NotNull
    @Secured(stereotype = MethodStereotype.SAVE)
    JobId<String> install(@NotNull SolutionKit solutionKit, @NotNull String bundle);

    @NotNull
    @Secured(stereotype = MethodStereotype.DELETE_BY_ID)
    JobId<String> uninstall(@NotNull Goid goid);

    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_ENTITY)
    SolutionKit get(@NotNull Goid goid) throws FindException;
}
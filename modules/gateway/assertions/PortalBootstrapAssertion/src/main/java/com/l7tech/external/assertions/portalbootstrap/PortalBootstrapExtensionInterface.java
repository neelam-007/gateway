package com.l7tech.external.assertions.portalbootstrap;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.Goid;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.SET_PROPERTY_BY_UNIQUE_ATTRIBUTE;
import static com.l7tech.objectmodel.EntityType.USER;

/**
 * A remote admin extension interface for the portal bootstrap dialog to activate the server-side
 * enrollment function.
 */
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
@Administrative
@Secured(types = USER)
public interface PortalBootstrapExtensionInterface extends AsyncAdminMethods {

    @Secured( stereotype = SET_PROPERTY_BY_UNIQUE_ATTRIBUTE ) // require abilty to update all users (equiv to admin power)
    AsyncAdminMethods.JobId<Boolean> enrollWithPortal( final String enrollmentUrl, final JdbcConnection otkConnection) throws IOException;

}

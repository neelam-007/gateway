package com.l7tech.external.assertions.radius;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin interface for SSM code to perform Radius functions in server-side
 *
 */

@Secured
@Administrative
public interface RadiusAdmin {
    /**
     * Validate Radius attribute name
     * @param name
     * @return  true if Radius attribute name is valid
     */
    @Transactional(readOnly=true)
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    boolean isAttributeNameValid(String name);

    /**
     * Validate the Radius attribute
     * @param name The attribute name
     * @param value The attribute value
     * @return True if the Radius attribute is valid, False if the Radius attribute is invalid.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    boolean isAttributeValid(String name, String value);

    /**
     * Retrieve the registered Authenticators.
     *
     * @return a list of registered authenticators
     */
    @Transactional(readOnly=true)
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    String[] getAuthenticators();

    /**
     * Validate the authenticator. The authenticator consider valid when it has registered in the Radius Client.
     *
     * @param authenticator The authenticator name
     * @return True if the Authenticator is valid, False if the Authenticator is invalid.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    boolean isAuthenticatorSupport(String authenticator);

}

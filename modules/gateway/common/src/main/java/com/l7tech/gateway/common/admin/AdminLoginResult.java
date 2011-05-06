package com.l7tech.gateway.common.admin;

import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;

import java.io.Serializable;

/**
 * The return value from {@link AdminLogin#login(String, String)}.
 */
public final class AdminLoginResult implements Serializable {
    private static final long serialVersionUID = 13L;

    private final User user;
    private final String sessionCookie;
    private final String version;
    private final String softwareVersion;
    private final String logonWarningBanner;

    public AdminLoginResult(User user, String secret, String version, String softwareVersion, String logonWarningBanner) {
        if (user == null || secret == null || secret.length() == 0 || version == null || softwareVersion == null) throw new IllegalArgumentException();
        this.user = bean(user);
        this.sessionCookie = secret;
        this.version = version;
        this.softwareVersion = softwareVersion;
        this.logonWarningBanner = logonWarningBanner;
    }

    public User getUser() {
        return user;
    }

    public String getSessionCookie() {
        return sessionCookie;
    }

    /**
     * Get the protocol version.
     *
     * @return The version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get the software version
     *
     * @return The version
     */
    public String getSoftwareVersion() {
        return softwareVersion;
    }

    /**
     * Get the warning banner to be displayed to the user.
     *
     * @return the warning banner or null if there is none
     */
    public String getLogonWarningBanner() {
        return logonWarningBanner;
    }

    private static User bean( User user ) {
        final UserBean bean;
        if ( user instanceof UserBean ) {
            bean = (UserBean) user;
        } else {
            bean = new UserBean( user.getProviderId(), user.getLogin() );
            bean.setUniqueIdentifier( user.getId() );
            bean.setName( user.getName() );
            bean.setFirstName( user.getFirstName() );
            bean.setLastName( user.getLastName() );
            bean.setDepartment( user.getDepartment() );
            bean.setEmail( user.getEmail() );
            bean.setSubjectDn( user.getSubjectDn() );
        }
        return bean;
    }
}

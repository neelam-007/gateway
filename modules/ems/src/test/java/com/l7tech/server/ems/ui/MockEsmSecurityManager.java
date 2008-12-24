package com.l7tech.server.ems.ui;

import com.l7tech.gateway.common.security.rbac.AttemptedOperation;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

import org.apache.wicket.Component;

/**
 * Mock EMS security manager
 */
public class MockEsmSecurityManager implements EsmSecurityManager {
    private final LoginInfo loginInfo = new LoginInfo("login", new Date(), null);

    @Override
    public boolean login(HttpSession session, String username, String password) {
        return true;
    }

    @Override
    public boolean logout(HttpSession session) {
        return true;
    }

    @Override
    public boolean canAccess(HttpSession session, HttpServletRequest request) {
        return true;
    }

    @Override
    public boolean changePassword(HttpSession session, String password, String newPassword) {
        return true;
    }

    @Override
    public boolean hasPermission(AttemptedOperation ao) {
        return true;
    }

    @Override
    public boolean isAuthorized(Class componentClass) {
        return true;
    }

    @Override
    public boolean hasPermission(Class clazz) {
        return true;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public boolean isAuthenticated(Component component) {
        return true;
    }

    @Override
    public boolean isAuthenticated(Class componentClass) {
        return true;
    }

    @Override
    public boolean isAuthorized(Component component) {
        return true;
    }

    @Override
    public boolean isLicensed(Component component) {
        return true;
    }

    @Override
    public boolean isLicensed(Class componentClass) {
        return true;
    }

    @Override
    public LoginInfo getLoginInfo(HttpSession session) {
        return loginInfo;
    }
}

package com.l7tech.server.ems;

import com.l7tech.gateway.common.security.rbac.AttemptedOperation;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * Mock EMS security manager
 */
public class MockEmsSecurityManager implements EmsSecurityManager {
    private final LoginInfo loginInfo = new LoginInfo("login", new Date(), null);

    public boolean login(HttpSession session, String username, String password) {
        return true;
    }

    public boolean logout(HttpSession session) {
        return true;
    }

    public boolean canAccess(HttpSession session, HttpServletRequest request) {
        return true;
    }

    public boolean changePassword(HttpSession session, String password, String newPassword) {
        return true;
    }

    public boolean hasPermission(AttemptedOperation ao) {
        return true;
    }

    public LoginInfo getLoginInfo(HttpSession session) {
        return loginInfo;
    }
}

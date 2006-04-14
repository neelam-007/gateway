package com.l7tech.server.config;

import java.util.ArrayList;

/**
 * User: megery
 * Date: Apr 13, 2006
 * Time: 11:36:22 AM
 */
public interface PasswordValidator {
    String[] validate(String password1, String password2);
}

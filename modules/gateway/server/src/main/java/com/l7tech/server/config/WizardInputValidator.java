package com.l7tech.server.config;

import java.util.Map;

/**
 * User: megery
 * Date: Apr 13, 2006
 * Time: 11:36:22 AM
 */
public interface WizardInputValidator {
    String[] validate(Map inputs);
}

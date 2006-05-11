package com.l7tech.server.config;

import com.l7tech.common.gui.util.InputValidator;

import java.util.ArrayList;
import java.util.Map;

/**
 * User: megery
 * Date: Apr 13, 2006
 * Time: 11:36:22 AM
 */
public interface WizardInputValidator {
    String[] validate(Map inputs);
}

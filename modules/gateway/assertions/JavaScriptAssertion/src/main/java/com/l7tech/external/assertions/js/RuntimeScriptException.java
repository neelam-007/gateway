package com.l7tech.external.assertions.js;

import javax.script.ScriptException;

/**
 * Represents unchecked version for ScriptException
 */
public class RuntimeScriptException extends RuntimeException {
    public RuntimeScriptException(ScriptException ex) {
        super(ex);
    }
}

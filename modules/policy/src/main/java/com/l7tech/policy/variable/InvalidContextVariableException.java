/**
 * The exception class is for throwing an exception when creating/editing/processing an invalid context variable. 
 */

package com.l7tech.policy.variable;

/**
 * @auther: ghuang
 */
public class InvalidContextVariableException extends RuntimeException {
    public InvalidContextVariableException(String message) {
        super(message);
    }
}

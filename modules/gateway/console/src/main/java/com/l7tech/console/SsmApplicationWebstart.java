package com.l7tech.console;

/**
 * Webstart version of SsmApplication.
 */
public class SsmApplicationWebstart extends SsmApplicationHeavy {

    public boolean isWebStart() {

        return true;
    }

    public boolean isApplet() {
        return false;
    }
}

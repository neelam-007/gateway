package com.l7tech.console.logging;

import java.util.logging.StreamHandler;

/**
 * Class DefaultHandler.
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a> 
 */
public class DefaultHandler extends StreamHandler {
    public DefaultHandler() {
        setOutputStream(ConsoleDialog.getInstance().getOutputStream());
    }
}

package com.l7tech.gateway.config.backuprestore;

/**
 * A command line option passed to the flash utility
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
class CommandLineOption {
    CommandLineOption(String name, String desc) {
        this.name = name;
        this.description = desc;
    }
    CommandLineOption(String name, String desc, boolean isValuePath, boolean hasNoValue) {
        this.name = name;
        this.description = desc;
        this.isValuePath = isValuePath;
        this.hasNoValue = hasNoValue;
    }
    String name;
    String description;
    boolean isValuePath = false;
    boolean hasNoValue = false;
}

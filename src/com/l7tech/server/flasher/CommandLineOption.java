package com.l7tech.server.flasher;

/**
 * A command line option passed to the flash utility
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
public class CommandLineOption {
    CommandLineOption(String name, String desc) {
        this.name = name;
        this.description = desc;
    }
    String name;
    String description;
}

/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Feb 23, 2005<br/>
 */
package com.l7tech.server.antivirus;

/**
 * This exception signifies that the infection or cleanliness of something cannot be determined.
 * This may occur for example if a connection to the virus scanner cannot be established.
 *
 * @author flascelles@layer7-tech.com
 */
public class CannotDetermineInfectionException extends Exception {
    public CannotDetermineInfectionException(Throwable cause) {
        super("Cannot determine infection", cause);
    }

    public CannotDetermineInfectionException(String message) {
        super(message);
    }

    public CannotDetermineInfectionException(String message, Throwable cause) {
        super(message, cause);
    }
}

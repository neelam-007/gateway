package com.l7tech.message;

/**
 * The command knob is used to hold information about a command that is requested to be executed by a policy.
 * It is currently used only with scp and sftp command but could also be reused for ftp(s) commands.
 *
 * @author Victor Kazakov
 */
public interface CommandKnob extends MessageKnob {
    //The list of command types
    public enum CommandType {
        GET, PUT, LIST, DELETE, MKDIR, RMDIR, STAT, MOVE
    }

    /**
     * The command type of the request.
     *
     * @return the command type for the request.
     */
    CommandType getCommandType();

    /**
     * Retrieves a parameter associated with this command
     *
     * @param name The parameter name
     * @return The parameter value
     */
    String getParameter(String name);
}

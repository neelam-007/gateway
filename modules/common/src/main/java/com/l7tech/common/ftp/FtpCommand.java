package com.l7tech.common.ftp;

/**
 * Supported FTP Commands
 * 
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public enum FtpCommand {

    /* Upload commands */
    APPE("Append", true, true),
    STOR("Store", true, true),
    STOU("Store unique", true, false), // N.B. many servers accept an argument for STOU but that is not RFC compliant

    /* Download commands */
    RETR("Retrieve", true, true),

    /* List commands */
    LIST("List", true, false),
    MLSD("Machine list directory", true, false),
    NLST("Name list", true, false),

    /* Simple commands */
    CDUP("Change to parent directory", false,false),
    CWD("Change working directory", true, true),
    DELE("Delete", true, true),
    MDTM("Modification time", true, true),
    MLST("Machine list", true, false),
    MKD("Make directory", true, true),
    NOOP("No operation", false, false),
    PWD("Print working directory", false, false),
    RMD("Remove directory", true, true),
    SIZE("Size of file", true, true);

    private final String description;
    private final boolean argumentAccepted;
    private final boolean argumentRequired;
    
    FtpCommand(String description, boolean argumentAccepted, boolean argumentRequired) {
        this.description = description;
        this.argumentAccepted = argumentAccepted;
        this.argumentRequired = argumentRequired;
    }
    
    public String getDescription() {
        return description;
    }

    public boolean isArgumentAccepted() {
        return argumentAccepted;
    }

    public boolean isArgumentRequired() {
        return argumentRequired;
    }
}

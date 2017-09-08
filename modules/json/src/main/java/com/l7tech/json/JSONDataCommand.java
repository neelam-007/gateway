package com.l7tech.json;

import java.io.IOException;

/**
 * To be used internally only by implementations of {@link JSONData}
 */
public interface JSONDataCommand {
    void execute() throws IOException, InvalidJsonException;
}

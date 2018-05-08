/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.message;

import com.l7tech.json.InvalidJsonException;
import com.l7tech.json.JSONData;
import com.l7tech.json.JsonSchemaVersion;

import java.io.IOException;

public interface JsonKnob extends MessageKnob{

    JSONData getJsonData() throws IOException, InvalidJsonException;

    JSONData getJsonData(JsonSchemaVersion version) throws IOException, InvalidJsonException;
}

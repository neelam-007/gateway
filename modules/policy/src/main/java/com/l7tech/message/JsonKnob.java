/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.message;

import com.l7tech.json.InvalidJsonException;
import com.l7tech.json.JSONData;

import java.io.IOException;

public interface JsonKnob extends MessageKnob{

    public JSONData getJsonData() throws IOException, InvalidJsonException;
}

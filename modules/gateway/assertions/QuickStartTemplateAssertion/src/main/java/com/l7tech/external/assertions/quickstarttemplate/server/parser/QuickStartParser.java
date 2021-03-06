package com.l7tech.external.assertions.quickstarttemplate.server.parser;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class QuickStartParser {

    public ServiceContainer parseJson(final InputStream is) throws IOException {
        return new ObjectMapper().readValue(is, ServiceContainer.class);
    }

}

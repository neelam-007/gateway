package com.l7tech.external.assertions.logmessagetosyslog;

import com.l7tech.external.assertions.logmessagetosyslog.server.ServerLogMessageToSysLogAssertionTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        LogMessageToSysLogAssertionTest.class,
        ServerLogMessageToSysLogAssertionTest.class})
public class TestSuiteLogMessageToSysLogAssertion {
}

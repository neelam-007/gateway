package com.l7tech.portal.reports.parameter;

import org.junit.Test;

import static org.junit.Assert.*;

public class RankingReportParametersTest {
    @Test
    public void defaultLimit() {
        assertEquals(RankingReportParameters.DEFAULT_LIMIT, new RankingReportParameters().getLimit());
    }
}

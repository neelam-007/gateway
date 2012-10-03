package com.l7tech.portal.reports;

import com.l7tech.portal.reports.format.Format;
import com.l7tech.portal.reports.parameter.ApiUsageReportParameters;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.util.*;

/**
 * Convenience class for retrieving report data using the report generator. Do not use outside of your local development environment.
 */
public class MetricsReportGeneratorMain {
    public static void main(final String[] args) {
        final String dbUrl = args[0];
        final String dbUser = "lrs";
        final String dbPass = "lrs";
        final int resolution = Integer.valueOf(args[1]).intValue();
        final String uuidString = args[2];
        final String[] split = StringUtils.split(uuidString, ",");
        for (int i = 0; i < split.length; i++) {
            final String s = split[i];
        }
        final ArrayList<String> uuids = new ArrayList<String>(Arrays.asList(split));
        final Calendar calendar = new GregorianCalendar();
        final long endTime = calendar.getTime().getTime();
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        final long startTime = calendar.getTime().getTime();
        final long interval = 15 * 60 * 1000;
        final Map<String, Object> defaultValues = new HashMap<String, Object>();

        final MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUrl(dbUrl);
        dataSource.setUser(dbUser);
        dataSource.setPassword(dbPass);
        final MetricsReportGenerator generator = new MetricsReportGenerator(dataSource);
        generator.getJsonFormatter().setIndentSize(1);

        final ApiUsageReportParameters parameters = new ApiUsageReportParameters(startTime, endTime, resolution, Format.JSON, uuids, Collections.<String>emptyList());

        try {
            final String data = generator.generateApiUsageReport(parameters, true, interval, defaultValues);
            System.out.println(data);
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }
}

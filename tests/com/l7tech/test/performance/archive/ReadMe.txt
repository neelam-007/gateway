This folder is for archiving important Japex test reports, e.g., tests ran on final releases.
To use these reports, they must be copied into the directory ${test.perf.reports.dir}
defined in build.xml and then specified in Ant target test.perf.trend and/or test.perf.regression.

Currently, those Ant targets are run on tyan64 nightly. That means archive reports
should be copied into tyan64:/var/www/html/testperf/reports/.
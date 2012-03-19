=============================================================================
README for Configurable Audits and Metrics branch
=============================================================================

Overview
========
This branch is related to the feature:

  http://sarek/mediawiki/index.php/Configurable_Audit_and_Metrics_Store

Status
======
Some UI changes are mocked up, these were used to produce the screenshots in
the functional specification.

The basics for offboxing of audits is functional, metrics and more advanced
options are not implemented.

Usage
=====
The temporary "audit.external.name" cluster property is used to configure the
name of the JDBC Connection to use for audit persistence.

Development Notes
=================
Hibernate identifier generator configuration has been removed from
configuration files and annotations to allow the generation strategy to be
switched depending on the database in use.

The new "ConfiguredSessionFactoryBean" extends the base Spring functionality
to add support for configuration of the available generators. There is also a
"ConfiguredHiLoGenerator" which customizes the configuration for a sequence
hilo generator.

There is a new "EntityManagementContextProvider" and associated context that
allows access to "beans" backed by a particular database. This is used to
provide access to a "SimpleAuditRecordManager" which is a cut down version of
the regular "AuditRecordManager" with any MySQL specific functionality
removed. At runtime the "DefaultSimpleAuditRecordManager" is used and
switches the actual implementation used based on configuration.

The database schema is split up into sections in this branch, but this is not
the final approach that was discussed. It was later decided that a better
approach would be to mark up the single "ssg.sql" schema (as far as possible)
to allow it to be broken into sections at build time.

TODO
====
* UI implementation
* Audit schema changes (Add GUID, flatten tables)
* Schema management / upgrades
* Etc

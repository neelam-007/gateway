package com.l7tech.external.assertions.xacmlpdp.console;

import java.util.TreeSet;
import java.util.Set;
import java.util.Collections;
import java.util.Arrays;

/**
 * Constants for XACML
 */
class XacmlConstants {

    public static final String XACML_POLICY_ELEMENT = "Policy";

    public static final String XACML_10_POLICY_NAMESPACE = "urn:oasis:names:tc:xacml:1.0:policy";
    public static final String XACML_20_POLICY_NAMESPACE = "urn:oasis:names:tc:xacml:2.0:policy:schema:os";

    public static final Set<String> XACML_POLICY_NAMESPACES = Collections.unmodifiableSet( new TreeSet<String>( Arrays.asList(
            XACML_10_POLICY_NAMESPACE,
            XACML_20_POLICY_NAMESPACE
     )) );

    public static final String XACML_10_SUBJECT_LOCALITY_DNS = "urn:oasis:names:tc:xacml:1.0:subject:authn-locality:dns-name";
    public static final String XACML_10_SUBJECT_LOCALITY_IP = "urn:oasis:names:tc:xacml:1.0:subject:authn-locality:ip-address";
    public static final String XACML_10_SUBJECT_AUTH_METHOD = "urn:oasis:names:tc:xacml:1.0:subject:authentication-method";
    public static final String XACML_10_SUBJECT_AUTH_TIME = "urn:oasis:names:tc:xacml:1.0:subject:authentication-time";
    public static final String XACML_10_SUBJECT_KEY_INFO = "urn:oasis:names:tc:xacml:1.0:subject:key-info";
    public static final String XACML_10_SUBJECT_REQUEST_TIME = "urn:oasis:names:tc:xacml:1.0:subject:request-time";
    public static final String XACML_10_SUBJECT_SESSION_START_TIME = "urn:oasis:names:tc:xacml:1.0:subject:session-start-time";
    public static final String XACML_10_SUBJECT_ID = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";
    public static final String XACML_10_SUBJECT_ID_QUALIFIER = "urn:oasis:names:tc:xacml:1.0:subject:subject-id-qualifier";

    public static final Set<String> XACML_10_SUBJECT_IDS = Collections.unmodifiableSet( new TreeSet<String>( Arrays.asList(
            XACML_10_SUBJECT_LOCALITY_DNS,
            XACML_10_SUBJECT_LOCALITY_IP,
            XACML_10_SUBJECT_AUTH_METHOD,
            XACML_10_SUBJECT_AUTH_TIME,
            XACML_10_SUBJECT_KEY_INFO,
            XACML_10_SUBJECT_REQUEST_TIME,
            XACML_10_SUBJECT_SESSION_START_TIME,
            XACML_10_SUBJECT_ID,
            XACML_10_SUBJECT_ID_QUALIFIER
    )) );

    public static final String XACML_10_SUBJECTCATEGORY_ACCESS_SUBJECT = "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";
    public static final String XACML_10_SUBJECTCATEGORY_CODEBASE = "urn:oasis:names:tc:xacml:1.0:subject-category:codebase";
    public static final String XACML_10_SUBJECTCATEGORY_INTERMEDIARY_SUBJECT = "urn:oasis:names:tc:xacml:1.0:subject-category:intermediary-subject";
    public static final String XACML_10_SUBJECTCATEGORY_RECIPIENT_SUBJECT = "urn:oasis:names:tc:xacml:1.0:subject-category:recipient-subject";
    public static final String XACML_10_SUBJECTCATEGORY_REQUESTING_MACHINE = "urn:oasis:names:tc:xacml:1.0:subject-category:requesting-machine";

    public static final Set<String> XACML_10_SUBJECTCATEGORIES = Collections.unmodifiableSet( new TreeSet<String>( Arrays.asList(
            XACML_10_SUBJECTCATEGORY_ACCESS_SUBJECT,
            XACML_10_SUBJECTCATEGORY_CODEBASE,
            XACML_10_SUBJECTCATEGORY_INTERMEDIARY_SUBJECT,
            XACML_10_SUBJECTCATEGORY_RECIPIENT_SUBJECT,
            XACML_10_SUBJECTCATEGORY_REQUESTING_MACHINE
    )) );

    public static final String XACML_10_RESOURCE_LOCATION = "urn:oasis:names:tc:xacml:1.0:resource:resource-location";
    public static final String XACML_10_RESOURCE_ID = "urn:oasis:names:tc:xacml:1.0:resource:resource-id";
    public static final String XACML_10_RESOURCE_SCOPE = "urn:oasis:names:tc:xacml:1.0:resource:scope";
    public static final String XACML_10_RESOURCE_FILE_NAME = "urn:oasis:names:tc:xacml:1.0:resource:simple-file-name";

    public static final Set<String> XACML_10_RESOURCE_IDS = Collections.unmodifiableSet( new TreeSet<String>( Arrays.asList(
            XACML_10_RESOURCE_LOCATION,
            XACML_10_RESOURCE_ID,
            XACML_10_RESOURCE_SCOPE,
            XACML_10_RESOURCE_FILE_NAME
    )) );

    public static final String XACML_10_ACTION_ID = "urn:oasis:names:tc:xacml:1.0:action:action-id";
    public static final String XACML_10_ACTION_IMPLIED_ACTION = "urn:oasis:names:tc:xacml:1.0:action:implied-action";

    public static final Set<String> XACML_10_ACTION_IDS = Collections.unmodifiableSet( new TreeSet<String>( Arrays.asList(
            XACML_10_ACTION_ID,
            XACML_10_ACTION_IMPLIED_ACTION
    )) );

    public static final String XACML_10_ENVIRONMENT_TIME = "urn:oasis:names:tc:xacml:1.0:environment:current-time";
    public static final String XACML_10_ENVIRONMENT_DATE = "urn:oasis:names:tc:xacml:1.0:environment:current-date";
    public static final String XACML_10_ENVIRONMENT_DATETIME = "urn:oasis:names:tc:xacml:1.0:environment:current-dateTime";

    public static final Set<String> XACML_10_ENVIRONMENT_IDS = Collections.unmodifiableSet( new TreeSet<String>( Arrays.asList(
            XACML_10_ENVIRONMENT_TIME,
            XACML_10_ENVIRONMENT_DATE,
            XACML_10_ENVIRONMENT_DATETIME
    )) );

    public static final String XACML_10_DATATYPE_STRING = "http://www.w3.org/2001/XMLSchema#string";
    public static final String XACML_10_DATATYPE_BOOLEAN = "http://www.w3.org/2001/XMLSchema#boolean";
    public static final String XACML_10_DATATYPE_INTEGER = "http://www.w3.org/2001/XMLSchema#integer";
    public static final String XACML_10_DATATYPE_DOUBLE = "http://www.w3.org/2001/XMLSchema#double";
    public static final String XACML_10_DATATYPE_TIME = "http://www.w3.org/2001/XMLSchema#time";
    public static final String XACML_10_DATATYPE_DATE = "http://www.w3.org/2001/XMLSchema#date";
    public static final String XACML_10_DATATYPE_DATETIME = "http://www.w3.org/2001/XMLSchema#dateTime";
    public static final String XACML_10_DATATYPE_DAYTIME_DURATION = "http://www.w3.org/TR/2002/WD-xquery-operators-20020816#dayTimeDuration";
    public static final String XACML_10_DATATYPE_YEARMONTH_DURATION = "http://www.w3.org/TR/2002/WD-xquery-operators-20020816#yearMonthDuration";
    public static final String XACML_10_DATATYPE_ANYURI = "http://www.w3.org/2001/XMLSchema#anyURI";
    public static final String XACML_10_DATATYPE_HEXBIN = "http://www.w3.org/2001/XMLSchema#hexBinary";
    public static final String XACML_10_DATATYPE_BASE64BIN = "http://www.w3.org/2001/XMLSchema#base64Binary";
    public static final String XACML_10_DATATYPE_RFC822NAME = "urn:oasis:names:tc:xacml:1.0:data-type:rfc822Name";
    public static final String XACML_10_DATATYPE_X500NAME = "urn:oasis:names:tc:xacml:1.0:data-type:x500Name";

    public static final Set<String> XACML_10_DATATYPES = Collections.unmodifiableSet( new TreeSet<String>( Arrays.asList(
            XACML_10_DATATYPE_STRING,
            XACML_10_DATATYPE_BOOLEAN,
            XACML_10_DATATYPE_INTEGER,
            XACML_10_DATATYPE_DOUBLE,
            XACML_10_DATATYPE_TIME,
            XACML_10_DATATYPE_DATE,
            XACML_10_DATATYPE_DATETIME,
            XACML_10_DATATYPE_DAYTIME_DURATION,
            XACML_10_DATATYPE_YEARMONTH_DURATION,
            XACML_10_DATATYPE_ANYURI,
            XACML_10_DATATYPE_HEXBIN,
            XACML_10_DATATYPE_BASE64BIN,
            XACML_10_DATATYPE_RFC822NAME,
            XACML_10_DATATYPE_X500NAME 
    )) );

}

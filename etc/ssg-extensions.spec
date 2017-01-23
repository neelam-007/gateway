Summary: Layer 7 Gateway, Copyright Layer 7 Technologies 2003-2012
Name: ssg-extensions%{?nameSuffix}
Version: 0.0
Release: 0
Group: Applications/Internet
License: Commercial
URL: http://www.layer7tech.com
Vendor: Layer 7 Technologies
Packager: Layer 7 Technologies, <support@layer7tech.com>
Source0: ssg-extensions.tar.gz

BuildRoot: %{_builddir}/%{name}-%{version}
Prefix: /opt/SecureSpan/Gateway

# Prevents rpm build from erroring and halting
%undefine __check_files

%description
Layer 7 Gateway Software Package

%clean
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}

%setup -T -b 0 -qcn %{buildroot}

%build

%files
%defattr(0444,layer7,layer7,0755)

%if "%{?isTwelveFactor}" == "1"
/opt/SecureSpan/Gateway/runtime/modules/assertions/AdaptiveLoadBalancingAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/BufferDataAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/BulkJdbcInsertAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/CORSAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/CacheAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/CassandraAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/CertificateAttributesAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/ComparisonAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/ConcurrentAllAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/CsrSignerAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/CsrfProtectionAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/EchoRoutingAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/EncodeDecodeAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/EvaluateJsonPathExpressionAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/GatewayManagementAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/GenerateOAuthSignatureBaseStringAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/GenerateSecurityHashAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/GenericIdentityManagementServiceAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/IdentityAttributesAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/JSONSchemaAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/JdbcQueryAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/JsonDocumentStructureAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/JsonTransformationAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/JsonWebTokenAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/JwtAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/KerberosAuthenticationAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/LDAPQueryAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/LookupDynamicContextVariablesAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/ManageCookieAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/ManipulateMultiValuedVariableAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/MessageContextAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/NtlmAuthenticationAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/OdataValidationAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/PortalBootstrapAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/RateLimitAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/ReplaceTagContentAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/SamlIssuerAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/SamlpAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/SiteMinderAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/SnmpTrapAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/SplitJoinAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/SwaggerAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/UDDINotificationAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/UUIDGeneratorAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/ValidateCertificateAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/ValidateNonSoapSamlAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/WsAddressingAssertion-*.aar
/opt/SecureSpan/Gateway/runtime/modules/assertions/XmlSecurityAssertion-*.aar
%endif


Summary: SecureSpan Enterprise Manager, Copyright Layer 7 Technologies 2008
Name: ssem
Version: 1.0
Release: 1
Group: Applications/Internet
License: Commercial
URL: http://www.layer7tech.com
Vendor: Layer 7 Technologies
Packager: Layer 7 Technologies, <support@layer7tech.com>
source0: ssem.tar.gz
buildroot: %{_builddir}/%{name}-%{version}
requires: ssg-appliance >= 5.0
Prefix: /opt/SecureSpan/EnterpriseManager

%description
SecureSpan Enterprise Manager Software Package

%clean
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}

%setup -qc %{buildroot}

%build

%files
# Root owned OS components
%attr(0755,root,root) /etc/init.d/ssem

# Main tree, owned by gateway
%defattr(0644,gateway,gateway,0755)
%dir /opt/SecureSpan/EnterpriseManager

# Binaries / scripts
%dir /opt/SecureSpan/EnterpriseManager/bin
%attr(0755,gateway,gateway) /opt/SecureSpan/EnterpriseManager/bin/*

# Libraries
/opt/SecureSpan/EnterpriseManager/EnterpriseManager.jar
/opt/SecureSpan/EnterpriseManager/lib

# Runtime files
%dir /opt/SecureSpan/EnterpriseManager/logs
%dir /opt/SecureSpan/EnterpriseManager/var

%post
/sbin/chkconfig --list ssem &>/dev/null
if [ ${?} -ne 0 ]; then
    # add service if not found
    /sbin/chkconfig --add ssem
fi


Summary: SecureSpan Enterprise Service Manager, Copyright Layer 7 Technologies 2008
Name: ssem
Version: 0.0
Release: 0
Group: Applications/Internet
License: Commercial
URL: http://www.layer7tech.com
Vendor: Layer 7 Technologies
Packager: Layer 7 Technologies, <support@layer7tech.com>
source0: ssem.tar.gz
buildroot: %{_builddir}/%{name}-%{version}
requires: ssg-appliance >= 4.7
Prefix: /opt/SecureSpan/EnterpriseManager

%description
SecureSpan Enterprise Service Manager Software Package

%clean
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}

%setup -qc %{buildroot}

%build

%files
# Root owned OS components
%attr(0755,root,root) /etc/init.d/ssem

# Main directory
%dir /opt/SecureSpan/EnterpriseManager

# Binaries / scripts
%defattr(0644,layer7,layer7,0755)
%dir /opt/SecureSpan/EnterpriseManager/bin
%attr(0555,layer7,layer7) /opt/SecureSpan/EnterpriseManager/bin/*

# Libraries
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/EnterpriseManager/EnterpriseManager.jar
/opt/SecureSpan/EnterpriseManager/lib

# Runtime files
%defattr(0644,ssem,ssem,0755)
/opt/SecureSpan/EnterpriseManager/var

%pre
grep -q ^ssem: /etc/group || groupadd ssem

# If user ssem already exists ensure group membership is ok, if it doesn't exist add it
grep -qvL ^ssem: /etc/passwd || usermod -g ssem -G '' ssem
grep -q   ^ssem: /etc/passwd || useradd -g ssem -G '' ssem

SSEMENTRY=`grep ^ssem /etc/sudoers`
if [ -n "${SSEMENTRY}" ]; then
    #user already exists in the sudoers file but since the paths have changed we'll remove everything and reset
    perl -pi.bak -e 's/^ssem.*$//gs' /etc/sudoers
fi

# The ssem user is allowed to update the firewall configuration
echo "ssem ALL = NOPASSWD: /opt/SecureSpan/Appliance/libexec/update_firewall" >> /etc/sudoers

%post
/sbin/chkconfig --list ssem &>/dev/null
if [ ${?} -ne 0 ]; then
    # add service if not found
    /sbin/chkconfig --add ssem
fi

%preun
# Modifications to handle upgrades properly
if [ "$1" = "0" ] ; then
    # $1 is  on last uninstall, ie, package remove, not upgrade

    grep -qvL ^ssem: /etc/passwd || ssem -r gateway
    grep -qvL ^ssem: /etc/group || groupdel ssem

    SSEMENTRY=`grep ^ssem /etc/sudoers`
    if [ -n "${SSEMENTRY}" ]; then
        #remove the sudoers entry for ssem
        perl -pi.bak -e 's/^ssem.*$//gs' /etc/sudoers
    fi


fi


Summary: SecureSpan Gateway, Copyright Layer 7 Technologies 2003-2008
Name: ssg
Version: 0.0
Release: 1
Group: Applications/Internet
License: Commercial
URL: http://www.layer7tech.com
Vendor: Layer 7 Technologies
Packager: Layer 7 Technologies, <support@layer7tech.com>
Source0: ssg.tar.gz
BuildRoot: %{_builddir}/%{name}-%{version}
Prefix: /opt/SecureSpan/Gateway

# Prevents rpm build from erroring and halting
#%undefine       __check_files

%description
SecureSpan Gateway Software Package

%clean
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}

%setup -qc %{buildroot}

%build

%files
# Root owned OS components

# Main tree, owned by root
%defattr(0644,root,root,0755)
%dir /opt/SecureSpan
%dir /opt/SecureSpan/Gateway

# Group writable config files
%defattr(0644,layer7,layer7,0755)
%dir /opt/SecureSpan/Gateway/controller
%dir /opt/SecureSpan/Gateway/node
%dir /opt/SecureSpan/Gateway/runtime
%dir /opt/SecureSpan/Gateway/runtime/etc
/opt/SecureSpan/Gateway/runtime/etc/profile
/opt/SecureSpan/Gateway/controller/etc/conf
%dir /opt/SecureSpan/Gateway/node/default/etc
%config(noreplace) /opt/SecureSpan/Gateway/node/default/etc/conf

# Group directories and files
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Gateway/controller/Controller.jar
%dir /opt/SecureSpan/Gateway/controller/bin
%dir /opt/SecureSpan/Gateway/controller/etc
%dir /opt/SecureSpan/Gateway/controller/etc/conf
%attr(0555,layer7,layer7) /opt/SecureSpan/Gateway/controller/bin/*
/opt/SecureSpan/Gateway/controller/lib
/opt/SecureSpan/Gateway/runtime/modules

# Gateway process controller writeable files
%defattr(0644,layer7,layer7,0755)
/opt/SecureSpan/Gateway/controller/var

# The main Gateway jar
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Gateway/runtime/Gateway.jar

# Ssg bin
%defattr(0555,layer7,layer7,0755)
%dir /opt/SecureSpan/Gateway/runtime/bin
/opt/SecureSpan/Gateway/runtime/bin/*
%dir /opt/SecureSpan/Gateway/runtime/etc/profile.d
%config(noreplace) /opt/SecureSpan/Gateway/runtime/etc/profile.d/ssgruntimedefs.sh
%config(noreplace) /opt/SecureSpan/Gateway/runtime/etc/profile.d/ssg-utilities.sh

# Other stuff
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Gateway/config/etc
/opt/SecureSpan/Gateway/runtime/web
/opt/SecureSpan/Gateway/runtime/lib
%defattr(0444,root,root,0755)
/opt/SecureSpan/Gateway/runtime/modules
%defattr(0644,gateway,gateway,0755)
/opt/SecureSpan/Gateway/node/default/var

#Configuration
%defattr(0644,layer7,layer7,0755)
%dir /opt/SecureSpan/Gateway/config
/opt/SecureSpan/Gateway/config/*.properties
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Gateway/config/lib
/opt/SecureSpan/Gateway/config/*.jar
%defattr(0555,layer7,layer7,0755)
/opt/SecureSpan/Gateway/config/*.sh

%defattr(0644,layer7,layer7,0755)
%dir /opt/SecureSpan/Gateway/config/migration
/opt/SecureSpan/Gateway/config/migration/cfg
/opt/SecureSpan/Gateway/config/migration/*.properties
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Gateway/config/migration/lib
/opt/SecureSpan/Gateway/config/migration/*.jar
%defattr(0555,layer7,layer7,0755)
/opt/SecureSpan/Gateway/config/migration/*.sh

%pre
grep -q ^gateway: /etc/group || groupadd gateway
grep -q ^layer7: /etc/group || groupadd layer7

# If user gateway already exists ensure group membership is ok, if it doesn't exist add it
grep -qvL ^gateway: /etc/passwd || usermod -g gateway -G '' gateway
grep -q   ^gateway: /etc/passwd || useradd -g gateway -G '' gateway

# If user layer7 already exists ensure group membership is ok, if it doesn't exist add it
grep -qvL ^layer7: /etc/passwd || usermod -g layer7 -G '' layer7
grep -q   ^layer7: /etc/passwd || useradd -g layer7 -G '' layer7

%preun
# Modifications to handle upgrades properly
if [ "$1" = "0" ] ; then
    # $1 is  on last uninstall, ie, package remove, not upgrade

    grep -qvL ^gateway: /etc/passwd || userdel -r gateway
    grep -qvL ^layer7: /etc/passwd || userdel -r layer7

    grep -qvL ^gateway: /etc/group || groupdel gateway
    grep -qvL ^layer7: /etc/group || groupdel layer7
fi

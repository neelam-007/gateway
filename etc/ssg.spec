Summary: SecureSpan Gateway, Copyright Layer 7 Technologies 2003-2009
Name: ssg
Version: 0.0
Release: 0
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
%dir /opt/SecureSpan/Gateway/node
%dir /opt/SecureSpan/Gateway/node/default
%dir /opt/SecureSpan/Gateway/runtime
%dir /opt/SecureSpan/Gateway/runtime/etc
%dir /opt/SecureSpan/Gateway/node/default/etc
%config(noreplace) /opt/SecureSpan/Gateway/node/default/etc/conf

# Group directories and files
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Gateway/runtime/etc/profile
/opt/SecureSpan/Gateway/runtime/modules

# The main Gateway jar
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Gateway/runtime/Gateway.jar

# Ssg bin
%defattr(0555,layer7,layer7,0755)
%dir /opt/SecureSpan/Gateway/runtime/bin
/opt/SecureSpan/Gateway/runtime/bin/*
%dir /opt/SecureSpan/Gateway/runtime/etc/profile.d
/opt/SecureSpan/Gateway/runtime/etc/profile.d/ssgnodedefs.sh
/opt/SecureSpan/Gateway/runtime/etc/profile.d/ssgruntimedefs.sh
/opt/SecureSpan/Gateway/runtime/etc/profile.d/ssg-utilities.sh
%config(noreplace) %attr(755,layer7,layer7) /opt/SecureSpan/Gateway/runtime/etc/profile.d/xlocaldefs.sh 

# Other stuff
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Gateway/config/etc
/opt/SecureSpan/Gateway/runtime/web
/opt/SecureSpan/Gateway/runtime/lib
%defattr(0644,gateway,gateway,0755)
/opt/SecureSpan/Gateway/node/default/var

#Configuration
%defattr(0644,layer7,layer7,0755)
%dir /opt/SecureSpan/Gateway/config
/opt/SecureSpan/Gateway/config/*.properties
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Gateway/config/*.jar
%defattr(0555,layer7,layer7,0755)
/opt/SecureSpan/Gateway/config/*.sh

%defattr(0644,layer7,layer7,0755)
%dir /opt/SecureSpan/Gateway/config/backup
/opt/SecureSpan/Gateway/config/backup/cfg
/opt/SecureSpan/Gateway/config/backup/*.properties
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Gateway/config/backup/*.jar
%defattr(0555,layer7,layer7,0755)
/opt/SecureSpan/Gateway/config/backup/*.sh

%pre
grep -q ^gateway: /etc/group || groupadd gateway
grep -q ^layer7: /etc/group || groupadd layer7

# If user gateway already exists ensure group membership is ok, if it doesn't exist add it
if grep -q ^gateway: /etc/passwd; then
  usermod -g gateway -G '' gateway
else
  useradd -g gateway -G '' gateway
fi

# If user layer7 already exists ensure group membership is ok, if it doesn't exist add it
if grep -q ^layer7: /etc/passwd; then
  usermod -g layer7 -G '' layer7
else
  useradd -g layer7 -G '' layer7
fi

# Chown any files that have been left behind by a previous installation
[ ! -d %{prefix}/config ] || chown -R layer7.layer7 %{prefix}/config
[ ! -d %{prefix}/node/default/etc/conf ] || chown -R layer7.layer7 %{prefix}/node/default/etc/conf
[ ! -d %{prefix}/node/default/var ] || chown -R gateway.gateway %{prefix}/node/default/var

%preun
# Modifications to handle upgrades properly
if [ "$1" = "0" ] ; then
    # $1 is  on last uninstall, ie, package remove, not upgrade

    if grep -q '^gateway:' /etc/passwd; then userdel -r gateway; fi
    if grep -q '^layer7:' /etc/passwd; then userdel -r layer7; fi

    if grep -q ^gateway: /etc/group; then groupdel gateway; fi
    if grep -q ^layer7: /etc/group; then groupdel layer7; fi
fi

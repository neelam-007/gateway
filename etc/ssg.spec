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
%dir /opt/SecureSpan/Gateway/Nodes/default/etc
%attr(0755,gateway,gateway) %config(noreplace) /opt/SecureSpan/Gateway/Nodes/default/etc/conf
%attr(0755,gateway,gateway) /opt/SecureSpan/Gateway/Nodes/default/etc/profile

# Group writeable directories and files

/opt/SecureSpan/Gateway/Controller/Controller.jar
%dir /opt/SecureSpan/Gateway/Controller/bin
%dir /opt/SecureSpan/Gateway/Controller/etc
%attr(0755,root,root) /opt/SecureSpan/Gateway/Controller/bin/*
/opt/SecureSpan/Gateway/Controller/lib
%defattr(0644,gateway,gateway,0755)
/opt/SecureSpan/Gateway/Controller/var

# The main Gateway jar
%defattr(0644,root,root,0755)
/opt/SecureSpan/Gateway/Nodes/default/Gateway.jar

# Ssg bin
%dir /opt/SecureSpan/Gateway/Nodes/default/bin
%attr(0755,root,root) /opt/SecureSpan/Gateway/Nodes/default/bin/*

%defattr(0775,gateway,gateway,0755)
%dir /opt/SecureSpan/Gateway/Nodes/default/etc/profile.d
%config(noreplace) /opt/SecureSpan/Gateway/Nodes/default/etc/profile.d/ssgruntimedefs.sh
%config(noreplace) /opt/SecureSpan/Gateway/Nodes/default/etc/profile.d/ssg-utilities.sh
%config(noreplace) /opt/SecureSpan/Gateway/Nodes/default/etc/profile.d/setopts.sh
%config(noreplace) /opt/SecureSpan/Gateway/Nodes/default/etc/profile.d/java.sh
%config(noreplace) /opt/SecureSpan/Gateway/Nodes/default/etc/profile.d/jvmoptions
%config(noreplace) /opt/SecureSpan/Gateway/Nodes/default/etc/profile.d/output_redirection.sh

# Other stuff
%defattr(0644,root,root,0755)
/opt/SecureSpan/Gateway/Nodes/default/etc/sql
/opt/SecureSpan/Gateway/Nodes/default/web
/opt/SecureSpan/Gateway/Nodes/default/lib
%defattr(0644,root,root,0755)
/opt/SecureSpan/Gateway/Nodes/default/modules
%defattr(0644,gateway,gateway,0755)
/opt/SecureSpan/Gateway/Nodes/default/var

#Config Wizard
%defattr(0664,gateway,gateway,0775)
%dir /opt/SecureSpan/Gateway/configwizard
/opt/SecureSpan/Gateway/configwizard/lib
/opt/SecureSpan/Gateway/configwizard/*.properties
%attr(0775,gateway,gateway) /opt/SecureSpan/Gateway/configwizard/*.sh
/opt/SecureSpan/Gateway/configwizard/*.jar

# Group writable for migration stuff
%defattr(0664,gateway,gateway,0755)
%dir /opt/SecureSpan/Gateway/migration
/opt/SecureSpan/Gateway/migration/cfg
/opt/SecureSpan/Gateway/migration/lib
/opt/SecureSpan/Gateway/migration/*.jar
/opt/SecureSpan/Gateway/migration/*.properties
%attr(0775,gateway,gateway) /opt/SecureSpan/Gateway/migration/*.sh

%pre
if [ `grep ^gateway: /etc/group` ]; then
	echo -n ""
else
	groupadd gateway
fi

if [ `grep ^gateway: /etc/passwd` ]; then
    #user gateway already exists, but needs it's group membership modified
    usermod -G gateway gateway
else
    useradd -G gateway -g gateway gateway
fi

if [ `grep ^ssgconfig: /etc/passwd` ]; then
    #user ssgconfig already exists, but needs it's group membership modified
    usermod -G gateway ssgconfig
else
    useradd -G gateway -g gateway ssgconfig

    # GEN001880
    # Update the permissions on ssgconfig's initialization files
    if [ -e /home/ssgconfig/.bash_logout ]; then
      chmod 740 /home/ssgconfig/.bash_logout
    fi
    if [ -e /home/ssgconfig/.bash_profile ]; then
      chmod 740 /home/ssgconfig/.bash_profile
    fi
    if [ -e /home/ssgconfig/.bashrc ]; then
      chmod 740 /home/ssgconfig/.bashrc
    fi
fi

%post

# After above item has executed, on first install only 
# we need to set password for ssgconfig and pre-expire it
# $1 equals what for first install?

if [ "$1" = "1" ] ; then
  # $1 is 1 on first install, not for upgrade
  echo "7layer" | passwd ssgconfig --stdin >/dev/null
  chage -M 365 ssgconfig
  chage -d 0   ssgconfig
fi

%preun
# Modifications to handle upgrades properly
if [ "$1" = "0" ] ; then
    # $1 is  on last uninstall, ie, package remove, not upgrade
    if [ `grep ^gateway: /etc/passwd` ]; then
        userdel -r gateway
    else
        echo -n ""
    fi

    if [ `grep ^ssgconfig: /etc/passwd` ]; then
        userdel -r ssgconfig
    else
        echo -n ""
    fi

    if [ `grep ^gateway: /etc/group` ]; then
        groupdel gateway
    else
        echo -n ""
    fi
fi

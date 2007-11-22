Summary: SecureSpan Gateway, Copyright Layer 7 Technologies 2003-2007
Name: ssg
Version: 4.3
Release: 1
Group: Applications/Internet
License: Commercial
URL: http://www.layer7tech.com
Vendor: Layer 7 Technologies
Packager: Layer 7 Technologies, <support@layer7tech.com>
source0: ssg.tar.gz
buildroot: %{_builddir}/%{name}-%{version}

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
mkdir -p %{buildroot}/ssg/etc/profile.d/
mv %{buildroot}/ssg/bin/profile %{buildroot}/ssg/etc/
mv %{buildroot}/ssg/bin/java.sh %{buildroot}/ssg/etc/profile.d/
mv %{buildroot}/ssg/bin/ssgruntimedefs.sh %{buildroot}/ssg/etc/profile.d/

mv %{buildroot}/ssg/bin/ssg-java.security %{buildroot}/ssg/etc/conf/partitions/partitiontemplate_/java.security
mv %{buildroot}/ssg/etc/conf/*.properties %{buildroot}/ssg/etc/conf/partitions/partitiontemplate_/
mv %{buildroot}/ssg/etc/conf/cluster_hostname-dist %{buildroot}/ssg/etc/conf/partitions/partitiontemplate_/

%files
# Root owned OS components

# Main tree, owned by gateway
%defattr(0644,gateway,gateway,0755)
%dir /ssg

# Group writable config files
%dir /ssg/etc
%config(noreplace) /ssg/etc/conf
%attr(0755,gateway,gateway) /ssg/etc/profile

# Group writeable directories and files

# The main Gateway jar
/ssg/Gateway.jar

# Ssg bin
%dir /ssg/bin
%attr(0755,gateway,gateway) /ssg/bin/*

%dir /ssg/etc/profile.d
%attr(0775,gateway,gateway) /ssg/etc/profile.d/*.sh

# Other stuff
/ssg/etc/ldapTemplates
/ssg/etc/uddiTemplates
/ssg/etc/sql
/ssg/etc/inf
/ssg/lib
%dir /ssg/logs
/ssg/modules
/ssg/var

#Config Wizard
%dir /ssg/configwizard
/ssg/configwizard/lib
/ssg/configwizard/*.jar
/ssg/configwizard/*.properties
%attr(0755,gateway,gateway) /ssg/configwizard/*.sh

# Group writable for migration stuff
%dir /ssg/migration
/ssg/migration/cfg
/ssg/migration/lib
/ssg/migration/*.properties
/ssg/migration/*.jar
%attr(0755,gateway,gateway) /ssg/migration/*.sh

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
fi

%post

#chown some files that may have been written as root in a previous install so that this, and future rpms can write them
/bin/chown -Rf gateway.gateway /ssg
chmod -f 775 /ssg/configwizard
chmod -f 664 /ssg/configwizard/*
chmod -f 775 /ssg/configwizard/*.sh
chmod -f 775 /ssg/configwizard/lib
chmod -fR 775 /ssg/etc/keys  2&>/dev/null

chmod -Rf 775 /ssg/etc/conf
chmod -Rf 775 /ssg/migration

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


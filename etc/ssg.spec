Summary: SecureSpan Gateway, Copyright Layer 7 Technologies 2003-2007
Name: ssg
Version: 4.0
Release: 1
Group: Applications/Internet
License: Commercial
URL: http://www.layer7tech.com
Vendor: Layer 7 Technologies
Packager: Layer 7 Technologies, <support@layer7tech.com>
source0: ssg.tar.gz
source1: jdk.tar.gz
buildroot: %{_builddir}/%{name}-%{version}

# Prevents rpm build from erroring and halting
#%undefine       __check_files

%description
SecureSpan Gateway software package

%clean
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}

%setup -qc %{buildroot}
%setup -qDTa 1 %{buildroot}
rm -rf %{buildroot}/ssg/jdk
mv %{buildroot}/jdk %{buildroot}/ssg/
[ ! -e %{buildroot}/ssg/jdk/db ] || rm -rf %{buildroot}/ssg/jdk/db
[ ! -e %{buildroot}/ssg/jdk/demo ] || rm -rf %{buildroot}/ssg/jdk/demo
[ ! -e %{buildroot}/ssg/jdk/sample ] || rm -rf %{buildroot}/ssg/jdk/sample
[ ! -e %{buildroot}/ssg/jdk/man ] || rm -rf %{buildroot}/ssg/jdk/man
[ ! -e %{buildroot}/ssg/jdk/jre/.systemPrefs ] || rm -rf %{buildroot}/ssg/jdk/jre/.systemPrefs
[ ! -e %{buildroot}/ssg/jdk/jre/javaws ] || rm -rf %{buildroot}/ssg/jdk/jre/javaws
[ ! -e %{buildroot}/ssg/jdk/jre/plugin ] || rm -rf %{buildroot}/ssg/jdk/jre/plugin
[ ! -e %{buildroot}/ssg/jdk/jre/CHANGES ] || rm -f %{buildroot}/ssg/jdk/jre/CHANGES
[ ! -e %{buildroot}/ssg/jdk/jre/lib/deploy ] || rm -rf %{buildroot}/ssg/jdk/jre/lib/deploy
[ ! -e %{buildroot}/ssg/jdk/jre/lib/desktop ] || rm -rf %{buildroot}/ssg/jdk/jre/lib/desktop
# Ensure that the libs are not executable, if they are then their dependencies are required by this rpm.
chmod -R '-x+X' %{buildroot}/ssg/jdk/jre/lib

%build
mkdir %{buildroot}/etc/
mkdir %{buildroot}/etc/snmp/
mkdir %{buildroot}/etc/profile.d/
mkdir %{buildroot}/etc/init.d/
mkdir %{buildroot}/etc/sysconfig
mkdir %{buildroot}/etc/logrotate.d/
mkdir -p %{buildroot}/home/ssgconfig/


mv %{buildroot}/ssg/bin/ssg-initd %{buildroot}/etc/init.d/ssg
mv %{buildroot}/ssg/bin/sysconfigscript-initd %{buildroot}/etc/init.d/ssgsysconfig
mv %{buildroot}/ssg/bin/ssg-dbstatus-initd %{buildroot}/etc/init.d/ssg-dbstatus
mv %{buildroot}/ssg/bin/my.cnf %{buildroot}/etc/my.cnf.ssg
mv %{buildroot}/ssg/bin/iptables %{buildroot}/etc/sysconfig/iptables
mv %{buildroot}/ssg/bin/ssgruntimedefs.sh %{buildroot}/etc/profile.d/ssgruntimedefs.sh
mv %{buildroot}/ssg/bin/tcp_tune.sh %{buildroot}/etc/init.d/tcp_tune
mv %{buildroot}/ssg/bin/snmpd.conf %{buildroot}/etc/snmp/snmpd.conf_example
mv %{buildroot}/ssg/bin/configuser_bashrc %{buildroot}/home/ssgconfig/.bashrc
mv %{buildroot}/ssg/etc/conf/*.properties %{buildroot}/ssg/etc/conf/partitions/partitiontemplate_/
mv %{buildroot}/ssg/etc/conf/cluster_hostname-dist %{buildroot}/ssg/etc/conf/partitions/partitiontemplate_/
mv %{buildroot}/ssg/tomcat/conf/server.xml %{buildroot}/ssg/etc/conf/partitions/partitiontemplate_/

# Root war is redundant
rm -f %{buildroot}/ssg/dist/*

chmod 755 %{buildroot}/etc/init.d/*
chmod 755 %{buildroot}/etc/profile.d/*.sh

%files
# Root owned OS components
%defattr(0755,root,root)
/etc/init.d/ssg
/etc/init.d/ssgsysconfig
/etc/init.d/ssg-dbstatus
/etc/init.d/tcp_tune
/etc/profile.d/ssgruntimedefs.sh
%config(noreplace) /etc/sysconfig/iptables
%defattr(0644,root,root)
/etc/snmp/snmpd.conf_example
# Config components, owned by root
%config(noreplace) /etc/my.cnf.ssg

# Main tree, owned by gateway
%defattr(0644,gateway,gateway,0755)
%dir /ssg

# Group writable config files
%dir /ssg/etc
%config(noreplace) /ssg/etc/conf

# Group writeable directories and files

# Ssg bin
%dir /ssg/bin
/ssg/bin/*.txt
%attr(0755,gateway,gateway) /ssg/bin/iptables*
%attr(0755,gateway,gateway) /ssg/bin/*.pl
%attr(0755,gateway,gateway) /ssg/bin/*.sh
%attr(0755,gateway,gateway) /ssg/bin/*-initd

# Tomcat
%dir /ssg/tomcat
/ssg/tomcat/LICENSE
/ssg/tomcat/NOTICE
/ssg/tomcat/RELEASE-NOTES
/ssg/tomcat/RUNNING.txt
%dir /ssg/tomcat/bin
/ssg/tomcat/bin/*.jar
%attr(0755,gateway,gateway) /ssg/tomcat/bin/*.sh
/ssg/tomcat/bin/*.xml
/ssg/tomcat/common
%config(noreplace) /ssg/tomcat/conf
%dir /ssg/tomcat/logs
/ssg/tomcat/server
/ssg/tomcat/shared
/ssg/tomcat/temp
/ssg/tomcat/webapps
/ssg/tomcat/work

# JDK
%dir /ssg/jdk
/ssg/jdk/COPYRIGHT
/ssg/jdk/LICENSE
/ssg/jdk/README.html
/ssg/jdk/THIRDPARTYLICENSEREADME.txt
%attr(0755,gateway,gateway) /ssg/jdk/bin
/ssg/jdk/include
%dir /ssg/jdk/jre
/ssg/jdk/jre/COPYRIGHT
/ssg/jdk/jre/LICENSE
/ssg/jdk/jre/README
/ssg/jdk/jre/THIRDPARTYLICENSEREADME.txt
/ssg/jdk/jre/Welcome.html
%attr(0755,gateway,gateway) /ssg/jdk/jre/bin
/ssg/jdk/jre/lib
%config(noreplace) /ssg/jdk/jre/lib/security/java.security
/ssg/jdk/lib

# Other stuff
/ssg/etc/ldapTemplates
/ssg/etc/sql
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

#System Config Wizard
%dir /ssg/sysconfigwizard
%dir /ssg/sysconfigwizard/configfiles
/ssg/sysconfigwizard/lib
/ssg/sysconfigwizard/*.jar
/ssg/sysconfigwizard/*.properties
# this script does not need to be executable
/ssg/sysconfigwizard/ssg_sys_config.pl
%attr(0755,gateway,gateway) /ssg/sysconfigwizard/*.sh

%attr(0664,ssgconfig,gateway) /home/ssgconfig/.bashrc

# Group writable for migration stuff
%dir /ssg/migration
/ssg/migration/cfg
/ssg/migration/lib
/ssg/migration/*.properties
/ssg/migration/*.jar
%attr(0755,gateway,gateway) /ssg/migration/*.sh

%pre
if [ `grep ^gateway: /etc/passwd` ]; then
	echo -n ""
       #  echo "user/group gateway already exists"
else
  adduser gateway
fi

if [ `grep ^ssgconfig: /etc/passwd` ]; then
	echo -n ""
       #  echo "user ssgconfig already exists"
else
  adduser -g gateway ssgconfig
  echo "7layer" | passwd ssgconfig --stdin >/dev/null
  chage -M 365 ssgconfig
  chage -d 0   ssgconfig
fi

SSGCONFIGENTRY=`grep ^ssgconfig /etc/sudoers`
if [ -n "${SSGCONFIGENTRY}" ]; then
    echo -n ""
    #user already exists in the sudoers file
else
    #the ssgconfig user is allowed to reboot the system, even when not at the console
    echo "ssgconfig  ALL = NOPASSWD: /sbin/reboot" >> /etc/sudoers
fi

rebootparam=`grep kernel.panic /etc/sysctl.conf`

if [ "$rebootparam" ]; then
	echo -n ""
	# its got the panic time in there already"
else
	echo "# kernel panic will reboot in 10 seconds " >> /etc/sysctl.conf
	echo "kernel.panic = 10" >> /etc/sysctl.conf
fi

# fix file limits

limits=`egrep -e \^\*\.\*soft\.\*nofile\.\*4096\$ /etc/security/limits.conf`

if [ "$limits" ]; then
	echo -n ""
	# already installed
else
	echo "# Layer 7 Limits"  >> /etc/security/limits.conf
	echo "*               soft    nproc   2047"  >> /etc/security/limits.conf
	echo "*               hard    nproc   16384"  >> /etc/security/limits.conf
	echo "*               soft    nofile  4096"  >> /etc/security/limits.conf
	echo "*               hard    nofile  63536"  >> /etc/security/limits.conf
	# 4096 files open and stuff
fi

# fix the getty

gettys=`grep ^s0:2345:respawn:/sbin/agetty /etc/inittab`

if [ "$gettys" ]; then
	echo -n ""
	# serial line agetty exists
else
	echo 	's0:2345:respawn:/sbin/agetty -L 115200 ttyS0 vt100' >> /etc/inittab
	echo 	'ttyS0' >> /etc/securetty
fi

connt=`grep "options ip_conntrack" /etc/modprobe.conf`

if [ "$connt" ]; then
	echo -n ""
	# connection tracking already set
else
	echo "options ip_conntrack hashsize=65536" >> /etc/modprobe.conf
	# add in larger hash size. final conntrack size will be 8* hashsize
	# This allows larger number of in-flight connections
fi


%post

echo "Layer 7 SecureSpan(tm) Gateway v4.0" >/etc/issue
echo "Kernel \r on an \m" >>/etc/issue
#add the ssg and the configuration service to chkconfig if they are not already there
/sbin/chkconfig --list ssg &>/dev/null
if [ ${?} -ne 0 ]; then
    # add service if not found
    /sbin/chkconfig --add ssg
fi

/sbin/chkconfig --list ssgsysconfig &>/dev/null
if [ ${?} -ne 0 ]; then
    # add service if not found
    /sbin/chkconfig --add ssgsysconfig
fi

/sbin/chkconfig --list ssg-dbstatus &>/dev/null
if [ ${?} -ne 0 ]; then
    # add service if not found
    /sbin/chkconfig --add ssg-dbstatus
fi

#chown some files that may have been written as root in a previous install so that this, and future rpms can write them
/bin/chown -Rf gateway.gateway /ssg
chmod -f 775 /ssg/configwizard
chmod -f 664 /ssg/configwizard/*
chmod -f 775 /ssg/configwizard/*.sh
chmod -f 775 /ssg/configwizard/lib
chmod -fR 775 /ssg/etc/keys  2&>/dev/null

chmod -f 775 /ssg/sysconfigwizard
chmod -f 664 /ssg/sysconfigwizard/*
chmod -f 775 /ssg/sysconfigwizard/lib
chmod -f 775 /ssg/sysconfigwizard/configfiles
chmod -f 775 /ssg/sysconfigwizard/*.sh

chmod -Rf 775 /ssg/etc/conf
chmod -Rf 775 /ssg/tomcat/conf
chmod -Rf 775 /ssg/jdk/jre/lib/security/
chmod -Rf 775 /ssg/migration

#migrate the structure to the new partitioning scheme using the configuration wizard
/ssg/configwizard/ssgconfig.sh -partitionMigrate &>/dev/null

%preun
# Modifications to handle upgrades properly
if [ "$1" = "0" ] ; then
	# last uninstall
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

    SSGCONFIGENTRY=`grep ^ssgconfig /etc/sudoers`
    if [ -n "${SSGCONFIGENTRY}" ]; then
        #remove the sudoers entry for ssgconfig
        perl -pi.bak -e 's/^ssgconfig.*$//g' /etc/sudoers
    fi

    gettys=`grep ^s0:2345:respawn:/sbin/agetty /etc/inittab`

	if [ "$gettys" ]; then
		perl -pi.bak -e 's/^s0.*agetty.*//' /etc/inittab
		perl -pi.bak -e 's/ttyS0//' /etc/securetty
	fi

    chkconfig --del ssg-dbstatus
    chkconfig --del ssgsysconfig
    chkconfig --del ssg
fi

%changelog
* Tue May 15 2007 CY
- 4.0
* Mon Jan 29 2007 CY
- 3.7
* Wed Dec 20 2006 SMJ
- add ssb-dbstatus service
* Tue Nov 28 2006 MJE
- added partition migration step to %post
* Tue Nov 14 2006 MJE
- 3.6.5
* Mon Sep 25 2006 CY
- 3.6rc4
* Mon Aug 21 2006 CY
- 3.6m4d-1
* Mon Aug 14 2006 CY
- 3.6m4b-1
* Mon Jul 17 2006 CY
- 3.6m3b-1
* Fri Jul 14 2006 CY
- 3.6m3a-2
* Fri Jul 14 2006 CY
- 3.6m3a-1
* Fri Jun 30 2006 CY
- 3.6m3-2
* Thu Jun 29 2006 CY
- 3.6m3
* Tue Jan 31 2006 JWT
- install.pl is gone, other changes to track version 4.0
* Tue Aug 04 2005 JWT
- Build 3200 Serial line console modifications
* Mon May 02 2005 JWT
- Build 3133 Modifies Issue files to show SSG id
* Thu Oct 28 2004 JWT
- Build 3028 First version

Summary: Secure Span Gateway
Name: ssg
Version: 3.6m4d
Release: 1
Group: Applications/Internet
License: Copyright Layer7 Technologies 2003-2005
URL: http://www.layer7tech.com
Packager: Layer7 Technologies, <support@layer7tech.com>
Source0: ~/rpm/SOURCES/ssg.tar.gz
buildroot: %{_builddir}/%{name}-%{version}
provides: ssg

# Prevents rpm build from erroring and halting
%undefine       __check_files

%description
Secure Span Gateway software package

%clean
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}
mkdir %{buildroot}
cd %{buildroot}
tar -xzf ~/rpm/SOURCES/ssg.tar.gz

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
mv %{buildroot}/ssg/bin/my.cnf %{buildroot}/etc/my.cnf.ssg
mv %{buildroot}/ssg/bin/iptables %{buildroot}/etc/sysconfig/iptables
mv %{buildroot}/ssg/bin/ssgruntimedefs.sh %{buildroot}/etc/profile.d/ssgruntimedefs.sh
mv %{buildroot}/ssg/bin/tcp_tune.sh %{buildroot}/etc/init.d/tcp_tune
mv %{buildroot}/ssg/bin/snmpd.conf %{buildroot}/etc/snmp/snmpd.conf_example
mv %{buildroot}/ssg/bin/configuser_bashrc %{buildroot}/home/ssgconfig/.bashrc

# Root war is redundant
rm -f %{buildroot}/ssg/dist/*

chmod 755 %{buildroot}/etc/init.d/*
chmod 755 %{buildroot}/etc/profile.d/*.sh

%files
%defattr(-,root,root)
/etc/init.d/ssg
/etc/init.d/ssgsysconfig
/etc/init.d/tcp_tune
/etc/snmp/snmpd.conf_example
/etc/profile.d/ssgruntimedefs.sh
%config(noreplace) /etc/my.cnf.ssg
%config(noreplace) /etc/sysconfig/iptables
%defattr(-,gateway,gateway)
/ssg/*
%defattr(775,gateway,gateway)
%config(noreplace) /ssg/etc/conf/
%config(noreplace) /ssg/etc/conf/*
%config(noreplace) /ssg/etc/keys/
%config(noreplace) /ssg/tomcat/conf/*
%config(noreplace) /ssg/configwizard
%config(noreplace) /ssg/configwizard/*
%config(noreplace) /ssg/sysconfigwizard
%config(noreplace) /ssg/sysconfigwizard/*
%config(noreplace) /ssg/jdk/jre/lib/ext/*
%config(noreplace) /ssg/jdk/jre/lib/security/
%config(noreplace) /ssg/jdk/jre/lib/security/java.security
%defattr(-,ssgconfig,gateway)
/home/ssgconfig/.bashrc

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

echo "Layer 7 SecureSpan(tm) Gateway v3.6" >/etc/issue
echo "Kernel \r on an \m" >>/etc/issue
#add the ssg and the configuration service to chkconfig if they are not already there
SSGEXISTS=`/sbin/chkconfig --list | grep "ssg\b"`
if [ "${SSGEXISTS}" ]; then
    echo -n ""
    #if we find a ssg entry in chkconfig, leave it alone
else
    /sbin/chkconfig --add ssg
fi

SSGCONFIGEXISTS=`/sbin/chkconfig --list | grep "ssgsysconfig\b"`
if [ "${SSGCONFIGEXISTS}" ]; then
    echo -n ""
    #if we find a ssgsysconfig entry in chkconfig, leave it alone
else
    /sbin/chkconfig --add ssgsysconfig
fi
#chown some files that may have been written as root in a previous install so that this, and future rpms can write them
/bin/chown -Rf gateway.gateway /ssg
chmod -Rf 775 /ssg/configwizard
chmod -Rf 775 /ssg/sysconfigwizard
chmod -Rf 775 /ssg/etc/conf
chmod -Rf 775 /ssg/etc/keys
chmod -Rf 775 /ssg/tomcat/conf
chmod -Rf 775 /ssg/jdk/jre/lib/security/

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

    chkconfig --del ssgsysconfig
    chkconfig --del ssg
fi

%changelog
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

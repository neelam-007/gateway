Summary: Secure Span Gateway
Name: ssg
Version: 4.0m4
Release: 1
Group: Applications/Internet
Copyright: Copyright Layer7 Technologies 2003-2005
URL: http://www.layer7tech.com
Packager: Layer7 Technologies, <support@layer7tech.com> 
Source0: /tmp/ssg.tar.gz
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
tar -xzf /tmp/ssg.tar.gz

%build
mkdir %{buildroot}/etc/
mkdir %{buildroot}/etc/snmp/
mkdir %{buildroot}/etc/profile.d/
mkdir %{buildroot}/etc/init.d/
mkdir %{buildroot}/etc/sysconfig
mkdir %{buildroot}/etc/logrotate.d/

mv %{buildroot}/ssg/bin/ssg-initd %{buildroot}/etc/init.d/ssg 
mv %{buildroot}/ssg/bin/my.cnf %{buildroot}/etc/my.cnf.ssg
mv %{buildroot}/ssg/bin/iptables %{buildroot}/etc/sysconfig/iptables
mv %{buildroot}/ssg/bin/ssgruntimedefs.sh %{buildroot}/etc/profile.d/ssgruntimedefs.sh
mv %{buildroot}/ssg/bin/tcp_tune.sh %{buildroot}/etc/init.d/tcp_tune
mv %{buildroot}/ssg/bin/snmpd.conf %{buildroot}/etc/snmp/snmpd.conf_example

# Root war is redundant
rm -f %{buildroot}/ssg/dist/*

chmod 755 %{buildroot}/etc/init.d/*
chmod 755 %{buildroot}/etc/profile.d/*.sh
chmod 755 %{buildroot}/ssg/configwizard/*.sh

%files 
%defattr(-,root,root)
/etc/init.d/ssg 
/etc/init.d/tcp_tune
/etc/snmp/snmpd.conf_example
/etc/profile.d/ssgruntimedefs.sh
%config(noreplace) /etc/my.cnf.ssg
%config(noreplace) /etc/sysconfig/iptables
%defattr(-,gateway,gateway)
%config(noreplace) /ssg/etc/conf/*
%config(noreplace) /ssg/tomcat/conf/*
%config(noreplace) /ssg/*/jre/lib/security/java.security
/ssg/*


%pre
if [ `grep ^gateway: /etc/passwd` ]; then
	echo -n ""
       #  echo "user/group gateway already exists"
else
  adduser gateway
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

echo "Layer 7 SecureSpan(tm) Gateway v4.0m4" >/etc/issue
echo "Kernel \r on an \m" >>/etc/issue
echo "Layer 7 SecureSpan(tm) Gateway v4.0m4" >/etc/issue.net
echo "Kernel \r on an \m" >>/etc/issue.net

%preun
# Modifications to handle upgrades properly
if [ "$1" = "0" ] ; then 
	# last uninstall
        if [ `grep ^gateway: /etc/passwd` ]; then
            userdel -r gateway
        else
            echo -n ""
        fi
	gettys=`grep ^s0:2345:respawn:/sbin/agetty /etc/inittab`

	if [ "$gettys" ]; then
		perl -pi.bak -e 's/^s0.*agetty.*//' /etc/inittab
		perl -pi.bak -e 's/ttyS0//' /etc/securetty
	fi

fi

%changelog 
* Tue Jan 31 2006 JWT
- install.pl is gone, other changes to track version 4.0
* Tue Aug 04 2005 JWT
- Build 3200 Serial line console modifications
* Mon May 02 2005 JWT
- Build 3133 Modifies Issue files to show SSG id 
* Thu Oct 28 2004 JWT 
- Build 3028 First version

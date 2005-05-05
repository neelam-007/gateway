Summary: Secure Span Gateway
Name: ssg
Version: 3.1
Release: rc1
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
SSG software distribution on standard system
Does: ssg, network config, profiles
Modifies startup config to run only expected services

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
mkdir %{buildroot}/etc/iptables
mkdir %{buildroot}/etc/sysconfig

mv %{buildroot}/ssg/bin/ssg-initd %{buildroot}/etc/init.d/ssg 
mv %{buildroot}/ssg/bin/tarari-initd %{buildroot}/etc/init.d/tarari
mv %{buildroot}/ssg/bin/my.cnf %{buildroot}/etc/my.cnf.ssg
mv %{buildroot}/ssg/bin/iptables %{buildroot}/etc/sysconfig/iptables
mv %{buildroot}/ssg/bin/ssgruntimedefs.sh %{buildroot}/etc/profile.d/ssgruntimedefs.sh
mv %{buildroot}/ssg/bin/tarari.sh %{buildroot}/etc/profile.d/tarari.sh
mv %{buildroot}/ssg/bin/back_route %{buildroot}/etc/init.d/back_route
mv %{buildroot}/ssg/bin/tcp_tune.sh %{buildroot}/etc/init.d/tcp_tune
mv %{buildroot}/ssg/bin/snmpd.conf %{buildroot}/etc/snmp/snmpd.conf_example

%files 
%defattr(-,root,root)
/etc/init.d/ssg 
/etc/init.d/tarari
/etc/init.d/back_route
/etc/init.d/tcp_tune
/etc/snmp/snmpd.conf_example
/etc/profile.d/ssgruntimedefs.sh
/etc/profile.d/tarari.sh
%config(noreplace) /etc/my.cnf.ssg
%config(noreplace) /etc/sysconfig/iptables
%defattr(-,gateway,gateway)
%config(noreplace) /ssg/etc/conf/*
%config(noreplace) /ssg/tomcat/conf/*
/ssg/


%pre
if [ `grep ^gateway: /etc/passwd` ]; then
  echo "user/group gateway already exists"
else
  adduser gateway
fi

%post
# Check for existence of install crumbs left by install.pl
if [ -e /etc/SSG_INSTALL ]; then 
	if [ grep "# SSG_INSTALL V3.1" /etc/SSG_INSTALL ]; then
		# reinstall or minor upgrade
		echo -n ""
	else 
		echo "** Upgrading from 3.0 to 3.1: Run upgrade script: /ssg/bin/upgrade.sh doUpgrade **"
	fi
else 
	echo "** New system: Run interactive /ssg/bin/install.pl to configure this system **"
fi

# FIXME: update for new version
echo "Layer 7 SecureSpan(tm) Gateway v3.1\nKernel \r on an \m\n" >/etc/issue
echo "Layer 7 SecureSpan(tm) Gateway v3.1\nKernel \r on an \m\n" >/etc/issue.net

%postun

%changelog 

* Mon May 02 2005 JWT
- Build 3133. Modifies Issue files to show SSG id 
* Thu Oct 28 2004 JWT 
- Build 3079. Does not yet overwrite mysql configuration files. Provides relatively small version of install.pl

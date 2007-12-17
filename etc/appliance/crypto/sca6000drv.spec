Summary: SCA 6000 Support for SecureSpan Gateway
Name: SCA6000drv
Version: 1.0
Release: 4b
Group: Applications/Internet
License: GPL
URL: http://www.layer7tech.com
Packager: Layer 7 Technologies, <support@layer7tech.com>
Source0: /tmp/sca6000drv.tgz
buildroot: %{_builddir}/%{name}-%{version}
provides: sca6000drv-l7
requires: kernel-smp >= 2.6.9-55.EL
requires: kernel-smp <= 2.6.9-55.0.2.EL
requires: sun-sca6000
# Prevents rpm build from erroring and halting
%undefine       __check_files

%description
Sun SCA6000 card hardware drivers

%clean
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}
mkdir %{buildroot}
cd %{buildroot}
tar -xzf /tmp/sca6000drv.tgz

%build
# Add in the chkconfig items so we can use the redhat tools on the
# appliance
# need chkconfig, description, processname lines
sed -i '5i # chkconfig: - 95 05 \
# description: Startup SCA6000 card support driver and daemon\
# processname: scakiod' %{buildroot}/etc/init.d/sca

%files
%defattr(-,root,root)
/etc/init.d/sca*
/opt
%pre


%post
chkconfig sca on
%preun

%changelog
* Thu Jul 10 2007 JWT
- Rebuild with better version number
* Thu Jun 7 2007 JWT
- Build First version - binaries only

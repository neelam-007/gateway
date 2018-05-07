Summary: SCA 6000 Support for Layer 7 Gateway
Name: SCA6000drv
Version: 1.1.6
Release: 1.el5
Group: Applications/Internet
License: GPL
URL: http://www.layer7tech.com
Packager: Layer 7 Technologies, <support@layer7tech.com>
Source0: /tmp/sca6000drv.tgz
buildroot: %{_builddir}/%{name}-%{version}
provides: sca6000drv-l7
requires: kernel >= 2.6.18-128.el5
requires: kernel <= 2.6.18-164.6.1.el5
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

%files
%defattr(-,root,root)
/opt

%pre

if [ ! -e /etc/profile.d/scapath.sh ] ; then
    echo 'PATH=$PATH:/opt/sun/sca6000/bin:/opt/sun/sca6000/sbin' > /etc/profile.d/scapath.sh
fi

%post

%preun

if [ -e /etc/profile.d/scapath.sh ] ; then
    rm /etc/profile.d/scapath.sh
fi

%changelog
* Thu Jan 3 2008 MJE
- Added script in profile.d to append the sca binaries to the path.
* Thu Jul 10 2007 JWT
- Rebuild with better version number
* Thu Jun 7 2007 JWT
- Build First version - binaries only

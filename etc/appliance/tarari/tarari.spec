Summary: Tarari Support for SecureSpan Gateway
Name: ssg-tarari
Version: 5.1.1.52s
Release: 2
Group: Applications/Internet
License: Copyright Layer 7 Technologies. Portions copyright Tarari 2003-2008
URL: http://www.layer7tech.com
Packager: Layer 7 Technologies, <support@layer7tech.com>
Source0: /tmp/tarari.tar.gz
Source1: /tmp/tarari-kernel-drivers.tar.gz
buildroot: %{_builddir}/%{name}-%{version}
requires: ssg >= 4.3
requires: kernel-smp-x86_64 = 2.6.9-67.0.1.EL
provides: ssg-tarari

# Prevents rpm build from erroring and halting
%undefine       __check_files

%description
Tarari Support Package for SecureSpan Gateway
Include RaxJ, base package, crypto, xmlcp, etc
Adds scripts. Version support is specific:
Release conforms to Tarari version, version conforms to kernel

%clean
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}
%setup -qc %{buildroot}
%setup -qDTa 1 %{buildroot}

%build
mkdir -p %{buildroot}/etc/init.d/
mkdir -p %{buildroot}/ssg/etc/profile.d/

mv %{buildroot}/etc/appliance/tarari/tarari-initd %{buildroot}/etc/init.d/tarari
mv %{buildroot}/etc/appliance/tarari/tarariopts.sh %{buildroot}/ssg/etc/profile.d/tarariopts.sh

mkdir -p %{buildroot}/ssg/lib/ext
cp %{buildroot}/usr/local/Tarari/lib/tarari_raxj.jar %{buildroot}/ssg/lib/ext

rm -rf %{buildroot}/usr/local/Tarari/test/
rm -rf %{buildroot}/usr/local/Tarari/src
rm -rf %{buildroot}/usr/local/Tarari/include
rm -rf %{buildroot}/usr/local/Tarari/docs

%files
%defattr(-,root,root)
/etc/init.d/tarari
/usr/local/Tarari/
/usr/local/Tarari/*

%defattr(-,gateway,gateway)
/ssg/lib/ext/tarari_raxj.jar
/ssg/etc/profile.d/tarariopts.sh

%pre

%post
if [ -e "/etc/init.d/tarari" ]; then
    /bin/chmod -f +x /etc/init.d/tarari
fi

%preun

%changelog
* Thu Oct 5 2006 MJE
- build for any kernels contained in tarari-kernel-drivers.tar.gz. Change descriptions and chmod the startup script
* Thu Mar 9 2006 JWT
- Build First version - for 2.6.9-34 and Tarari build 64

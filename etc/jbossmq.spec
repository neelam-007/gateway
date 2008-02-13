Summary: SSG JBoss MQ kit
Name: ssg-jbossmq
Version: 3.7
Release: 1
Group: Applications/Internet
License: Copyright Layer7 Technologies 2003-2008
URL: http://www.layer7tech.com
Packager: Layer7 Technologies, <support@layer7tech.com>
Source0: ~/rpm/SOURCES/jbossmq.tar.gz

buildroot: %{_builddir}/%{name}-%{version}
provides: ssg-jbossmq
requires: ssg >= 3.2

# Prevents rpm build from erroring and halting
%undefine       __check_files

%description
Secure Span Gateway JBoss MQ Kit (Sourced From JBoss 4.0.5GA Build)

%clean
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}
mkdir %{buildroot}
cd %{buildroot}
mkdir tmp
cd tmp
tar -xzvf ~/rpm/SOURCES/jbossmq.tar.gz


%build
mkdir -p %{buildroot}/ssg/tomcat/webapps/ROOT/WEB-INF/lib/

cd %{buildroot}/tmp
mv *.jar  %{buildroot}/ssg/tomcat/webapps/ROOT/WEB-INF/lib/

%files
# Root owned OS components
%defattr(0775,gateway,gateway)
/ssg

%pre

%changelog
* Wed Feb 02 2006 JWT
- First version

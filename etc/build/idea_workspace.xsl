<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSL Transform to update an IDEA workspace file.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <!-- Parameters for customizing options -->
    <xsl:param name="idea.teamcity.server">http://devteamcity.l7tech.com:8111/</xsl:param>
    <xsl:param name="idea.teamcity.user"></xsl:param>
    <xsl:param name="idea.teamcity.password"></xsl:param>
    <xsl:param name="idea.teamcity.remember">true</xsl:param>
    <xsl:param name="idea.client.vmOptions">-Dcom.l7tech.util.buildVersion=5.0</xsl:param>
    <xsl:param name="idea.gateway.vmOptions">-Dcom.l7tech.server.home="build/deploy/Gateway" -Dcom.l7tech.util.buildVersion=5.0</xsl:param>
    <xsl:param name="idea.controller.vmOptions">-Dcom.l7tech.util.buildVersion=5.0 -Dcom.l7tech.server.log.console=true -Dcom.l7tech.gateway.home=../../Gateway</xsl:param>
    <xsl:param name="idea.configwiz.vmOptions">-Dcom.l7tech.util.buildVersion=5.0</xsl:param>
    <xsl:param name="idea.pcconfig.vmOptions">-Dcom.l7tech.util.buildVersion=5.0 -Dcom.l7tech.server.controller.home=build/deploy/Controller</xsl:param>
    <xsl:param name="idea.manager.vmOptions">-Dcom.l7tech.util.buildVersion=5.0</xsl:param>
    <xsl:param name="idea.esm.vmOptions">-Dcom.l7tech.util.buildVersion=1.0</xsl:param>
    <xsl:param name="idea.controller.home">build/deploy/Controller</xsl:param>
    <xsl:param name="idea.esm.home">build/deploy/EnterpriseManager</xsl:param>

    <!-- Global options -->
    <xsl:output method="xml" encoding="utf-8" indent="yes"/>

    <!-- Add teamcity options if not present -->
    <xsl:template match="/project/component[@name = 'BuildServerSettings' and not(option)]">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <option name="SERVER_URL" value="{$idea.teamcity.server}"/>
            <option name="LOGIN" value="{$idea.teamcity.user}"/>
            <xsl:if test="$idea.teamcity.password != ''">
                <option name="PASSWORD" value="{$idea.teamcity.password}"/>
            </xsl:if>
            <option name="REMEMBER_ME" value="{$idea.teamcity.remember}"/>
        </xsl:copy>
    </xsl:template>

    <!-- Update run options if there are none -->
    <xsl:template match="/project/component[@name = 'RunManager' and not(configuration)]">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
                <configuration default="false" name="EnterpriseManagerServer" type="Application" factoryName="Application"
                               enabled="false" merge="false">
                    <option name="MAIN_CLASS_NAME" value="com.l7tech.server.ems.EsmMain"/>
                    <option name="VM_PARAMETERS" value="{$idea.esm.vmOptions}"/>
                    <option name="PROGRAM_PARAMETERS" value=""/>
                    <option name="WORKING_DIRECTORY" value="{$idea.esm.home}"/>
                    <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false"/>
                    <option name="ALTERNATIVE_JRE_PATH" value=""/>
                    <option name="ENABLE_SWING_INSPECTOR" value="false"/>
                    <option name="ENV_VARIABLES"/>
                    <option name="PASS_PARENT_ENVS" value="true"/>
                    <envs/>
                    <RunnerSettings RunnerId="Run"/>
                    <ConfigurationWrapper RunnerId="Run"/>
                    <method>
                        <option name="Make" value="true"/>
                    </method>
                </configuration>
                <configuration default="false" name="Gateway" type="Application" factoryName="Application"
                               enabled="false" merge="false">
                    <option name="MAIN_CLASS_NAME" value="com.l7tech.server.boot.GatewayMain"/>
                    <option name="VM_PARAMETERS" value="{$idea.gateway.vmOptions}"/>
                    <option name="PROGRAM_PARAMETERS" value=""/>
                    <option name="WORKING_DIRECTORY" value="file://$PROJECT_DIR$"/>
                    <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false"/>
                    <option name="ALTERNATIVE_JRE_PATH" value=""/>
                    <option name="ENABLE_SWING_INSPECTOR" value="false"/>
                    <option name="ENV_VARIABLES"/>
                    <option name="PASS_PARENT_ENVS" value="true"/>
                    <module name="layer7-gateway-server"/>
                    <envs/>
                    <RunnerSettings RunnerId="Run"/>
                    <ConfigurationWrapper RunnerId="Run"/>
                    <method>
                        <option name="Make" value="true"/>
                    </method>
                </configuration>
                <configuration default="false" name="Process Controller" type="Application" factoryName="Application"
                               enabled="false" merge="false">
                    <option name="MAIN_CLASS_NAME" value="com.l7tech.server.processcontroller.ProcessControllerDaemon"/>
                    <option name="VM_PARAMETERS" value="{$idea.controller.vmOptions}"/>
                    <option name="PROGRAM_PARAMETERS" value=""/>
                    <option name="WORKING_DIRECTORY" value="{$idea.controller.home}"/>
                    <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false"/>
                    <option name="ALTERNATIVE_JRE_PATH" value=""/>
                    <option name="ENABLE_SWING_INSPECTOR" value="false"/>
                    <option name="ENV_VARIABLES"/>
                    <option name="PASS_PARENT_ENVS" value="true"/>
                    <module name="layer7-gateway-processcontroller"/>
                    <envs/>
                    <RunnerSettings RunnerId="Run"/>
                    <ConfigurationWrapper RunnerId="Run"/>
                    <method>
                        <option name="Make" value="true"/>
                    </method>
                </configuration>
                <configuration default="false" name="Manager" type="Application" factoryName="Application"
                               enabled="false" merge="false">
                    <option name="MAIN_CLASS_NAME" value="com.l7tech.console.Main"/>
                    <option name="VM_PARAMETERS" value="{$idea.manager.vmOptions}"/>
                    <option name="PROGRAM_PARAMETERS" value=""/>
                    <option name="WORKING_DIRECTORY" value="file://$PROJECT_DIR$"/>
                    <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false"/>
                    <option name="ALTERNATIVE_JRE_PATH" value=""/>
                    <option name="ENABLE_SWING_INSPECTOR" value="false"/>
                    <option name="ENV_VARIABLES"/>
                    <option name="PASS_PARENT_ENVS" value="true"/>
                    <module name="layer7-gateway-console"/>
                    <envs/>
                    <RunnerSettings RunnerId="Run"/>
                    <ConfigurationWrapper RunnerId="Run"/>
                    <method>
                        <option name="Make" value="true"/>
                    </method>
                </configuration>
                <configuration default="false" name="XML VPN Client" type="Application" factoryName="Application"
                               enabled="false" merge="false">
                    <option name="MAIN_CLASS_NAME" value="com.l7tech.client.gui.Main"/>
                    <option name="VM_PARAMETERS" value="{$idea.client.vmOptions}"/>
                    <option name="PROGRAM_PARAMETERS" value=""/>
                    <option name="WORKING_DIRECTORY" value="file://$PROJECT_DIR$"/>
                    <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false"/>
                    <option name="ALTERNATIVE_JRE_PATH" value=""/>
                    <option name="ENABLE_SWING_INSPECTOR" value="false"/>
                    <option name="ENV_VARIABLES"/>
                    <option name="PASS_PARENT_ENVS" value="true"/>
                    <module name="layer7-xmlvpnclient"/>
                    <envs/>
                    <RunnerSettings RunnerId="Run"/>
                    <ConfigurationWrapper RunnerId="Run"/>
                    <method>
                        <option name="Make" value="true"/>
                    </method>
                </configuration>
                <configuration default="false" name="XML VPN Client (Config)" type="Application"
                               factoryName="Application" enabled="false" merge="false">
                    <option name="MAIN_CLASS_NAME" value="com.l7tech.proxy.cli.Main"/>
                    <option name="VM_PARAMETERS" value="{$idea.client.vmOptions}"/>
                    <option name="PROGRAM_PARAMETERS" value=""/>
                    <option name="WORKING_DIRECTORY" value="file://$PROJECT_DIR$"/>
                    <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false"/>
                    <option name="ALTERNATIVE_JRE_PATH" value=""/>
                    <option name="ENABLE_SWING_INSPECTOR" value="false"/>
                    <option name="ENV_VARIABLES"/>
                    <option name="PASS_PARENT_ENVS" value="true"/>
                    <module name="layer7-xmlvpnclient"/>
                    <envs/>
                    <method>
                        <option name="Make" value="true"/>
                    </method>
                </configuration>
                <configuration default="false" name="Gateway Configuration Client" type="Application"
                               factoryName="Application" enabled="false" merge="false">
                    <option name="MAIN_CLASS_NAME" value="com.l7tech.gateway.config.client.Main"/>
                    <option name="VM_PARAMETERS" value="{$idea.configwiz.vmOptions}"/>
                    <option name="PROGRAM_PARAMETERS" value=""/>
                    <option name="WORKING_DIRECTORY" value="file://$PROJECT_DIR$"/>
                    <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false"/>
                    <option name="ALTERNATIVE_JRE_PATH" value=""/>
                    <option name="ENABLE_SWING_INSPECTOR" value="false"/>
                    <option name="ENV_VARIABLES"/>
                    <option name="PASS_PARENT_ENVS" value="true"/>
                    <module name="layer7-gateway-config"/>
                    <envs/>
                    <method>
                        <option name="Make" value="true"/>
                    </method>
            </configuration>
            <configuration default="false" name="PC Configuration Client" type="Application"
                           factoryName="Application" enabled="false" merge="false">
                <option name="MAIN_CLASS_NAME" value="com.l7tech.gateway.config.client.beans.trust.TrustInterviewer"/>
                <option name="VM_PARAMETERS" value="{$idea.pcconfig.vmOptions}"/>
                <option name="PROGRAM_PARAMETERS" value=""/>
                <option name="WORKING_DIRECTORY" value="file://$PROJECT_DIR$"/>
                <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false"/>
                <option name="ALTERNATIVE_JRE_PATH" value=""/>
                <option name="ENABLE_SWING_INSPECTOR" value="false"/>
                <option name="ENV_VARIABLES"/>
                <option name="PASS_PARENT_ENVS" value="true"/>
                <module name="layer7-gateway-config"/>
                <envs/>
                <method>
                    <option name="Make" value="true"/>
                </method>
        </configuration>
        </xsl:copy>
    </xsl:template>

    <!-- Copy by default -->
    <xsl:template match="@*|*|processing-instruction()|comment()">
        <xsl:copy>
            <xsl:apply-templates select="*|@*|text()|processing-instruction()|comment()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
        

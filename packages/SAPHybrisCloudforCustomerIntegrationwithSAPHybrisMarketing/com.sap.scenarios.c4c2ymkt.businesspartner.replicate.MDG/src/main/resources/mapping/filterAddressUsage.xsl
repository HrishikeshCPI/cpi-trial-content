<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">  
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/> 
	<xsl:template match="node() | @*">
		<xsl:copy>
			<xsl:apply-templates select="node() | @*"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="AddressInformation">
        <xsl:variable name="countAddressUsage" select="count(//AddressUsage/AddressUsageCode)" />	
		<xsl:variable name="strAddressUsageCode" select="AddressUsage/AddressUsageCode"/>

		<xsl:if test="not(($countAddressUsage eq 1) and ($strAddressUsageCode ne 'XXDEFAULT'))">
			<xsl:copy>
				<xsl:apply-templates select="@* | node()" />
			</xsl:copy>
		</xsl:if>	
	</xsl:template>
	
	<xsl:template match="AddressUsage">
		<xsl:if test="AddressUsageCode eq 'XXDEFAULT'">
			<xsl:copy>
				<xsl:apply-templates select="@* | node()" />
			</xsl:copy>
		</xsl:if>	
	</xsl:template>
</xsl:stylesheet>
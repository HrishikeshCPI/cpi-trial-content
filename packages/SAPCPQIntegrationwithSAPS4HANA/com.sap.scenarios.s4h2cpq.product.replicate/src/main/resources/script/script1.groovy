/*
 The integration developer needs to create the method processData 
 This method takes Message object of package com.sap.gateway.ip.core.customdev.util 
which includes helper methods useful for the content developer:
The methods available are:
    public java.lang.Object getBody()
	public void setBody(java.lang.Object exchangeBody)
    public java.util.Map<java.lang.String,java.lang.Object> getHeaders()
    public void setHeaders(java.util.Map<java.lang.String,java.lang.Object> exchangeHeaders)
    public void setHeader(java.lang.String name, java.lang.Object value)
    public java.util.Map<java.lang.String,java.lang.Object> getProperties()
    public void setProperties(java.util.Map<java.lang.String,java.lang.Object> exchangeProperties) 
       public void setProperty(java.lang.String name, java.lang.Object value)
 */
import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import groovy.xml.*;
import java.util.regex.*;


def Message processData(Message message) {
    
    // check if the logging is switched on 
    def properties = message.getProperties();
    String logger = properties.get("Logger");
    
    if(logger.equals("true"))
    {

    
    def body = message.getBody(java.lang.String) as String;
    def messageLog = messageLogFactory.getMessageLog(message);

      
    // some XML parser stuff 
    /*
            def xmlutil = new XmlUtil();
    		def xmldoc = new XmlParser().parseText(payload);
     		def serialized = xmlutil.serialize(xmldoc);
    		String log = serialized;

   */
   
   // Messagelog 
    messageLog.addAttachmentAsString("Request_SOAP", body, "text/plain");
    
    }
    
    return message;
}



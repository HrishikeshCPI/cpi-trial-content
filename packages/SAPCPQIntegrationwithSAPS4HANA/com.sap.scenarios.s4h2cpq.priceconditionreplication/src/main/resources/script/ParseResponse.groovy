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

def Message processData(Message message) {

  def payload = message.getBody(String.class);
  def messageLog = messageLogFactory.getMessageLog(message)

    
// get Status Info from Response
  def root = new XmlSlurper().parseText(payload);
  def result = root.UpsertPricebookWithDistributionChainResult.text();
  int start_index = result.indexOf('<Status>');
  start_index = start_index + 8;
  int end_index = result.indexOf('</Status>');
  String status = result.substring(start_index, end_index);
  
// looging  
    def properties = message.getProperties();
    String logger = properties.get("Logger");
    if(logger.equals("true"))
    {
     messageLog.setStringProperty("payload", payload)
     messageLog.addAttachmentAsString("Response_SOAP", payload, "text/plain");
     messageLog.addAttachmentAsString("Status", status, "text/plain");
    }

// throw exception in case of error
    if (!status.equals("ALL_ENTRIES_IMPORTED")) {
        throw new Exception("Error in CPQ Response");
    }
    
return message;

}
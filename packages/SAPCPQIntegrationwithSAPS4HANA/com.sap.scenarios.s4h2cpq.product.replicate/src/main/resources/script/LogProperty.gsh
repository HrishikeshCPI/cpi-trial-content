import com.sap.gateway.ip.core.customdev.util.Message;

import java.util.HashMap;

 

 

def Message processData(Message message) {
    
    // external Parameter via WEBUI ca be created via {{<Parameter>}} token
    
    // check if the logging is switched on 
    def properties = message.getProperties();
    String logger = properties.get("Logger");
    
    if(logger.equals("true"))
    {

         def payload = message.getBody(String.class);
        def messageLog = messageLogFactory.getMessageLog(message)

         messageLog.setStringProperty("payload", payload)
  
         def Header = message.getHeaders();
         def IDocNr = Header.get("SAP_ApplicationID");
 

        //messageLog.addAttachmentAsString("IDOC Nr:", IDocNr, "text/plain");
        messageLog.addAttachmentAsString("SOAP", payload, "text/plain");

    }   
  return message;

}


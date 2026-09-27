import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;

def Message processData(Message message) {

  def payload = message.getBody(String.class);
  def messageLog = messageLogFactory.getMessageLog(message)

// log the Response Payload
  // check if the logging is switched on 
    def properties = message.getProperties();
    String logger = properties.get("Logger");
    
    if(logger.equals("true"))
    {
     messageLog.setStringProperty("payload", payload)
    messageLog.addAttachmentAsString("Response_SOAP", payload, "text/plain");
    }
  
// get + log Status field from Response
  def root = new XmlSlurper().parseText(payload);
  def status = root.CustomerAdministrationResult.Result.Status.text();
  
// set some proiperties which are passed to mail 
    String CPIUrl = System.getenv("HC_APPLICATION_URL");
    message.setHeader( "Url", CPIUrl);


// throw exception in case of error
    if (status.equals("NOK")) {

        throw new Exception("Error in CPQ Response");

    }
    
return message;

}
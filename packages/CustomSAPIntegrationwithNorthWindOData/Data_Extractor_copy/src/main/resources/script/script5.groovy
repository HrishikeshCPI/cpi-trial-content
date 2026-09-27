/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
def Message processData(Message message) {
  //def lastSuccess_queueError = message.getProperty("lastSuccessError")
  //def apiLog = message.getProperty("apiLog")
 // def log = ( message.getProperty("lastSuccessError") ? "Error" : "\n" ) + lastSuccess_queueError + "\n" + apiLog
    def messageLog = messageLogFactory.getMessageLog(message);
    if(messageLog != null)
    {
        if ( message.getProperty("keyId") ) {
        messageLog.addCustomHeaderProperty("Persistent Queue Id", message.getProperty("keyId"));
        }
     /*    if ( message.getProperty("apilog") ) {
            messageLog.addAttachmentAsString("Ariba LOG",message.getProperty("apilog").toString(),"text/xml");
      //  messageLog.addAttachmentAsString("LOG",log,"text/xml");
         } */
         
         if ( message.getProperty("collectedApiReponse") ) {
             messageLog.addAttachmentAsString("Signavio LOG",message.getProperty("collectedApiReponse").toString(),"text/xml");
         }
    }
   // messageLog.setStringProperty("LogMessage", body )
    //messageLog.addAttachmentAsString("LogMessage",body,"text/plain");

  /*  if ( message.getProperty("chunknamelist") ) {
        
        message.getProperty("chunknamelist").each{ key -> 
            def logname = key;
             messageLog.setStringProperty(logname,"Printing chunks As Attachment")
        // messageLog.addAttachmentAsString(logname,message.getProperty(propertyname).toString(),"text/csv");
         messageLog.addAttachmentAsString(logname,message.getProperty(key).toString(),"text/csv");
        }
    } */
    return message;
}
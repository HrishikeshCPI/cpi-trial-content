/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
def Message processData(Message message) {
    //Body
 def map = message.getProperties();
                
                // get an exception java class instance
                def ex = map.get("CamelExceptionCaught");
                def errorMessage;

                if (ex!=null) {
                                
                            if (ex.getClass().getCanonicalName().equals("org.apache.camel.component.ahc.AhcOperationFailedException") ) {                     
                                                // save the http error response as a message attachment 
                                                def messageLog = messageLogFactory.getMessageLog(message);
                                                messageLog.addAttachmentAsString("http.ResponseBody", ex.getResponseBody(), "text/xml");

                                                // copy the http error response to an exchange property
                                                message.setProperty("http.ResponseBody",ex.getResponseBody());

                                                // copy the http error response to the message body
                                                message.setBody(ex.getResponseBody());

                                                // copy the value of http error code (i.e. 500) to a property
                                                message.setProperty("http.StatusCode",ex.getStatusCode());

                                                // copy the value of http error text (i.e. "Internal Server Error") to a property
                                                message.setProperty("http.StatusText",ex.getStatusText());
                                                
                            }
                }
                
                   if ( ex != null ) {
                       
        // Extract validation error message if present
        def match = ex.toString().find(/iFlow failed due to validation error: .*?(?=@|, cause:)/);
        
        if  ( match ) {
         errorMessage = "Run Time Exception" + "\n" + match
         message.setProperty("ResponseCode", 400)
        }
        if  ( match == null ) {
            errorMessage = ex.toString();
        }
        
        // **Fix:** Check if `getResponseBody()` exists before calling
        if (ex.metaClass.respondsTo(ex, "getResponseBody")) {
        if ( !ex.getResponseBody() ) {
        message.setProperty("http.ResponseBody", errorMessage)
        }
        } else {
            // If ex does not have getResponseBody() (e.g., TimeoutException)
            message.setProperty("http.ResponseBody", errorMessage);
            if ( ex.toString().contains("java.util.concurrent.TimeoutException") ) {
            message.setProperty("ResponseCode", 429)
            }
        }
        
        if ( ex.toString().contains("com.google.common.util.concurrent.UncheckedExecutionException") ) {
            message.setProperty("ResponseCode", 429)
            }
    } 
    
    if ( !message.getProperty("ResponseCode") ) {
        message.setProperty("ResponseCode", 400)
    }

                return message;
}
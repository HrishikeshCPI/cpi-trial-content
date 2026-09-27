/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
def Message processData(Message message) {
    def ex = message.getProperty("CamelExceptionCaught");
    def retrylog = message.getProperty("retryLog") ?: null;
    def msg;
   
    
      /*  if ( ex != null ) {
        if ( ex.toString().find(/iFlow failed due to validation error: .*?(?=@|, cause:)/) != null )    {
            msg = msg + "Run Time Exception:" + "\n" + ex.toString().find(/iFlow failed due to validation error: .*?(?=@|, cause:)/)
        } else if  ( ex.toString().find(/iFlow failed due to validation error: .*?(?=@|, cause:)/) == null ) {
            msg = msg + ex
        }
       
    } */
    
     if (ex != null) {
      msg = msg + "exception message: " + ex.getMessage();
        if (ex.getClass().getCanonicalName().equals("org.apache.camel.component.ahc.AhcOperationFailedException")) {
            msg = msg + " status code: " + ex.getStatusCode() + "\nstatus text: " + ex.getStatusText() + "\nresponse body:" + ex.getResponseBody();
        }
    }
    
    if ( retrylog != null ) {
        msg = msg + "Retry Error: " + retrylog
    }
    

     message.setProperty("http.ResponseBody", msg)
    return message;
}

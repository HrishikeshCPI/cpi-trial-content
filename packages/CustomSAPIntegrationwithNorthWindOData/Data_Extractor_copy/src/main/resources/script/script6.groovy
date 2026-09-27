/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
def Message processData(Message message) {
    //Body
      def body = message.getBody();
      def loopcheck;
    def properties = message.getProperties();
    //def header = message.getHeaders();
    def sleep_time = properties.get("PersistentQueryQueueLockSleep").toInteger();
     def retry = properties.get("PersistentQueryQueueLockRetry").toInteger(); 
     def count = properties.get("count");
     count = count + 1;
    if (count <= retry) {
      // def loopcheck = 'true';
       sleep(sleep_time);
    }
       if ( count <= retry ) {
         loopcheck = 'true';
       }
        else {
         loopcheck = 'false';
        }
       
    
       message.setBody(body);
       message.setProperty("count", count);
        message.setProperty("loopcheck", loopcheck);
    return message;
}
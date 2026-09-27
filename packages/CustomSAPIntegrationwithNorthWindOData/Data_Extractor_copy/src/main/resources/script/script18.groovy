/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en-IN/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en-IN/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper;

def Message processData(Message message) {
    //Body
    def Response = new JsonSlurper().parse(message.getBody(java.io.Reader.class))
    def executionId = ( Response.status == 200 ) ? Response.payload.executionId : null
    def properties = message.getProperties();
    if ( executionId != null ){
       def ingestionUrl = properties.get("ingestionStatusURL")  + '/' + executionId + '/status' ;
        message.setProperty("statusURL", ingestionUrl)
    }
    
    sleep(20000)
    return message;
}
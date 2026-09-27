/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en-IN/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en-IN/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper;

def Message processData(Message message) {
    //Body
    def Response = new JsonSlurper().parse(message.getBody(java.io.Reader.class))
    def statusCode = ( Response.status == 200 ) ? Response.payload.status : null
    def statusmessage = Response.payload.message
    def properties = message.getProperties();
    try {
    if ( properties.get("ResponseCode").toInteger() == 200 && ( statusCode == 'REQUEST_VALIDATION_FAILED' || statusCode == 'FILE_CONVERSION_FAILED' || statusCode == 'FILE_UPLOAD_FAILED' || statusCode == 'INTERNAL_SYNCHRONISING_FAILED' ) ){
             message.setHeader("CamelHttpResponseCode", 400 );
         throw new IllegalArgumentException("Signavio Load failed: " + statusmessage.toString() )    
    }
    } catch ( Exception e ) {
         throw new RuntimeException("iFlow failed due to validation error: " + e.message)
    }
    return message;
}
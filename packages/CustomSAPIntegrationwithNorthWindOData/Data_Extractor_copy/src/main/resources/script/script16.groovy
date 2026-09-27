/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

def Message processData(Message message) {
    
/*To set the body, you can use the following method. Refer SCRIPT APIs document for more detail*/
    //message.setBody(body + " Body is modified");
    //Headers
    def resourceJson = new JsonSlurper().parse(message.getBody(java.io.Reader.class))
	message.setProperty("resourceConfigJson",resourceJson)

	def timezone = resourceJson?.config?.find { it.containsKey("timezone") }?.get("timezone")
	if ( !timezone ) {
	    timezone = 'UTC'
	    }

    def timeStamp = Instant.ofEpochMilli(message.getProperty("dataExtractorTimestamp"))
                                .atZone(ZoneId.of(timezone))
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    
                                
	message.setProperty("timeStamp",timeStamp)
    return message;
}
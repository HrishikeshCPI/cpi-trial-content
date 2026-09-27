/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonBuilder;
import java.time.Instant;

def Message processData(Message message) {
//Body
//def body = message.getBody(String);

// Get the current date and time in milliseconds
long dataExtractorTimestamp = Instant.now().toEpochMilli();

// Set the timestamp as a property
message.setProperty("dataExtractorTimestamp", dataExtractorTimestamp);
def initialJson = new JsonBuilder([:]).toString() ;  // Empty map
message.setProperty("globalVarCollection", initialJson);

    return message;
}
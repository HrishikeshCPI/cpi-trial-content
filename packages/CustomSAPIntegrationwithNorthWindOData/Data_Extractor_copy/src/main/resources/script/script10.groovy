/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en-US/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en-US/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import groovy.json.JsonSlurper;
import groovy.json.JsonBuilder;

def Message processData(Message message) {
    //Body
   // def body = message.getBody(String);
    def properties = message.getProperties();
    def apiCallBody  = [:];
    def ViewInitialization = properties.get("ViewInitialization")
    def query_to_process = properties.get("filteredValue");
    //query_to_process and ViewInitialization are message properties (already Strings in memory), not the message body. 
    //message.getBody(java.io.Reader.class) is not applicable here.
    def jsonMap = new JsonSlurper().parseText(query_to_process);
    def url = properties.get("queueURL");
    def resourceJson = properties.get("resourceConfigJson");
    def timezone = resourceJson?.config?.find { it.containsKey("timezone") }?.get("timezone")
    

   if (url.contains("-reporting-job/") && jsonMap.value.queryType == "initial" && jsonMap.value.fromTimestamp < 0 ) {
    // Create the API call body JSON
    apiCallBody["viewTemplateName"] = jsonMap.value.repositoryItem;
  
} else if ( url.contains("-reporting-job/") && ( jsonMap.value.queryType == "initial" || jsonMap.value.queryType == "delta" || jsonMap.value.queryType == "range" ) && jsonMap.value.fromTimestamp > 0 ) {
    //convert millis to date and time format
    def fromDate = Instant.ofEpochMilli(jsonMap.value.fromTimestamp)
                                .atZone(ZoneId.of(timezone))
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
                                
    def toDate = Instant.ofEpochMilli(jsonMap.value.toTimestamp)
                                .atZone(ZoneId.of(timezone))
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
                                
                                
    apiCallBody["viewTemplateName"] = jsonMap.value.repositoryItem;
    apiCallBody["filters"] = [
        "updatedDateFrom": fromDate,
        "updatedDateTo"  : toDate
    ]
}

if ( url.contains("reporting-jobresult/") ) { 
    message.setProperty("apicallmethod" , "GET" );
    if (url.contains("/files/")) { message.setHeader("Accept", "*/*")}
    } 
else { message.setProperty("apicallmethod" , "POST" ); }
if ( !apiCallBody.isEmpty() ) {
def apiCallBodyJson = new JsonBuilder(apiCallBody).toPrettyString();
message.setBody(apiCallBodyJson);
}

    if ( jsonMap.value.queryType == 'initialization' && !jsonMap.value.url.contains("/patch") ) {
        message.setBody(ViewInitialization);
    } else if ( jsonMap.value.queryType == 'initialization' && jsonMap.value.url.contains("/patch") ) {
        try {
        def viewJson = new JsonSlurper().parseText(ViewInitialization);
        if ( viewJson ) {
             viewJson.remove('documentType')
            ViewInitialization = new JsonBuilder(viewJson).toPrettyString();
            message.setBody(ViewInitialization);
        }
        } catch ( Exception e ) {
            throw RuntimeException ("iFlow failed due to validation error: View Body couldn't be determined because of incorrect dataSource/repository item in config file.")
        }
        
    }

  return message;
}
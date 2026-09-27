/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

def convertUTCToTargetMillis(utcTimestamp, targetTimeZone) {
    def formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC)
    def instant = Instant.from(formatter.parse(utcTimestamp))
    
    // Convert UTC Instant to ZonedDateTime in target timezone
    def zonedDateTime = instant.atZone(ZoneId.of(targetTimeZone))

    // Return millis from that ZonedDateTime
    return zonedDateTime.toInstant().toEpochMilli()
}

def Message processData(Message message) {
    //Body
    def body = message.getBody(String);
/*To set the body, you can use the following method. Refer SCRIPT APIs document for more detail*/
    //message.setBody(body + " Body is modified");
    def properties = message.getProperties();
    def resourceJson = properties.get("resourceConfigJson");
    def extractortime = properties.get("dataExtractorTimestamp");
//    def extractorTimestamp = properties.get("extractorTimeStamp");
    def jsonSlurper = new JsonSlurper();
    def jsonArray = jsonSlurper.parseText(body);
    
//    if ( resourceJson ) {
//        def timezone = resourceJson?.config?.find { it.containsKey("timezone") }?.get("timezone")
//        if ( timezone && timezone != 'UTC' ) {
//               message.setProperty("currenttimezone",convertUTCToTargetMillis(extractorTimestamp, timezone) )               
//        }
//    }

   // get inExecution key value pair and set to scheduled with retry 02/09/25
 //  def inExecutionEntry = jsonArray.find { key, value -> value.status == 'inExecution' && value.url.contains('/files/') }?.key
   def inExecutionEntry = jsonArray.find { key, value -> value.status == 'inExecution' }?.key
   if ( inExecutionEntry ) {
       jsonArray[inExecutionEntry]?.retry = (((jsonArray[inExecutionEntry].retry) ?: 0 ) as int ) + 1
       if ( jsonArray[inExecutionEntry]?.retry <= 10 ) {
           jsonArray[inExecutionEntry].status = 'Scheduled'
            jsonArray[inExecutionEntry].info = 'Managing inExecution queue item at ${extractortime} with retry mechanism'
            message.setProperty("inExecutionQueueRetry",inExecutionEntry)
       } else {
            jsonArray[inExecutionEntry].status = 'Failed'
            jsonArray[inExecutionEntry].updatedTimestamp = properties.get("dataExtractorTimestamp")
       }
   }
   
   // get inExecution key value pair and set to scheduled with retry 02/09/25
    
   // def filter_Scheduled = jsonArray.findAll { it.containsKey('status') && it.status == 'Scheduled' };
   def filter_Scheduled = jsonArray.findAll { key, value -> value.status == 'Scheduled' && value.executionTimestamp.toLong() <= properties.get("dataExtractorTimestamp") && value.queryType == 'initialization'};
   
   if ( filter_Scheduled.isEmpty() ) {
       filter_Scheduled = jsonArray.findAll { key, value -> value.status == 'Scheduled' && value.queryType == 'initialization'};
   }
       if ( filter_Scheduled.isEmpty() ) {
// if queue with "inexecution" for /files url, look for the next process queue item without /files url. if no /files url in exection status, it need not apply this filter
      def hasInExecutionWithFiles = jsonArray.values().any { item ->
                        item.status == 'inExecution' && item.url.contains('/files/') }
        if ( hasInExecutionWithFiles ) {
        filter_Scheduled = jsonArray.findAll { key, value -> value.status == 'Scheduled' && !value.url.contains('/files/') && value.executionTimestamp.toLong() <= properties.get("dataExtractorTimestamp") };
       } else {  
           filter_Scheduled = jsonArray.findAll { key, value -> value.status == 'Scheduled' && value.executionTimestamp.toLong() <= properties.get("dataExtractorTimestamp") }
        }
        
   }

     // Find the first JSON object based on earliest timeCreated
    def firstEntry = filter_Scheduled.min { entry -> 
         entry.value.executionTimestamp.toLong();
        }
     def updatedJson = JsonOutput.prettyPrint(JsonOutput.toJson(firstEntry));
     // def payload = updatedJson.collect { JsonOutput.toJson(it) }.join('\n');
     def iteration_count = filter_Scheduled.size();
    if (iteration_count) {
        message.setProperty("loop", 'true');
    }
     message.setProperty("iteration_count", iteration_count);
     if ( firstEntry ) {
     message.setProperty("keyId",firstEntry.key);
     }
     else {
         message.setProperty("keyId", "null");
     }
     // Delete queue items based on retention period
     if ( resourceJson ) {
        def retentionPeriod = resourceJson?.config?.find { it.containsKey("retentionPeriod") }?.get("retentionPeriod")
        if ( retentionPeriod ) {
        def retentionPeriodinmillis = retentionPeriod * 24L * 60 * 60 * 1000
        message.setProperty("retentionPeriodinmillis",retentionPeriodinmillis)
        // Find and remove entries older than 60 days
      /*  jsonObject.keySet().removeAll { key ->
             if ( jsonObject[key].queryType != 'lastSuccess' ) {
             def entry = jsonObject[key]
                def updatedTimestamp = entry.updatedTimestamp
                return (extractortime - updatedTimestamp) > retentionPeriodinmillis
             }
        } */
        message.setProperty("jsonArray",jsonArray)
        jsonArray.keySet().removeAll { key ->
                def entry = jsonArray[key]
                return entry.queryType != 'lastSuccess' &&
                        (entry.status == 'Completed' || entry.status == 'Failed') && 
                        (extractortime - entry.updatedTimestamp) > retentionPeriodinmillis
                    }
                    message.setProperty("queueAfterRetention" , JsonOutput.prettyPrint(JsonOutput.toJson(jsonArray)))
        
       }
    }
     
    // Set the updated JSON as the message body
    message.setBody(updatedJson);

    return message;
}

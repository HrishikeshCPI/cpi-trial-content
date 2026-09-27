/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en/index.html */
//import java.io.ByteArrayInputStream;
import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.UUID;
import java.util.zip.ZipInputStream;
import groovy.json.JsonSlurper;
import groovy.json.JsonBuilder;
import java.util.zip.ZipEntry;
import groovy.json.JsonOutput;

def parseResponseBody(responsebodyIn) {
    if (responsebodyIn instanceof Map) {
        return responsebodyIn // Already a Map, return as is
    } else if (responsebodyIn instanceof String) {
        try {
            def parsed = new JsonSlurper().parseText(responsebodyIn)
            if (parsed instanceof Map) { // Ensure it's a valid JSON object
                return parsed
            } 
        } catch (Exception ignored) { } // Ignore errors and return original string
    } else if (responsebodyIn != null) {
        return responsebodyIn.toString() // Convert non-string objects to string
    }
    return responsebodyIn // Return the original string if not JSON
}
    
def Message processData(Message message) {
    //Body
    //def body = message.getBody(String);
     def apiResponse;
    /*To set the body, you can use the following method. Refer SCRIPT APIs document for more detail*/
    //message.setBody(body + " Body is modified");
    //Headers
    //def jsonObject = new JsonSlurper().parseText(body);
    def jsonObject = new JsonSlurper().parse(message.getBody(java.io.Reader.class));
    def properties = message.getProperties();
    def key_to_update = properties.get("keyId");
    def inExecutionQueue = properties.get("inExecutionQueueRetry");
    def globalvar = properties.get("globalVarCollection");
     def globaljson = new JsonSlurper().parseText(globalvar);
    def extractortime = properties.get("dataExtractorTimestamp");
    def resourceJson = properties.get("resourceConfigJson");
    def response = properties.get("ResponseCode");
    def deltaBuffer = properties.get("deltaBuffer") ? properties.get("deltaBuffer").toInteger() : 0;
    def responseMsg, responsebodyJson
    def responsebody = properties.get("http.ResponseBody") ?: null;
    if  ( responsebody != null) {
     responsebodyJson = parseResponseBody(responsebody);
    // responseMsg = responsebodyJson?.code ?: null
    }
    def apiresp = properties.get("apilog");
    def retryTime = properties.get("retryinmillis").toInteger();
    def postSignavio = properties.get("postSignavio");
    // Check if the value exists in the JSON object
  //  def valueExists = jsonObject.find { key, value ->
  //  value.id == key_to_update } //not used for now
    if ( key_to_update != null ) {
    if ( responsebodyJson != null && jsonObject[key_to_update].queryType == "initialization" && response == 400)  {
        if ( responsebodyJson instanceof Map && responsebodyJson.containsKey('code') ) {
        responseMsg =  responsebodyJson?.code  
        } else {
            responseMsg = null
        }
    }
    // Added on 03/09/2025 for auto processing stuck queue items
    if ( inExecutionQueue == key_to_update && jsonObject[key_to_update].status == "inExecution" && !response ) {
        jsonObject[key_to_update].status == "Scheduled"
        jsonObject[key_to_update].retry = (((jsonObject[key_to_update].retry) ?: 0 ) as int ) + 1
       if ( jsonObject[key_to_update].retry <= message.getProperty("PersistentQueryQueueLockRetry").toInteger() ) {
           jsonObject[key_to_update].status = 'Scheduled'
        //   jsonArray[inExecutionEntry].executionTimestamp = properties.get("dataExtractorTimestamp")
           jsonObject[key_to_update].updatedTimestamp = extractortime
            jsonObject[key_to_update].info = "Managing inExecution queue item at ${extractortime} with retry mechanism"
       } else {
            jsonObject[key_to_update].status = 'Failed'
            jsonObject[key_to_update].updatedTimestamp = extractortime
            jsonObject[key_to_update].failedReason = "Failed due to maximum retries"
       }
    }
        // Added on 03/09/2025 for auto processing stuck queue items
   if (jsonObject.containsKey(key_to_update) && jsonObject[key_to_update].status == "Scheduled") {
    jsonObject[key_to_update].status = "inExecution";
    }
    
 //&& jsonObject[key_to_update].queryType == "initialization"
    if (jsonObject[key_to_update].status == "inExecution" && response == 200  && (jsonObject[key_to_update].api == 'analytics-reporting' || jsonObject[key_to_update].api == 'procurement-reporting' || jsonObject[key_to_update].api == 'sourcing-reporting' )) {
        
        jsonObject[key_to_update].status = "Completed";
        jsonObject[key_to_update].updatedTimestamp = extractortime;
        def lastSuccessDate = jsonObject[key_to_update].dataSource + '_' + jsonObject[key_to_update].api + '_' + jsonObject[key_to_update].repositoryItem + '_' + 'lastSuccessDate';
        message.setProperty("lastSuccessDate", lastSuccessDate);
       
       if ( lastSuccessDate ) {
           // Add new key-value pairs
            globaljson['id'] = lastSuccessDate;
           globaljson['value'] =  0;
       //  resultmap = globaljson.collectEntries{ item ->
         //               [item.id, item] ;
          //  } 
            } 
    }
    if ( jsonObject[key_to_update].status == "inExecution" &&  response == 400 && responseMsg == "viewAlreadyExists" ) { 
        if ( jsonObject[key_to_update].retry < message.getProperty("PersistentQueryQueueLockRetry").toInteger() ) {
        jsonObject[key_to_update].status = "Scheduled";
        jsonObject[key_to_update].retry = jsonObject[key_to_update].retry + 1;
        }
        else if ( jsonObject[key_to_update].retry >= message.getProperty("PersistentQueryQueueLockRetry").toInteger() ) {
            jsonObject[key_to_update].status = "Failed";
             jsonObject[key_to_update].failedReason = "Failed due to maximum retries: ${responsebodyJson}";
        }
        jsonObject[key_to_update].executionTimestamp = extractortime + ( retryTime * jsonObject[key_to_update].retry );
        jsonObject[key_to_update].updatedTimestamp = extractortime;
        def updatedUrl = jsonObject[key_to_update].url.replaceFirst("(viewTemplates/[^?]+)", "\$1/patch")
        jsonObject[key_to_update].url = updatedUrl;
        
    } else if ( jsonObject[key_to_update].status == "inExecution" && ( response == 400 || response == 401 )) { // || response == 500 )) { 
        jsonObject[key_to_update].status = "Failed";
        jsonObject[key_to_update].updatedTimestamp = extractortime;
         jsonObject[key_to_update].failedReason = "${responsebodyJson}" //"Failed due to maximum retries";
    }
    
    if ( jsonObject[key_to_update].status == "inExecution" && ( response == 404 || response == 429 || response == 500 || response == 504 || response == 443 || response == 503 || response == 502 || response == 408 ) ) { 
        if ( jsonObject[key_to_update].retry < message.getProperty("PersistentQueryQueueLockRetry").toInteger() ) {
        jsonObject[key_to_update].status = "Scheduled";
        jsonObject[key_to_update].retry = jsonObject[key_to_update].retry + 1;
        }
        else if ( jsonObject[key_to_update].retry >= message.getProperty("PersistentQueryQueueLockRetry").toInteger() ) {
            jsonObject[key_to_update].status = "Failed";
            jsonObject[key_to_update].failedReason = "Failed due to maximum retries: ${responsebodyJson}";
        }
        
        jsonObject[key_to_update].executionTimestamp = extractortime + ( retryTime * jsonObject[key_to_update].retry );
        jsonObject[key_to_update].updatedTimestamp = extractortime;
    }
    
        // Adding new items to queue
        if ( response == 200 && ( jsonObject[key_to_update].api == 'analytics-reporting' || jsonObject[key_to_update].api == 'procurement-reporting' || jsonObject[key_to_update].api == 'sourcing-reporting' ) ) {
        if (  jsonObject[key_to_update].queryType == 'initial' || jsonObject[key_to_update].queryType == 'delta' || jsonObject[key_to_update].queryType == 'range'   ) {
            if ( jsonObject[key_to_update].url.contains("-reporting-job/")) {
                apiResponse =  new JsonSlurper().parseText(apiresp);
                def jobId = apiResponse.jobId;
                def uuid = key_to_update + '___' + UUID.randomUUID().toString();
                def start = jsonObject[key_to_update].url.split("api/")[0] + "api/";
                def end = "-reporting-job";
                def realm = "?realm=";
                def version = jsonObject[key_to_update].url.find(/\/(v\d+)\//) { match, v -> v }
                def queue_url = start + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(start) + start.length(), jsonObject[key_to_update].url.indexOf(end)).trim() + "-reporting-jobresult/" + version + "/prod/jobs/" + jobId + "?realm=" + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(realm) + realm.length()).trim();
                
                def query_lastSuccess = jsonObject[key_to_update].dataSource + '|' + jsonObject[key_to_update].api + '|' + jsonObject[key_to_update].repositoryItem
                if ( jsonObject[key_to_update].queryType == 'initial' || jsonObject[key_to_update].queryType == 'range' || ( jsonObject[key_to_update].queryType == 'delta' && jsonObject.containsKey(query_lastSuccess) ) ) {
                def newQueue = [ 
                    "id" : uuid,
                    "dataSource" : jsonObject[key_to_update].dataSource,
                    "api" : jsonObject[key_to_update].api,
                    "createdTimestamp" : extractortime,
                    "repositoryItem" : jsonObject[key_to_update].repositoryItem,
                    "url" : queue_url,
                    "queryType" : jsonObject[key_to_update].queryType,
                    "status" : "Scheduled",
                    "executionTimestamp" : extractortime,
                    "retry" : 0,
                    "fromTimestamp" : jsonObject[key_to_update].fromTimestamp,
                    "toTimestamp" : jsonObject[key_to_update].toTimestamp,
                    "updatedTimestamp" : extractortime
                    ]
                    
                    jsonObject[uuid] = newQueue;
                    
                    if ( jsonObject[key_to_update].queryType == 'delta' ) {
                        jsonObject[uuid].deltaPeriod = jsonObject[key_to_update].deltaPeriod
                    }
                }
                
              /*    //Commenting logic to add lastSuccess key as it is handled post Siganavio call in the end of script
                if ( jsonObject[key_to_update].queryType == 'delta' && jsonObject.containsKey(query_lastSuccess) ) {
                    if ( jsonObject.find { it.value.repositoryItem == jsonObject[key_to_update].repositoryItem && it.value.repositoryItem == jsonObject[key_to_update].dataSource && it.value.api == jsonObject[key_to_update].api && it.value.status == 'Scheduled' }) {
                    } else {
                    def executionTime = extractortime + ( jsonObject[key_to_update].deltaPeriod.toInteger() * 3600000 ) + 60000
                    def toDelta = extractortime + ( jsonObject[key_to_update].deltaPeriod.toInteger() * 3600000 )
                   // def toDelta = jsonObject[query_lastSuccess].toTimestamp + ( jsonObject[key_to_update].deltaPeriod.toInteger() * 3600000 )
                    def newQueue = [ 
                    "id" : uuid,
                    "dataSource" : jsonObject[key_to_update].dataSource,
                    "api" : jsonObject[key_to_update].api,
                    "createdTimestamp" : extractortime,
                    "repositoryItem" : jsonObject[key_to_update].repositoryItem,
                    "url" : jsonObject[key_to_update].url,
                    "queryType" : jsonObject[key_to_update].queryType,
                    "status" : "Scheduled",
                    "executionTimestamp" : executionTime,  // if 200, extractore time+ delta period +1 min. if !200(failed) or max retry (failed), extractore time+ delta period +1 min
                    "retry" : 0,
                    "fromTimestamp" : jsonObject[key_to_update].toTimestamp, // if 200, current queue totimestamp. if !200(failed) or max retry (failed), lastSucess Key totimestamp
                    "toTimestamp" : toDelta, // if 200, extractore time+ delta period 
                    "updatedTimestamp" : extractortime,
                    "deltaPeriod" : jsonObject[key_to_update].deltaPeriod
                    ]
                    
                    jsonObject[uuid] = newQueue;
                }
                }
                    
                    //Commenting logic to add lastSuccess key as it is handled post Siganavio call in the end of script
                   if ( jsonObject[key_to_update].queryType == 'initial' ) {
                         query_lastSuccess = jsonObject[key_to_update].dataSource + '|' + jsonObject[key_to_update].api + '|' + jsonObject[key_to_update].repositoryItem
                        if ( !jsonObject.containsKey(query_lastSuccess) ) {
                            def queue = [
                                "dataSource" : jsonObject[key_to_update].dataSource,
                                 "api" : jsonObject[key_to_update].api,
                                 "id" : query_lastSuccess,
                                 "createdTimestamp" : extractortime,
                                 "repositoryItem" : jsonObject[key_to_update].repositoryItem,
                                 "url" : "",
                                 "queryType" : "lastSuccess",
                                 "status" : "Completed",
                                 "executionTimestamp" : -1,
                                 "retry" : 0,
                                 "fromTimestamp" : -1,
                                 "toTimestamp" : jsonObject[key_to_update].toTimestamp,
                                 "updatedTimestamp" : extractortime
                                ]
                                
                                jsonObject[query_lastSuccess] = queue;
                        } else {
                            if ( jsonObject[query_lastSuccess].toTimestamp < jsonObject[key_to_update].toTimestamp ) {
                            jsonObject[query_lastSuccess].updatedTimestamp = extractortime;
                            jsonObject[query_lastSuccess].toTimestamp = jsonObject[key_to_update].toTimestamp;
                            }
                        }
                    } */
                    
                    if ( jsonObject[key_to_update].queryType == 'delta' ) {
                         query_lastSuccess = jsonObject[key_to_update].dataSource + '|' + jsonObject[key_to_update].api + '|' + jsonObject[key_to_update].repositoryItem
                         //try { } catch ( Exception e ) { } 
                        if ( !jsonObject.containsKey(query_lastSuccess) ) {
                            //**************************** Need to fail in this case for delta ******************
                            jsonObject[key_to_update].status = "Failed"
                            message.setProperty("lastSuccessError" ,  "Error: ${query_lastSuccess} doesn't exist in the queue to update for delta")
                        } 
                    }
            }
            
            else if ( jsonObject[key_to_update].url.contains("-reporting-jobresult/") && !jsonObject[key_to_update].url.contains("/files/") ) {
                apiResponse =  new JsonSlurper().parseText(apiresp);
                 if ( !apiResponse.isEmpty() && ( apiResponse.status == 'pending' || apiResponse.status == 'processing' ) && jsonObject[key_to_update].retry < message.getProperty("PersistentQueryQueueLockRetry").toInteger() ) {
                             jsonObject[key_to_update].status = "Scheduled";

                 jsonObject[key_to_update].retry = jsonObject[key_to_update].retry + 1;
                 jsonObject[key_to_update].executionTimestamp = extractortime + ( retryTime * jsonObject[key_to_update].retry );
                 }
                   if ( !apiResponse.isEmpty() && ( apiResponse.status == 'pending' || apiResponse.status == 'processing' ) && jsonObject[key_to_update].retry >= message.getProperty("PersistentQueryQueueLockRetry").toInteger() )    {               
                   if ( jsonObject[key_to_update].retry >= message.getProperty("PersistentQueryQueueLockRetry").toInteger() ) {
                         jsonObject[key_to_update].status = "Failed";
                         jsonObject[key_to_update].updatedTimestamp = extractortime;
                         jsonObject[key_to_update].failedReason = "Failed due to maximum retries";
                    }
                   }
                 if (apiResponse.status == 'completedZeroRecords' ) { 
                     jsonObject[key_to_update].status = "Completed";
                    // adding delta queue items
                    
                    def query_lastSuccess = jsonObject[key_to_update].dataSource + '|' + jsonObject[key_to_update].api + '|' + jsonObject[key_to_update].repositoryItem
                
                if ( jsonObject[key_to_update].queryType == 'delta' && jsonObject.containsKey(query_lastSuccess) ) {
                    if ( jsonObject.values().any { it -> it.repositoryItem == jsonObject[key_to_update].repositoryItem && it.dataSource == jsonObject[key_to_update].dataSource && it.api == jsonObject[key_to_update].api && it.queryType == 'delta' && it.status == 'Scheduled' } ) { 
                    } else {
                       
               if ( jsonObject[key_to_update].deltaPeriod == jsonObject[query_lastSuccess].deltaPeriod ) {   
                  //  } else { 
                            def contentBefore = key_to_update.split("___")[0];
                            uuid = contentBefore + '___' + UUID.randomUUID().toString();
                            def start = jsonObject[key_to_update].url.split("api/")[0] + "api/";
                            def end = "-reporting-jobresult";
                            def realm = "?realm=";
                            def realmValue = ( jsonObject[key_to_update].url.indexOf(realm) != -1 ) ? jsonObject[key_to_update].url.substring((jsonObject[key_to_update].url.indexOf(realm) + realm.length()), (jsonObject[key_to_update].url.indexOf("&", jsonObject[key_to_update].url.indexOf(realm)) != -1) ? jsonObject[key_to_update].url.indexOf("&", jsonObject[key_to_update].url.indexOf(realm)) : jsonObject[key_to_update].url.length()) : null
                            message.setProperty("realmValue", realmValue)
                             def parts = key_to_update?.split('___') ?: []
                             def getJobKey = parts.size() >= 2 ? parts[0..1].join('___') : parts ? parts[0] : null
                             def queue_url;
                            def version = jsonObject[key_to_update].url.find(/\/(v\d+)\//) { match, v -> v }
                         if ( getJobKey != null && jsonObject[getJobKey]?.url?.contains("-reporting-job/") && jsonObject[getJobKey]?.url?.contains("&includeInactive=true") ) {
                            queue_url = start + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(start) + start.length(), jsonObject[key_to_update].url.indexOf(end)).trim() + "-reporting-job/" + version + "/prod/jobs" + "?realm=" + realmValue + "&includeInactive=true";
                        } else if ( jsonObject[parts[0]] != null && jsonObject[parts[0]]?.url?.contains("-reporting-job/") && jsonObject[parts[0]]?.url?.contains("&includeInactive=true") ) {
                        queue_url = start + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(start) + start.length(), jsonObject[key_to_update].url.indexOf(end)).trim() + "-reporting-job/" + version + "/prod/jobs" + "?realm=" + realmValue + "&includeInactive=true"; 
                             } else if ( jsonObject[key_to_update]?.url?.contains("-reporting-job/") && jsonObject[key_to_update]?.url?.contains("&includeInactive=true") ) {
                        queue_url = start + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(start) + start.length(), jsonObject[key_to_update].url.indexOf(end)).trim() + "-reporting-job/" + version + "/prod/jobs" + "?realm=" + realmValue + "&includeInactive=true";
                             }
                             else {
                         queue_url = start + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(start) + start.length(), jsonObject[key_to_update].url.indexOf(end)).trim() + "-reporting-job/" + version + "/prod/jobs" + "?realm=" + realmValue;
                             }
                       // logic for extractor start time  added on 19/01/2026
                     def extractorstarttime
                        if ( getJobKey != null && jsonObject[getJobKey]?.url?.contains("-reporting-job/") ) {
                              extractorstarttime = jsonObject[getJobKey].executionTimestamp 
                        } else if ( jsonObject[parts[0]] != null && jsonObject[parts[0]]?.url?.contains("-reporting-job/") ) {
                             extractorstarttime = jsonObject[parts[0]].executionTimestamp 
                        } else {
                            extractorstarttime = extractortime
                        }                
                    // logic for extractor start time added on 19/01/2026      
                    def executionTime = extractorstarttime + ( jsonObject[key_to_update].deltaPeriod.toInteger() * 3600000 ) + ( 60000 * deltaBuffer ) // changed to extractorstarttime from extractortime on 19/01/2026
                    def toDelta = extractorstarttime + ( jsonObject[key_to_update].deltaPeriod.toInteger() * 3600000 )
                   // def toDelta = jsonObject[query_lastSuccess].toTimestamp + ( jsonObject[key_to_update].deltaPeriod.toInteger() * 3600000 )
                    def newQueue = [ 
                    "id" : uuid,
                    "dataSource" : jsonObject[key_to_update].dataSource,
                    "api" : jsonObject[key_to_update].api,
                    "createdTimestamp" : extractortime,
                    "repositoryItem" : jsonObject[key_to_update].repositoryItem,
                    "url" : queue_url,
                    "queryType" : jsonObject[key_to_update].queryType,
                    "status" : "Scheduled",
                    "executionTimestamp" : executionTime,  // if 200, extractore time+ delta period +1 min. if !200(failed) or max retry (failed), extractore time+ delta period +1 min
                    "retry" : 0,
                    "fromTimestamp" : jsonObject[key_to_update].toTimestamp, // if 200, current queue totimestamp. if !200(failed) or max retry (failed), lastSucess Key totimestamp
                    "toTimestamp" : toDelta, // if 200, extractore time+ delta period 
                    "updatedTimestamp" : extractortime,
                    "deltaPeriod" : jsonObject[query_lastSuccess].deltaPeriod
                    ]
                    
                    jsonObject[uuid] = newQueue;
                }
                    }
                }
                
                   def  filter_Scheduled = jsonObject.findAll { key, value ->
                         ( value.queryType == 'initial' || value.queryType == 'delta' ) && value.dataSource == jsonObject[key_to_update].dataSource && value.api == jsonObject[key_to_update].api && value.repositoryItem == jsonObject[key_to_update].repositoryItem && value.url.contains("-reporting-jobresult/")    // && value.url.contains("/files/") 
                       
                   }
                    def firstEntry = filter_Scheduled ? filter_Scheduled.max { entry -> entry.value.toTimestamp.toLong() } : null
                    
                    if ( firstEntry != null && jsonObject[key_to_update].toTimestamp == firstEntry.value.toTimestamp)   {
                    // check if any queue item is in scheduled status with same toTimeStamp as max time stamp. if so, should not create last Success item
                    def check_scheduled = filter_Scheduled.values().any { items -> items.toTimestamp == jsonObject[key_to_update].toTimestamp && items.status == 'Scheduled' }
                   // message.setProperty("check_scheduled",check_scheduled)
                    if ( !check_scheduled ) {  
                    if ( jsonObject[key_to_update].queryType == 'initial' ) {
                         query_lastSuccess = jsonObject[key_to_update].dataSource + '|' + jsonObject[key_to_update].api + '|' + jsonObject[key_to_update].repositoryItem
                        if ( !jsonObject.containsKey(query_lastSuccess) ) {
                            def queue = [
                                "dataSource" : jsonObject[key_to_update].dataSource,
                                 "api" : jsonObject[key_to_update].api,
                                 "id" : query_lastSuccess,
                                 "createdTimestamp" : extractortime,
                                 "repositoryItem" : jsonObject[key_to_update].repositoryItem,
                                 "url" : "",
                                 "queryType" : "lastSuccess",
                                 "status" : "Completed",
                                 "executionTimestamp" : -1,
                                 "retry" : 0,
                                 "fromTimestamp" : -1,
                                 "toTimestamp" : jsonObject[key_to_update].toTimestamp,
                                 "updatedTimestamp" : extractortime
                                ]
                                
                                jsonObject[query_lastSuccess] = queue;
                        } else {
                            if ( jsonObject[query_lastSuccess].toTimestamp <= jsonObject[key_to_update].toTimestamp ) {
                            jsonObject[query_lastSuccess].updatedTimestamp = extractortime;
                            jsonObject[query_lastSuccess].toTimestamp = jsonObject[key_to_update].toTimestamp;
                            }
                        }
                    } else if ( jsonObject[key_to_update].queryType == 'delta' ) {
                        if ( jsonObject[key_to_update].deltaPeriod == jsonObject[query_lastSuccess].deltaPeriod ) {  
                        if ( jsonObject[query_lastSuccess].toTimestamp <= jsonObject[key_to_update].toTimestamp ) {
                            jsonObject[query_lastSuccess].updatedTimestamp = extractortime;
                            jsonObject[query_lastSuccess].toTimestamp = jsonObject[key_to_update].toTimestamp;
                            }
                        }
                    }
                    } // added for check_scheduled
                    }
                    
                    //adding delta queue items 
                     }
                 if ( !apiResponse.isEmpty() && apiResponse.status == 'completed' ) {
                 def jobId = apiResponse.jobId;
                def start = jsonObject[key_to_update].url.split("jobs/")[0] + "jobs/";
                //def end = "-reporting-job";
                def realm = "?realm=";
                def uuid;
                    for ( int i = 0; i < apiResponse.files.size(); i++ ) {
                     def queue_url = start + jobId + "/files/" + apiResponse.files[i] + "?realm=" + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(realm) + realm.length()).trim();
                        if ( key_to_update.contains("___") ) {
                        def contentBefore = key_to_update.split("___")[0];
                         uuid = contentBefore + '___' + UUID.randomUUID().toString();
                         } 
                         else {
                         uuid = key_to_update + '___' + UUID.randomUUID().toString();
                             }
                         if ( jsonObject[key_to_update].queryType == 'delta' ) {
                            uuid = key_to_update + '___' + UUID.randomUUID().toString();
                             }   // added on 19/2026 for delta file modification to carry job/jobresult uuid
                        def newQueue = [
                            "id" : uuid,
                            "dataSource" : jsonObject[key_to_update].dataSource,
                            "api" : jsonObject[key_to_update].api,
                            "createdTimestamp" : extractortime,
                            "repositoryItem" : jsonObject[key_to_update].repositoryItem,
                            "url" : queue_url,
                            "queryType" : jsonObject[key_to_update].queryType,
                            "status" : "Scheduled",
                            "executionTimestamp" : extractortime,
                            "retry" : 0,
                            "fromTimestamp" : jsonObject[key_to_update].fromTimestamp,
                            "toTimestamp" : jsonObject[key_to_update].toTimestamp,
                            "updatedTimestamp" : extractortime
                            ]
                            
                            jsonObject[uuid] = newQueue;
                            
                            if ( jsonObject[key_to_update].queryType == 'delta' ) {
                            jsonObject[uuid].deltaPeriod = jsonObject[key_to_update].deltaPeriod
                                            }
                          }
                 }
                 if ( !apiResponse.isEmpty() && apiResponse.status == 'completed' && apiResponse.pageToken != null ) {
                //def jobId = apiResponse.jobId;
                def prefixKey = key_to_update.split("___")[0]
                def uuid;
                uuid = prefixKey + '___' + UUID.randomUUID().toString();
                 if ( jsonObject[key_to_update].queryType == 'delta' &&  key_to_update.split('___').size() >= 2 ) {
                    uuid = key_to_update.split("___")[0] + '___' + key_to_update.split("___")[1] + '___' + UUID.randomUUID().toString();
                    }   // added on 19/2026 for delta file modification to carry job/jobresult uuid
                def start = jsonObject[key_to_update].url.split("api/")[0] + "api/";
                def end = "-reporting-jobresult";
                def realm = "?realm=";
                def realmValue = ( jsonObject[key_to_update].url.indexOf(realm) != -1 ) ? jsonObject[key_to_update].url.substring((jsonObject[key_to_update].url.indexOf(realm) + realm.length()), (jsonObject[key_to_update].url.indexOf("&", jsonObject[key_to_update].url.indexOf(realm)) != -1) ? jsonObject[key_to_update].url.indexOf("&", jsonObject[key_to_update].url.indexOf(realm)) : jsonObject[key_to_update].url.length()) : null
                message.setProperty("realmValue", realmValue)
                                    
                def version = jsonObject[key_to_update].url.find(/\/(v\d+)\//) { match, v -> v }
                def queue_url
                if ( jsonObject[key_to_update].url.contains("&includeInactive=true") ) {
                 queue_url = start + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(start) + start.length(), jsonObject[key_to_update].url.indexOf(end)).trim() + "-reporting-job/" + version + "/prod/jobs" + "?realm=" + realmValue + "&includeInactive=true" +"&pageToken=" + apiResponse.pageToken;
                } else {
                    queue_url = start + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(start) + start.length(), jsonObject[key_to_update].url.indexOf(end)).trim() + "-reporting-job/" + version + "/prod/jobs" + "?realm=" + realmValue + "&pageToken=" + apiResponse.pageToken;
                }
                
                def newQueue = [ 
                    "id" : uuid,
                    "dataSource" : jsonObject[key_to_update].dataSource,
                    "api" : jsonObject[key_to_update].api,
                    "createdTimestamp" : extractortime,
                    "repositoryItem" : jsonObject[key_to_update].repositoryItem,
                    "url" : queue_url,
                    "queryType" : jsonObject[key_to_update].queryType,
                    "status" : "Scheduled",
                    "executionTimestamp" : extractortime,
                    "retry" : 0,
                    "fromTimestamp" : jsonObject[key_to_update].fromTimestamp,
                    "toTimestamp" : jsonObject[key_to_update].toTimestamp,
                    "updatedTimestamp" : extractortime
                    ]
                    
                    jsonObject[uuid] = newQueue;
                    
                    if ( jsonObject[key_to_update].queryType == 'delta' ) {
                            jsonObject[uuid].deltaPeriod = jsonObject[key_to_update].deltaPeriod
                                            }
                 }
            }
            
            else if ( jsonObject[key_to_update].url.contains("-reporting-jobresult/") && jsonObject[key_to_update].url.contains("/files/") && postSignavio != 'true') {
                    def zipFileContent = message.getProperty("apilog");
                    // Initialize variables
                   // def txtFileFound = false;
                    def jsonContent = "";
                    
                    try {
                     ZipInputStream zipStream = new ZipInputStream(zipFileContent)
                     ZipEntry entry;

                 // Iterate through each entry in the zip file
                     while ((entry = zipStream.nextEntry) != null) {
                         if (entry.name == "records.txt") { // Look for records.txt
                     //   txtFileFound = true
                         jsonContent = zipStream.text // Read the content of records.txt
                       break
                   }
                   
                }
                    zipStream.close();
                //    message.setProperty("json1", jsonContent);
                } catch (Exception e) {
                    throw new RuntimeException("Error while processing the zip file: ${e.message}")
                    }
                 if ( jsonContent ) {
                      // Parse the JSON content from the text file
                      def jsonSlurper = new JsonSlurper()
                      def jsonList = jsonSlurper.parseText(jsonContent)
    
                      if (jsonList instanceof List) {
                     // Process the list in chunks of 1000
                    def chunkSize = properties.get("chunkSize").toInteger();
                    def chunkIndex = 1;
                        // Function to flatten JSON

                    jsonList.collate(chunkSize).eachWithIndex { chunk, index ->
                    // Log or process each chunk
                     /*  println "Processing chunk ${index + 1}:"
                      println chunk */
                      // Generate a JSON object for each chunk
                     def chunkJson = new JsonBuilder(chunk).toPrettyString();
                      // Store the JSON for the chunk in a property
                      def propertyName = "chunk_${chunkIndex}"
                      message.setProperty(propertyName, chunkJson)
                    // Add chunk size as csv attachment. Commenting as conversion to flat json and csv  are handled in separate groovy   
                /*    def allKeys = new LinkedHashSet();
                    chunk.each { record ->
                          allKeys.addAll(record.keySet())
                            }
                       def csvHeader = allKeys.join(",") + "\n"
                       def  csvName = "chunk_${chunkIndex}" + ".csv"
                      // def csvRow = flattenedJson.values().collect { it != null ? it.toString() : '' }.join(',')
                       def  csvData = new StringBuilder(csvHeader)
                       def csvHeaders = [] // To collect headers
                       def csvRows = []   // To collect rows of data
                         chunk.each { record ->
                             def row = allKeys.collect { key -> 
                             def value = record[key]
                             value != null ? value.toString().replace(",", "&") : "" // Handle nulls and commas
                             value != null ? value.toString().replace("\n", "\\,") : "" 
                             }.join(",")
                         csvData.append(row + "\n") 
                            }   */
                            // Add chunk size as csv attachment  Commenting as conversion to flat json and csv  are handled in separate groovy
                            // Combine header and row into CSV format

                      //      message.setProperty(csvName, csvData);
                    // Add chunk size as attachment
                     // Increment chunk index
                     chunkIndex++
                     }
                     message.setProperty("chunkCount", chunkIndex);
                  } else {
                     throw new RuntimeException("The content in the .txt file is not a valid JSON array.")
                    }
                    } else {
                    throw new RuntimeException("No .txt file found in the zip or the content is empty.")
                    }
            } else if ( jsonObject[key_to_update].url.contains("-reporting-jobresult/") && jsonObject[key_to_update].url.contains("/files/") && postSignavio == 'true') {
                def query_lastSuccess = jsonObject[key_to_update].dataSource + '|' + jsonObject[key_to_update].api + '|' + jsonObject[key_to_update].repositoryItem
                
                if ( jsonObject[key_to_update].queryType == 'delta' && jsonObject.containsKey(query_lastSuccess) ) {
                    if ( jsonObject.values().any { it -> it.repositoryItem == jsonObject[key_to_update].repositoryItem && it.dataSource == jsonObject[key_to_update].dataSource && it.api == jsonObject[key_to_update].api && it.queryType == 'delta' && it.status == 'Scheduled' } ) { 
                    } else {
                       
               if ( jsonObject[key_to_update].deltaPeriod == jsonObject[query_lastSuccess].deltaPeriod ) {   
                  //  } else { 
                            def contentBefore = key_to_update.split("___")[0];
                            uuid = contentBefore + '___' + UUID.randomUUID().toString();
                            def start = jsonObject[key_to_update].url.split("api/")[0] + "api/";
                            def end = "-reporting-jobresult";
                            def realm = "?realm=";
                            def realmValue = ( jsonObject[key_to_update].url.indexOf(realm) != -1 ) ? jsonObject[key_to_update].url.substring((jsonObject[key_to_update].url.indexOf(realm) + realm.length()), (jsonObject[key_to_update].url.indexOf("&", jsonObject[key_to_update].url.indexOf(realm)) != -1) ? jsonObject[key_to_update].url.indexOf("&", jsonObject[key_to_update].url.indexOf(realm)) : jsonObject[key_to_update].url.length()) : null
                            message.setProperty("realmValue", realmValue)
                             def queue_url 
                             def parts = key_to_update?.split('___') ?: []
                             def getJobKey = parts.size() >= 2 ? parts[0..1].join('___') : parts ? parts[0] : null
                            def version = jsonObject[key_to_update].url.find(/\/(v\d+)\//) { match, v -> v }
                            if ( getJobKey != null && jsonObject[getJobKey]?.url?.contains("-reporting-job/") && jsonObject[getJobKey]?.url?.contains("&includeInactive=true") ) {
                        queue_url = start + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(start) + start.length(), jsonObject[key_to_update].url.indexOf(end)).trim() + "-reporting-job/" + version + "/prod/jobs" + "?realm=" + realmValue + "&includeInactive=true";  
                            } else if ( jsonObject[parts[0]] != null && jsonObject[parts[0]]?.url?.contains("-reporting-job/") && jsonObject[parts[0]]?.url?.contains("&includeInactive=true") ) {
                        queue_url = start + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(start) + start.length(), jsonObject[key_to_update].url.indexOf(end)).trim() + "-reporting-job/" + version + "/prod/jobs" + "?realm=" + realmValue + "&includeInactive=true"; 
                             } else if ( jsonObject[key_to_update]?.url?.contains("-reporting-job/") && jsonObject[key_to_update]?.url?.contains("&includeInactive=true") ) {
                               queue_url = start + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(start) + start.length(), jsonObject[key_to_update].url.indexOf(end)).trim() + "-reporting-job/" + version + "/prod/jobs" + "?realm=" + realmValue + "&includeInactive=true";   
                             }
                             else {
                             queue_url = start + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(start) + start.length(), jsonObject[key_to_update].url.indexOf(end)).trim() + "-reporting-job/" + version + "/prod/jobs" + "?realm=" + realmValue;
                            }
                            // logic for extractor start time  added on 19/01/2026
                     def extractorstarttime
                        if ( getJobKey != null && jsonObject[getJobKey]?.url?.contains("-reporting-job/") ) {
                              extractorstarttime = jsonObject[getJobKey].executionTimestamp 
                        } else if ( jsonObject[parts[0]] != null && jsonObject[parts[0]]?.url?.contains("-reporting-job/") ) {
                             extractorstarttime = jsonObject[parts[0]].executionTimestamp 
                        } else {
                            extractorstarttime = extractortime
                        }                
                    // logic for extractor start time added on 19/01/2026
                    def executionTime = extractorstarttime + ( jsonObject[key_to_update].deltaPeriod.toInteger() * 3600000 ) + ( 60000 * deltaBuffer ) //changed to extractorstarttime instead of extractortime on 19/01/2026
                    def toDelta = extractorstarttime + ( jsonObject[key_to_update].deltaPeriod.toInteger() * 3600000 )
                   // def toDelta = jsonObject[query_lastSuccess].toTimestamp + ( jsonObject[key_to_update].deltaPeriod.toInteger() * 3600000 )
                    def newQueue = [ 
                    "id" : uuid,
                    "dataSource" : jsonObject[key_to_update].dataSource,
                    "api" : jsonObject[key_to_update].api,
                    "createdTimestamp" : extractortime,
                    "repositoryItem" : jsonObject[key_to_update].repositoryItem,
                    "url" : queue_url,
                    "queryType" : jsonObject[key_to_update].queryType,
                    "status" : "Scheduled",
                    "executionTimestamp" : executionTime,  // if 200, extractore time+ delta period +1 min. if !200(failed) or max retry (failed), extractore time+ delta period +1 min
                    "retry" : 0,
                    "fromTimestamp" : jsonObject[key_to_update].toTimestamp, // if 200, current queue totimestamp. if !200(failed) or max retry (failed), lastSucess Key totimestamp
                    "toTimestamp" : toDelta, // if 200, extractore time+ delta period 
                    "updatedTimestamp" : extractortime,
                    "deltaPeriod" : jsonObject[query_lastSuccess].deltaPeriod
                    ]
                    
                    jsonObject[uuid] = newQueue;
                }
                    }
                }
                
                   def  filter_Scheduled = jsonObject.findAll { key, value ->
                         ( value.queryType == 'initial' || value.queryType == 'delta' ) && value.dataSource == jsonObject[key_to_update].dataSource && value.api == jsonObject[key_to_update].api && value.repositoryItem == jsonObject[key_to_update].repositoryItem && value.url.contains("-reporting-jobresult/") && value.url.contains("/files/") 
                       
                   }
                    def firstEntry = filter_Scheduled ? filter_Scheduled.max { entry -> entry.value.toTimestamp.toLong() } : null
                    
                    if ( firstEntry != null && jsonObject[key_to_update].toTimestamp == firstEntry.value.toTimestamp)   {
                    // check if any queue item is in scheduled status with same toTimeStamp as max time stamp. if so, should not create last Success item
                    def check_scheduled = filter_Scheduled.values().any { items -> items.toTimestamp == jsonObject[key_to_update].toTimestamp && items.status == 'Scheduled' }
                   // message.setProperty("check_scheduled",check_scheduled)
                    if ( !check_scheduled ) {  
                    if ( jsonObject[key_to_update].queryType == 'initial' ) {
                         query_lastSuccess = jsonObject[key_to_update].dataSource + '|' + jsonObject[key_to_update].api + '|' + jsonObject[key_to_update].repositoryItem
                        if ( !jsonObject.containsKey(query_lastSuccess) ) {
                            def queue = [
                                "dataSource" : jsonObject[key_to_update].dataSource,
                                 "api" : jsonObject[key_to_update].api,
                                 "id" : query_lastSuccess,
                                 "createdTimestamp" : extractortime,
                                 "repositoryItem" : jsonObject[key_to_update].repositoryItem,
                                 "url" : "",
                                 "queryType" : "lastSuccess",
                                 "status" : "Completed",
                                 "executionTimestamp" : -1,
                                 "retry" : 0,
                                 "fromTimestamp" : -1,
                                 "toTimestamp" : jsonObject[key_to_update].toTimestamp,
                                 "updatedTimestamp" : extractortime
                                ]
                                
                                jsonObject[query_lastSuccess] = queue;
                        } else {
                            if ( jsonObject[query_lastSuccess].toTimestamp <= jsonObject[key_to_update].toTimestamp ) {
                            jsonObject[query_lastSuccess].updatedTimestamp = extractortime;
                            jsonObject[query_lastSuccess].toTimestamp = jsonObject[key_to_update].toTimestamp;
                            }
                        }
                    } else if ( jsonObject[key_to_update].queryType == 'delta' ) {
                        if ( jsonObject[key_to_update].deltaPeriod == jsonObject[query_lastSuccess].deltaPeriod ) {  
                        if ( jsonObject[query_lastSuccess].toTimestamp <= jsonObject[key_to_update].toTimestamp ) {
                            jsonObject[query_lastSuccess].updatedTimestamp = extractortime;
                            jsonObject[query_lastSuccess].toTimestamp = jsonObject[key_to_update].toTimestamp;
                            }
                        }
                    }
                    } // added for check_scheduled
            }
            }
        }
        }
     // Adding new items to queue
        if ( jsonObject[key_to_update].status == 'Failed' && ( jsonObject[key_to_update].api == 'analytics-reporting' || jsonObject[key_to_update].api == 'procurement-reporting' || jsonObject[key_to_update].api == 'sourcing-reporting' && jsonObject[key_to_update].queryType == 'delta') ) { //&& postSignavio == 'true' ) {
          def query_lastSuccess = jsonObject[key_to_update].dataSource + '|' + jsonObject[key_to_update].api + '|' + jsonObject[key_to_update].repositoryItem
            //Creating next delta incase queue is completely failed ( either with retry or other failure status code)
            if ( jsonObject[key_to_update].queryType == 'delta' && jsonObject.containsKey(query_lastSuccess) ) {
                   if ( jsonObject.values().any { it -> it.repositoryItem == jsonObject[key_to_update].repositoryItem && it.dataSource == jsonObject[key_to_update].dataSource && it.api == jsonObject[key_to_update].api && it.queryType == 'delta' && it.status == 'Scheduled' } ) {
                   } else {
               if ( jsonObject[key_to_update].deltaPeriod == jsonObject[query_lastSuccess].deltaPeriod ) {   
                   //} else {
                    def contentBefore = key_to_update.split("___")[0];    
                    def uuid = contentBefore + '___' + UUID.randomUUID().toString();
                             def start = jsonObject[key_to_update].url.split("api/")[0] + "api/";
                            def end = "-reporting-"; // "-reporting-jobresult" was replaced with "-reporting-"
                            def realm = "?realm=";
                            def realmValue = ( jsonObject[key_to_update].url.indexOf(realm) != -1 ) ? jsonObject[key_to_update].url.substring((jsonObject[key_to_update].url.indexOf(realm) + realm.length()), (jsonObject[key_to_update].url.indexOf("&", jsonObject[key_to_update].url.indexOf(realm)) != -1) ? jsonObject[key_to_update].url.indexOf("&", jsonObject[key_to_update].url.indexOf(realm)) : jsonObject[key_to_update].url.length()) : null
                            message.setProperty("realmValue", realmValue)
                            def  queue_url; 
                            def parts = key_to_update?.split('___') ?: []
                            def getJobKey = parts.size() >= 2 ? parts[0..1].join('___') : parts ? parts[0] : null
                            def version = jsonObject[key_to_update].url.find(/\/(v\d+)\//) { match, v -> v }
                            if ( getJobKey != null && jsonObject[getJobKey]?.url?.contains("-reporting-job/") && jsonObject[getJobKey]?.url?.contains("&includeInactive=true") ) {
                             queue_url = start + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(start) + start.length(), jsonObject[key_to_update].url.indexOf(end)).trim() + "-reporting-job/" + version + "/prod/jobs" + "?realm=" + realmValue + "&includeInactive=true";   
                            } else if ( jsonObject[parts[0]] != null && jsonObject[parts[0]]?.url?.contains("-reporting-job/") && jsonObject[parts[0]]?.url?.contains("&includeInactive=true") ) {
                             queue_url = start + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(start) + start.length(), jsonObject[key_to_update].url.indexOf(end)).trim() + "-reporting-job/" + version + "/prod/jobs" + "?realm=" + realmValue + "&includeInactive=true";      
                            } else if ( jsonObject[key_to_update]?.url?.contains("-reporting-job/") && jsonObject[key_to_update]?.url?.contains("&includeInactive=true") ) {
                              queue_url = start + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(start) + start.length(), jsonObject[key_to_update].url.indexOf(end)).trim() + "-reporting-job/" + version + "/prod/jobs" + "?realm=" + realmValue + "&includeInactive=true";    
                            } else {
                             queue_url = start + jsonObject[key_to_update].url.substring(jsonObject[key_to_update].url.indexOf(start) + start.length(), jsonObject[key_to_update].url.indexOf(end)).trim() + "-reporting-job/" + version + "/prod/jobs" + "?realm=" + realmValue;
                            }
                    // logic for extractor start time  added on 19/01/2026
                     def extractorstarttime
                    //def getJobKey = parts.size() >= 2 ? parts[0..1].join('___') : parts ? parts[0] : null
                        if ( getJobKey != null && jsonObject[getJobKey]?.url?.contains("-reporting-job/") ) {
                              extractorstarttime = jsonObject[getJobKey].executionTimestamp 
                        } else if ( jsonObject[parts[0]] != null && jsonObject[parts[0]]?.url?.contains("-reporting-job/") ) {
                             extractorstarttime = jsonObject[parts[0]].executionTimestamp 
                        }  else {
                            extractorstarttime = extractortime
                        }            
                    // logic for extractor start time added on 19/01/2026
                    def executionTime = extractorstarttime + ( jsonObject[key_to_update].deltaPeriod.toInteger() * 3600000 ) + ( 60000 * deltaBuffer )
                    def toDelta = extractorstarttime + ( jsonObject[key_to_update].deltaPeriod.toInteger() * 3600000 )
                   // def toDelta = jsonObject[query_lastSuccess].toTimestamp + ( jsonObject[key_to_update].deltaPeriod.toInteger() * 3600000 )
                    def newQueue = [ 
                    "id" : uuid,
                    "dataSource" : jsonObject[key_to_update].dataSource,
                    "api" : jsonObject[key_to_update].api,
                    "createdTimestamp" : extractortime,
                    "repositoryItem" : jsonObject[key_to_update].repositoryItem,
                    "url" : queue_url,
                    "queryType" : jsonObject[key_to_update].queryType,
                    "status" : "Scheduled",
                    "executionTimestamp" : executionTime,  // if 200, extractore time+ delta period +1 min. if !200(failed) or max retry (failed), extractore time+ delta period +1 min
                    "retry" : 0,
                    "fromTimestamp" : jsonObject[query_lastSuccess].toTimestamp, // if 200, current queue totimestamp. if !200(failed) or max retry (failed), lastSucess Key totimestamp
                    "toTimestamp" : toDelta, // if 200, extractore time+ delta period 
                    "updatedTimestamp" : extractortime,
                    "deltaPeriod" : jsonObject[query_lastSuccess].deltaPeriod
                    ]
                    
                    jsonObject[uuid] = newQueue;
                }
                }
                }
        }
                
                // Clean up of queue based on retention period
    if ( resourceJson ) {
        def retentionPeriod = resourceJson?.config?.find { it.containsKey("retentionPeriod") }?.get("retentionPeriod")
        if ( retentionPeriod ) {
        def retentionPeriodinmillis = retentionPeriod * 24L * 60 * 60 * 1000
        // Find and remove entries older than 60 days
      /*  jsonObject.keySet().removeAll { key ->
             if ( jsonObject[key].queryType != 'lastSuccess' ) {
             def entry = jsonObject[key]
                def updatedTimestamp = entry.updatedTimestamp
                return (extractortime - updatedTimestamp) > retentionPeriodinmillis
             }
        } */
        jsonObject.keySet().removeAll { key ->
                def entry = jsonObject[key]
                 return   entry.queryType != 'lastSuccess' && 
                          (entry.status == 'Completed' || entry.status == 'Failed') &&
                          (extractortime - entry.updatedTimestamp) > retentionPeriodinmillis
                    }
        
       }
    }
    
    if ( jsonObject[key_to_update].status == 'Failed' ) {
        message.setProperty("QueueStatus", jsonObject[key_to_update].status )
    }
    
     // Clean up of queue based on retention period   
    def buildjson = new JsonBuilder(globaljson);
    message.setProperty("globalVarCollection", buildjson.toPrettyString());
   def updatedJsonString = JsonOutput.prettyPrint(JsonOutput.toJson(jsonObject));
   message.setBody(updatedJsonString);
    }
    
    
    return message;
}

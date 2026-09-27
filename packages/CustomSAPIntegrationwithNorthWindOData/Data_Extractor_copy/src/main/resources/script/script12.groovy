import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;

def Message processData(Message message) {
    //Body
    //query_to_process is a message property (String), not the message body. message.getBody(java.io.Reader.class) is not applicable.
    // def body = message.getBody();
    // def map = message.getHeaders();
	def property = message.getProperties();
	def query_to_process = property.get("filteredValue");
    def resourceJson = property.get("resourceConfigJson");
//	def messageLog = messageLogFactory.getMessageLog(message);
//	def view;

	 // Parse the JSON string into a Groovy Map
        def jsonMap = new JsonSlurper().parseText(query_to_process)
        
        if (jsonMap && jsonMap.value.containsKey('queryType') && jsonMap.value.queryType == 'initialization') { 
           //def apiType = 'ViewCreation';
            message.setProperty("queryType", jsonMap.value.queryType);
        }
        
        if (jsonMap && jsonMap.value.containsKey('dataSource') && jsonMap.value.dataSource.contains("SAPAriba")) { 
           //def apiType = 'ViewCreation';
            message.setProperty("aribaSpecific", "true" );
            // Fetch the resource item configuration
            try {
             if ( resourceJson && jsonMap.value.queryType == 'initialization' ) {
             def resourceConfig = resourceJson?.resourceItemsConfig?.find { 
          it.dataSource == jsonMap.value.dataSource && it.api == jsonMap.value.api }
               // Fetch the specific resource item
            if ( resourceConfig ) { def resourceItem = resourceConfig?.resourceItems?.get(jsonMap.value.repositoryItem)
                if ( resourceItem ) {
                     def viewContent = resourceItem?.view
                     if ( viewContent instanceof Map ) { // added on 24/06 as part of config file simplification
                     message.setProperty("ViewInitialization",JsonOutput.prettyPrint(JsonOutput.toJson(viewContent))); //view content from resource item
                     } else {
                         def type = resourceJson?.dataSources?.find { it.containsKey(jsonMap.value.dataSource)}?.get(jsonMap.value.dataSource)?.type
                        def resourceCommonconfig = resourceJson?.resourceItemsCommonConfig?.find { it.containsKey(type)}?.get(type)?.get(jsonMap.value.repositoryItem)?.view
                        message.setProperty("ViewInitialization",JsonOutput.prettyPrint(JsonOutput.toJson(resourceCommonconfig)));
                     } // added on 24/06 as part of config file simplification
                }
            }
            
             
        }
        
        if (jsonMap.value.api.contains("analytics") || jsonMap.value.api.contains("procurement") || jsonMap.value.api.contains("sourcing") ) {
            if ( resourceJson ) {
                def credentials = resourceJson?.dataSources?.find { it.containsKey(jsonMap.value.dataSource)}?.get(jsonMap.value.dataSource)?.apis?.get(jsonMap.value.api).credentials
                message.setProperty("oauthCredentials", credentials)
                def apiKey = resourceJson?.dataSources?.find { it.containsKey(jsonMap.value.dataSource)}?.get(jsonMap.value.dataSource)?.apis?.get(jsonMap.value.api).apikey
                message.setProperty("apiKey", apiKey)
            }
        } 
        } catch (Exception e) {
                    throw new RuntimeException("iFlow failed due to validation error: Error reading the Config file while determining credentials or apikey")
                    }
        }
        
        def url = jsonMap.value.url;
      //  def viewCreateBody = JsonOutput.prettyPrint(JsonOutput.toJson(view));
        
        message.setProperty("queueURL",url);
      //  message.setProperty("apiType", apiType);
      //  message.setProperty("viewCreateBody",viewCreateBody);
	return message;
}
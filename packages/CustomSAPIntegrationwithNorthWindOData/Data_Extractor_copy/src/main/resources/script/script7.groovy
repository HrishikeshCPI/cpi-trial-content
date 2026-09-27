import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import java.util.*;
import java.util.Map.Entry;
import com.sap.it.api.ITApiFactory;
import com.sap.it.api.ITApi;
import com.sap.it.api.mapping.ValueMappingApi;
import com.sap.it.spi.ITApiHandler;
import com.sap.xi.mapping.camel.valmap.VMStore;
import com.sap.xi.mapping.camel.valmap.VMValidationException;
import com.sap.xi.mapping.camel.valmap.ValueMappingApiHandler;
import com.sap.it.api.exception.InvalidContextException;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;

def Message processData(Message message) {
	
	 def body = message.getBody();
	def map = message.getHeaders();
	def property = message.getProperties();
	
	def query_to_process = property.get("filteredValue");
	def messageLog = messageLogFactory.getMessageLog(message);
	
	String postURL=null;
	def aribaAddress, view, postURL2;
	def datacenterin = property.get("datacenter");
	
	
	 try{  
	
            def service = ITApiFactory.getApi(ValueMappingApi.class, null);
      
            if( service != null)
            {
             if (datacenterin == 'us'|| datacenterin == 'eu'||datacenterin == 'us3'||datacenterin == 'au'||datacenterin == 'cn'||datacenterin == 'jp'||datacenterin == 'ru'){
			 aribaAddress = service.getMappedValue("Input", "datacenterin", datacenter, "Ouput", "Tokenurl");
		
			 }
			 else {
			 aribaAddress = service.getMappedValue("Input", "datacenterin", "other", "Output", "Tokenurl");
			
			 }
	   		}
      	//	postURL = 'https://api.ariba.com/v2/oauth/token?grant_type=openapi_2lo';
      		postURL = aribaAddress;
      }
      catch(Exception e)
      {
        messageLog.setStringProperty("Exception",e.toString())
        return null;
      }
      
      message.setProperty("postURL",postURL);

	 //addCustomHeader(message,"postURL",postURL);
	 
	 // Parse the JSON string into a Groovy Map
        def jsonMap = new JsonSlurper().parseText(query_to_process)
        
        if (jsonMap && jsonMap.value.containsKey('queryType') && jsonMap.value.queryType == 'initialization') { 
           //def apiType = 'ViewCreation';
            message.setProperty("queryType", jsonMap.value.queryType);
        }
        
        if (jsonMap && jsonMap.value.containsKey('dataSource') && jsonMap.value.dataSource.contains("SAPAriba")) { 
           //def apiType = 'ViewCreation';
            message.setProperty("aribaSpecific", "true" );
        }
        def url = jsonMap.value.url;
        
        if ('ViewCreation'.equals(property.get("apiType"))) { 
        }
        
        def viewCreateBody = JsonOutput.prettyPrint(JsonOutput.toJson(view));
        
        message.setProperty("queueURL",url);
      //  message.setProperty("apiType", apiType);
      //  message.setProperty("viewCreateBody",viewCreateBody);
	return message;
}

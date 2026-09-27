import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import groovy.util.*;
import groovy.xml.*;


def Message processData(Message message) {

  def payload = message.getBody(String.class);
  def messageLog = messageLogFactory.getMessageLog(message);
  

            //get root node 
    		def root = new XmlSlurper().parseText(payload);
    		// get relevant sub node, which expects value as CDATA
    		def node = root.dataXML.Root;
    		
    		// get the XML of the subnode including aall children and pass as string
    		def sw1 = new StringWriter();
    		def xmlutil = new XmlUtil();
     		xmlutil.serialize(node, sw1);
    		String dataXML_s = sw1.toString();
    		// add the CDATA tag
    		dataXML_s = "<![CDATA[" + dataXML_s + "]]>";
    		
    		
    		//now set marker for subnode
    		root.dataXML = "MARKER";
    		
    		// get the updated XML from root level with all children and pass as String
    		def sw2 = new StringWriter();
    		xmlutil.serialize(root, sw2);
    	    String root_s_marker = sw2.toString();

            // concatenate the 2 substrings and add to payload
    	    String root_s_modif = root_s_marker.replace("MARKER", dataXML_s);
    	    payload = root_s_modif;
    	    
    	    // pass back to body 
            message.setBody(payload);
    		
 
            // logging
            def properties = message.getProperties();
            String logger = properties.get("Logger");
    
            if(logger.equals("true"))
                 {
                  messageLog.setStringProperty("payload", payload);
                  messageLog.addAttachmentAsString("Modified_SOAP", payload, "text/plain");
                }
  
return message;

}
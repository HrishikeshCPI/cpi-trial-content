import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import groovy.json.*

def Message processData(Message message) {
    //Body 
    def body = message.getBody(String.class);
    def jsonSlurper = new JsonSlurper()
    def list = jsonSlurper.parseText(body)
    
    // Default datetime
    String defaultTime =  ' 00:00:00';
    
    // Date time format
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   
    // Batch Payload 
    String completePayload = "";
  
    // Generate Random Id to be used in Batch Code
    String batchId = UUID.randomUUID().toString();
  
    // Prepare Batch Code
    String batchCode = "batch_" + batchId; 
   
    list.AppDocDeliveryItems.AppDocDeliveryItem.each
    {   
        
        it.appGUID = (it.get("appGUID").toString()).replaceAll("[- ]", "") //remove the hyphens in from appGUID
        
        it.grossQuantity = handleDoubleQuantity(it.get("grossQuantity").toString());
        
        it.netQuantity = handleDoubleQuantity(it.get("netQuantity").toString());
        
        if(it.storageAgreement == "") 
            {
                it.storageAgreement = null
            }
            
        if(it.storageAgreementItem == "") 
            {
                it.storageAgreementItem = null
            }

        if(it.storageStartDate == "") 
            {
                it.storageStartDate = null
            } 
        else 
            {
                it.storageStartDate =  ('/Date(').concat(((sdf.parse((it.get("storageStartDate").toString()).concat(defaultTime))).getTime()).toString()).concat(')/');
            }
        
        if(it.storageEndDate == "") 
            {
                it.storageEndDate = null
            } 
        else
            {
                it.storageEndDate =  ('/Date(').concat(((sdf.parse((it.get("storageEndDate").toString()).concat(defaultTime))).getTime()).toString()).concat(')/');
            }

        if(it.storageRatesLocked == "") 
            {
                it.storageRatesLocked = null
            }
        
        if(it.storageFreeDays == "") 
            {
                it.storageFreeDays = null
            }
            
        if(it.warehouseReceiptNumber == "") 
            {
                it.warehouseReceiptNumber = null
            }
        
        if(it.warehouseReceiptTypeId == "") 
            {
                it.warehouseReceiptTypeId = null
            }
            
        if(it.warehouseStartDate == "") 
            {
                it.warehouseStartDate = null
            } 
        else
            {
                it.warehouseStartDate =  ('/Date(').concat(((sdf.parse((it.get("warehouseStartDate").toString()).concat(defaultTime))).getTime()).toString()).concat(')/');
            }
        
            
        if(it.warehouseEndDate == "") 
            {
                it.warehouseEndDate = null
            } 
        else 
            {
                it.warehouseEndDate =  ('/Date(').concat(((sdf.parse((it.get("warehouseEndDate").toString()).concat(defaultTime))).getTime()).toString()).concat(')/');
            }
        
        it.warehouseReceiptQuantity = handleDoubleQuantity(it.get("warehouseReceiptQuantity").toString());
        
        it.openWarehouseRecieptQuantity = handleDoubleQuantity(it.get("openWarehouseRecieptQuantity").toString()); 
                   
        if(it.spotIndicatorId == "") {
            it.spotIndicatorId = null
            }
            
        if(it.loadLocationId == "") {
            it.loadLocationId = null
            }
        
        if(it.sourceLocationId == "") {
            it.sourceLocationId = null
            }
        
        if(it.deliveryStartDate == "") {
            it.deliveryStartDate = null
            } 
        else
            {
            it.deliveryStartDate =  ('/Date(').concat(((sdf.parse((it.get("deliveryStartDate").toString()).concat(defaultTime))).getTime()).toString()).concat(')/');
            }
        
            
        if(it.deliveryEndDate == "") {
            it.deliveryEndDate = null
            } else {
            it.deliveryEndDate =  ('/Date(').concat(((sdf.parse((it.get("deliveryEndDate").toString()).concat(defaultTime))).getTime()).toString()).concat(')/');
            }
        
		it.LDCEventKey = (it.get("LDCEventKey").toString()).replaceAll("[- ]", "") //remove the hyphens in from eventKey
		
        if(it.LDCEventDate == "") 
            {
                it.LDCEventDate = null
            } 
            else 
            {
                it.LDCEventDate =  ('/Date(').concat(((sdf.parse((it.get("LDCEventDate").toString()).concat(defaultTime))).getTime()).toString()).concat(')/');
            }
        
            
        if(it.billOfLadingDate == "") 
            {
                it.billOfLadingDate = null
            } 
        else 
            {
                it.billOfLadingDate =  ('/Date(').concat(((sdf.parse((it.get("billOfLadingDate").toString()).concat(defaultTime))).getTime()).toString()).concat(')/');
            }
        
        it.LDCConvertedQuantity = handleDoubleQuantity(it.get("LDCConvertedQuantity").toString()); 
            
        it.LDCUnconvertedQuantity = handleDoubleQuantity(it.get("LDCUnconvertedQuantity").toString()); 
        
        it.overfillQuantity = handleDoubleQuantity(it.get("overfillQuantity").toString()); 
     
        it.underfillQuantity = handleDoubleQuantity(it.get("underfillQuantity").toString()); 
            
            // Remove empty string
            it.tolerancesEvaluations.remove('')

            it.tolerancesEvaluations.each
            {
                
                it.appGUID = (it.get("appGUID").toString()).replaceAll("[- ]", "") //remove the hyphens in from appGUID
                
                if(it.percentageTo == "") 
                    {
                        it.percentageTo = 0.00
                        it.percentageTo.toString()
                    } 
                 else 
                    {
                        it.percentageTo = Double.parseDouble(it.get("percentageTo").toString());
                        it.percentageTo.toString()
                    }
                
                if(it.percentageFrom == "") 
                    {
                        it.percentageFrom = 0.00
                        it.percentageFrom.toString()
                    } 
                 else 
                    {
                        it.percentageFrom = Double.parseDouble(it.get("percentageFrom").toString());
                        it.percentageFrom.toString()  
                    }
            }
            
            // Remove empty string
            it.qualities.remove('')

            it.qualities.each
            {
                it.LDCEventKey = (it.get("LDCEventKey").toString()).replaceAll("[- ]", "") //remove the hyphens in from eventKey
                
                if(it.value == "") 
                    {
                        it.value = 0.00
                        it.value.toString()
                    }
                else
                    {
                        it.value = Double.parseDouble(it.get("value").toString());
                        it.value.toString()
                    }
            }
            
            // Remove empty string
            it.DPQSEvaluations.remove('')

            it.DPQSEvaluations.each
            {
                it.appGUID = (it.get("appGUID").toString()).replaceAll("[- ]", "") //remove the hyphens in from appGUID
                
                if(it.minimumValue == "") 
                    {
                        it.minimumValue = 0.00
                        it.minimumValue.toString()
                    } 
                else 
                    {
        
                        it.minimumValue = Double.parseDouble(it.get("minimumValue").toString());
                        it.minimumValue.toString()
                    }
                
                if(it.maximumValue == "") 
                    {
                        it.maximumValue = 0.00
                        it.maximumValue.toString()
                    } 
                else 
                    {
                        it.maximumValue = Double.parseDouble(it.get("maximumValue").toString());
                        it.maximumValue.toString()
                    }
                
                it.evaluatedQuantity = handleDoubleQuantity(it.get("evaluatedQuantity").toString()); 
                
                it.adjustedQuantity = handleDoubleQuantity(it.get("adjustedQuantity").toString()); 

                // Remove empty string
                it.DPQSUoMConversions.remove('')
					
				it.DPQSUoMConversions.each
				{	
				    it.evaluatedQuantity = handleDoubleQuantity(it.get("evaluatedQuantity").toString(), true); 
                
                    it.adjustedQuantity = handleDoubleQuantity(it.get("adjustedQuantity").toString(), true); 
				}
            }
			
            // Remove empty string
            it.appDocUoMConversions.remove('')

			it.appDocUoMConversions.each
			{	
			    it.LDCConvertedQuantity = handleDoubleQuantity(it.get("LDCConvertedQuantity").toString(), true);
			    
			    it.grossQuantity = handleDoubleQuantity(it.get("grossQuantity").toString(), true);

				it.netQuantity = handleDoubleQuantity(it.get("netQuantity").toString(), true);
				
				it.overfillQuantity = handleDoubleQuantity(it.get("overfillQuantity").toString(), true);
				
				it.underfillQuantity = handleDoubleQuantity(it.get("underfillQuantity").toString(), true);
			}
			
            // Remove empty string
            it.warehouseUoMConversions.remove('')
			
			it.warehouseUoMConversions.each
			{	
				it.warehouseReceiptQuantity = handleDoubleQuantity(it.get("warehouseReceiptQuantity").toString(), true);
				
				it.openWarehouseRecieptQuantity = handleDoubleQuantity(it.get("openWarehouseRecieptQuantity").toString(), true);
			}
        
        
        String changeset = "changeset_0"
        
        // Prepare Payload for containing employee id in current loop pass
		String tempPayload = "--batch_" + batchId + "\r\n"
		tempPayload = tempPayload + "Content-Type: multipart/mixed; boundary=" + changeset + "\r\n" + "\r\n"
		tempPayload = tempPayload + "--" + changeset + "\r\n"
		tempPayload = tempPayload + "Content-Type: application/http " + "\r\n" + "Content-Transfer-Encoding:binary" + "\r\n" + "\r\n"
		tempPayload = tempPayload + "PUT AppDocDeliveryItem(appDocNumber='" + it.appDocNumber.toString() + "',appDocItem='" + it.appDocItem.toString() + "') HTTP/1.1" + "\r\n" + "Content-Type: application/json" + "\r\n" + "\r\n"
		
		def jsonOP = JsonOutput.toJson(it)
		
		tempPayload = tempPayload + jsonOP.toString() + "\r\n"
		tempPayload = tempPayload + "--" + changeset + "--" + "\r\n" + "\r\n"
		
		// Add the above payload in complete payload
		completePayload=completePayload.concat(tempPayload)
    
    }
    
    // Close the full payload
	completePayload = completePayload + "--batch_" + batchId + "--" + "\r\n"
	
	// Prepare Message Body
	message.setBody(completePayload)
	
	// Prepare Boundary Header Parameter to be added in message header
	String boundaryParam = "multipart/mixed; boundary=" + batchCode;
	
	// Prepare Message Header
	message.setHeader('Content-Type', boundaryParam)

    // save the BTP Payload as a message property 
    message.setProperty("BTP_Payload", "Deliveries_Payload: " + completePayload);
	
    return message
}

def handleDoubleQuantity(fieldValue, precision20Required=false)
{
    if(fieldValue == "") 
    {
        fieldValue = null
    }
    else
    {
        fieldValue = Double.parseDouble(fieldValue.toString());
        fieldValue = precision20Required ? String.format("%.20f",fieldValue) : String.format("%.3f",fieldValue);
        fieldValue.toString();
    }
}
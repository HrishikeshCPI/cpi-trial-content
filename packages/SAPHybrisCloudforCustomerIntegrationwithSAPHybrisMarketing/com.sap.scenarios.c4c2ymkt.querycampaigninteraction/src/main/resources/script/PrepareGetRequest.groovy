import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;

def Message processData(Message message) {
	
	//Body 
	def body = message.getBody(String.class);
	def root = new XmlSlurper().parseText(body);
	def BP = root.CampaignQueryRequest.BusinessPartners.BusinessPartnerInternalID[0].text();
	def contact = root.CampaignQueryRequest.BusinessPartners.ContactPersonInternalID[0].text();
	def lang = root.CampaignQueryRequest.LanguageCode.text();
	map = message.getProperties();

	if ( BP ) {
	message.setProperty("InteractionContactID",BP);
	}
		
	else{
		message.setProperty("InteractionContactID", contact);
	}
	
	message.setProperty("sap-language",lang);
	return message;

}


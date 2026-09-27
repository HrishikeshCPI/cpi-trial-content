import com.sap.gateway.ip.core.customdev.util.Message;

import java.util.HashMap;

 

 

def Message processData(Message message) {

  def payload = message.getBody(String.class);

  def messageLog = messageLogFactory.getMessageLog(message)

  messageLog.setStringProperty("payload", payload)

  messageLog.addAttachmentAsString("Get_OAuthResponse_Payload", payload, "application/json");

  return message;

}


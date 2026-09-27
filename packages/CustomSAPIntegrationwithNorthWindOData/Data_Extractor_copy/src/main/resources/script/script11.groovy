/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonOutput;
def Message processData(Message message) {
    //Body
    def body;
    def url = message.getProperty("queueURL");
    def headers = message.getHeaders();
    def response = headers.get("CamelHttpResponseCode")
   // def updatedJson = JsonOutput.prettyPrint(JsonOutput.toJson(body));
    if ( url.contains("reporting-jobresult/") && url.contains("/files/") ) {
        if ( response != 200 ){ 
            body = message.getBody(String);
            message.setProperty("http.ResponseBody",body)
        } else {
          body = message.getBody(InputStream);
        }
           message.setProperty("apilog", body);
    } else if ( url.contains("reporting-jobresult/") && !url.contains("/files/") ) {
        body = message.getBody(String);
          message.setProperty("apilog", body);
    } else {
         body = message.getBody(String);
           message.setProperty("apilog", body);
    }
  
    return message;
}
/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper;
import java.io.StringReader;
def Message processData(Message message) {
    //Body
   // def body = message.getBody(String);
    //   def map = message.getProperties();
	   // schema = map.get("schema");
	    // def json = new JsonSlurper().parseText(schema) // Parse JSON
    def loopCounter = message.getProperty("signavioLoop") as Integer ?: 0  // Get property, default to 0
     
     // get each chunk from message.getProperty("chunknamelist") to call with signavio API.
    def chunkname = message.getProperty("chunknamelist");
    chunkname = chunkname[loopCounter];
    def schemaname = "${chunkname}" + "_Schema"
    message.setProperty("schemaname",schemaname)
    def primarykeysname = "${chunkname}" + "_pk"
    def csvName = "${chunkname}" + "_Name"
    def schema = message.getProperty(schemaname);
    def primarykeys = message.getProperty(primarykeysname);
    def fileName = message.getProperty(csvName) 
    message.setProperty("csvfileName",fileName)
    def apiBody = message.getProperty(chunkname) as String;
   // Handling Order Schema fields based on csv header order && Remove additional headers and values from csv if any field in csv doesn't exist in schema
    //Schema is read from a message property (String), not the message body. message.getBody(java.io.Reader.class) is not applicable here.
    //def schemaJson = new JsonSlurper().parseText(schema)  // Parse JSON 
    def schemaJson = new JsonSlurper().parse(new StringReader(schema))
    def schemaFields = schemaJson.fields.collect { it.name }  // Extract field names
    // Read CSV content from message body
    def csvLines = apiBody.readLines()
    def csvHeader = csvLines[0].split(",").toList()  // Extract header row
    //def validHeaders = csvHeader.findAll { schemaFields.contains(it) }  // Keep only schema fields
    def orderedHeaders = schemaFields.findAll { true } //{ validHeaders.contains(it) } // Reorder based on schema
    // Process CSV rows
 //   def filteredCsv1 = csvLines.collect { line ->
  //       def values = line.split(",", -1).toList()
//        def mappedValues = orderedHeaders.collect { header ->
//           def index = csvHeader.indexOf(header)
            //return index != -1 ? values[index] : "" // Keep order and filter out extra fields
//            return (index != -1 && index < values.size()) ? values[index] : "" // Keep order, avoid null
//        }
//        return mappedValues.join(",")
//    }.join("\n")
    
    // Build new header row from ordered headers (includes missing ones)
    def filteredCsv = orderedHeaders.join(",") + "\n"

    // Process data rows (skip header)
    csvLines.tail().each { line ->
    def values = line.split(",", -1).toList()
    def rowMap = [:]
    csvHeader.eachWithIndex { h, i -> rowMap[h] = values[i] }     // Map original header -> value

    def mappedValues = orderedHeaders.collect { header ->
        return rowMap.containsKey(header) ? rowMap[header] : "" // Fill with "" if missing
    }
    filteredCsv += mappedValues.join(",") + "\n"
    }

    // Handling Order Schema fields based on csv header order && Remove additional headers and values from csv if any field in csv doesn't exist in schema
    def apiBodyAfter = filteredCsv.replaceAll("\n", "\r\n");

       def body = """--cpi\r\nContent-Disposition: form-data; name="schema"\r\n\r\n""" + schema + """\r\n\r\n--cpi\r\n""" + """Content-Disposition: form-data; name="files"; filename="""+ "\"${fileName}\"" + """\r\n\r\n""" + apiBodyAfter +"""\r\n--cpi\r\nContent-Disposition: form-data; name="primaryKeys"\r\n\r\n""" + primarykeys + """\r\n--cpi--"""
       
     message.setBody(body);
     
     if ( loopCounter > 0 ) {
        // def sleeptime = 20000 + ( loopCounter * 10000 )
        def sleeptime = 20000
         sleep(sleeptime.toInteger())
     } else { sleep(10000) }
    loopCounter += 1  // Increment by 1
    message.setProperty("signavioLoop", loopCounter)  // Set the updated value
    return message
}
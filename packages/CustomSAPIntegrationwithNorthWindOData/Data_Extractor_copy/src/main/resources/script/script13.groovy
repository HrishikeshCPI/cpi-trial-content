/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper;
import java.text.SimpleDateFormat;
import groovy.json.JsonOutput;
import java.util.TimeZone;
import java.util.UUID;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import com.sap.it.api.ITApiFactory;
import com.sap.it.api.securestore.SecureStoreService;
import com.sap.it.api.securestore.exception.SecureStoreException

    // Function to flatten JSON recursively
def flattenMap(String parentKey, def data, Map result, String complexObjectItemsSeparator) {
         if (data instanceof Map) { 
        if (data.isEmpty()) {
            // Empty map — preserve the key 
             result[parentKey] = "" 
              } else {
            data.each { k, v -> 
            def safeKey = k.replace('.', complexObjectItemsSeparator)
            def newKey = parentKey ? "${parentKey}${complexObjectItemsSeparator}${safeKey}" : safeKey
            flattenMap(newKey, v, result, complexObjectItemsSeparator) 
                } 
                 } 
        
            }else if (data instanceof List) {
            result[parentKey] = data.isEmpty() ? "" : JsonOutput.toJson(data) // Keep lists as JSON strings
            } else if (data == null) { 
                    // Explicitly handle null values 
                    result[parentKey] = "" 
                    
                } else {
                    // Base case — simple value
                    result[parentKey] = data 
                }
                return result
}

// Renaming logic for field headers
def renameHeaders(headers, headerMappings) {
    if ( headerMappings != null) {
    return  headers.collect { header -> headerMappings.containsKey(header) ? headerMappings[header] : header }
    } else { return headers }
}

// **Rename row keys based on JSON mappings**
def renameHeadersAndKeys(row, mappings) {
    if ( mappings != null) {
    def renamedRow = [:]
    row.each { key, value ->
        def renamedKey = mappings.containsKey(key) ? mappings[key] : key
        renamedRow[renamedKey] = value
    }
    return renamedRow
    } else { return row }
}

def Message processData(Message message) {
    //Body
    def jsonObject = new JsonSlurper().parse(message.getBody(java.io.Reader.class));
    def properties = message.getProperties();
    //def iflow_headers = message.getHeaders();
    def key_to_update = properties.get("keyId");
    def resourceJson = properties.get("resourceConfigJson");
    def timezone = resourceJson?.config?.find { it.containsKey("timezone") }?.get("timezone")
    def chunknames = []
    def fromDate, toDate
  //  def parentQueue = key_to_update.contains('___') ? key_to_update.split("___")[0] : null;

        if (jsonObject[key_to_update]) {
        if ( jsonObject[key_to_update].fromTimestamp > 0 ) {
       fromDate =  Instant.ofEpochMilli(jsonObject[key_to_update].fromTimestamp)
                                .atZone(ZoneId.of(timezone))
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        } else {
            fromDate = "noFrom"
        }
        if (jsonObject[key_to_update].toTimestamp > 0) {
       toDate = Instant.ofEpochMilli(jsonObject[key_to_update].toTimestamp)
                                .atZone(ZoneId.of(timezone))
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        } else {
            toDate = "noTo"
        }
        } else {
            fromDate = "noFrom"
            toDate = "noTo"
        }
    if ( message.getProperty("chunkCount") ) {
    for ( int i = 1 ; i < message.getProperty("chunkCount").toInteger() ; i++) {
     def name = "chunk_${i}";
     def csv = "chunk_${i}" + ".csv"
        def chunk = message.getProperty(name);
        def jsonSlurper = new JsonSlurper();
        def json = jsonSlurper.parseText(chunk);

          def csvHeader = [] // List for CSV Headers
          def csvRows = []   // List for CSV Rows
          def csvData = [:].withDefault { [] } // Stores data for specified vector fields
          def resourceConfig,headerMappings,primaryKeys,extractVectors,includeTenantIdKey,includeTenantId,vectorConfig,signavioSchema,tenantIdColumnName,apiPrimaryKeys,signavioPipeline,signavioCredentials,signavioUrl, signavioStatusURL, csvName_config,complexObjectItemsSeparator
          def headerMappingsv2, primaryKeysv2, includeTenantIdKeyv2,includeTenantIdv2,signavioSchemav2,generateUUIDv2, generateIndexv2, vectorname,tenantIdColumnNamev2,apiPrimaryKeysv2//,csvName_configv2,signavioCredentialsv2
          //def primaryKeys = ["Organization.Organization.OrganizationId", "Suppliers.Suppliers.SupplierId"]  // Define primary keys dynamically
        //  def extractVectors = ["Region.Region", "Commodity.Commodity"]   // Specify which vectors to extract as separate CSVs
        
        if ( resourceJson && key_to_update != null ) {
              resourceConfig = resourceJson?.resourceItemsConfig?.find { 
          it.dataSource == jsonObject[key_to_update].dataSource && it.api == jsonObject[key_to_update].api }
               // Fetch the specific resource item
            if ( resourceConfig ) { def resourceItem = resourceConfig?.resourceItems?.get(jsonObject[key_to_update].repositoryItem)
                if ( resourceItem ) {
                    // added on 24/06 as part of config file simplification
                    if ( resourceItem?.columnMappings && resourceItem?.columnMappings instanceof Map) {
                      headerMappings = resourceItem?.columnMappings
                    } else {
                        def type = resourceJson?.dataSources?.find { it.containsKey(jsonObject[key_to_update].dataSource)}?.get(jsonObject[key_to_update].dataSource)?.type
                        headerMappings = resourceJson?.resourceItemsCommonConfig?.find { it.containsKey(type)}?.get(type)?.get(jsonObject[key_to_update].repositoryItem)?.columnMappings
                    }
                    // added on 24/06 as part of config file simplification
                    if ( resourceItem?.primaryKeys && resourceItem?.primaryKeys instanceof List) {
                      primaryKeys = resourceItem?.primaryKeys
                    } else {
                        def type = resourceJson?.dataSources?.find { it.containsKey(jsonObject[key_to_update].dataSource)}?.get(jsonObject[key_to_update].dataSource)?.type
                        primaryKeys = resourceJson?.resourceItemsCommonConfig?.find { it.containsKey(type)}?.get(type)?.get(jsonObject[key_to_update].repositoryItem)?.primaryKeys
                    }
                      
                      csvName_config = resourceItem?.name
                      if ( csvName_config ) { csvName_config = csvName_config.replaceAll("<from>",fromDate).replaceAll("<to>",toDate) + "_${i}.csv"}
                      if ( primaryKeys ) {
                         // apiPrimaryKeys = primaryKeys.join(",")
                          def separator = resourceJson?.config?.find { it.containsKey("complexObjectItemsSeparator") }?.get("complexObjectItemsSeparator")
                        if ( complexObjectItemsSeparator == null ) {
                          complexObjectItemsSeparator = "_";
                                 }
                          def primaryKeys_sep = primaryKeys.collect { pk -> pk.replaceAll("\\.", separator) }
                          apiPrimaryKeys = renameHeaders(primaryKeys_sep,headerMappings)
                          apiPrimaryKeys = apiPrimaryKeys.join(",")
                      }
                      includeTenantIdKey = resourceItem?.containsKey("includeTenantId")
                      if ( includeTenantIdKey ) {
                          includeTenantId = resourceItem?.includeTenantId
                      } else {
                          includeTenantId = resourceJson?.config?.find { it.containsKey("includeTenantId") }?.get("includeTenantId")
                      }
                      if ( includeTenantId == true ) {
                         includeTenantId = resourceJson?.config?.find { it.includeTenantId == true }?.includeTenantId
                           if ( resourceItem?.tenantIdColumnName ) {
                              tenantIdColumnName = resourceItem?.tenantIdColumnName
                          } else {
                              tenantIdColumnName = resourceJson?.config?.find { it.includeTenantId == true }?.tenantIdColumnName
                               message.setProperty("tenantIdColumnName",tenantIdColumnName)
                          }
                      }
                      complexObjectItemsSeparator = resourceJson?.config?.find { it.containsKey("complexObjectItemsSeparator") }?.get("complexObjectItemsSeparator")
                      if ( complexObjectItemsSeparator == null ) {
                          complexObjectItemsSeparator = "_";
                      }
                     //  timezone = resourceJson?.config?.find { it.containsKey("timezone") }?.get("timezone")
                     // added on 24/06 as part of config file simplification
                     if ( resourceItem?.vectors && resourceItem?.vectors instanceof List) {
                      vectorConfig = resourceItem?.vectors
                      extractVectors = resourceItem?.vectors.collect { it.vector }?.flatten()
                    } else { 
                        def type = resourceJson?.dataSources?.find { it.containsKey(jsonObject[key_to_update].dataSource)}?.get(jsonObject[key_to_update].dataSource)?.type
                        vectorConfig = resourceJson?.resourceItemsCommonConfig?.find { it.containsKey(type)}?.get(type)?.get(jsonObject[key_to_update].repositoryItem)?.vectors
                        extractVectors = resourceJson?.resourceItemsCommonConfig?.find { it.containsKey(type)}?.get(type)?.get(jsonObject[key_to_update].repositoryItem)?.vectors.collect { it.vector }?.flatten()
                    }
                      
                      signavioSchema = resourceItem?.extensions?.find { it.type == "Signavio" }?.schema
                      if ( signavioSchema == null || !(signavioSchema instanceof Map) ) {
                         def type = resourceJson?.dataSources?.find { it.containsKey(jsonObject[key_to_update].dataSource)}?.get(jsonObject[key_to_update].dataSource)?.type
                        signavioSchema = resourceJson?.resourceItemsCommonConfig?.find { it.containsKey(type)}?.get(type)?.get(jsonObject[key_to_update].repositoryItem)?.extensions?.get("schema")
                            }
                    def dataTargetv1 = resourceItem?.extensions?.find { it.type == 'Signavio'}?.dataTarget // added for multiple data Target logic 30/06 
                     signavioPipeline = resourceItem?.extensions?.find { it.type == "Signavio" }?.pipeline 
                     if ( signavioPipeline == 'procurement' || signavioPipeline == 'sourcing' ) {
                        def signavioPipeline_temp = resourceJson?.pipelineMappings?.find { it.containsKey(signavioPipeline) }?.get(signavioPipeline)?.get(jsonObject[key_to_update].dataSource)
                        signavioPipeline = signavioPipeline_temp ?: signavioPipeline
                     }
                      if ( dataTargetv1 == 'All' || dataTargetv1 == null ) { // added for multiple data Target logic 30/06 
                      resourceJson?.dataTargets?.each { target ->
                                                target.each { key, value ->
                                                    if (key == "SAPSignavio_editor" && value.type == "SAP Signavio") {  // Check type
                                                    signavioUrl = value.baseURL
                                                    signavioStatusURL = value.baseURL.replaceAll('/data$', '')
                                                        value.apis.ingestion.pipelines.each { pipeline ->
                                                      if (pipeline.name == signavioPipeline) { // Check name
                                                      signavioCredentials = pipeline.credentials
                                                              }
                                                            }
                                                          }
                                                         } 
                                                        }
                                                       }
                                                       
                        
                        if ( dataTargetv1 != 'All' && dataTargetv1 != null ) { // added for multiple data Target logic 30/06
                          def Target =  resourceJson?.dataTargets?.find { it.containsKey(dataTargetv1) }?.get(dataTargetv1)
                          message.setProperty("Target", Target)
                          message.setProperty("signavioUrl", Target?.baseURL)
                            signavioUrl = Target?.baseURL
                            signavioStatusURL = Target?.baseURL?.replaceAll('/data$', '')
                                Target?.apis?.ingestion?.pipelines?.each { pipeline ->
                                  if (pipeline.name == signavioPipeline) { // Check name
                                         signavioCredentials = pipeline.credentials
                                    }
                                } 
                        }                               
                                                       
                }
            }
             
        } 
          
              json.each { record ->
        def baseRecord = extractNonPrimaryFields(record, primaryKeys, extractVectors, complexObjectItemsSeparator) // Extract non-primary fields
       
         // Rename "IsTestProject" to "TestProject"
      /*  if (baseRecord.containsKey("IsTestProject")) {
            baseRecord["TestProject"] = baseRecord.IsTestProject;
            baseRecord.remove("IsTestProject");
        } */
         message.setProperty("baseRecord", baseRecord);
        // Extract primary key values dynamically
        def primaryKeyValues = primaryKeys.collect { key -> extractValues(record, key, complexObjectItemsSeparator) }
        
        // Generate CSV rows based on primary key combinations
        def combinations = cartesianProduct(primaryKeyValues)
        def processedTopLevelVectors = []
        combinations.each { keys ->
            def row = baseRecord.clone() // Copy base fields

           primaryKeys.eachWithIndex { key, index -> row[key.replaceAll("\\.", complexObjectItemsSeparator)] = sanitize(keys[index]) 
          //  primaryKeys.eachWithIndex { key, index -> row[key] = sanitize(keys[index]) 
                // Dynamically extract related fields from the same nested structure
                def extraFields = extractSiblingFields(record, key, keys[index], complexObjectItemsSeparator)
                message.setProperty("extraFields", extraFields);
                extraFields.each { k, v -> row[k] = sanitize(v) }
            }
            if ( tenantIdColumnName ) {
                row[tenantIdColumnName] = jsonObject[key_to_update].dataSource.split("_")[1]
               
            }
            message.setProperty("row", row)
            message.setProperty("headerMappings",headerMappings)
            csvRows << renameHeadersAndKeys(row, headerMappings)
            csvHeader.addAll(row.keySet()) // Collect headers dynamically
            
            // Handle only specified vector fields dynamically
            extractVectors.each { vectorField ->
            //resource vectorConfig
                if ( vectorConfig ) {
                    def vectorresdata = vectorConfig?.find { vectorField in it.vector }
                    message.setProperty("vectorresdata", vectorresdata)
                    if ( vectorresdata ) {
                        def vectorresourceItem = vectorresdata?.resourceItem
                         vectorname = vectorresdata?.name
                          message.setProperty("${vectorField}_vfilename" ,vectorname)
                       //  if ( vectorConfig?.any {it.containsKey("generateUUID")}){
                             generateUUIDv2 = vectorresdata?.generateUUID
                             generateIndexv2 =  vectorresdata?.generateIndex // added on 16/12/25 as part of index functionalty
                         //}
                        if ( vectorresourceItem ) {
                            def resourceItemv2 = resourceConfig?.resourceItems?.get(vectorresourceItem)
                            if ( resourceItemv2?.type == 'vector' ) {
                               // headerMappingsv2 = resourceItemv2?.columnMappings
                               // primaryKeysv2 = resourceItemv2?.primaryKeys
                     // added on 24/06 as part of config file simplification
                    if ( resourceItemv2?.columnMappings && resourceItemv2?.columnMappings instanceof Map) {
                       headerMappingsv2 = resourceItemv2?.columnMappings
                    } else {
                        def type = resourceJson?.dataSources?.find { it.containsKey(jsonObject[key_to_update].dataSource)}?.get(jsonObject[key_to_update].dataSource)?.type
                        headerMappingsv2 = resourceJson?.resourceItemsCommonConfig?.find { it.containsKey(type)}?.get(type)?.get(vectorresourceItem)?.columnMappings
                    }
                    // added on 24/06 as part of config file simplification
                    if ( resourceItemv2?.primaryKeys && resourceItemv2?.primaryKeys instanceof List) {
                      primaryKeysv2 = resourceItemv2?.primaryKeys
                    } else {
                        def type = resourceJson?.dataSources?.find { it.containsKey(jsonObject[key_to_update].dataSource)}?.get(jsonObject[key_to_update].dataSource)?.type
                        primaryKeysv2 = resourceJson?.resourceItemsCommonConfig?.find { it.containsKey(type)}?.get(type)?.get(vectorresourceItem)?.primaryKeys
                    }
                    
                                if ( primaryKeysv2 ) { 
                                  //  apiPrimaryKeysv2 = primaryKeysv2.join(",") 
                                    def primaryKeysv2_sep = primaryKeysv2.collect { pk -> pk.replaceAll("\\.", complexObjectItemsSeparator) }
                                  apiPrimaryKeysv2 = renameHeaders(primaryKeysv2_sep,headerMappingsv2)
                                apiPrimaryKeysv2 = apiPrimaryKeysv2.join(",")
                                message.setProperty("${vectorField}_pk" ,apiPrimaryKeysv2)
                                }
                              // includeTenantIdv2 = resourceItemv2?.includeTenantId ?: true
                              includeTenantIdKeyv2 = resourceItemv2?.containsKey("includeTenantId")
                              if ( includeTenantIdKeyv2 ) {
                                  includeTenantIdv2 = resourceItemv2?.includeTenantId
                              } else {
                                 includeTenantIdv2 = resourceJson?.config?.find { it.containsKey("includeTenantId") }?.get("includeTenantId")
                                 }
                        signavioSchemav2 = resourceItemv2?.extensions?.find { it.type == "Signavio" }?.schema
                        if ( signavioSchemav2 == null || !(signavioSchemav2 instanceof Map) ) {
                             def type = resourceJson?.dataSources?.find { it.containsKey(jsonObject[key_to_update].dataSource)}?.get(jsonObject[key_to_update].dataSource)?.type
                            signavioSchemav2 = resourceJson?.resourceItemsCommonConfig?.find { it.containsKey(type)}?.get(type)?.get(vectorresourceItem)?.extensions?.get("schema")
                            }
                            if ( signavioSchemav2 ) { message.setProperty("${vectorField}_Schema" ,JsonOutput.toJson(signavioSchemav2)) }
                               if (  includeTenantIdv2 == true ) {
                              includeTenantIdv2 = resourceJson?.config?.find { it.includeTenantId == true}?.includeTenantId
                           if ( resourceItemv2?.tenantIdColumnName ) {
                              tenantIdColumnNamev2 = resourceItemv2?.tenantIdColumnName
                          } else {
                              tenantIdColumnNamev2 = resourceJson?.config?.find { it.includeTenantId == true }?.tenantIdColumnName
                          }
                      }
                            }
                        }
                    }
                }
            //resource vectorConfig
            
            //Start - added on 09/11/25 for handling flattening of vector csv and also handle duplicate lines if vector is outside main csv  
            def vectorItems = []
             def parts = vectorField.split("\\.")
              def vectorRoot = parts[0]       // e.g. "LineItems"
            def vectorChildParts = (parts.length > 1) ? parts[1..-1] : []  // remaining path segments, possibly empty
            // If vectorRoot matches a primaryKey root (e.g. LineItems.NumberOnReq),
            // find the parent primaryKey that refers to that root
             def parentPrimaryIndex = primaryKeys.findIndexOf { it.startsWith(vectorRoot + ".") }
            // Case 1: Vector field is inside LineItems (or any array within primary key hierarchy)
            if (parentPrimaryIndex >= 0) {
        // Example: parentPrimary = "LineItems.NumberOnReq"
        def parentPrimary = primaryKeys[parentPrimaryIndex]
        def parentField = parentPrimary.split("\\.")[1] // e.g. "NumberOnReq"

        def lineItems = record[vectorRoot]
        if (lineItems instanceof List) {
            // current combination value for that parent primary key
            def currentValue = keys[parentPrimaryIndex]

            // find the specific parent item that matches currentValue
            def currentLine = lineItems.find { li ->
                // safe access in case li[parentField] is nested or missing
                def v = li[parentField]
                v != null && v.toString() == (currentValue == null ? null : currentValue.toString())
            }

            if (currentLine != null) {
                // traverse vectorChildParts on currentLine to get the child array/value (supports nested child paths)
                def target = currentLine
                vectorChildParts.each { seg ->
                    if (target instanceof Map) target = target[seg]
                    else target = null
                }
                if (target instanceof List) {
                    vectorItems = target
                } else if (target != null) {
                    // if the child exists but is not a list, wrap single object in list for uniform processing
                    vectorItems = [target]
                            }
                        }
                    }
                } 
            // Case 2: Vector field is top-level (like OrderConfirmations)
            else {
                if (processedTopLevelVectors.contains(vectorField)) {
            return // continue to next vectorField
             }
                vectorItems = extractValues(record, vectorField, complexObjectItemsSeparator)  //"${vectorField}.*") [{"RegionId":"EU"},{"RegionId":"US"}]
            }
            
            //Start Fix added on 01/07/2026 for Mulit level vector extraction 
            // Remove dummy/blank vector items
               if (vectorItems instanceof List) {
                 vectorItems = vectorItems.findAll { item ->
                item instanceof Map
                }
                }
            //End fix added on 01/07/2026 for Multii level vector extraction 
            
            // ---- continue only if vectorItems exist ----
            if (vectorItems && vectorItems instanceof List && !vectorItems.isEmpty()) {
                def flatVectorRows = []
                vectorItems.eachWithIndex { item, vidx ->
                //Start Fix added on 01/07/2026 for Multi level vector extraction
                 // To prevent creation of fake vector rows when a record does not contain the vector.
                        if (!(item instanceof Map)) {
                          return
                       }
                //End Fix added on 01/07/2026 for Multi level vector extraction
                    def rowVector = [:]
                    // Attach current primary key combination (e.g. OrderID + LineItems_NumberOnReq) //to remove LineItems_NumberOnReq value incase of toplevel vectorItems like OC
                    primaryKeys.eachWithIndex { key, index ->
                    if (parentPrimaryIndex < 0 && key.contains(".")) {
                        rowVector[key.replaceAll("\\.", complexObjectItemsSeparator)] = ""
                    } else {
                        rowVector[key.replaceAll("\\.", complexObjectItemsSeparator)] = sanitize(keys[index])
                    }
                    }
                //Start Fix added on 01/07/2026 for Multilevel Vector extraction 
                // Parent keys are now derived from vector primaryKeys itself
                    populateParentKeysFromVectorPrimaryKeys(
                    rowVector,
                    record,
                    vectorField,
                    item,
                    primaryKeysv2,
                    complexObjectItemsSeparator
                    )
                 //End Fix added on 01/07/2026 for Multilevel Vector extraction
                   /* fieldHeaders.each { field ->
                    rowVector["${vectorField.split("\\.")[0].replaceAll("\\.", complexObjectItemsSeparator)}${complexObjectItemsSeparator}${field}"] = sanitize(item[field] ?: "")
                    } */
        
                    // Flatten nested maps/lists inside this vector item
                    def flatItem = [:]
                    flattenMap(vectorField, item, flatItem, complexObjectItemsSeparator)
        
                    // Merge flattened vector data
                    flatItem.each { k, v ->
                        rowVector[k] = sanitize(v)
                    }
                     if ( tenantIdColumnNamev2 ) {
                    rowVector[tenantIdColumnNamev2] = jsonObject[key_to_update].dataSource.split("_")[1]
                    }
                    if ( generateUUIDv2 ) {
                      rowVector[generateUUIDv2] = UUID.randomUUID().toString();   // Assign a unique UUID
                    }
                    if ( generateIndexv2 != null ) {
                        rowVector[generateIndexv2] = vidx; // assign index for each vector row item
                    }
        
                    flatVectorRows << renameHeadersAndKeys(rowVector, headerMappingsv2)
                }
                // ---- Merge into csvData and generate CSV ----
                if (!flatVectorRows.isEmpty()) {
                    if (!csvData.containsKey(vectorField)) {
                        csvData[vectorField] = []
                    }
                    csvData[vectorField].addAll(flatVectorRows)
                    if (parentPrimaryIndex < 0) {
                       processedTopLevelVectors << vectorField
                     }
                }
            }
            //End - added on 09/11/25 for handling flattening of vector csv and also handle duplicate lines if vector is outside main csv
            
            //    def vectorItems = extractValues(record, vectorField,complexObjectItemsSeparator ) //"${vectorField}.*") [{"RegionId":"EU"},{"RegionId":"US"}]
            //    message.setProperty("vectorItems",JsonOutput.toJson(vectorItems));
            //    def fieldHeaders = detectFieldNames(vectorItems)
              //Commented on 09/11 for new vector logic 
              /*   vectorItems.each { item ->  def rowVector = [:]
                  //  primaryKeys.eachWithIndex { key, index -> rowVector[key.replaceAll("\\.", "_")] = sanitize(keys[index]) }
                    primaryKeys.eachWithIndex { key, index -> rowVector[key.replaceAll("\\.", complexObjectItemsSeparator)] = sanitize(keys[index]) }
                    fieldHeaders.each { field ->
                    rowVector["${vectorField.split("\\.")[0].replaceAll("\\.", complexObjectItemsSeparator)}${complexObjectItemsSeparator}${field}"] = sanitize(item[field] ?: "")
                    }
                    if ( tenantIdColumnNamev2 ) {
                    rowVector[tenantIdColumnNamev2] = jsonObject[key_to_update].dataSource.split("_")[1]
                    }
                    if ( generateUUIDv2 ) {
                      rowVector[generateUUIDv2] = UUID.randomUUID().toString();   // Assign a unique UUID
                    }
                    csvData[vectorField] << renameHeadersAndKeys(rowVector, headerMappingsv2)
                } */ 
                //Commented on 09/11 for new vector logic 
            } 
        }
    }

    csvHeader = renameHeaders(csvHeader.unique().sort(), headerMappings) // Ensure unique and sorted headers
    def csvString = csvHeader.join(",").replaceAll("\\.", complexObjectItemsSeparator) + "\n" // CSV Header Row //Added replace on 09/11 to handle fields with '.'

   /* csvRows.each { row ->
        def line = csvHeader.collect { sanitize(row[it] ?: "") }.join(",") // Ensure column alignment
        csvString += line + "\n"
    } */
    csvRows.each { row ->
    def line = csvHeader.collect { h ->
        // keep original raw value so we can decide quoting only for strings
        def raw = row.containsKey(h) ? row[h] : null   // do NOT coerce to "" here
        def val = sanitize(raw)
        // convert to string at output stage, but preserve null -> empty string for CSV
        def sval = (val == null) ? "" : val.toString()
        // only quote when original raw was a String and contains special CSV chars
        if (raw instanceof String && (sval =~ /[",\n\r]/)) {
            return "\"${sval}\""
        }
        return sval
    }.join(",")
    csvString += line + "\n"
}
    
    // Generate CSVs for each specified vector field dynamically
    extractVectors.each { vectorField ->
        if (csvData[vectorField]) {
            def headers = csvData[vectorField].collectMany { it.keySet() }.unique().sort()
            headers = renameHeaders(headers, headerMappingsv2)
             // 🚀 Split vector data into chunks of 1000 rows
            def vectorData = csvData[vectorField]
            def chunkSize = properties.get("chunkSize").toInteger();
           // def numChunks = (vectorData.size() / chunkSize).ceil() // Calculate number of chunks
            def numChunks = Math.ceil(vectorData.size() / (double) chunkSize) as int
            message.setProperty("vectorChunkCount", numChunks + 1 );
            (0..<numChunks).each { chunkIndex ->
            def start = chunkIndex * chunkSize
            def end = Math.min(start + chunkSize, vectorData.size())
            def chunk2 = vectorData[start..<end] // Extract chunk
            
            def csvStringVector = generateCsv(headers, chunk2, complexObjectItemsSeparator)
            
            // Store each chunk separately in message properties
            def chunkname = vectorField.replaceAll('\\.', '_') + "_Part_" + ( chunkIndex + 1 ) + ".csv"
            chunknames << chunkname
            def vfilename_name = "${vectorField}" + "_vfilename"
            def vfilename_val = message.getProperty(vfilename_name)
            if ( vfilename_val ) { 
                def vectornamev2 = vfilename_val.replaceAll("<from>",fromDate).replaceAll("<to>",toDate) + "_" + "${chunkIndex + 1}" + ".csv"
                message.setProperty("${chunkname}_Name",vectornamev2)
            }
            message.setProperty("${chunkname}", csvStringVector)
            def vschema_name = "${vectorField}" + "_Schema"
            def vschema_val = message.getProperty(vschema_name)
            message.setProperty("${chunkname}_Schema", vschema_val)
            def vpk_name = "${vectorField}" + "_pk"
            def vpk_val = message.getProperty(vpk_name)
             message.setProperty("${chunkname}_pk", vpk_val)
             
            //message.setProperty("${vectorField.replaceAll('\\.', '_')}_Part_${chunkIndex + 1}.csv_creds" ,signavioCredentialsv2)
            }
            //message.setProperty("chunknamelist", chunknames)
                  
           // def csvStringVector = generateCsv(headers, csvData[vectorField])
            //message.setProperty("${vectorField.replaceAll('\\.', '_')}.csv", csvStringVector)
           // message.setProperty("vectorbeforecsv", csvData[vectorField])
           // message.setBody(csvStringVector)
        }
    }
   //message.setBody(csvString) // Set CSV output as the new body
   message.setProperty(csv, csvString) // Set each chunk with primary fields as chunk1.csv etc.,
   message.setProperty("${csv}_Name", csvName_config )
   message.setProperty("${csv}_Schema", JsonOutput.toJson(signavioSchema))
   message.setProperty("${csv}_pk" ,apiPrimaryKeys)
   message.setProperty("signavioCredentials",signavioCredentials)
   message.setProperty("ingestionUrl",signavioUrl)
   message.setProperty("ingestionStatusURL",signavioStatusURL)
   chunknames << csv

  ///This is to flatten the whole json. Commenting it as it is not the desired structure. 

    // Process each JSON object in the array
   /* jsonArray.each { record ->
        def flatRecord = flattenJson(record, null)
        flatRecord.each { key, value ->
            if ( value instanceof String ) {
                flatRecord[key] = value.replaceAll(",", "&") // Escape commas
                                       .replaceAll("\n", " ")   // Replace newlines with a space
                                         .replaceAll("\r", "")    // Remove carriage returns if present
            }
        }
        csvRows << flatRecord
        // Collect unique headers dynamically
        flatRecord.keySet().each { key ->
            if (!csvHeader.contains(key)) {
                csvHeader << key
            }
        }
    }
    // Sort headers alphabetically for consistency
    csvHeader.sort()
    // Generate CSV content
    def csvContent = new StringBuilder()
    csvContent.append(csvHeader.join(",")).append("\n")
    csvRows.each { row ->
        def csvLine = csvHeader.collect { key -> row.get(key, "") }
        csvContent.append(csvLine.join(",")).append("\n")
    }

    // Set CSV content as the message body
    message.setProperty(csv, csvContent)
    */
    //This is to flatten the whole json. Commenting it as it is not the desired structure. 

    }
       def totalCsvCount =  chunknames.size().toInteger(); // main csv count + Vector csv count
        message.setProperty("totalCsvCount", totalCsvCount);
         message.setProperty("chunknamelist", chunknames);
         //Fetch Signavio Credentials
        // if ( !message.getProperty("PWD") ) {
         def signaviokey_alias = message.getProperty("signavioCredentials")
          def service = ITApiFactory.getApi(SecureStoreService.class, null);
          def credential
          try {
           credential = service.getUserCredential(signaviokey_alias);
             if (  !credential ) {
                    throw new IllegalStateException("No credential found for Signavio. Credential name in config file and security artifacts should match");
                 }
                 String lv_password = new String(credential.getPassword());
                 message.setProperty("PWD", lv_password);
          } catch (Exception e) {
                 throw new SecureStoreException("Secure Parameter not available for Signavio Pipeline")
              }   
         //   }
                //String lv_password = new String(credential.getPassword());
                // message.setProperty("PWD", lv_password);
    }
    
    return message;
}


// Extracts non-primary fields while keeping nested structures intact
def extractNonPrimaryFields(input, primaryKeys, excludeKeys, complexObjectItemsSeparator) {
    def result = [:]
     def excludeRootKeys = excludeKeys.collect { it.split("\\.")[0] }.toSet()
    input.each { key, value ->
        def keyPath = key.replaceAll("\\.", complexObjectItemsSeparator) // Replace '.' with '_'
        if ( !primaryKeys.any { keyPath.startsWith(it) } && !(key in primaryKeys.collect { it.split("\\.")[0] }) 
                && !excludeRootKeys.any { key.startsWith(it) } ) { // Keep only non-primary keys
          // result[keyPath] = (value instanceof Map || value instanceof List) ? JsonOutput.toJson(value) : sanitize(value)
                      if (value instanceof Map) {
                 def flattenedMap = [:]
                flattenMap(keyPath, value, flattenedMap, complexObjectItemsSeparator)  // Flatten map
                 result.putAll(flattenedMap)  // Merge flattened map
            } else if (value instanceof List) { 
                 if (!excludeRootKeys.any { key.startsWith(it) }) {
                // Keep lists as JSON strings to preserve structure
                result[keyPath] = JsonOutput.toJson(value)
                 }
            }else {
                result[keyPath] = sanitize(value)
            }
        }
    }
    return result
}

// Extracts values from JSON for a given dot notation key

def extractValues(record, key, complexObjectItemsSeparator) {
    def keys = key.split("\\.")
    def values = [record] // Start with the entire JSON record

    keys.each { k ->
        values = values.collectMany { val ->
            if (val instanceof Map) {
                return val.containsKey(k) ? [val[k]] : []  // Keep full object if it's a Map
            } else if (val instanceof List) {
                return val.collectMany { entry ->
                    if (entry instanceof Map && entry.containsKey(k)) {
                        return [entry[k]]  // Keep full nested object
                    }
                    return []
                }
            }
            return []
        }
    }

    // If we reach a map, return it instead of flattening to key-value pairs
    
        return values.collect { 
        if (it instanceof Map) {
            def flattenedMap = [:]
            flattenMap("", it, flattenedMap, complexObjectItemsSeparator) // Flatten nested map
            return flattenedMap
        } else if ( it instanceof List) { return it }
        else { return [it] }
    }.flatten().findAll { it != null && it.toString().trim() != "" } ?: [""]
}



// Computes cartesian product of lists (for multiple primary keys)
def cartesianProduct(lists) {
    if (lists.isEmpty()) return [[]]
    def firstList = lists.head()
    def rest = cartesianProduct(lists.tail())
    return firstList.collectMany { firstElem -> rest.collect { [firstElem] + it } }
}

// Dynamically extracts sibling fields from the same primary key structure
def extractSiblingFields(record, primaryKey, primaryValue, complexObjectItemsSeparator) {
    def keys = primaryKey.split("\\.")
    def parentListKey = keys.dropRight(1).join(".") // Get parent array
    def parentRecords = extractValues(record, parentListKey, complexObjectItemsSeparator) // Fetch parent structure
     def lastKey = keys.last() 
  // def matchedRecord = parentRecords.find { it[keys.last()] == primaryValue }
   def matchedRecord = parentRecords.find { entry ->
        (entry instanceof Map) && entry[lastKey] == primaryValue
    } 
    if (matchedRecord) {
        def extractedFields = [:]
        matchedRecord.each { k, v ->
            if (k != keys.last()) {
                 def baseKey = primaryKey.split("\\.")[0]
               extractedFields["${baseKey}${complexObjectItemsSeparator}${k}"] = v // To handle issue of main csv sibling field names Eg: Suppliers_Suppliers_SupplierId_SourceSystem & Suppliers_Suppliers_SupplierId_SupplierLocationId instead of Suppliers_SourceSystem and Suppliers_SupplierLocationId
                //extractedFields["${primaryKey.replaceAll("\\.", "_")}_${k}"] = v
                // Start- add on 09/11 for proper flattening
                def safeKeyPrefix = "${baseKey}${complexObjectItemsSeparator}${k}" 
             if (v instanceof Map) { 
                  def nestedFlattened = [:]
                  flattenMap(safeKeyPrefix, v, nestedFlattened, complexObjectItemsSeparator) 
                  extractedFields.putAll(nestedFlattened) 
                  
              } else if (v instanceof List && v.every { it instanceof Map }) { 
                  // v.eachWithIndex { item, idx -> 
                  // flattenMap("${safeKeyPrefix}${complexObjectItemsSeparator}${idx}", item, extractedFields, complexObjectItemsSeparator) 
                  // } 
                  extractedFields[safeKeyPrefix] = JsonOutput.toJson(v) 
                  
              }  else {
                  extractedFields[safeKeyPrefix] = v 
                  
              }
              // End- add on 09/11 for proper flattening
            }
        }
        return extractedFields
    }
    return [:]
}

// Detects field names inside vector fields
def detectFieldNames(items) {
    def fieldNames = []
    items.each { item ->
        if (item instanceof Map) {
            fieldNames.addAll(item.keySet())
        }
    }
    return fieldNames.unique()
}

// Replaces problematic characters in values
def sanitize(value) {
    
    if (value instanceof Boolean && value != null ) {
        return value.toString()
    }
    if (value instanceof String) {
       value = value.replaceAll("\"", "\"\"").replaceAll(",", "&").replaceAll("\n", "&").trim() //replaceAll("(?i)null", " ") - makes caseinsenstive replacement
      //  return value.replaceAll("\n", "&").trim()
              // Convert ISO 8601 date-time to milliseconds
        def millis = convertToMillis(value)
        if (millis != null) {
            return millis
        }
    }
   return value //"${value}"
}

// Converts ISO 8601 date-time (e.g., "2024-08-05T10:32:50Z") to milliseconds
def convertToMillis(dateStr) {
    try {
        def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
        return dateFormat.parse(dateStr).time
    } catch (Exception e) {
        return null
    }
}

// Generates CSV string from headers and rows
def generateCsv(headers, rows, complexObjectItemsSeparator) {
    def csvString = headers.collect { it }.join(",").replaceAll("\\.",complexObjectItemsSeparator) + "\n"
    rows.each { row ->
       // def line = headers.collect { sanitize(row[it] ?: "") }.join(",")
       def line = headers.collect { h ->
            def raw = row[h]   // ❗ do not coerce null here
            def val = sanitize(raw)
            // convert to string only at CSV level
            def sval = (val == null) ? "" : val.toString()
            // Only quote original strings with special chars
            if (raw instanceof String && (sval =~ /[",\n\r]/)) {
                return "\"${sval}\""
            }
            return sval
        }.join(",")
        csvString += line + "\n"
    }
    return csvString
}

def convertHeaderDate(headerDate) {
    def inputFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH) // Parses with timezone
    def outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") // ISO 8601 UTC format
    outputFormat.setTimeZone(TimeZone.getTimeZone("UTC")) // Convert to UTC
    
    Date parsedDate = inputFormat.parse(headerDate) // Parse date with correct timezone
    return outputFormat.format(parsedDate) // Convert to UTC and return
}


//Start Fix added on 01/07/2026 for Multi level vector extraction 
def findParentMapForItem(def current, List pathParts, def targetItem, int level = 0) {

    if (current == null || level >= pathParts.size()) {
        return null
    }

    def key = pathParts[level]

    if (current instanceof Map) {
        def next = current[key]

        if (next instanceof List && next.any { it.is(targetItem) }) {
            return current
        }

        return findParentMapForItem(next, pathParts, targetItem, level + 1)
    }

    if (current instanceof List) {
        for (def entry : current) {
            def found = findParentMapForItem(entry, pathParts, targetItem, level)
            if (found != null) {
                return found
            }
        }
    }

    return null
}

def populateParentKeysFromVectorPrimaryKeys(
        Map rowVector,
        def record,
        String vectorField,
        def item,
        List primaryKeysv2,
        String complexObjectItemsSeparator) {

    if (!primaryKeysv2 || !vectorField?.contains(".")) {
        return
    }

    def vectorParts = vectorField.tokenize(".")
    def parentMap = findParentMapForItem(record, vectorParts, item)

    if (!(parentMap instanceof Map)) {
        return
    }

    primaryKeysv2.each { pk ->

        if (!pk.contains(".")) {
            return
        }

        if (pk.startsWith(vectorField + ".")) {
            return
        }

        def pkParts = pk.tokenize(".")
        def pkParentPath = pkParts.dropRight(1)
        def pkField = pkParts.last()

        if (
            pkParentPath.size() < vectorParts.size() &&
            vectorParts[0..<pkParentPath.size()] == pkParentPath
        ) {
            def column = pk.replaceAll("\\.", complexObjectItemsSeparator)

            if (!rowVector.containsKey(column)) {
                rowVector[column] = sanitize(parentMap[pkField])
            }
        }
    }
}
// End Fix added on 01/07/2026  for Multi level vector extraction


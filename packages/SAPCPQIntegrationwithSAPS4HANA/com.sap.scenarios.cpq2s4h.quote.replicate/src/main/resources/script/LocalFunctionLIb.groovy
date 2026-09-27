import com.sap.it.api.mapping.*;

/*Add MappingContext parameter to read or set headers and properties
def String customFunc1(String P1,String P2,MappingContext context) {
         String value1 = context.getHeader(P1);
         String value2 = context.getProperty(P2);
         return value1+value2;
}

Add Output parameter to assign the output value.
def void custFunc2(String[] is,String[] ps, Output output, MappingContext context) {
        String value1 = context.getHeader(is[0]);
        String value2 = context.getProperty(ps[0]);
        output.addValue(value1);
        output.addValue(value2);
}*/
// Get IDOC Control Record information
def String getSenderPort(String P1, MappingContext context){
	return context.getProperty("p_SNDPOR");
}

def String getSenderPartnerNumber(String P1, MappingContext context){
	return context.getProperty("p_SNDPRN");
}

def String getRecipientPort(String P1, MappingContext context){
	return context.getProperty("p_RCVPOR");
}

def String getRecipientPartnerNumber(String P1, MappingContext context){
	return context.getProperty("p_RCVPRN");
}

def String getordertype(String P1, MappingContext context){
	return context.getProperty("p_DOCTYPE");
}

def String getDistributionChannel(String P1, MappingContext context){
    if (P1.length()>0)
        return P1;
    else
	    return context.getProperty("p_DistributionChannel");
}

def void getCustomFieldValue(String[] var1,String[] var2, String[] var3, Output output, MappingContext context){
    /*
    <CustomField>
      <Content><![CDATA[AN CPQ Quote 180929 16:11]]></Content>
      <Id>6</Id>
      <Name><![CDATA[PO Number]]></Name>
    </CustomField>
    var2 is PO Number <Name>
    var3 is AN CPQ Quote 180929 16:11 <Content>
    */
    for (int i=0;i<var1.length;i++)
	{ 
		if (var1[i].equals(var2[0]))
		{ 
				output.addValue(var3[i]);
				break;
	}
}
}


def void createPoItemNum(String[] var1,String[] var2, Output output, MappingContext context) {
    // Create PO_ITM_NO
       if (var1.length > 0){
          for (int i=0; i< var2.length;i++) 
	          output.addValue(String.format("%06d", (Integer.parseInt(var1[0]))));
    }
}

def void createItemNumber(String[] itemno,String[] parentItemno, Output erp_item_no, Output erp_parent_item_no, MappingContext context){

        List<String> erp_item_num = new ArrayList<>();
        int initalItemNo = 0;
		int subItemIncrement = 1;
		
		// Generate ITM_NUMBER
		for (int i = 0; i < itemno.length; i++) {
			try {
				initalItemNo = Integer.parseInt(itemno[i]) * 100;
				erp_item_no.addValue(String.valueOf(initalItemNo));
				erp_item_num.add(String.valueOf(initalItemNo));
				subItemIncrement = 1;

			} catch (NumberFormatException ex) {
			    String ChildItemNumber = String.valueOf(initalItemNo + subItemIncrement);
				erp_item_no.addValue(ChildItemNumber);
				erp_item_num.add(ChildItemNumber);
				subItemIncrement++;
			}
		}
		
		// Generate HG_LV_ITEM
		for (int i = 0; i < parentItemno.length; i++) {
			if (parentItemno[i].length() == 0)
				erp_parent_item_no.addValue("");
			else
				for (int j = 0; j < itemno.length; j++) {
					if (parentItemno[i].equals(itemno[j])) {
						erp_parent_item_no.addValue(erp_item_num.get(j));
						break;
					}
				}
		}
}

def void checkHeaderDiscount(String[] var1, Output output, MappingContext context){
    for (int i=0;i<var1.length;i++){
	double d = 0.0;
	try{
		d = Double.parseDouble(var1[i]);
	    
	}
	catch (Exception ex){
		d = 0.0;
	}
	if (d > 0.0)
		output.addValue(var1[i]); 
}
}

def String getConfigIdFromParentRolledUpQuoteItemId(String parentRolledUpQuoteItemId) {

        if (parentRolledUpQuoteItemId != null && parentRolledUpQuoteItemId != "")
        {
            return parentRolledUpQuoteItemId.tokenize('.')[0];
        }
        
        return null;
}

def String getInstIdFromCpsItemId(String parentRolledUpQuoteItemId, String cpsItemId) {

        if (parentRolledUpQuoteItemId != null && parentRolledUpQuoteItemId != "")
        {
            return cpsItemId;
        }
        
        return null;
}

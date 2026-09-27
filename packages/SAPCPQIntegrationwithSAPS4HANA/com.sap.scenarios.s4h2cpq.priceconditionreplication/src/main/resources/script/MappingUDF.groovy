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



def void getDescription(String[] langu, String[] desc, Output output, MappingContext context){
    
 for( Integer i=0; i<langu.length; i++ )
    {
        if (langu[i].equals("EN"))
        {
            String value1 = desc[i];
            output.addValue(value1);
        }
    }  

}



def String removeZeros(String input){

    BigDecimal bd;
    try
        {
	        bd = new BigDecimal(input);
	        return bd.toString();
        }
    catch(Exception e)
        {   
            return input;
        }           
}        
        

/* currently mutiple conditions incl multiple currencies per time period are not supported. ERP modelling has to checked.  
*/
def String checkCurrency(String arg1){
    
    if (arg1 > "1"){
        
          throw new Exception("MutipleCurrencies");
    }
    
    arg1 = "0";
	return arg1;
}        
        
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

def String uuid (String var1){

if ( var1.length() == 32 ) {
   var1.toLowerCase();
String uuid = var1.replaceAll(  "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "\$1-\$2-\$3-\$4-\$5");
     return uuid;
 }
 
else
    if ( var1.length() == 0 )
          return "00000000-0000-0000-0000-000000000000";
    else
        return var1;
}
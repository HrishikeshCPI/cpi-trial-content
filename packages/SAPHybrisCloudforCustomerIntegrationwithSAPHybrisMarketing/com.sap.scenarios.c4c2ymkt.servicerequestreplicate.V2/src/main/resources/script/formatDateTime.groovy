import com.sap.it.api.mapping.*;
import com.sap.aii.mapping.api.*;
import com.sap.aii.mapping.lookup.*;
import com.sap.aii.mappingtool.tf7.rt.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;


def String formatDatTime(String inp){
	if(inp.length() > 23){
	    return inp.substring(0, 23);
	}else{
	    return inp.substring(0, inp.length() - 1)
	}
}
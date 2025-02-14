/*
The MIT License (MIT)

Copyright (c) Terry Evans Vaughn 

All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package EDIMaps;

import com.blueseer.ctr.cusData;
import java.text.DecimalFormat;
import java.util.ArrayList;
import com.blueseer.utl.OVData;
import com.blueseer.edi.EDI.*;
import static com.blueseer.utl.BlueSeerUtils.*;
import com.blueseer.inv.invData;
import com.blueseer.utl.EDData;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 *
 * @author vaughnte
 */
public class Generic850i extends com.blueseer.edi.EDIMap {
    
    public String[] Mapdata(ArrayList doc, String[] c) throws IOException, UserDefinedException {
   
    setControl(c);    //required...set the super class variables per the inbound array passed from the Processor (See EDIMap javadoc for defs)
    
    if (isError) { return error;}  //required...check errors for master variables
    
    
    mappedInput = mapInput(c, doc, ISF); //required...sets the source data structure for all subsequent map functions
    
    setReference(getInput("BEG","e03")); //optional...but must be ran after mappedInput
   
    debuginput(mappedInput);  //optional... for debug purposes

    isDBWrite(c);// optional...unless this map is writing to internal database tables (orders, etc)
    
    //since this is a DB entry map, create class object specific to inbound doctype (edi850, edi824, etc)
    edi850 e = new edi850(getInputISA(6), getInputISA(8), getInputGS(2), getInputGS(3), getInputISA(13), getInputISA(9), doctype, stctrl);  // mandatory class creation
    
    //optional...set some global variables as necessary
    String  now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    int i = 0; 
    String po;
    double discount;
    double listprice;
    double netprice;
    boolean useInternalPrice = false;

    // begin mapping
    
    e.setOVBillTo(EDData.getEDIXrefIn(getInputISA(6), getInputGS(2), "BT", ""));   // 3rd parameter '0' is outbound direction '1' is inbound
    po = getInput("BEG","e03");
    e.setPO(po);  
    e.setPODate(convertDateFormat("yyyyMMdd", getInput("BEG","e05")));

    if (segmentExists("DTM","1:002","e01")) {
    e.setDueDate(convertDateFormat("yyyyMMdd", getInput("DTM","1:002","e02")));
    } else {
    e.setDueDate(now);    
    }   

   
    
    int n1count = getGroupCount("N1");
    boolean isN1ST = false;
    for (i = 1; i <= n1count; i++) {
        if (getInput(i,"N1",1).equals("ST")) {
        isN1ST = true;
        } else {
        isN1ST = false;
        }
        if (isN1ST) {
            e.setShipTo(getInput(i,"N1",4));
            e.setShipToName(getInput(i,"N1",2));
            e.setShipToLine1(getInput(i,"N1:N3",1));
            e.setShipToCity(getInput(i,"N1:N4",1));
            e.setShipToState(getInput(i,"N1:N4",2));
            e.setShipToZip(getInput(i,"N1:N4",3));
            e.setOVShipTo(EDData.getEDIXrefIn(getInputISA(6), getInputGS(2), "ST", getInput(i,"N1",4)));
        }
    }  // shipto loop

   if (! e.getOVShipTo().isEmpty()) {
   e.setOVBillTo(cusData.getcustBillTo(e.getOVShipTo()));
   } 
   // NOTE: it's imperative that we have an internal billto code assign for pricing and discounts look up during the detail loop
   // if here and we have a blank billto...then error out
   if (e.getOVBillTo().isEmpty()) {
   setError("No internal Billto Found PO: " + po);
   return error; 
   }

       /* Now the Detail LOOP  */ 
       /* Item Loop */
    DecimalFormat df = new java.text.DecimalFormat("0.000##");
    int itemcount = getGroupCount("PO1");
    int itemLoopCount = 0;
    int totalqty = 0;
    String uom = "";
    String item = "";
    for (i = 1; i <= itemcount; i++) {
        e.addDetail();  // INITIATE An ArrayList for Each PO1 SEGMENT....variable i is set at bottom of loop as index  i == 0 is first PO1
        itemLoopCount++;
        totalqty += Integer.valueOf(getInput(i,"PO1",2));
        e.setDetQty(i-1, getInput(i,"PO1",2));
        if (getInput(i,"PO1",6).equals("VP") || getInput(i,"PO1",6).equals("VN")) {
         e.setDetItem(i-1,getInput(i,"PO1",7));
        } else if (getInput(i,"PO1",8).equals("BP") || getInput(i,"PO1",8).equals("SK")) {
         e.setDetItem(i-1,getInput(i,"PO1",9));   
        } else {
         e.setDetItem(i-1,"UNKNOWN");   
        }
        item = e.getDetItem(i-1);
       // e.setDetCustItem(i,getInput("PO1",9,i));
        e.setDetPO(i-1,po);
        e.setDetLine(i-1,getInput(i,"PO1",1));

        //override incoming UOM with what is available in UOM Maintenance
        if (getInput(i,"P01",3).equals("CS")) {
            uom = "CA";
        } else {
         uom = OVData.getUOMByItem(item);
        }

        if (useInternalPrice) {
        listprice = invData.getItemPriceFromCust(e.getOVBillTo(), item, uom, cusData.getCustCurrency(e.getOVBillTo()));
        discount = invData.getItemDiscFromCust(e.getOVBillTo());
        netprice = listprice;
        if (discount != 0) {
        netprice = listprice - (listprice * (discount / 100));
        }
        e.setDetNetPrice(i-1,String.valueOf(currformatDouble(netprice)));
        e.setDetListPrice(i-1,String.valueOf(currformatDouble(listprice)));
        e.setDetDisc(i-1,String.valueOf(currformatDouble(discount)));
        } else {
         if (isParsableToDouble(getInput(i,"PO1",4))) {
            e.setDetNetPrice(i-1, df.format(Double.valueOf(getInput(i,"PO1",4))));
            e.setDetListPrice(i-1, df.format(Double.valueOf(getInput(i,"PO1",4))));
         } else {
            e.setDetNetPrice(i-1, "0");
            e.setDetListPrice(i-1, "0");	
         }   
        }
    }
    /* end of item loop */

    // mapping end
    
    mappedInput.clear();

     /* Load Sales Order */
     /* call processDB ONLY if the output is database write */
    processDB(c,com.blueseer.edi.EDI.createSOFrom850(e, c));

    return packagePayLoad(c);  //required...
        
    }

 
 
}



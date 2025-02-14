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

package com.blueseer.edi;

import bsmf.MainFrame;
import static bsmf.MainFrame.defaultDecimalSeparator;
import com.blueseer.ctr.cusData;
import static com.blueseer.edi.EDIMap.HASH;
import static com.blueseer.edi.EDIMap.ISF;
import static com.blueseer.edi.EDIMap.OMD;
import static com.blueseer.edi.EDIMap.OSF;
import static com.blueseer.edi.EDIMap.clearStaticVariables;
import static com.blueseer.edi.EDIMap.delimConvertIntToStr;
import com.blueseer.edi.ediData.dfs_mstr;
import static com.blueseer.edi.ediData.getDFSMstr;
import com.blueseer.frt.frtData;
import static com.blueseer.frt.frtData.addCFOTransaction;
import static com.blueseer.frt.frtData.getCFOPrevious;
import com.blueseer.ord.ordData;
import static com.blueseer.ord.ordData.addOrderTransaction;
import com.blueseer.pur.purData;
import com.blueseer.shp.shpData;
import com.blueseer.utl.EDData;
import com.blueseer.utl.BlueSeerUtils;
import static com.blueseer.utl.BlueSeerUtils.cleanDirString;
import static com.blueseer.utl.BlueSeerUtils.getGlobalProgTag;
import static com.blueseer.utl.EDData.getBSDocTypeFromStds;
import static com.blueseer.utl.EDData.getEDIDocTypeFromBSDoc;
import static com.blueseer.utl.EDData.getEDIFFDocType;
import static com.blueseer.utl.EDData.getEDIFFSubType;
import com.blueseer.utl.OVData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import static java.nio.file.Files.copy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author vaughnte
 */
public class EDI {
    public static boolean GlobalDebug = false;
    public static Map<String, ArrayList<String>> hanoi = new HashMap<>();
    
    private static Path edilogpath = FileSystems.getDefault().getPath("edi/error.log");
    
    public static void edilog(String s) {
        BufferedWriter output = null;
        String  now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        try {
            output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(edilogpath.toFile())));
            output.write("TIMESTAMP: " + now + "" + "\n");
            output.write(s);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(EDI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(EDI.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try { 
                output.close();
            } catch (IOException ex) {
                Logger.getLogger(EDI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
         
              
    }
    
    public static void edilog(Throwable t) {
        PrintStream ps = null;
        String  now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        try {
            ps = new PrintStream(new FileOutputStream(edilogpath.toFile(), true));
            ps.print("TIMESTAMP: " + now + "" + "\n");
            t.printStackTrace(ps);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(EDI.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            ps.close();
        }    
    }
    
    
    public static String[] initEDIControl() {   
        String[] controlarray = new String[39];
             /*  39 elements consisting of:
            c[0] = senderid;
            c[1] = doctype;
            c[2] = map;
            c[3] = infile;
            c[4] = controlnum;
            c[5] = gsctrlnbr;
            c[6] = docid;
            c[7] = ref;
            c[8] = outfile;
            c[9] = sd;
            c[10] = ed;
            c[11] = ud;
            c[12] = overideenvelope
            c[13] = isastring
            c[14] = gsstring
            c[15] = outdoctype
            c[16] = idxnbr
            c[17] = isastart
            c[18] = isaend
            c[19] = docstart
            c[20] = docend
            c[21] = receiverid;
            c[22] = comkey;
            c[23] = return messg from map (status)
            c[24] = inbatch
            c[25] = outbatch
            c[26] = indir
            c[27] = outdir
            c[28] = infiletype  // ff, x12, xml, etc
            c[29] = outfiletype  // ff, x12, xml, etc
            c[30] = debug  true or false
            c[31] = outisastart
            c[32] = outisaend
            c[33] = outdocstart
            c[34] = outdocend
            c[35] = outsegdelim
            c[36] = outeledelim
            c[37] = outsubdelim
            c[38] = message
            
              */
               for (int i = 0; i < 39; i++) {
                   controlarray[i] = (i == 4 || i == 5 || i == 6 || i == 31 || i == 32 || i == 33 || i == 34 || i == 35 || i == 36 || i == 37 ||
                           i == 9 || i == 10 || i == 11) ? "0" : "";
                }
                return controlarray;
    }
   
    public static void reduceFile(Path infile, String outdir, String[] positions) throws FileNotFoundException, IOException {
        DateFormat dfdate = new SimpleDateFormat("yyyyMMddHHmm");
        Date now = new Date();
        File newfile = new File(outdir  + infile.getFileName().toString() + dfdate.format(now) + "_bs");
        
        File file = new File(infile.toString());
        BufferedReader f = new BufferedReader(new FileReader(file));
         char[] cbuf = new char[(int) file.length()];
         f.read(cbuf); 
         f.close();
         
         for (int kk = 0; kk < positions.length; kk+=2) {
           char[] newarray = Arrays.copyOfRange(cbuf, Integer.valueOf(positions[kk]), Integer.valueOf(positions[kk+1]));
           FileWriter out = new FileWriter(newfile,true);
           out.write(newarray);
           out.close();
         }
         
         
        
    }
    
    public static String[] filterFile(String infile, String outdir, String[] doctypes, ArrayList<String[]> trafficarray) throws FileNotFoundException, IOException {
        String[] m = new String[]{"0","","","",""};  //status, message, doctype, tradeid, outdir
        
       
        String[] c = null;  // control values to pass to map and log
        
        File file = new File(infile);
        BufferedReader f = new BufferedReader(new FileReader(file));
         char[] cbuf = new char[(int) file.length()];
         int max = cbuf.length;
         f.read(cbuf); 
         f.close();
         
         
         DateFormat dfdate = new SimpleDateFormat("yyyyMMddHHmm");
         Date now = new Date();
         List<String> allowed = Arrays.asList(doctypes);
         
         // now lets see how many ISAs and STs within those ISAs and write character positions of each
         Map<Integer, Object[]> ISAmap = new HashMap<Integer, Object[]>();
         int start = 0;
         int end = 0;
         int isacount = 0;
         int isactrl = 0;
         int gscount = 0;
         int stcount = 0;
         int ststart = 0;
         int sestart = 0;
         String ed_escape = "";
         String sd_escape = "";
         int gsstart = 0;
         String doctype = "";
         String docid = "";
         String reference = "";
         ArrayList<String> isaList = new ArrayList<String>();
          
          char e = 0;
          char s = 0;
          char u = 0;
          
          int mark = 0;
         String filetype = "unknown";  
           Map<Integer, ArrayList> stse_hash = new HashMap<Integer, ArrayList>();
           ArrayList<Object> docs = new ArrayList<Object>();
          
            for (int i = 0; i < cbuf.length; i++) {
                
                if ( ((i+103) <= max) && cbuf[i] == 'I' && cbuf[i+1] == 'S' && cbuf[i+2] == 'A' 
                        && (cbuf[i+103] == cbuf[i+3]) && (cbuf[i+103] == cbuf[i+6]) ) {
                    e = cbuf[i+103];
                    u = cbuf[i+104];
                    s = cbuf[i+105];
                    filetype = "x12";
                    mark = i;
                    
                    
                    // lets bale if not proper ISA envelope.....unless the 106 is carriage return...then ok
                    if (i == mark && cbuf[mark+106] != 'G' && cbuf[mark+107] != 'S' && ! String.format("%02x",(int) cbuf[mark+106]).equals("0a")) {
                        return m = new String[]{"1","malformed envelope", "", "", ""};
                    }
                    ed_escape = escapeDelimiter(String.valueOf(e));
                    sd_escape = escapeDelimiter(String.valueOf(s));
                    if (String.format("%02x",(int) cbuf[i+105]).equals("0d") && String.format("%02x",(int) cbuf[i+106]).equals("0a"))
                        s = cbuf[i+106];
                    start = i;
                    isaList.add("ISA" + ":" + String.valueOf(i) + ":" + String.format("%02x",(int) s) );
                    isacount++;
                    String[] isa = new String(cbuf, i, 105).split(ed_escape);
                    isactrl = Integer.valueOf(isa[13]);
                    
                      // set control
                    c = initEDIControl();
                    c[0] = isa[6].trim(); // senderid
                    c[21] = isa[8].trim(); // receiverid
                    c[2] = "";
                    c[3] = infile;
                    c[4] = isa[13]; //isactrlnbr
                    c[8] = outdir;
                    c[9] = String.valueOf((int) s);
                    c[10] = String.valueOf((int) e);
                    c[11] = String.valueOf((int) u);
                    c[12] = ""; // override
                    c[13] = new String(cbuf,i,105);
                    c[15] = "0"; // inbound
                   
                }
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'G' && cbuf[i+1] == 'S') {
                    gscount++;
                    isaList.add("GS" + ":" + String.valueOf(i));
                    gsstart = i;
                    String[] gs = new String(cbuf, gsstart, 90).split(ed_escape);
                                      
                     c[5] = gs[6]; // gsctrlnbr
                    gs[8] = gs[8].split(sd_escape)[0];
                    c[14] = String.join(String.valueOf(e), Arrays.copyOfRange(gs, 0, 9));
                    c[14] = gs[2];  // overwriting usual use of c[14] for fileFilter
                   
                }
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'S' && cbuf[i+1] == 'T') {
                   
                    stcount++;
                    isaList.add("ST" + ":" + String.valueOf(i));
                    ststart = i;
                    
                    String[] st = new String(cbuf, i, 16).split(ed_escape);
                    doctype = st[1]; // doctype
                    docid = st[2].split(sd_escape)[0]; //docID  // to separate 2nd element of ST because grabbing 16 characters in buffer
                   
                   // System.out.println(c[0] + "/" + c[1] + "/" + c[4] + "/" + c[5]);
                } 
                
                // lets try to get the reference key of the doc...beg, bsn, etc
                // BEG 
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'B' && cbuf[i+1] == 'E' && cbuf[i+2] == 'G') {
                 String[] xx = new String(cbuf, i, 50).split(ed_escape);
                 reference = xx[3];
                }
                // BSN
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'B' && cbuf[i+1] == 'S' && cbuf[i+2] == 'N') {
                 String[] xx = new String(cbuf, i, 50).split(ed_escape);
                 reference = xx[2];
                }
                // BSN
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'B' && cbuf[i+1] == 'I' && cbuf[i+2] == 'G') {
                 String[] xx = new String(cbuf, i, 50).split(ed_escape);
                 reference = xx[2];
                }
                // END of reference grab
                
                
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'S' && cbuf[i+1] == 'E') {
                    isaList.add("SE" + ":" + String.valueOf(i));
                    sestart = i;
                    // add to hash if hash doesn't exist or insert into hash
                    docs.add(new Object[] {new Integer[] {ststart, sestart}, doctype, docid, reference});
                    // painful reminder that you have to create copy of array at instance in time
                    ArrayList copydocs = new ArrayList(docs);
                    stse_hash.put(isacount, copydocs);
                }
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'I' && cbuf[i+1] == 'E' && cbuf[i+2] == 'A') {
                    end = i + 14 + Integer.valueOf(String.valueOf(gscount).length()) + 1;
                    // now add to ISAmap
                   HashMap<Integer,ArrayList> mycopy = new HashMap<Integer,ArrayList>(stse_hash);
                  ISAmap.put(isacount, new Object[] {start, end, (int) s, (int) e, (int) u, mycopy, c});
                    isaList.add("IEA" + ":" + String.valueOf(i));
                    stcount = 0;
                    docs.clear();
                    stse_hash.remove(isacount);
                    
                } 
            }
      
    
            
    // now run through ISAMap records of this file
    // set mixbag flag as false...and assign true if File has any ISA patterns with diff type of docs        
    boolean mixbag = false;
    boolean prevISA = false;
    boolean isInSet = true;
    StringBuilder keepers = new StringBuilder(); 
    int q = 0;
    
    for (Map.Entry<Integer, Object[]> isa : ISAmap.entrySet()) {
     q++;
      //  int start =  Integer.valueOf(isa.getValue()[0].toString());  //starting ISA position in file
      //  int end = Integer.valueOf(isa.getValue()[1].toString());  // ending IEA position in file
        char segdelim = (char) Integer.valueOf(isa.getValue()[2].toString()).intValue(); // get segment delimiter
       String[] control = (String[]) isa.getValue()[6]; 
      // System.out.println("    envelope: cust/key/start/end : " + c[0] + "/" + isa.getKey() + "/" + isa.getValue()[0] + "/" + isa.getValue()[1]);
         
      //  String[] c = (String[]) isa.getValue()[6];
       // ArrayList d = (ArrayList) isa.getValue()[5];
        Map<Integer, ArrayList> d = (HashMap<Integer, ArrayList>)isa.getValue()[5];
    
      
       for (Map.Entry<Integer, ArrayList> z : d.entrySet()) {
         
       //    System.out.println("doc May entry key: " + z.getKey());
           
         for (Object r : z.getValue()) {
            Object[] x = (Object[]) r;
         
            Integer[] k = (Integer[])x[0];
          //  String doctype = (String)x[1];
          //  String docid = (String)x[2];
                  
         isInSet = (! allowed.contains((String)x[1])) ? false : true;
         
         m[2] = (String)x[1]; // set doctype here
         m[3] = c[0]; // set tradeid here
         if (isInSet) {
         m[4] = outdir;  // set as default outdir unless overridden...leave blank if ! isInSet
         }         
         // now override outdir with def file element 2 if not empty
         // def file defined as doctype, tradeid, outputdir, singlefilename
         for (String[] def : trafficarray) { 
                      // least to most significant
                      
              if (def[0].equals(m[2]) && def[1].isEmpty() && ! def[2].isEmpty()) { // if element 1 of def file matches doctype
                  m[4] = def[2];
              }   
              if (def[0].equals(m[2]) && def[1].equals(m[3]) && ! def[2].isEmpty() ) { // if maches doctype and tradeid
                 m[4] = def[2]; 
              } 
         }
         
         
        
         System.out.println(dfdate.format(now) + "," + infile + "," + filetype + "," + (String)x[3] + "," + c[0] + "," + c[14] + "," + 
                 isa.getKey() + "," + isa.getValue()[0] + "," + isa.getValue()[1] + "," +
                 k[0] + "," + k[1] + "," + (String)x[1] + "," + (String)x[2] + "," + m[4] + "," + isInSet ); 
      
  
      } // r         
               
    } // object k
     // now determine how many ISAs are of allowed doc types
     if (q > 1 && prevISA != isInSet) {
       mixbag = true;  
     }
     if (isInSet) {
         keepers.append(isa.getValue()[0] + "," + isa.getValue()[1]).append(",");
     }
     prevISA = isInSet;
    } // ISAMap entries
    
    // if unknown...then no isa hashmap will be processed...just tag it.        
    if (filetype.equals("unknown")) {
    System.out.println(dfdate.format(now) + "," + infile + "," + filetype + ",,,,,,,,,,,," + "false" );
    isInSet = false;
    }
    
    
    if (! isInSet && ! mixbag) {
        m[0] = "1"; // discard...otherwise keep....m[0] = "0" initialized above
    }
    if (mixbag) {
        System.out.println(dfdate.format(now) + "," + infile + "," + "mixbag" + ",,,,,,,,,,,,," );
        m[0] = "9"; // mixbag...pass code 9 along with start,end of buffer to keep
        m[1] = keepers.deleteCharAt(keepers.length() - 1).toString();
    }
    return m;
    }
     
    public static String[] filterFileWF(String infile, String outdir, BufferedWriter log, String[] doctypes, ArrayList<String[]> trafficarray) throws FileNotFoundException, IOException {
        String[] m = new String[]{"0","","","",""};  //status, message, doctype, tradeid, outdir
        
       
        String[] c = null;  // control values to pass to map and log
        
        File file = new File(infile);
        BufferedReader f = new BufferedReader(new FileReader(file));
         char[] cbuf = new char[(int) file.length()];
         int max = cbuf.length;
         f.read(cbuf); 
         f.close();
         
         
         DateFormat dfdate = new SimpleDateFormat("yyyyMMddHHmm");
         Date now = new Date();
         List<String> allowed = Arrays.asList(doctypes);
         
         // now lets see how many ISAs and STs within those ISAs and write character positions of each
         Map<Integer, Object[]> ISAmap = new HashMap<Integer, Object[]>();
         int start = 0;
         int end = 0;
         int isacount = 0;
         int isactrl = 0;
         int gscount = 0;
         int stcount = 0;
         int ststart = 0;
         int sestart = 0;
         String ed_escape = "";
         String sd_escape = "";
         int gsstart = 0;
         String doctype = "";
         String docid = "";
         String reference = "";
         ArrayList<String> isaList = new ArrayList<String>();
          
          char e = 0;
          char s = 0;
          char u = 0;
          
          int mark = 0;
         String filetype = "unknown";  
           Map<Integer, ArrayList> stse_hash = new HashMap<Integer, ArrayList>();
           ArrayList<Object> docs = new ArrayList<Object>();
          
            for (int i = 0; i < cbuf.length; i++) {
                
                if ( ((i+103) <= max) && cbuf[i] == 'I' && cbuf[i+1] == 'S' && cbuf[i+2] == 'A' 
                        && (cbuf[i+103] == cbuf[i+3]) && (cbuf[i+103] == cbuf[i+6]) ) {
                    e = cbuf[i+103];
                    u = cbuf[i+104];
                    s = cbuf[i+105];
                    filetype = "x12";
                    mark = i;
                    
                    
                    // lets bale if not proper ISA envelope.....unless the 106 is carriage return...then ok
                    if (i == mark && cbuf[mark+106] != 'G' && cbuf[mark+107] != 'S' && ! String.format("%02x",(int) cbuf[mark+106]).equals("0a")) {
                        return m = new String[]{"1","malformed envelope", "", "", ""};
                    }
                    ed_escape = escapeDelimiter(String.valueOf(e));
                    sd_escape = escapeDelimiter(String.valueOf(s));
                    if (String.format("%02x",(int) cbuf[i+105]).equals("0d") && String.format("%02x",(int) cbuf[i+106]).equals("0a"))
                        s = cbuf[i+106];
                    start = i;
                    isaList.add("ISA" + ":" + String.valueOf(i) + ":" + String.format("%02x",(int) s) );
                    isacount++;
                    String[] isa = new String(cbuf, i, 105).split(ed_escape);
                    isactrl = Integer.valueOf(isa[13]);
                    
                      // set control
                    c = initEDIControl();
                    c[0] = isa[6].trim(); // senderid
                    c[21] = isa[8].trim(); // receiverid
                    c[2] = "";
                    c[3] = infile;
                    c[4] = isa[13]; //isactrlnbr
                    c[8] = outdir;
                    c[9] = String.valueOf((int) s);
                    c[10] = String.valueOf((int) e);
                    c[11] = String.valueOf((int) u);
                    c[12] = ""; // override
                    c[13] = new String(cbuf,i,105);
                    c[15] = "0"; // inbound
                   
                }
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'G' && cbuf[i+1] == 'S') {
                    gscount++;
                    isaList.add("GS" + ":" + String.valueOf(i));
                    gsstart = i;
                    String[] gs = new String(cbuf, gsstart, 90).split(ed_escape);
                                      
                     c[5] = gs[6]; // gsctrlnbr
                    gs[8] = gs[8].split(sd_escape)[0];
                    c[14] = String.join(String.valueOf(e), Arrays.copyOfRange(gs, 0, 9));
                    c[14] = gs[2];  // overwriting usual use of c[14] for fileFilter
                   
                }
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'S' && cbuf[i+1] == 'T') {
                   
                    stcount++;
                    isaList.add("ST" + ":" + String.valueOf(i));
                    ststart = i;
                    
                    String[] st = new String(cbuf, i, 16).split(ed_escape);
                    doctype = st[1]; // doctype
                    docid = st[2].split(sd_escape)[0]; //docID  // to separate 2nd element of ST because grabbing 16 characters in buffer
                   
                   // System.out.println(c[0] + "/" + c[1] + "/" + c[4] + "/" + c[5]);
                } 
                
                // lets try to get the reference key of the doc...beg, bsn, etc
                // BEG 
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'B' && cbuf[i+1] == 'E' && cbuf[i+2] == 'G') {
                 String[] xx = new String(cbuf, i, 50).split(ed_escape);
                 reference = xx[3];
                }
                // BSN
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'B' && cbuf[i+1] == 'S' && cbuf[i+2] == 'N') {
                 String[] xx = new String(cbuf, i, 50).split(ed_escape);
                 reference = xx[2];
                }
                // BSN
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'B' && cbuf[i+1] == 'I' && cbuf[i+2] == 'G') {
                 String[] xx = new String(cbuf, i, 50).split(ed_escape);
                 reference = xx[2];
                }
                // END of reference grab
                
                
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'S' && cbuf[i+1] == 'E') {
                    isaList.add("SE" + ":" + String.valueOf(i));
                    sestart = i;
                    // add to hash if hash doesn't exist or insert into hash
                    docs.add(new Object[] {new Integer[] {ststart, sestart}, doctype, docid, reference});
                    // painful reminder that you have to create copy of array at instance in time
                    ArrayList copydocs = new ArrayList(docs);
                    stse_hash.put(isacount, copydocs);
                }
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'I' && cbuf[i+1] == 'E' && cbuf[i+2] == 'A') {
                    end = i + 14 + Integer.valueOf(String.valueOf(gscount).length()) + 1;
                    // now add to ISAmap
                   HashMap<Integer,ArrayList> mycopy = new HashMap<Integer,ArrayList>(stse_hash);
                  ISAmap.put(isacount, new Object[] {start, end, (int) s, (int) e, (int) u, mycopy, c});
                    isaList.add("IEA" + ":" + String.valueOf(i));
                    stcount = 0;
                    docs.clear();
                    stse_hash.remove(isacount);
                    
                } 
            }
      
    
            
    // now run through ISAMap records of this file
    // set mixbag flag as false...and assign true if File has any ISA patterns with diff type of docs        
    boolean mixbag = false;
    boolean prevISA = false;
    boolean isInSet = true;
    StringBuilder keepers = new StringBuilder(); 
    int q = 0;
    
    for (Map.Entry<Integer, Object[]> isa : ISAmap.entrySet()) {
     q++;
      //  int start =  Integer.valueOf(isa.getValue()[0].toString());  //starting ISA position in file
      //  int end = Integer.valueOf(isa.getValue()[1].toString());  // ending IEA position in file
        char segdelim = (char) Integer.valueOf(isa.getValue()[2].toString()).intValue(); // get segment delimiter
       String[] control = (String[]) isa.getValue()[6]; 
      // System.out.println("    envelope: cust/key/start/end : " + c[0] + "/" + isa.getKey() + "/" + isa.getValue()[0] + "/" + isa.getValue()[1]);
         
      //  String[] c = (String[]) isa.getValue()[6];
       // ArrayList d = (ArrayList) isa.getValue()[5];
        Map<Integer, ArrayList> d = (HashMap<Integer, ArrayList>)isa.getValue()[5];
    
      
       for (Map.Entry<Integer, ArrayList> z : d.entrySet()) {
         
       //    System.out.println("doc May entry key: " + z.getKey());
           
         for (Object r : z.getValue()) {
            Object[] x = (Object[]) r;
         
            Integer[] k = (Integer[])x[0];
          //  String doctype = (String)x[1];
          //  String docid = (String)x[2];
                  
         isInSet = (! allowed.contains((String)x[1])) ? false : true;
         
         m[2] = (String)x[1]; // set doctype here
         m[3] = c[0]; // set tradeid here
         if (isInSet) {
         m[4] = outdir;  // set as default outdir unless overridden...leave blank if ! isInSet
         }         
         // now override outdir with def file element 2 if not empty
         // def file defined as doctype, tradeid, outputdir, singlefilename
         for (String[] def : trafficarray) { 
                      // least to most significant
                      
              if (def[0].equals(m[2]) && def[1].isEmpty() && ! def[2].isEmpty()) { // if element 1 of def file matches doctype
                  m[4] = def[2];
              }   
              if (def[0].equals(m[2]) && def[1].equals(m[3]) && ! def[2].isEmpty() ) { // if maches doctype and tradeid
                 m[4] = def[2]; 
              } 
         }
         
         
        
         log.write(dfdate.format(now) + "," + infile + "," + filetype + "," + (String)x[3] + "," + c[0] + "," + c[14] + "," + 
                 isa.getKey() + "," + isa.getValue()[0] + "," + isa.getValue()[1] + "," +
                 k[0] + "," + k[1] + "," + (String)x[1] + "," + (String)x[2] + "," + m[4] + "," + isInSet ); 
         log.write("\n");
      
  
      } // r         
               
    } // object k
     // now determine how many ISAs are of allowed doc types
     if (q > 1 && prevISA != isInSet) {
       mixbag = true;  
     }
     if (isInSet) {
         keepers.append(isa.getValue()[0] + "," + isa.getValue()[1]).append(",");
     }
     prevISA = isInSet;
    } // ISAMap entries
    
    // if unknown...then no isa hashmap will be processed...just tag it.        
    if (filetype.equals("unknown")) {
    log.write(dfdate.format(now) + "," + infile + "," + filetype + ",,,,,,,,,,,," + "false" );
    log.write("\n");
    isInSet = false;
    }
    
    
    if (! isInSet && ! mixbag) {
        m[0] = "1"; // discard...otherwise keep....m[0] = "0" initialized above
    }
    if (mixbag) {
        log.write(dfdate.format(now) + "," + infile + "," + "mixbag" + ",,,,,,,,,,,,," );
        log.write("\n");
        m[0] = "9"; // mixbag...pass code 9 along with start,end of buffer to keep
        m[1] = keepers.deleteCharAt(keepers.length() - 1).toString();
    }
    
    return m;
    }
    
    
    public static String[] processFile(String infile, String map, String outfile, String isOverride, boolean isDebug, boolean reprocess, int comkey, int idxnbr) throws FileNotFoundException, IOException, ClassNotFoundException {
        
        if (isDebug) {
            GlobalDebug = true;
        }
        
        if (GlobalDebug)
        System.out.println("processFile: entering function with file :" + infile);
        
        String[] m = new String[]{"0",""};
        String[] c = null;  // control values to pass to map and log
        File file = new File(infile);
        // need to research removing ^M characters here with possible solution...replaceAll("\\r$", "");
        BufferedReader f = new BufferedReader(new FileReader(file));
         char[] cbuf = new char[(int) file.length()];
         int max = cbuf.length;
         f.read(cbuf); 
         f.close();
         
         if (GlobalDebug)
        System.out.println("File Character length: " + cbuf.length);
        
         // get comkey that associates all subsequent docs in this file to this log entry
         
         if (isOverride.isEmpty() && comkey == 0) {
          comkey = OVData.getNextNbr("edilog");
         }
         
         /* lets check file type to see if its x12 or edifact or csv or xml */
         // returns filetype, doctype
         String[] editype = getEDIType(cbuf, infile);
         String batchfile = "";
         c = initEDIControl();
                    c[0] = ""; // senderid
                    c[1] = editype[1];
                    c[21] = ""; // receiverid
                    c[2] = map;
                    c[3] = file.getName();
                    c[4] = ""; //isactrlnbr
                    c[8] = outfile;
                    c[9] = "10";
                    c[10] = "42";
                    c[11] = "0";
                    c[12] = isOverride;
                    c[13] = "";
                    c[16] = "0";  // initial idxnbr
                    c[22] = String.valueOf(comkey);
                    c[26] = file.getParent(); // indir
                    c[28] = editype[0];
                    c[30] = String.valueOf(isDebug);
                    
         // create batch file of incoming file
         
         // create batch file
              if (! reprocess) {
              int filenumber = OVData.getNextNbr("edifile");
              batchfile = "R" + String.format("%07d", filenumber); 
              c[24] = batchfile;
              Files.copy(file.toPath(), new File(cleanDirString(EDData.getEDIBatchDir()) + batchfile).toPath(), StandardCopyOption.REPLACE_EXISTING);
              } else {
                  batchfile = file.getName();
              }
          
          // start initial log entry for incoming file here 
          if (! reprocess) {
            EDData.writeEDIFileLog(c);      
          }
          
          
          
         if (editype[0].isEmpty()) {
           m = new String[]{"1","unknown file type: " + infile};
           EDData.writeEDILog(c, "error", "Unknown File Type: " + infile + " DOCTYPE:FILETYPE " + editype[1] + ":" + editype[0]); 
           return m; 
         } else if (editype[1].isEmpty()) {
            m = new String[]{"1","unknown doc type: " + infile};
           EDData.writeEDILog(c, "error", "Unknown Doc Type: " + infile + " DOCTYPE:FILETYPE " + editype[1] + ":" + editype[0]); 
           return m; 
         } else if (editype[1].equals("??")) {
            m = new String[]{"1","unknown doc type: " + infile};
           EDData.writeEDILog(c, "error", "Unknown Doc Type: " + infile + " DOCTYPE:FILETYPE " + editype[1] + ":" + editype[0]); 
           return m;   
         } else {
           EDData.writeEDILog(c, "info", "File Type Info: " + " DOCTYPE:FILETYPE " + editype[1] + ":" + editype[0]);
         }
             
        
       
        // if type is FF
        if (editype[0].equals("FF")) {
            String landmark = EDData.getEDIFFLandmark(editype[1]);
            ArrayList<String[]> ffdata = EDData.getEDIFFDataRules(editype[1]);
            
            StringBuilder segment = new StringBuilder();
            char segdelim = (char) Integer.valueOf("10").intValue(); 
           // for (int i = 0; i < cbuf.length; i++) {
           ArrayList<String> doc = new ArrayList<String>();
           for (int i = 0; i < cbuf.length; i++) {
                if (cbuf[i] == segdelim) {
                    doc.add(segment.toString());
                    segment.delete(0, segment.length());
                } else {
                    if (! (String.format("%02x",(int) cbuf[i]).equals("0d") || String.format("%02x",(int) cbuf[i]).equals("0a")) ) {
                        segment.append(cbuf[i]);
                    } 
                }
            }
           // loop through file and count each Doc landmarks of FF type in case of concatenation
           Map<Integer, ArrayList<String>> docRegister = new HashMap<Integer, ArrayList<String>>();
           Map<Integer, Integer[]> docPosition = new HashMap<Integer, Integer[]>();
          
           ArrayList<String> singledoc = new ArrayList<String>();
           int doccount = 0;
           int linecount = 0;
           int startline = 0;
           for (String x : doc) {
               linecount++;
               if (x.startsWith(landmark) && linecount == 1) {
                   singledoc.add(x);  
                   doccount++;
                   startline = linecount;
                   if (GlobalDebug)
                   System.out.println("processFF: linecount = " + linecount + "/ landmark=" + landmark );
               }
               if (x.startsWith(landmark) && linecount > 1) {
                   docRegister.put(doccount, singledoc);
                   docPosition.put(doccount, new Integer[]{0,cbuf.length,startline,linecount - 1});
                   singledoc.clear();
                   singledoc.add(x);
                   doccount++;
                   if (GlobalDebug)
                   System.out.println("processFF: linecount = " + linecount + "/ landmark=" + landmark );
             
               }
               if (! x.startsWith(landmark)) {
                   singledoc.add(x);
               }
               
           }
           docRegister.put(doccount, singledoc);
           docPosition.put(doccount, new Integer[]{0,cbuf.length,startline,linecount - 1});       
           
           processFF(docRegister, docPosition, c, ffdata, idxnbr);
           
        }
        
        // if file type is x12 
        if (editype[0].equals("X12")) {
            // create batch file and assign to control replacing infile name 
         // now lets see how many ISAs and STs within those ISAs and write character positions of each
         
         Map<Integer, Object[]> ISAmap = createIMAP(cbuf, c, "", "", "", "");
      
             if (isOverride.isEmpty()) {
             processX12(ISAmap, cbuf, batchfile, idxnbr);
             } else {
              processX12(ISAmap, cbuf, infile, idxnbr);   
             }
             
         }  // if x12
        
        // if file type is EDIFACT 
        if (editype[0].equals("UNE")) {
            // create batch file and assign to control replacing infile name 
         // now lets see how many ISAs and STs within those ISAs and write character positions of each
         
         Map<Integer, Object[]> UNBmap = createMAPUNE(cbuf, c, "", "", "", "");
      
             if (isOverride.isEmpty()) {
             processUNE(UNBmap, cbuf, batchfile, idxnbr);
             } else {
              processUNE(UNBmap, cbuf, infile, idxnbr);   
             }
             
         }  // if EDIFACT
        
        
        // if type is CSV
        if (editype[0].equals("CSV")) {
            ArrayList<String[]> ffdata = EDData.getEDIFFDataRules(editype[1]);
            
            StringBuilder segment = new StringBuilder();
            char segdelim = (char) Integer.valueOf("10").intValue(); 
           // for (int i = 0; i < cbuf.length; i++) {
           ArrayList<String> doc = new ArrayList<String>();
           for (int i = 0; i < cbuf.length; i++) {
                if (cbuf[i] == segdelim) {
                    doc.add(segment.toString());
                    segment.delete(0, segment.length());
                } else {
                    if (! (String.format("%02x",(int) cbuf[i]).equals("0d") || String.format("%02x",(int) cbuf[i]).equals("0a")) ) {
                        segment.append(cbuf[i]);
                    } 
                }
            }
           // loop through file and count each Doc landmarks of FF type in case of concatenation
           Map<Integer, ArrayList<String>> docRegister = new HashMap<Integer, ArrayList<String>>();
           Map<Integer, Integer[]> docPosition = new HashMap<Integer, Integer[]>();
          
           ArrayList<String> singledoc = new ArrayList<String>();
           int doccount = 0;
           int linecount = 0;
           int startline = 0;
           for (String x : doc) {
               linecount++;
               singledoc.add(x);
           }
           docRegister.put(doccount, singledoc);
           docPosition.put(doccount, new Integer[]{0,cbuf.length,startline,linecount - 1});       
           
           processCSV(docRegister, docPosition, c, ffdata, idxnbr);
           
        }
        
         // if type is JSON
        if (editype[0].equals("JSON")) {
            
            StringBuilder jsonstring = new StringBuilder();
            for (int i = 0; i < cbuf.length; i++) {
                jsonstring.append(cbuf[i]);
            }
            
            if (isOverride.isEmpty()) {
             processJSON(jsonstring.toString(), c, batchfile, idxnbr);
             } else {
              processJSON(jsonstring.toString(), c, infile, idxnbr);   
             }
        }
        
        
         // if type is XML
        if (editype[0].equals("XML")) {
            
            StringBuilder xmlstring = new StringBuilder();
            for (int i = 0; i < cbuf.length; i++) {
                xmlstring.append(cbuf[i]);
            }
            
            if (isOverride.isEmpty()) {
                processXML(xmlstring.toString(), c, batchfile, idxnbr);
            } else {
                processXML(xmlstring.toString(), c, infile, idxnbr);   
            } 
        }
        
       EDData.updateEDIFileLogStatus(c[22], c[0], editype[0], editype[1]);   
       
       // if hanoi is not empty...must have some multi-doc envelopes to create
       packageEnvelopes();
       
       return m;  
    }
    
    public static Map<Integer, Object[]> createMAPUNE(char[] cbuf, String[] c, String infile, String outfile, String map, String isOverride) {
         Map<Integer, Object[]> UNBmap = new HashMap<Integer, Object[]>();
         String[] m = new String[]{"0",""};
         int start = 0;
         int end = 0;
         int unbcount = 0;
         int gscount = 0;
         int unhcount = 0;
         int unhstart = 0;
         int untstart = 0;
         String ed_escape = "";
         String sd_escape = "";
         String ud_escape = "";
         int gsstart = 0;
         String doctype = "";
         String docid = "";
         ArrayList<String> unbList = new ArrayList<String>();
          
          char e = '+';
          char s = '\'';
          char u = ':';
          char x = '?'; // escape char
          int mark = 0;
           
           Map<Integer, ArrayList> stse_hash = new HashMap<Integer, ArrayList>();
           ArrayList<Object> docs = new ArrayList<Object>();
          char[] cbufx = null; // placeholder for revised cbuf (remove of UNA if present)
          boolean isUNA = false;
            for (int i = 0; i < cbuf.length; i++) {
             if (((i+11) <= cbuf.length) && cbuf[i] == 'U' && cbuf[i+1] == 'N' && cbuf[i+2] == 'A' 
                        && (cbuf[i+9] == 'U') && (cbuf[i+10] == 'N') && (cbuf[i+11] == 'B')) {
                // must be UNA service tag of EDIFACT
                e = cbuf[i + 4];
                s = cbuf[i + 8];
                u = cbuf[i + 3];
                x = cbuf[i + 6];
                break;                
             }   
            }
            
            if (isUNA) {
              cbufx = Arrays.copyOfRange(cbuf, 9, cbuf.length);  
            } else {
              cbufx = cbuf;  
            }
            
            for (int i = 0; i < cbufx.length; i++) {
                
             if ( ((i+4) <= cbufx.length) && cbufx[i] == 'U' && cbufx[i+1] == 'N' && cbufx[i+2] == 'B' 
                        && cbufx[i+3] == e ) {
                   
                    mark = i;
            
                    ed_escape = escapeDelimiter(String.valueOf(e));
                    sd_escape = escapeDelimiter(String.valueOf(s));
                    ud_escape = escapeDelimiter(String.valueOf(u));
                    start = i;
                    unbList.add("UNB" + ":" + String.valueOf(i) + ":" + String.format("%02x",(int) s) );
                    unbcount++;
                    // find end of UNB
                    int unb_end = 0;
                    for (unb_end = i; unb_end < 100; unb_end++) {
                        if (cbufx[unb_end] == s) {
                            break;
                        }
                    }
                    StringBuilder sb_obj = new StringBuilder();
                    for(int v = i; v <= (unb_end - 1); v++) {
                        sb_obj.append(cbufx[v]);
                    }
                    
                    String[] unb = sb_obj.toString().split(ed_escape);
                  
                   
                    c[0] = unb[2].split(":")[0].trim(); // senderid
                    c[21] = unb[3].split(":")[0].trim(); // receiverid
                    c[2] = map;
                    c[3] = infile;
                    c[4] = unb[5]; //isactrlnbr
                    c[8] = outfile;
                    c[9] = String.valueOf((int) s);
                    c[10] = String.valueOf((int) e);
                    c[11] = String.valueOf((int) u);
                    c[12] = isOverride;
                    c[13] = String.join("+", unb);
                    c[15] = "0"; // inbound
                    
                }
                // UNG may not be there...needs revisiting
                if (i > 1 && cbufx[i-1] == s && cbufx[i] == 'U' && cbufx[i+1] == 'N' && cbufx[i+2] == 'G') {
                    gscount++;
                    unbList.add("UNG" + ":" + String.valueOf(i));
                    gsstart = i;
                    String[] gs = new String(cbufx, gsstart, 90).split(ed_escape);
                    c[5] = gs[6]; // gsctrlnbr
                    gs[8] = gs[8].split(sd_escape)[0];
                    c[14] = String.join(String.valueOf(e), Arrays.copyOfRange(gs, 0, 9));
                    
                }
                
                if (i > 1 && cbufx[i-1] == s && cbufx[i] == 'U' && cbufx[i+1] == 'N' && cbufx[i+2] == 'H') {
                   
                    unhcount++;
                    unbList.add("UNH" + ":" + String.valueOf(i));
                    unhstart = i;
                    int unh_end = 0;
                    for (unh_end = i; unh_end < (i + 40); unh_end++) {
                        if (cbufx[unh_end] == s) {
                            break;
                        }
                    }
                    String[] st = new String(cbufx, i, unh_end - 1).split(ed_escape);
                    doctype = st[2].split(ud_escape)[0]; // doctype
                    docid = st[1]; //docID  
                    // convert actual doctype to blueseer doctype
                    doctype = getBSDocTypeFromStds(doctype);
                    c[1] = doctype; // override standard doc type for bs type
                   // System.out.println(c[0] + "/" + c[1] + "/" + c[4] + "/" + c[5]);
                } 
                if (i > 1 && cbufx[i-1] == s && cbufx[i] == 'U' && cbufx[i+1] == 'N' && cbufx[i+2] == 'T') {
                    unbList.add("UNT" + ":" + String.valueOf(i));
                    untstart = i;
                    // add to hash if hash doesn't exist or insert into hash
                    docs.add(new Object[] {new Integer[] {unhstart, untstart}, doctype, docid});
                    // painful reminder that you have to create copy of array at instance in time
                    ArrayList copydocs = new ArrayList(docs);
                    stse_hash.put(unbcount, copydocs);
                }
                if (i > 1 && cbufx[i-1] == s && cbufx[i] == 'U' && cbufx[i+1] == 'N' && cbufx[i+2] == 'Z') {
                    int unz_end = 0;
                    for (unz_end = i; unz_end < (i + 40); unz_end++) {
                        if (cbufx[unz_end] == s) {
                            break;
                        }
                    }
                    end = unz_end;
                    // now add to ISAmap
                   HashMap<Integer,ArrayList> mycopy = new HashMap<Integer,ArrayList>(stse_hash);
                  UNBmap.put(unbcount, new Object[] {start, end, (int) s, (int) e, (int) u, mycopy, c});
                    unbList.add("UNZ" + ":" + String.valueOf(i));
                    unhcount = 0;
                    docs.clear();
                    stse_hash.remove(unbcount);
                    
                } 
            } // for cbufx
            
            return UNBmap;
    }
        
    public static Map<Integer, Object[]> createIMAP(char[] cbuf, String[] c, String infile, String outfile, String map, String isOverride) {
         Map<Integer, Object[]> ISAmap = new HashMap<Integer, Object[]>();
         String[] m = new String[]{"0",""};
         int start = 0;
         int end = 0;
         int isacount = 0;
         int isactrl = 0;
         int gscount = 0;
         int stcount = 0;
         int ststart = 0;
         int sestart = 0;
         String ed_escape = "";
         String sd_escape = "";
         int gsstart = 0;
         String doctype = "";
         String docid = "";
         ArrayList<String> isaList = new ArrayList<String>();
          
          char e = 0;
          char s = 0;
          char u = 0;
          int mark = 0;
          
           
           Map<Integer, ArrayList> stse_hash = new HashMap<Integer, ArrayList>();
           ArrayList<Object> docs = new ArrayList<Object>();
          
            for (int i = 0; i < cbuf.length; i++) {
             if ( ((i+103) <= cbuf.length) && cbuf[i] == 'I' && cbuf[i+1] == 'S' && cbuf[i+2] == 'A' 
                        && (cbuf[i+103] == cbuf[i+3]) && (cbuf[i+103] == cbuf[i+6]) ) {
                    e = cbuf[i+103];
                    u = cbuf[i+104];
                    s = cbuf[i+105];
                    mark = i;
                    // lets bale if not proper ISA envelope.....unless the 106 is carriage return...then ok
                /*
                if (i == mark && cbuf[mark+106] != 'G' && cbuf[mark+107] != 'S' && ! String.format("%02x",(int) cbuf[mark+106]).equals("0a")) {
                        return m = new String[]{"1","malformed envelope"};
                }
                */
                    ed_escape = escapeDelimiter(String.valueOf(e));
                    sd_escape = escapeDelimiter(String.valueOf(s));
                    if (String.format("%02x",(int) cbuf[i+105]).equals("0d") && String.format("%02x",(int) cbuf[i+106]).equals("0a"))
                        s = cbuf[i+106];
                    start = i;
                    isaList.add("ISA" + ":" + String.valueOf(i) + ":" + String.format("%02x",(int) s) );
                    isacount++;
                    String[] isa = new String(cbuf, i, 105).split(ed_escape);
                    isactrl = Integer.valueOf(isa[13]);
                    
                     // set control
                   
                    c[0] = isa[6].trim(); // senderid
                    c[21] = isa[8].trim(); // receiverid
                    c[2] = map;
                    c[3] = infile;
                    c[4] = isa[13]; //isactrlnbr
                    c[8] = outfile;
                    c[9] = String.valueOf((int) s);
                    c[10] = String.valueOf((int) e);
                    c[11] = String.valueOf((int) u);
                    c[12] = isOverride;
                    c[13] = new String(cbuf,i,105);
                    c[15] = "0"; // inbound
                    
                }
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'G' && cbuf[i+1] == 'S') {
                    gscount++;
                    isaList.add("GS" + ":" + String.valueOf(i));
                    gsstart = i;
                    String[] gs = new String(cbuf, gsstart, 90).split(ed_escape);
                    c[5] = gs[6]; // gsctrlnbr
                    gs[8] = gs[8].split(sd_escape)[0];
                    c[0] = gs[2]; // override ISA receiverid with GS senderid  TEV 20230105
                    c[21] = gs[3]; // override ISA receiverid with GS receiverid  TEV 20230105
                    c[14] = String.join(String.valueOf(e), Arrays.copyOfRange(gs, 0, 9));
                    
                }
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'S' && cbuf[i+1] == 'T') {
                   
                    stcount++;
                    isaList.add("ST" + ":" + String.valueOf(i));
                    ststart = i;
                    
                    String[] st = new String(cbuf, i, 16).split(ed_escape);
                     
                    doctype = st[1]; // doctype
                    // convert actual doctype to blueseer doctype
                    doctype = getBSDocTypeFromStds(doctype);
                    
                    docid = st[2].split(sd_escape)[0]; //docID  // to separate 2nd element of ST because grabbing 16 characters in buffer
                   
                   // System.out.println(c[0] + "/" + c[1] + "/" + c[4] + "/" + c[5]);
                } 
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'S' && cbuf[i+1] == 'E') {
                    isaList.add("SE" + ":" + String.valueOf(i));
                    sestart = i;
                    // add to hash if hash doesn't exist or insert into hash
                    docs.add(new Object[] {new Integer[] {ststart, sestart}, doctype, docid});
                    // painful reminder that you have to create copy of array at instance in time
                    ArrayList copydocs = new ArrayList(docs);
                    stse_hash.put(isacount, copydocs);
                }
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'I' && cbuf[i+1] == 'E' && cbuf[i+2] == 'A') {
                    end = i + 14 + Integer.valueOf(String.valueOf(gscount).length()) + 1;
                    // now add to ISAmap
                   HashMap<Integer,ArrayList> mycopy = new HashMap<Integer,ArrayList>(stse_hash);
                  ISAmap.put(isacount, new Object[] {start, end, (int) s, (int) e, (int) u, mycopy, c});
                    isaList.add("IEA" + ":" + String.valueOf(i));
                    stcount = 0;
                    docs.clear();
                    stse_hash.remove(isacount);
                    
                } 
            }
            
            return ISAmap;
    }
    
    public static String[] getEDIType(char[] cbuf, String filename) {
    
       
        
    String[] type = new String[]{"",""};
    // type = filetype, doctype
    String[] filenamesplit = null;
        
    if (cbuf == null || cbuf.length < 4) {
            return type;
    }
    
    if (GlobalDebug) {  
     System.out.println("getEDIType: checking for X12 type... " + filename );
    }
    
    int gs01Start = 109;
    StringBuilder sb = new StringBuilder();
    StringBuilder gs01 = new StringBuilder();
    sb.append(cbuf, 0, 3);
    if (sb.toString().equals("ISA")) {
         if ((String.format("%02x",(int) cbuf[106]).equals("0a")) ) {
                    gs01Start = 110;
         }
         gs01.append(cbuf,gs01Start,2);
         type[0] = "X12";
         type[1] = EDData.getBSDocTypeFromStds(EDData.getEDIDocTypeFromStds(gs01.toString())); // get from GS value...then convert to bsdoc
         if (GlobalDebug)
               System.out.println("getEDIType X12 type in/out: " + gs01 + "/" + type[1] );

         return type;
    }
         
    if (GlobalDebug) {
     System.out.println("Fell through...Not X12 " + filename );    
     System.out.println("getEDIType: checking for EDIFACT (UNE) type... " + filename );
    } 
    
    // check EDIFACT (UNE)
    String una_unb = new String(cbuf,9,3); // file may be prefixed with UNA service tag...EX: UNAxxxxx'UNB
    if (sb.toString().equals("UNB") || ( sb.toString().equals("UNA") && una_unb.equals("UNB")) ) {
         type[0] = "UNE";
         type[1] = "";
         // 39 = '
         // 58 = :
         // 43 = +
         // 63 = ?
         char segdelim = (char) Integer.valueOf("39").intValue(); 
         if (sb.toString().equals("UNA")) {
             segdelim = cbuf[8];
         }
        StringBuilder segment = new StringBuilder();
        ArrayList<String> doc = new ArrayList<String>();
        for (int i = 0; i < cbuf.length; i++) {
            if (cbuf[i] == segdelim) {
                doc.add(segment.toString());
                segment.delete(0, segment.length());
            } else {
                segment.append(cbuf[i]);
            }
        } 
        
        docloop:
        for (String s : doc) {
            if (s.startsWith("UNH")) {
                String[] x = s.split("\\+",-1);
                if (x.length > 2) {
                type[1] = x[2].substring(0,6);   // UNH+72+ORDERS:D:97A:UN'
                type[1] = EDData.getBSDocTypeFromStds(type[1]);
                break docloop;
                }
            }
        }
        
        
        return type;
    } // if edifact UNE
    
    if (GlobalDebug) {
     System.out.println("Fell through...Not EDIFACT " + filename );    
     System.out.println("getEDIType: checking for JSON... " + filename );
    }
    
    // lets check for JSON
    char firstchar = 0;
    char lastchar = 0;
    for (int i = 0; i < cbuf.length; i++) {
        if(! Character.isWhitespace(cbuf[i])){
            firstchar = cbuf[i];
            break;
        }
    }  
    for (int i = cbuf.length - 1; i >= 0; i--) {
        if(! Character.isWhitespace(cbuf[i])){
            lastchar = cbuf[i];
            break;
        }
    }
    if (firstchar == '{' && lastchar == '}') {
        StringBuilder jsonstring = new StringBuilder();
        for (int i = 0; i < cbuf.length; i++) {
            jsonstring.append(cbuf[i]);
        }
        String jsontype = getDocTypeJson(jsonstring.toString());
        if (jsontype == null) {
            System.out.println("Possible malformed JSON " + filename );
        }
        if (jsontype != null && ! jsontype.isEmpty()) {
        type[0] = "JSON";
        type[1] = jsontype;
        return type;
        } 
    }
    
    if (GlobalDebug) {
     System.out.println("Fell through...Not JSON " + filename );    
     System.out.println("getEDIType: checking for XML... " + filename );
    }
    
    // lets check for JSON...using firstchar and lastchar test in json above
    StringBuilder xmlcheck = new StringBuilder();
    for (int i = 0; i < cbuf.length; i++) {
        if(! Character.isWhitespace(cbuf[i])){
            xmlcheck.append(cbuf[i]);
            xmlcheck.append(cbuf[i+1]);
            xmlcheck.append(cbuf[i+2]);
            xmlcheck.append(cbuf[i+3]);
            xmlcheck.append(cbuf[i+4]);
            break;
        }
    }
    if (xmlcheck.toString().equals("<?xml")) {
        StringBuilder xmlstring = new StringBuilder();
        for (int i = 0; i < cbuf.length; i++) {
            xmlstring.append(cbuf[i]);
        }
        type[0] = "XML";
        type[1] = getDocTypeXML(xmlstring.toString());
        return type;
    }
    
    
    if (GlobalDebug) {
     System.out.println("Fell through...Not XML " + filename );    
     System.out.println("getEDIType: checking for FF,CSV type via Doc Recognition Rules... " + filename );
    }  
    // let's try flatfile....let's create arraylist of segments based on assumed newline delimiter     
    StringBuilder segment = new StringBuilder();
    char segdelim = (char) Integer.valueOf("10").intValue(); 
    ArrayList<String> doc = new ArrayList<String>();
    for (int i = 0; i < cbuf.length; i++) {
        if (cbuf[i] == segdelim) {
            doc.add(segment.toString());
            segment.delete(0, segment.length());
        } else {
            if (! (String.format("%02x",(int) cbuf[i]).equals("0d") || String.format("%02x",(int) cbuf[i]).equals("0a")) ) {
                segment.append(cbuf[i]);
            } 
        }
    }   
    // now run doc array through rules engine to determine doctype
    // match with the higher selection rules count wins
    HashMap<String, Integer> matches = new HashMap<String, Integer>();
    int k = 0; 
    boolean match = false;
    HashMap<String, ArrayList<String[]>> rules = EDData.getEDIFFSelectionRules();
    for (String s : doc) {
        k++;
        int rulecount = 0;
        int matchcount = 0;
        for (Map.Entry<String, ArrayList<String[]>> z : rules.entrySet()) {
            String key = z.getKey();
            ArrayList<String[]> v = z.getValue();
            // row, column, length, value, rectype, tag, xpath
            rulecount = 0;
            matchcount = 0;
            for (String[] r : v) {
                rulecount++;

             if (GlobalDebug)   
             System.out.println("getEDIType: FF/CSV rules: " + key + "/" + r[0] + "/" + r[1] + "/" + r[2] + "/" + r[3] + "/" + r[4]);

              if (r[4].equals("fixed")) {             
                if (Integer.valueOf(r[0]) == k) { // row check
                    if (((Integer.valueOf(r[1]) - 1) + Integer.valueOf(r[2])) > s.length()) {
                        continue;
                    }
                    if (s.substring(Integer.valueOf(r[1]) - 1, ((Integer.valueOf(r[1]) - 1) + Integer.valueOf(r[2]))).trim().equals(r[3])) {
                      matchcount++;
                    }
                }
              } else { // must be delimited rule...delimiter in r[2]
                if (Integer.valueOf(r[0]) == k) { // row check
                    String[] delarr = s.split(delimConvertIntToStr(r[10]), -1);
                    if (delarr.length >= Integer.valueOf(r[1])) {
                        if (delarr[Integer.valueOf(r[1]) - 1].trim().equals(r[3])) {
                          matchcount++;  
                        }
                    }
                }  
              }
            } // for inner loop r of v
            if ( (matchcount != 0) && (matchcount == rulecount)) {
                matches.put(key, matchcount);
            }
        } // for all selection rules
        
        // now get highest match count entry...if tie...first record in order wins
        int t = 0;
        String v = "";
        String vdoctype = "";
        for (Map.Entry<String, Integer> z : matches.entrySet()) {
            match = true;
            if (z.getValue() > t) {
                t = z.getValue();
                v = z.getKey();
                vdoctype = getEDIFFDocType(z.getKey());
            }
        }
        
        // assign type with highest match
        type[1] = v;
        if (vdoctype.contains("xml")) {
         type[0] = "XML";   
        } else if (vdoctype.contains("csv")) {
         type[0] = "CSV";   
        } else {
         type[0] = "FF";   
        }
        
        if (match) {
            return type; // break out
        }
        
    }  //for each Segment in Doc  FF check
    
    
    if (GlobalDebug && type[0].isBlank()) {
     System.out.println("Fell through...Unknown File type!! " + filename );  
    }
    return type;
}
    
    public static String getDocTypeJson(String x) {
        String doctype = "";
        JsonNode jsonNode = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            jsonNode = mapper.readTree(x);
        } catch (JsonProcessingException ex) {
            edilog(ex);
        }
        
        if (jsonNode == null) {
            return null;
        }
        if (jsonNode.isEmpty()) {
            return "";
        }
        
      
        int k = 0; 
        boolean match = false;
        HashMap<String, ArrayList<String[]>> rules = EDData.getEDIFFSelectionRules();
                
        k++;
        int rulecount = 0;
        int matchcount = 0;
        
        for (Map.Entry<String, ArrayList<String[]>> z : rules.entrySet()) {
            String key = z.getKey();
            ArrayList<String[]> v = z.getValue();
            // row, column, length, value, rectype, tag
            rulecount = 0;
            matchcount = 0;
            
            for (String[] r : v) {
            //bail (next loop) if r[5] tag does not begin with /  ...otherwise jsonNode.at will throw jackson illegalArgument
                if (! r[6].startsWith("/")) {
                    continue;
                }  
                if (r[4].equals("json")) {
                    rulecount++;
                    JsonNode nameNode = jsonNode.at(r[6]);
                    if (r[3].equals(nameNode.asText()) || (r[3].equals("*") && ! nameNode.getNodeType().toString().equals("MISSING"))) {
                      matchcount++;
                    }
                } 
            }
            if ( (matchcount != 0) && (matchcount == rulecount)) {
                doctype = key;
                break;
            }
        } // for each rule
        
        return doctype;
    }
    
    public static String getDocTypeXML(String x) {
        // InputStream inputStream = new ByteArrayInputStream(String.join(System.lineSeparator(), Arrays.asList(strings)).getBytes(StandardCharsets.UTF_8));
        String doctype = "";
        HashMap<String, ArrayList<String[]>> rules = EDData.getEDIFFSelectionRules();
        int rulecount = 0;
        int matchcount = 0;
        InputStream is = new ByteArrayInputStream(x.getBytes(StandardCharsets.UTF_8));
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
            Document document = docBuilder.parse(is);
            
            // gets all nodes
            /*
            NodeList nodeList = document.getElementsByTagName("*");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    // do something with the current element
                    System.out.println("NODE: " + node.getNodeName());
                }
            }
            */
             for (Map.Entry<String, ArrayList<String[]>> z : rules.entrySet()) {
            String key = z.getKey();
            ArrayList<String[]> v = z.getValue();
            // row, column, length, value, rectype, tag
            rulecount = 0;
            matchcount = 0;
            
            for (String[] r : v) {
            //bail (next loop) if r[5] tag does not begin with /  ...otherwise jsonNode.at will throw jackson illegalArgument
            if (! r[6].startsWith("/")) {
                continue;
            }  
              if (r[4].equals("xml")) {
                rulecount++;
                XPath xPath = XPathFactory.newInstance().newXPath();
                String expression = r[6];
                
                Node node = (Node) xPath.compile(expression).evaluate(document, XPathConstants.NODE);
                                
                if (node != null && ( r[3].equals(node.getTextContent()) || r[3].equals("*")) ) {  // only works for node content...not not attributes
                  matchcount++;
                }
              } 
            }
            if ( (matchcount != 0) && (matchcount == rulecount)) {
                doctype = key;
                break;
            }
        } // for each rule
            
        } catch (ParserConfigurationException ex) {
            edilog(ex);
        } catch (SAXException ex) {
            edilog(ex);
        } catch (IOException ex) {
            edilog(ex);
        } catch (XPathExpressionException ex) {
            edilog(ex);
        }
        
        return doctype;
    }
    
    public static String[] getFileInfo(ArrayList<String> docs, String[] c, ArrayList<String[]> tags) {
        // hm is list of variables and coordinates to find variables (f,m) r,c,l
        // f = fixed, m = regex, r = row, c = column, l = length
        String[] x = new String[]{"","","",""}; // doctype, rcvid, parentpartner, sndid
        int i = 0;
        /*
        for (String[] t : tags ) {
        edid_tag,edid_rectype,edid_valuetype,edid_row,edid_col,edid_length,edid_regex,edid_value
            System.out.println("here:" + t[0] + "/" + t[1] + "/" + t[2] + "/" + t[3] + "/" + t[4] + "/" + t[5] + "/" + t[6]);
        }
        */
        x[0] = getEDIFFDocType(c[1]); //overriding original recognition record edd_id with type; further overriden with tag below as necessary
        for (String s : docs) {
            i++;
            for (String[] t : tags ) {
                if (t[0].toLowerCase().equals("doctype")) {
                    if (t[2].toLowerCase().equals("constant")) {
                        x[0] = t[7];
                    } else {
                        if (t[1].toLowerCase().equals("delimited") && i == Integer.valueOf(t[3])) {
                        String[] delarr = s.split(delimConvertIntToStr(t[10]), -1);
                            if (delarr.length >= Integer.valueOf(t[4])) {
                                x[0] = delarr[Integer.valueOf(t[4]) - 1].trim();
                            }
                        }
                        if (t[1].toLowerCase().equals("fixed") && i == Integer.valueOf(t[3])) {
                        x[0] = s.substring(Integer.valueOf(t[4]) - 1,(Integer.valueOf(t[4]) - 1 + Integer.valueOf(t[5]))).trim();
                        }
                        if (t[1].toLowerCase().equals("regex") && s.matches(t[6])) {
                        x[0] = s.substring(Integer.valueOf(t[4]) - 1,(Integer.valueOf(t[4]) - 1 + Integer.valueOf(t[5]))).trim();
                        }
                    }
                    
                }
                if (t[0].toLowerCase().equals("rcvid")) {
                    if (t[2].toLowerCase().equals("constant")) {
                        x[1] = t[7];
                    } else {
                        if (t[1].toLowerCase().equals("delimited") && i == Integer.valueOf(t[3])) {
                        String[] delarr = s.split(delimConvertIntToStr(t[10]), -1);
                            if (delarr.length >= Integer.valueOf(t[4])) {
                                x[1] = delarr[Integer.valueOf(t[4]) - 1].trim();
                            }
                        }
                        if (t[1].toLowerCase().equals("fixed") && i == Integer.valueOf(t[3])) {
                        x[1] = s.substring(Integer.valueOf(t[4]) - 1,(Integer.valueOf(t[4]) - 1 + Integer.valueOf(t[5]))).trim();
                        }
                        if (t[1].toLowerCase().equals("regex") && s.matches(t[6])) {
                        x[1] = s.substring(Integer.valueOf(t[4]) - 1,(Integer.valueOf(t[4]) - 1 + Integer.valueOf(t[5]))).trim();
                        }
                    }
                    
                }
                 if (t[0].toLowerCase().equals("sndid")) {
                    if (t[2].toLowerCase().equals("constant")) {
                        x[3] = t[7];
                    } else {
                        if (t[1].toLowerCase().equals("delimited") && i == Integer.valueOf(t[3])) {
                        String[] delarr = s.split(delimConvertIntToStr(t[10]), -1);
                            if (delarr.length >= Integer.valueOf(t[4])) {
                                x[3] = delarr[Integer.valueOf(t[4]) - 1].trim();
                            }
                        }
                        if (t[1].toLowerCase().equals("fixed") && i == Integer.valueOf(t[3])) {
                        x[3] = s.substring(Integer.valueOf(t[4]) - 1,(Integer.valueOf(t[4]) - 1 + Integer.valueOf(t[5]))).trim();
                        }
                        if (t[1].toLowerCase().equals("regex") && s.matches(t[6])) {
                        x[3] = s.substring(Integer.valueOf(t[4]) - 1,(Integer.valueOf(t[4]) - 1 + Integer.valueOf(t[5]))).trim();
                        }
                    }
                    if (! x[3].isEmpty()) {
                        // look up parent partner
                        x[2] = EDData.getEDIPartnerFromAlias(x[3]);
                    }
                }
                
            }
            
            
            if (! x[0].isEmpty() && ! x[1].isEmpty() && ! x[3].isEmpty()) {
                break;
            }
        }
        if (GlobalDebug) {
            System.out.println("getFileInfo: doc/rcv/partner/snd " + x[0] + "/" + x[1] + "/" + x[2] + "/" + x[3]);
        }
        return x;
    }
    
    public static String[] getTagInfo(String content, String[] c) {
        // hm is list of variables and coordinates to find variables (f,m) r,c,l
        // f = fixed, m = regex, r = row, c = column, l = length
        String[] x = new String[]{"","","",""}; // doctype, rcvid, parentpartner, sndid
        int i = 0;
      
        
        
        x[0] = getEDIFFDocType(c[1]); //overriding original recognition record edd_id with type; further overriden with tag below as necessary
      
        JsonNode jsonNode = null;
        if (c[28].toUpperCase().equals("JSON")) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                jsonNode = mapper.readTree(content);
            } catch (JsonProcessingException ex) {
                edilog(ex);
            }
        }
        
        Document document = null;
        if (c[28].toUpperCase().equals("XML")) {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder;
            InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            try {
                docBuilder = docBuilderFactory.newDocumentBuilder();
                document = docBuilder.parse(is);
            } catch (ParserConfigurationException ex) {
                edilog(ex);
            } catch (SAXException ex) {
                edilog(ex);
            } catch (IOException ex) {
                edilog(ex);
            }
        }
        
        ArrayList<String[]> tags = EDData.getEDIFFDataRules(c[1]);
        
        if (GlobalDebug) {
            System.out.println("getTagInfo: c[1]/tags count: " + c[1] + "/" + tags.size());
        }
        
       
            for (String[] t : tags ) {
                
                if (GlobalDebug) {
                System.out.println("getTagInfo: tag array merge: " + String.join(",", t));
                }
                
                if (t[1].toLowerCase().equals("json") && ! t[8].startsWith("/")) {
                continue;
                }
                
                if (t[0].toLowerCase().equals("doctype")) {
                    if (t[2].toLowerCase().equals("constant")) {
                        x[0] = t[7];
                    } else {                        
                        if (t[1].toLowerCase().equals("json")) {
                            JsonNode nameNode = jsonNode.at(t[8]);
                            x[0]= nameNode.asText();
                        }
                        if (t[1].toLowerCase().equals("xml")) {
                            XPath xPath = XPathFactory.newInstance().newXPath();
                            Node node = null;
                            try {
                                node = (Node) xPath.compile(t[8]).evaluate(document, XPathConstants.NODE);
                            } catch (XPathExpressionException ex) {
                                edilog(ex);
                            }
                            if (node != null) {
                                x[0] = node.getTextContent();
                            }
                        }
                    }
                }
                if (t[0].toLowerCase().equals("rcvid")) {
                    if (t[2].toLowerCase().equals("constant")) {
                        x[1] = t[7];
                    } else {                        
                        if (t[1].toLowerCase().equals("json")) {
                            JsonNode nameNode = jsonNode.at(t[8]);
                            x[1] = nameNode.asText();
                        }
                        if (t[1].toLowerCase().equals("xml")) {
                            XPath xPath = XPathFactory.newInstance().newXPath();
                            Node node = null;
                            try {
                                node = (Node) xPath.compile(t[8]).evaluate(document, XPathConstants.NODE);
                            } catch (XPathExpressionException ex) {
                                edilog(ex);
                            }
                            if (node != null) {
                                x[1] = node.getTextContent();
                            }
                        }
                    }
                }
                 if (t[0].toLowerCase().equals("sndid")) {
                    if (t[2].toLowerCase().equals("constant")) {
                        x[3] = t[7];
                    } else {
                        if (t[1].toLowerCase().equals("json")) {
                            JsonNode nameNode = jsonNode.at(t[8]);
                            x[3] = nameNode.asText();
                        }
                        if (t[1].toLowerCase().equals("xml")) {
                            XPath xPath = XPathFactory.newInstance().newXPath();
                            Node node = null;
                            try {
                                node = (Node) xPath.compile(t[8]).evaluate(document, XPathConstants.NODE);
                            } catch (XPathExpressionException ex) {
                                edilog(ex);
                            }
                            if (node != null) {
                                x[3] = node.getTextContent();
                            }
                        }
                    }
                    if (! x[3].isEmpty()) {
                        // look up parent partner
                        x[2] = EDData.getEDIPartnerFromAlias(x[3]);
                    }
                }
            }
        
        if (GlobalDebug) {
            System.out.println("getTagInfo: doc/rcv/partner/snd " + x[0] + "/" + x[1] + "/" + x[2] + "/" + x[3]);
        }
        return x;
    }
    
    public static void processXML(String content, String[] c, String filename, int idxnbr)   {
    
        int callingidxnbr = idxnbr;      
        String eddmid = c[1]; // c[1] initially contains the edi_doc record id during eddm lookup
        String ifs = getEDIFFSubType(eddmid);
        ArrayList<String[]> messages = new ArrayList<String[]>();
        ArrayList<String> doc = new ArrayList<String>();
        doc.add(content);
        
        messages.add(new String[]{"info","processXML: " + c[1]});
        
            if (GlobalDebug)
            System.out.println("processing XML: " + c[1]);
            
            String[] x = getTagInfo(content, c);  // doctype, rcvid, parentpartner, sndid
            
            
            
             if (x[2].isEmpty()) {
                messages.add(new String[]{"error", "unable to determine parent partner from content: (doctype,rcvid,parent,sndid) (" + x[0] + "," + x[1] + "," + x[2] + "," + x[3] + ")"});
                EDData.writeEDILogMulti(c, messages);
                messages.clear(); 
                return;  
             }
            
            messages.add(new String[]{"info","XML: (doctype,rcvid,parent,sndid) (" + x[0] + "," + x[1] + "," + x[2] + "," + x[3] + ")"});
            messages.add(new String[]{"info","XML:  Doc Recog ID: " + eddmid});
            messages.add(new String[]{"info","XML:  IFS Type: " + ifs});
            
            c[1] = x[0];  // doctype override original eddm id
            c[0] = x[3];  // senderid
            c[21] = x[1]; // receiverID
      
            String[] defaults = EDData.getEDITPDefaults(x[0], x[3], x[1]);
            
            c[29] = defaults[15]; // defines outputfiletype
            
            // governs inbound only ...for outbound file...let postmap delimiters kick in
            c[9] = defaults[7].isBlank() ? "10" : defaults[7];
            c[10] = defaults[6].isBlank() ? "" : defaults[6];
            c[11] = defaults[8].isBlank() ? "" : defaults[8];
            
                 
             
        // insert isa and st start and stop integer points within the file
        
          c[17] = "0";
          c[18] = String.valueOf(content.length());
          c[19] = "0";
          c[20] = String.valueOf(content.length());
          
          
          
          // at this point...we need to log this doc in edi_idx table and use return ID for further logs against this doc idx.
         
          if (c[12].isEmpty() && callingidxnbr == 0) {   // if not override
          idxnbr = EDData.writeEDIIDX(c);
          }
          
          c[16] = String.valueOf(idxnbr);
          EDData.writeEDILogMulti(c, messages);
          messages.clear();  // clear message here...and at end
          String map = c[2];
             
               if (map.isEmpty() && c[12].isEmpty()) {
                   
                   if (GlobalDebug)  { 
                   System.out.println("Searching for Map (XML in) with values type/structure/x[3]/x[1]: " + c[1] + "/" + ifs + "/" + x[3] + "/" + x[1]);    
                   }
                   
                   map = EDData.getEDIMap(c[1], ifs, c[0], c[21]); // doctype, structure, senderid, receiverid 
                   
               } 
            
               // if no map then bail
               if (map.isEmpty()) {
                  messages.add(new String[]{"error", "XML: map variable is empty for: " + c[1] + "/" + ifs + "/" + x[3] + "/" + x[1]}); 
               } else if (! BlueSeerUtils.isEDIClassFile(map) && c[12].isEmpty()) { 
                  messages.add(new String[]{"error", "XML: unable to locate compiled map (" + map + ") for: " + c[1] + "/" + ifs + "/" + x[3] + "/" + x[1]}); 
               } else {
                
                messages.add(new String[]{"info","XML: Found map: " + map});
               
                   
                   // at this point I should have a doc set and a map ...now call map to operate on doc 
                URLClassLoader cl = null;
                try {
                      List<File> jars = Arrays.asList(new File("edi/maps").listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".jar");
                }
                }));
                URL[] urls = new URL[jars.size()];
                for (int i = 0; i < jars.size(); i++) {
                try {
                  urls[i] = jars.get(i).toURI().toURL();
                } catch (Exception e) {
                    edilog(e);
                }
                }
                cl = new URLClassLoader(urls);
                
                Class cls = Class.forName(map,true,cl);  
                Object obj = cls.getDeclaredConstructor().newInstance();
                Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class, ArrayList.class);
                Object oc = method.invoke(obj, doc, c, messages);
                String[] oString = (String[]) oc;
                messages.add(new String[]{oString[0], oString[1]});
                EDData.updateEDIIDX(idxnbr, c); 

                } catch (InvocationTargetException ex) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "invocation exception in map class " + map + "/" + c[0] + " / " + c[1] + " : " + ex.getCause().getMessage()});    
                    clearStaticVariables();
                    }
                    edilog(ex);  
                } catch (ClassNotFoundException ex) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "Map Class not found " + map + "/" + c[0] + " / " + c[1]});        
                    }
                    edilog(ex); 
                } catch (IllegalAccessException |
                         InstantiationException | NoSuchMethodException ex
                        ) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "IllegalAccess|Instantiation|NoSuchMethod " + map + "/" + c[0] + " / " + c[1]});        
                   }
                    edilog(ex);
                } finally {
                        if (cl != null) {
                           try {
                               cl.close();
                           } catch (IOException ex) {
                               edilog(ex);
                           }
                        }
                }
                   
               }
         EDData.writeEDILogMulti(c, messages);
           messages.clear();     
     
   
  
}
   
    
    public static void processJSON(String content, String[] c, String filename, int idxnbr)   {
    
        int callingidxnbr = idxnbr;   
        String eddmid = c[1]; // c[1] initially contains the edi_doc record id during eddm lookup
        String ifs = getEDIFFSubType(eddmid);
        ArrayList<String[]> messages = new ArrayList<String[]>();
        ArrayList<String> doc = new ArrayList<String>();
        doc.add(content);
        
        messages.add(new String[]{"info","processJSON: " + eddmid});
        
            if (GlobalDebug)
            System.out.println("processing JSON: " + eddmid);
            
            String[] x = getTagInfo(content, c);  // doctype, rcvid, parentpartner, sndid
            
            
            
             if (x[2].isEmpty()) {
                messages.add(new String[]{"error", "unable to determine parent partner from content:(doctype,rcvid,parent,sndid) (" + x[0] + "," + x[1] + "," + x[2] + "," + x[3] + ")"});
                EDData.writeEDILogMulti(c, messages);
                messages.clear(); 
                return;  
             }
            
            messages.add(new String[]{"info","JSON: (doctype,rcvid,parent,sndid) (" + x[0] + "," + x[1] + "," + x[2] + "," + x[3] + ")"});
            messages.add(new String[]{"info","JSON:  Doc Recog ID: " + eddmid});
            messages.add(new String[]{"info","JSON:  IFS Type: " + ifs});
            
            c[1] = x[0];  // doctype  //overfide eddm docid with bs doctype
            c[0] = x[3];  // senderid
            c[21] = x[1]; // receiverID
      
            String[] defaults = EDData.getEDITPDefaults(x[0], x[3], x[1]);
            
            c[29] = defaults[15]; // defines outputfiletype
            
            // governs inbound only ...for outbound file...let postmap delimiters kick in
            c[9] = defaults[7].isBlank() ? "10" : defaults[7];
            c[10] = defaults[6].isBlank() ? "" : defaults[6];
            c[11] = defaults[8].isBlank() ? "" : defaults[8];
            
                 
             
        // insert isa and st start and stop integer points within the file
        
          c[17] = "0";
          c[18] = String.valueOf(content.length());
          c[19] = "0";
          c[20] = String.valueOf(content.length());
          
          
          
          // at this point...we need to log this doc in edi_idx table and use return ID for further logs against this doc idx.
         
          if (c[12].isEmpty() && callingidxnbr == 0) {   // if not override
          idxnbr = EDData.writeEDIIDX(c);
          }
          
          c[16] = String.valueOf(idxnbr);
          EDData.writeEDILogMulti(c, messages);
          messages.clear();  // clear message here...and at end
          String map = c[2];
             
               if (map.isEmpty() && c[12].isEmpty()) {
                   
                   if (GlobalDebug)  { 
                   System.out.println("Searching for Map (JSON in) with values type/structure/x[3]/x[1]: " + c[1] + "/" + ifs + "/" + x[3] + "/" + x[1]);    
                   }
                   
                   map = EDData.getEDIMap(c[1], ifs, c[0], c[21]); // doctype, structure, senderid, receiverid 
               } 
            
               // if no map then bail
               if (map.isEmpty()) {
                  messages.add(new String[]{"error", "JSON: map variable is empty for: " + c[1] + "/" + ifs + "/" + x[3] + "/" + x[1]}); 
               } else if (! BlueSeerUtils.isEDIClassFile(map) && c[12].isEmpty()) { 
                  messages.add(new String[]{"error", "JSON: unable to locate compiled map (" + map + ") for: " + c[1] + "/" + ifs + "/" + x[3] + "/" + x[1]}); 
               } else {
                
                messages.add(new String[]{"info","JSON: Found map: " + map});
               
                   
                   // at this point I should have a doc set and a map ...now call map to operate on doc 
                URLClassLoader cl = null;
                try {
                      List<File> jars = Arrays.asList(new File("edi/maps").listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".jar");
                }
                }));
                URL[] urls = new URL[jars.size()];
                for (int i = 0; i < jars.size(); i++) {
                try {
                  urls[i] = jars.get(i).toURI().toURL();
                } catch (Exception e) {
                    edilog(e);
                }
                }
                cl = new URLClassLoader(urls);
                
                Class cls = Class.forName(map,true,cl);  
                Object obj = cls.getDeclaredConstructor().newInstance();
                Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class, ArrayList.class);
                Object oc = method.invoke(obj, doc, c, messages);
                String[] oString = (String[]) oc;
                messages.add(new String[]{oString[0], oString[1]});
                EDData.updateEDIIDX(idxnbr, c); 

                } catch (InvocationTargetException ex) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "invocation exception in map class " + map + "/" + c[0] + " / " + c[1] + " : " + ex.getCause().getMessage()});    
                    clearStaticVariables();
                    }
                    edilog(ex);  
                } catch (ClassNotFoundException ex) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "Map Class not found " + map + "/" + c[0] + " / " + c[1]});        
                    }
                    edilog(ex); 
                } catch (IllegalAccessException |
                         InstantiationException | NoSuchMethodException ex
                        ) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "IllegalAccess|Instantiation|NoSuchMethod " + map + "/" + c[0] + " / " + c[1]});        
                   }
                    edilog(ex);
                } finally {
                        if (cl != null) {
                           try {
                               cl.close();
                           } catch (IOException ex) {
                               edilog(ex);
                           }
                        }
                }
                   
               }
         EDData.writeEDILogMulti(c, messages);
           messages.clear();     
     
   
  
}
       
    public static void processCSV(Map<Integer, ArrayList<String>> CSVDocs, Map<Integer, Integer[]> CSVPositions, String[] c, ArrayList<String[]> tags, int idxnbr)   {
    
    int callingidxnbr = idxnbr;     
    String eddmid = c[1]; // c[1] initially contains the edi_doc record id during eddm lookup
        String ifs = getEDIFFSubType(eddmid);
    for (Map.Entry<Integer, ArrayList<String>> z : CSVDocs.entrySet()) {
            ArrayList<String[]> messages = new ArrayList<String[]>();
            ArrayList<String> doc = z.getValue();
            Integer[] positions = CSVPositions.get(z.getKey());
            
            messages.add(new String[]{"info","processCSV: " + z.getKey()});
            
            if (GlobalDebug)
            System.out.println("processCSV: " + z.getKey());
            
            
            String[] x = getFileInfo(doc, c, tags);  // doctype, rcvid, parentpartner, sndid
            
            
             
            if (x[2] == null || x[2].isEmpty()) {
             messages.add(new String[]{"error","unable to determine parent partner with alias:  " + x[3]});  
            } else {
             messages.add(new String[]{"info","Found parent partner: " + x[2]});  
            }
           
            c[1] = x[0];  // doctype
            c[0] = x[3];  // senderid
            c[21] = x[1]; // receiverID
            
            messages.add(new String[]{"info","getFileInfo: " + x[0] + "/" + x[3] + "/" + x[1]});
            messages.add(new String[]{"info","CSV:  Doc Recog ID: " + eddmid});
            messages.add(new String[]{"info","CSV:  IFS Type: " + ifs});
            
            String[] defaults = EDData.getEDITPDefaults(x[0], x[3], x[1]);
            dfs_mstr dfs = getDFSMstr(new String[]{ifs});
            
            c[9]  = defaults[7].isBlank() ? "10" : defaults[7];
            c[10] = defaults[6].isBlank() ? "0" : defaults[6];
            c[11] = defaults[8].isBlank() ? "0" : defaults[8];
            
            // for csv inbound...override the defaults[6]
            if (dfs != null && ! dfs.dfs_delimiter().isBlank()) {
                c[10] = dfs.dfs_delimiter();
            }
            
            c[29] = defaults[15]; // defines outputfiletype
            
        // insert isa and st start and stop integer points within the file
        
          c[17] = String.valueOf(positions[0]);
          c[18] = String.valueOf(positions[1]);
          c[19] = String.valueOf(positions[2]);
          c[20] = String.valueOf(positions[3]);
          
          
          
          // at this point...we need to log this doc in edi_idx table and use return ID for further logs against this doc idx.
         
          if (c[12].isEmpty() && callingidxnbr == 0) {   // if not override
          idxnbr = EDData.writeEDIIDX(c);
          }
          
          c[16] = String.valueOf(idxnbr);
          EDData.writeEDILogMulti(c, messages);
          messages.clear();  // clear message here....and at end
         
             String map = c[2];
             
               if (map.isEmpty() && c[12].isEmpty()) {
                   if (GlobalDebug)   
                   System.out.println("Searching for Map (CSV in) with values type/structure/x[3]/x[1]: " + c[1] + "/" + ifs + "/" + x[3] + "/" + x[1]);    
                   map = EDData.getEDIMap(c[1], ifs, c[0], c[21]); // doctype, structure, senderid, receiverid 
               } 
            
               // if no map then bail
               if (map.isEmpty()) {
                    messages.add(new String[]{"error","CSV:  CSV: map variable is empty for doc/structure/sender/receiver: " + c[1] + "/" + ifs + "/" + x[3] + "/" + x[1]});
                } else if (! BlueSeerUtils.isEDIClassFile(map) && c[12].isEmpty()) { 
                    messages.add(new String[]{"error","CSV: unable to locate compiled map (" + map + ") for: " + c[1] + "/" + ifs + "/" + x[3] + "/" + x[1]});
               } else {
                   
                   // at this point I should have a doc set (ST to SE) and a map ...now call map to operate on doc 
                if (GlobalDebug)   
                   System.out.println("Found map: " + map);    
                
                messages.add(new String[]{"info","Found Map: " + map + " with " + c[1] + "/" + c[0] + "/" + c[21]});
                messages.add(new String[]{"info","Using Map: " + map + " for docID: " + c[6]});
            
                   
                URLClassLoader cl = null;
                 try {
                    List<File> jars = Arrays.asList(new File("edi/maps").listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".jar");
                }
                }));
                URL[] urls = new URL[jars.size()];
                for (int i = 0; i < jars.size(); i++) {
                try {
                  urls[i] = jars.get(i).toURI().toURL();
                } catch (Exception e) {
                    edilog(e);
                }
                }
                cl = new URLClassLoader(urls);
                
                Class cls = Class.forName(map,true,cl);  
                Object obj = cls.getDeclaredConstructor().newInstance();
                Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class, ArrayList.class);
                Object oc = method.invoke(obj, doc, c, messages);
                String[] oString = (String[]) oc;
                messages.add(new String[]{oString[0], oString[1]});
                EDData.updateEDIIDX(idxnbr, c); 

                } catch (InvocationTargetException ex) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "invocation exception in map class " + map + "/" + c[0] + " / " + c[1] + " : " + ex.getCause().getMessage()});    
                    clearStaticVariables();
                    }
                    edilog(ex);  
                } catch (ClassNotFoundException ex) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "Map Class not found " + map + "/" + c[0] + " / " + c[1]});        
                    }
                    edilog(ex); 
                } catch (IllegalAccessException |
                         InstantiationException | NoSuchMethodException ex
                        ) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "IllegalAccess|Instantiation|NoSuchMethod " + map + "/" + c[0] + " / " + c[1]});        
                   }
                    edilog(ex);
                } finally {
                        if (cl != null) {
                           try {
                               cl.close();
                           } catch (IOException ex) {
                               edilog(ex);
                           }
                        }
                }
                   
               }
             EDData.writeEDILogMulti(c, messages);
           messages.clear(); 
     
    } // FFDocs entries
   
  
}
   
    public static void packageEnvelopes() {
        if (hanoi != null && hanoi.size() > 0) {
            int c = 0;
            int docid = 0;
            String content = "";
            String ST = "";
            String SE = "";
            for (Map.Entry<String, ArrayList<String>> z : hanoi.entrySet()) {
                String[] k = z.getKey().split(",", -1);
                String[] tp = EDData.getEDITPDefaults(k[0], k[1], k[2]);
                String seg = delimConvertIntToStr(tp[7]); // segment delimiter
                String seg_esc = escapeDelimiter(seg);
                String ele = delimConvertIntToStr(tp[6]); // element delimiter
                String ele_esc = escapeDelimiter(ele);
                String outdir = cleanDirString(tp[9]);
                String outfile = z.getValue().get(2);
                c = 0;
                docid = 0;
                content = "";
                String isactrl = z.getValue().get(0).split(ele_esc,-1)[13];
                String gsctrl = z.getValue().get(1).split(ele_esc,-1)[6];
                for (String s : z.getValue()) {
                    c++;
                    if (c > 3) {
                        docid++;
                        ST = "ST" + ele + k[0] + ele + String.format("%04d", docid);
                        SE = "SE" + ele + String.valueOf(s.chars().filter(ch -> ch == seg_esc.charAt(0)).count() + 2) + ele + String.format("%04d", docid);
                       // content += ("ST" + ele + k[0] + ele + String.format("%04d", docid) + seg + s + "SE" + ele + s.chars().filter(ch -> ch == seg_esc.charAt(0)).count() + ele + String.format("%04d", docid) + seg);
                        content += ST + seg + s + SE + seg;
                    }
                }
                content = z.getValue().get(0)+ seg + z.getValue().get(1) + seg + content;
                content += "GE" + ele + String.valueOf(c - 3) + ele + gsctrl + seg;
                content += "IEA" + ele + "1" + ele + isactrl + seg;
                
                // now write out to file
                if (outdir.isEmpty()) {
                    outdir = cleanDirString(EDData.getEDIOutDir());
                }
                try {
                    EDI.writeFile(content, outdir, outfile);
                } catch (SmbException ex) {
                    edilog(ex);
                } catch (IOException ex) {
                    edilog(ex);
                }
            } // for each key (doctype, gssender, gsreceiver) of hanoi
        }
        hanoi.clear();
    }
    
    public static void processUNE(Map<Integer, Object[]> UNBmap, char[] cbuf, String batchfile, int idxnbr)   {
    
    
    
    // ISAmap is defined as new Integer[] {start, end, (int) s, (int) e, (int) u}, mycopy, c);
    
    // loop through ISAMap entries
    
             
    int callingidxnbr = idxnbr;         
    for (Map.Entry<Integer, Object[]> unb : UNBmap.entrySet()) {
        ArrayList<String[]> messages = new ArrayList<String[]>();
        
        int start =  Integer.valueOf(unb.getValue()[0].toString());  //starting UNB position in file
        int end = Integer.valueOf(unb.getValue()[1].toString());  // ending UNZ position in file
        char segdelim = (char) Integer.valueOf(unb.getValue()[2].toString()).intValue(); // get segment delimiter
           
       // System.out.println("envelope key/start/end : " + unb.getKey() + "/" + unb.getValue()[0] + "/" + unb.getValue()[1]);
    
    
        String[] c = (String[]) unb.getValue()[6];
        
       // ArrayList d = (ArrayList) isa.getValue()[5];
        Map<Integer, ArrayList> d = (HashMap<Integer, ArrayList>)unb.getValue()[5];
        
       // System.out.println("doc entryset is : " + d.entrySet());
        
        ArrayList<String> falist = new ArrayList<String>();
        
       //for (Object z : d) {
       for (Map.Entry<Integer, ArrayList> z : d.entrySet()) {
         
       //    System.out.println("doc Map entry key: " + z.getKey());
           
         for (Object r : z.getValue()) {
            Object[] x = (Object[]) r;
         
            Integer[] k = (Integer[])x[0];
            String doctype = (String)x[1];
            String docid = (String)x[2];
           
            c[1] = doctype;
            c[6] = docid;
      
            messages.add(new String[]{"info","File: " + c[3]});
            messages.add(new String[]{"info","UNB#: " + c[4] + " / " + "UNG#: " + c[5]});
            messages.add(new String[]{"info","Doc Type: " + c[1] + " / " + "DocID: " + c[6]});
       //    System.out.println("doc key/{start/end},doctype,docid " + k[0] + "/" + k[1] + "/" + doctype + "/" + docid);
            
           // Integer[] k = (Integer[])z.getValue()[0];
           // String doctype = (String)z.getValue()[1];
           // String docid = (String)z.getValue()[2];
           
            
         //   System.out.println("doc start/end : " + k[0] + "/" + k[1]);
         if (! doctype.equals("997une"))
         falist.add(docid); // add ST doc id to falist for functional acknowledgement
      //        System.out.println("control values: " + docid + "/" + k[0] + "/" + k[1] );
       
    //  you are inserting 'segments' into ArrayList doc
    StringBuilder segment = new StringBuilder();
   // for (int i = 0; i < cbuf.length; i++) {
   ArrayList<String> doc = new ArrayList<String>();
   for (int i = k[0]; i < k[1]; i++) {
        if (cbuf[i] == segdelim) {
            doc.add(segment.toString());
            segment.delete(0, segment.length());
        } else {
            if (! (String.format("%02x",(int) cbuf[i]).equals("0d") || String.format("%02x",(int) cbuf[i]).equals("0a")) ) {
                segment.append(cbuf[i]);
            } 
        }
    }
     
             c[24] = batchfile;
             messages.add(new String[]{"info","Processing Batch File: " + c[24]});
             
        // insert isa and st start and stop integer points within the file
        
          c[17] = String.valueOf(start);
          c[18] = String.valueOf(end);
          c[19] = String.valueOf(k[0]);
          c[20] = String.valueOf(k[1]);
          
          
          int j = Integer.valueOf(c[10]);
          String delim = String.valueOf(Character.toString((char) j));
          
          
          String parentPartner = EDData.getEDIPartnerFromAlias(c[0]);
          
          if (parentPartner == null || parentPartner.isBlank()) {
            messages.add(new String[]{"error","Unknown parent partner using: " + c[0]});  
          } else {
            messages.add(new String[]{"info","Found parent partner: " + parentPartner});  
          }
          
          
          // at this point...we need to log this doc in edi_idx table and use return ID for further logs against this doc idx.
          if (c[12].isEmpty() && callingidxnbr == 0) {   // if not override
              idxnbr = EDData.writeEDIIDX(c);
              if (GlobalDebug) 
                  System.out.println("insert edi_idx " + idxnbr + ": " + c);
              
          }
          
          c[16] = String.valueOf(idxnbr);
          EDData.writeEDILogMulti(c, messages);
          messages.clear();  // clear message here...and at 997...and at end
          
          String map = c[2];
             
          if (doctype.equals("997une")) {
            if (GlobalDebug)   {
            System.out.println("Encountered 997une...processing and return" + c[1] + "/" + c[0] + "/" + c[21]);    
            }
            String[] m = processCONTRL(doc, c);
            messages.add(new String[]{m[0], m[1]});
            EDData.updateEDIIDXStatus(idxnbr, m[0]);
            EDData.writeEDILogMulti(c, messages);
            messages.clear();
            continue;
          }   
          
           if (map.isEmpty() && c[12].isEmpty()) {
            if (GlobalDebug)   
            System.out.println("Searching for Map (UNE in) with GS values type/gs02/gs03: " + c[1] + "/" + c[0] + "/" + c[21]);    

              map = EDData.getEDIMap(c[1], c[0], c[21]); 
           } 

            // should have partner by now
            String[] defaults = EDData.getEDITPDefaults(c[1], c[0], c[21]);
            c[29] = defaults[15]; // defines outputfiletype
           
           // if no map then bail
           if (map.isEmpty()) {
                messages.add(new String[]{"error","UNE:  map variable is empty for parent/gs02/gs03/doc: " + parentPartner + "/" + c[0] + "/" + c[21] + " / " + c[1]});
           } else if (! BlueSeerUtils.isEDIClassFile(map) && c[12].isEmpty()) {
                messages.add(new String[]{"error","UNE: unable to locate compiled map (" + map + ") for parent/gs02/gs03/doc: " + parentPartner + "/" + c[0] + "/" + c[21] + " / " + c[1]});
           } else {
               
            if (GlobalDebug)   
            System.out.println("Entering Map " + map + " with: " +  c[1] +  "/" + c[21]);    

            messages.add(new String[]{"info","Found Map: " + map + " with " + c[1] + "/" + c[21]});
            messages.add(new String[]{"info","Using Map: " + map + " for docID: " + c[6]});
            
               // at this point I should have a doc set (ST to SE) and a map ...now call map to operate on doc 
                URLClassLoader cl = null;
                try {
                    List<File> jars = Arrays.asList(new File("edi/maps").listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".jar");
                }
                }));
                URL[] urls = new URL[jars.size()];
                for (int i = 0; i < jars.size(); i++) {
                try {
                  urls[i] = jars.get(i).toURI().toURL();
                } catch (Exception e) {
                    edilog(e);
                }
                }
                cl = new URLClassLoader(urls);
                
                    Class cls = Class.forName(map,true,cl);  
                Object obj = cls.getDeclaredConstructor().newInstance();
                Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class, ArrayList.class);
                Object oc = method.invoke(obj, doc, c, messages);
                String[] oString = (String[]) oc;
                messages.add(new String[]{oString[0], oString[1]});
                EDData.updateEDIIDX(idxnbr, c); 

                } catch (InvocationTargetException ex) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "invocation exception in map class " + map + "/" + c[0] + " / " + c[1] + " : " + ex.getCause().getMessage()});    
                    clearStaticVariables();
                    }
                    edilog(ex);  
                } catch (ClassNotFoundException ex) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "Map Class not found " + map + "/" + c[0] + " / " + c[1] + " / " + c[21]});        
                    }
                    edilog(ex); 
                } catch (IllegalAccessException |
                         InstantiationException | NoSuchMethodException ex
                        ) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "IllegalAccess|Instantiation|NoSuchMethod " + map + "/" + c[0] + " / " + c[1] + " / " + c[1]});        
                   }
                    edilog(ex);
                } finally {
                        if (cl != null) {
                           try {
                               cl.close();
                           } catch (IOException ex) {
                               edilog(ex);
                           }
                        }
                }

           }
           
           EDData.writeEDILogMulti(c, messages);
           messages.clear();
            
               
      } // r         
               
    } // object k
        
             
       
            // 997une
            ArrayList<String> falistcopy = new ArrayList<String>(falist);
            falist.clear();
            
            if (GlobalDebug)   
            System.out.println("997 potential: " + c[12] + "/" +  (falistcopy.size() > 0) + "/" + BlueSeerUtils.ConvertStringToBool(EDData.getEDIFuncAck(c[1], c[0].trim(), c[21].trim())) );    

            if (c[12].isEmpty() && falistcopy.size() > 0 && BlueSeerUtils.ConvertStringToBool(EDData.getEDIFuncAck(c[1], c[0], c[21]))) {
             generateCONTRL(falistcopy, c);
            }
            
     
     
    } // ISAMap entries
   
  
}
        
    public static void processX12(Map<Integer, Object[]> ISAmap, char[] cbuf, String batchfile, int idxnbr)   {
    
    
    
    // ISAmap is defined as new Integer[] {start, end, (int) s, (int) e, (int) u});
    
    // loop through ISAMap entries
    
             
    int callingidxnbr = idxnbr;         
    for (Map.Entry<Integer, Object[]> isa : ISAmap.entrySet()) {
        ArrayList<String[]> messages = new ArrayList<String[]>();
        
        int start =  Integer.valueOf(isa.getValue()[0].toString());  //starting ISA position in file
        int end = Integer.valueOf(isa.getValue()[1].toString());  // ending IEA position in file
        char segdelim = (char) Integer.valueOf(isa.getValue()[2].toString()).intValue(); // get segment delimiter
        String gs02 = ""; 
        String gs03 = "";       
//  System.out.println("envelope key/start/end : " + isa.getKey() + "/" + isa.getValue()[0] + "/" + isa.getValue()[1]);
    
    
        String[] c = (String[]) isa.getValue()[6];
        
       // ArrayList d = (ArrayList) isa.getValue()[5];
        Map<Integer, ArrayList> d = (HashMap<Integer, ArrayList>)isa.getValue()[5];
        
     //   System.out.println("doc entryset is : " + d.entrySet());
        
        ArrayList<String> falist = new ArrayList<String>();
        
       //for (Object z : d) {
       for (Map.Entry<Integer, ArrayList> z : d.entrySet()) {
         
       //    System.out.println("doc May entry key: " + z.getKey());
           
         for (Object r : z.getValue()) {
            Object[] x = (Object[]) r;
         
            Integer[] k = (Integer[])x[0];
            String doctype = (String)x[1];
            String docid = (String)x[2];
           
            c[1] = doctype;
            c[6] = docid;
      
            messages.add(new String[]{"info","File: " + c[3]});
            messages.add(new String[]{"info","ISA#: " + c[4] + " / " + "GS#: " + c[5]});
            messages.add(new String[]{"info","Doc Type: " + c[1] + " / " + "DocID: " + c[6]});
       //    System.out.println("doc key/{start/end},doctype,docid " + k[0] + "/" + k[1] + "/" + doctype + "/" + docid);
            
           // Integer[] k = (Integer[])z.getValue()[0];
           // String doctype = (String)z.getValue()[1];
           // String docid = (String)z.getValue()[2];
           
            
         //   System.out.println("doc start/end : " + k[0] + "/" + k[1]);
         if (! doctype.equals("997x12"))
         falist.add(docid); // add ST doc id to falist for functional acknowledgement
      //        System.out.println("control values: " + docid + "/" + k[0] + "/" + k[1] );
       
    //  you are inserting 'segments' into ArrayList doc
    StringBuilder segment = new StringBuilder();
   // for (int i = 0; i < cbuf.length; i++) {
   ArrayList<String> doc = new ArrayList<String>();
   for (int i = k[0]; i < k[1]; i++) {
        if (cbuf[i] == segdelim) {
            doc.add(segment.toString());
            segment.delete(0, segment.length());
        } else {
            if (! (String.format("%02x",(int) cbuf[i]).equals("0d") || String.format("%02x",(int) cbuf[i]).equals("0a")) ) {
                segment.append(cbuf[i]);
            } 
        }
    }
     
             c[24] = batchfile;
             messages.add(new String[]{"info","Processing Batch File: " + c[24]});
             
        // insert isa and st start and stop integer points within the file
        
          c[17] = String.valueOf(start);
          c[18] = String.valueOf(end);
          c[19] = String.valueOf(k[0]);
          c[20] = String.valueOf(k[1]);
          
          
          int j = Integer.valueOf(c[10]);
          String delim = String.valueOf(Character.toString((char) j));
          String[] gs = c[14].split(escapeDelimiter(delim));
          
          if (gs != null && gs.length > 2) {
              gs02 = gs[2];
              gs03 = gs[3];
          }
          
          String parentPartner = EDData.getEDIPartnerFromAlias(gs02);
          if (parentPartner == null || parentPartner.isBlank()) {
             parentPartner = EDData.getEDIPartnerFromAlias(c[0]); 
          }
          
          if (parentPartner == null || parentPartner.isBlank()) {
            messages.add(new String[]{"error","Unknown parent partner using: " + gs02});  
          } else {
            messages.add(new String[]{"info","Found parent partner: " + parentPartner});  
          }
          
          
          // at this point...we need to log this doc in edi_idx table and use return ID for further logs against this doc idx.
          if (c[12].isEmpty() && callingidxnbr == 0) {   // if not override
              idxnbr = EDData.writeEDIIDX(c);
              if (GlobalDebug) 
                  System.out.println("insert edi_idx " + idxnbr + ": " + c);
              
          }
          
          c[16] = String.valueOf(idxnbr);
          EDData.writeEDILogMulti(c, messages);
          messages.clear();  // clear message here...and at 997...and at end
          
          String map = c[2];
             
          if (doctype.equals("997x12")) {
            if (GlobalDebug)   {
            System.out.println("Encountered 997...processing and return" + c[1] + "/" + gs02 + "/" + gs03);    
            }
            String[] m = process997(doc, c);
            messages.add(new String[]{m[0], m[1]});
            EDData.updateEDIIDXStatus(idxnbr, m[0]);
            EDData.writeEDILogMulti(c, messages);
            messages.clear();
            continue;
          }   
          
            
            
          
           if (map.isEmpty() && c[12].isEmpty()) {
            if (GlobalDebug)   
            System.out.println("Searching for Map (X12 in) with GS values type/gs02/gs03: " + c[1] + "/" + gs02 + "/" + gs03);    

              map = EDData.getEDIMap(c[1], gs02, gs03); 
           } 

           // should have partner by now
            String[] defaults = EDData.getEDITPDefaults(c[1], gs02, gs03);
            c[29] = defaults[15]; // defines outputfiletype
           
           // if no map then bail
           if (map.isEmpty()) {
                messages.add(new String[]{"error","X12:  map variable is empty for parent/gs02/gs03/doc: " + parentPartner + "/" + gs02 + "/" + gs03 + " / " + c[1]});
           } else if (! BlueSeerUtils.isEDIClassFile(map) && c[12].isEmpty()) {
                messages.add(new String[]{"error","X12: unable to locate compiled map (" + map + ") for parent/gs02/gs03/doc: " + parentPartner + "/" + gs02 + "/" + gs03 + " / " + c[1]});
           } else {
             
               
            if (GlobalDebug)   
            System.out.println("Entering Map " + map + " with: " +  c[1] + "/" + gs02 + "/" + gs03);    

            messages.add(new String[]{"info","Found Map: " + map + " with " + c[1] + "/" + gs02 + "/" + gs03});
            messages.add(new String[]{"info","Using Map: " + map + " for docID: " + c[6]});
            
               // at this point I should have a doc set (ST to SE) and a map ...now call map to operate on doc 
                URLClassLoader cl = null;
                try {
                    List<File> jars = Arrays.asList(new File("edi/maps").listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".jar");
                }
                }));
                URL[] urls = new URL[jars.size()];
                for (int i = 0; i < jars.size(); i++) {
                try {
                  urls[i] = jars.get(i).toURI().toURL();
                } catch (Exception e) {
                    edilog(e);
                }
                }
                cl = new URLClassLoader(urls);
                
                Class cls = Class.forName(map,true,cl);  
                Object obj = cls.getDeclaredConstructor().newInstance();
                Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class, ArrayList.class);
                Object oc = method.invoke(obj, doc, c, messages);
                String[] oString = (String[]) oc;
                messages.add(new String[]{oString[0], oString[1]});
                EDData.updateEDIIDX(idxnbr, c); 

                } catch (InvocationTargetException ex) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "invocation exception in map class " + map + "/" + c[0] + " / " + c[1] + " : " + ex.getCause().getMessage()});    
                    clearStaticVariables();
                    }
                    edilog(ex);  
                } catch (ClassNotFoundException ex) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "Map Class not found " + map + "/" + c[0] + " / " + c[1]});        
                    }
                    edilog(ex); 
                } catch (IllegalAccessException |
                         InstantiationException | NoSuchMethodException ex
                        ) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "IllegalAccess|Instantiation|NoSuchMethod " + map + "/" + c[0] + " / " + c[1]});        
                   }
                    edilog(ex);
                } finally {
                        if (cl != null) {
                           try {
                               cl.close();
                           } catch (IOException ex) {
                               edilog(ex);
                           }
                        }
                }
           
           } // else
           
           EDData.writeEDILogMulti(c, messages);
           messages.clear();
            
               
      } // r         
               
    } // object k
        
             
       
            // 997
            ArrayList<String> falistcopy = new ArrayList<String>(falist);
            falist.clear();
            if (GlobalDebug)   
            System.out.println("997 potential: " + c[12] + "/" +  (falistcopy.size() > 0) + "/" + BlueSeerUtils.ConvertStringToBool(EDData.getEDIFuncAck(c[1], c[0].trim(), c[21].trim())) );    

            if (c[12].isEmpty() && falistcopy.size() > 0 && BlueSeerUtils.ConvertStringToBool(EDData.getEDIFuncAck(c[1], gs02, gs03))) {
             generate997(falistcopy, c);
            }
            
     
     
    } // ISAMap entries
   
  
}
    
    public static void processFF(Map<Integer, ArrayList<String>> FFDocs, Map<Integer, Integer[]> FFPositions, String[] c, ArrayList<String[]> tags, int idxnbr)   {
    
    int callingidxnbr = idxnbr;    
    String eddmid = c[1]; // c[1] initially contains the edi_doc record id during eddm lookup
    String ifs = getEDIFFSubType(eddmid);
    for (Map.Entry<Integer, ArrayList<String>> z : FFDocs.entrySet()) {
            ArrayList<String[]> messages = new ArrayList<String[]>();
            ArrayList<String> doc = z.getValue();
            Integer[] positions = FFPositions.get(z.getKey());
            
            if (GlobalDebug)
            System.out.println("processFF: " + z.getKey());
            
            String[] x = getFileInfo(doc, c, tags);  // doctype, rcvid, parentpartner, sndid
            
            
            
             if (x[2].isEmpty()) {
                messages.add(new String[]{"error", "unable to determine parent partner with alias: " + x[1]} ); 
                return;  
             }
            
            messages.add(new String[]{"info","FF: (doctype,rcvid,parent,sndid) (" + x[0] + "," + x[1] + "," + x[2] + "," + x[3] + ")"});
            messages.add(new String[]{"info","FF:  Doc Recog ID: " + eddmid});
            messages.add(new String[]{"info","FF:  IFS Type: " + ifs});
            
            c[1] = x[0];  // doctype override c1 that had original eddm id
            c[0] = x[3];  // senderid
            c[21] = x[1]; // receiverID
      
            String[] defaults = EDData.getEDITPDefaults(x[0], x[3], x[1]);
            
            c[29] = defaults[15]; // defines outputfiletype
            
            // governs inbound only ...for outbound file...let postmap delimiters kick in
            c[9] = defaults[7].isBlank() ? "10" : defaults[7];
            c[10] = defaults[6].isBlank() ? "" : defaults[6];
            c[11] = defaults[8].isBlank() ? "" : defaults[8];
            
                 
             
        // insert isa and st start and stop integer points within the file
        
          c[17] = String.valueOf(positions[0]);
          c[18] = String.valueOf(positions[1]);
          c[19] = String.valueOf(positions[2]);
          c[20] = String.valueOf(positions[3]);
          
          
          
          // at this point...we need to log this doc in edi_idx table and use return ID for further logs against this doc idx.
         
          if (c[12].isEmpty() && callingidxnbr == 0) {   // if not override
          idxnbr = EDData.writeEDIIDX(c);
          }
          //EDData.writeEDILogMulti(c, messages);
         // messages.clear();  // clear message here...and at end
          c[16] = String.valueOf(idxnbr);
          String map = c[2];
             
               if (map.isEmpty() && c[12].isEmpty()) {
                   
                   if (GlobalDebug)  { 
                   System.out.println("Searching for Map (FF in) with values type/structure/x[3]/x[1]: " + c[1] + "/" + ifs + "/" + x[3] + "/" + x[1]);    
                   }
                   
                   map = EDData.getEDIMap(c[1], c[0], c[21]); // doctype, senderid, receiverid 
               } 
            
               // if no map then bail
               if (map.isEmpty()) {
                  messages.add(new String[]{"error", "FF: map variable is empty for: " + c[1] + "/" + ifs + "/" + x[3] + "/" + x[1]}); 
               } else if (! BlueSeerUtils.isEDIClassFile(map) && c[12].isEmpty()) { 
                  messages.add(new String[]{"error", "FF: unable to locate compiled map (" + map + ") for: " + c[1] + "/" + ifs + "/" + x[3] + "/" + x[1]}); 
               } else {
                
                messages.add(new String[]{"info","FF: Found map: " + map});
               
                   
                   // at this point I should have a doc set (ST to SE) and a map ...now call map to operate on doc 
                URLClassLoader cl = null;
                try {
                      List<File> jars = Arrays.asList(new File("edi/maps").listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".jar");
                }
                }));
                URL[] urls = new URL[jars.size()];
                for (int i = 0; i < jars.size(); i++) {
                try {
                  urls[i] = jars.get(i).toURI().toURL();
                } catch (Exception e) {
                    edilog(e);
                }
                }
                cl = new URLClassLoader(urls);
                
                Class cls = Class.forName(map,true,cl);  
                Object obj = cls.getDeclaredConstructor().newInstance();
                Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class, ArrayList.class);
                Object oc = method.invoke(obj, doc, c, messages);
                String[] oString = (String[]) oc;
                messages.add(new String[]{oString[0], oString[1]});
                EDData.updateEDIIDX(idxnbr, c); 

                } catch (InvocationTargetException ex) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "invocation exception in map class " + map + "/" + c[0] + " / " + c[1] + " : " + ex.getCause().getMessage()});    
                    clearStaticVariables();
                    }
                    edilog(ex);  
                } catch (ClassNotFoundException ex) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "Map Class not found " + map + "/" + c[0] + " / " + c[1]});        
                    }
                    edilog(ex); 
                } catch (IllegalAccessException |
                         InstantiationException | NoSuchMethodException ex
                        ) {
                    if (c[12].isEmpty()) {
                    messages.add(new String[]{"error", "IllegalAccess|Instantiation|NoSuchMethod " + map + "/" + c[0] + " / " + c[1]});        
                   }
                    edilog(ex);
                } finally {
                        if (cl != null) {
                           try {
                               cl.close();
                           } catch (IOException ex) {
                               edilog(ex);
                           }
                        }
                }
                   
               }
         EDData.writeEDILogMulti(c, messages);
           messages.clear();     
     
    } // FFDocs entries
   
  
}
   
    public static Map<String, ArrayList<String>> loadADF(String adf) {
        Map<String, ArrayList<String>> hm = new HashMap<String, ArrayList<String>>();
        return hm;
    }

    public static ArrayList<String> getEnvFromFileAsArrayListwRegex(File file) throws FileNotFoundException, IOException, ClassNotFoundException {
        
        ArrayList<String> envelopes = new ArrayList<String>();
        
         StringBuilder contents = new StringBuilder();
         BufferedReader f = new BufferedReader(new FileReader(file));
         String text = "";
         while ((text = f.readLine()) != null) {
         contents.append(text).append("&");
         //contents.append(text);
         }
         f.close();
       Pattern p = Pattern.compile("(ISA(?:(?!ISA.*GS.*GE.*IEA).).*?GS.*?GE.*?IEA)");
       Matcher m = p.matcher(contents);
       int i = 0;
       
       while(m.find()) {
            envelopes.add(m.group(i));
            i++;
        }
      return envelopes;
    }
     
    public static ArrayList<String[]> getSegFromEnvAsArrayList(ArrayList<String> envelopes)  {
        String[] segments = null;
        ArrayList<String[]> mylist = new ArrayList<String[]>();  
         String sd = "";
         String ed = "";
         String ud = "";
         
         for (String s : envelopes) {
          // one 's' record per envelope....s = envelope ISA to IEA
          ed = s.substring(103,104);
          ud = s.substring(104,105);
          sd = s.substring(105,106);
          
          if (GlobalDebug) {
          System.out.println("getSegFromEnvAsArrayList: segment=" + s);
          System.out.println("getSegFromEnvAsArrayList: segDelimiter=" + sd);
          }
          
          if (sd.equals("\\")) {
            sd = "\\\\";
          }
          segments  = s.split(sd);
          mylist.add(segments);
         }
         
      return mylist;
    }
    
    
    // inbound
    public static void createOrderFrom830(edi830 e, String[] control) {
        
        int sonbr = 0;
        
        String orderline = "";
        String[] orderlinearray  = null;
        String[] fcsarray = null;
        String order = "";
        String line = "";
        
            control[7] = e.ov_shipto;
           EDData.writeEDILog(control, "INFO", "Load");
            // control = e.isaSenderID + "," + e.doctype + "," + e.isaCtrlNum + "," + e.po ;
             
             
             
             //EDData.CreateSalesOrderHdr(control, String.valueOf(sonbr), e.ov_billto, shipto, e.po, e.duedate, e.podate, e.remarks);
               for (int j = 0; j < e.getDetCount(); j++ ) {
                  
               // find the blanket order type with key shipto, part, po ...if not availabe raise error and next loop    
               orderline = EDData.GetBlanketOrderLine(control, e.ov_shipto, e.getDetItem(j), e.getDetPO(j));
               
               
                   if (! orderline.isEmpty()) {
                       orderlinearray = orderline.split(",", -1);
                       order = orderlinearray[0].toString();
                       line = orderlinearray[1].toString();

                     
                       for (String mystring : e.map.get(j)  ) { 
                           fcsarray = mystring.split(",", -1);
                           OVData.CreateSalesOrderSchedDet(
                                   order,    // sales order
                                   line,    // order line
                                   fcsarray[0],
                                   fcsarray[1],
                                   fcsarray[2],
                                   fcsarray[3],
                                   e.rlse
                                   );
                                 
                       }
                             
                   } // if orderline not empty
               
               } // for each j
       
       
    
    }
    
    public static void createOrderFrom862(edi862 e, String[] control) {
        
        int sonbr = 0;
       
        String orderline = "";
        String[] orderlinearray  = null;
        String[] fcsarray = null;
        String order = "";
        String line = "";
        
         control[7] = e.ov_shipto;
        EDData.writeEDILog(control, "INFO", "Load");
        //   control = e.isaSenderID + "," + e.doctype + "," + e.isaCtrlNum + "," + e.po ;
             
             
             
             //EDData.CreateSalesOrderHdr(control, String.valueOf(sonbr), e.ov_billto, shipto, e.po, e.duedate, e.podate, e.remarks);
               for (int j = 0; j < e.getDetCount(); j++ ) {
                  
               // find the blanket order type with key shipto, part, po ...if not availabe raise error and next loop    
               orderline = EDData.GetBlanketOrderLine(control, e.ov_shipto, e.getDetItem(j), e.getDetPO(j));
               
               
                   if (! orderline.isEmpty()) {
                       orderlinearray = orderline.split(",");
                       order = orderlinearray[0].toString();
                       line = orderlinearray[1].toString();

                     
                       for (String mystring : e.map.get(j)  ) { 
                         
                           fcsarray = mystring.split(",");
                           OVData.CreateSalesOrderSchedDet(
                                   order,    // sales order
                                   line,    // order line
                                   fcsarray[0],
                                   fcsarray[1],
                                   fcsarray[2],
                                   fcsarray[3],
                                   e.rlse
                                   );
                                 
                       }
                             
                   } // if orderline not empty
               
               } // for each j
       
       
   
    }
   
        
    
    public static String[] createSOFrom850(edi850 e, String[] control) {
        String[] m = new String[]{"",""};
        int sonbr = OVData.getNextNbr("order");
        String shipto;
        
        if (e.ov_shipto.isEmpty())
            shipto = OVData.CreateShipTo(e.ov_billto, e.st_name, e.st_line1, e.st_line2, e.st_line3, e.st_city, e.st_state, e.st_zip, e.st_country, e.shipto);
        else
            shipto = e.ov_shipto;
    
        String[] custinfo = cusData.getCustInfo(e.ov_billto);
        String site = OVData.getDefaultSite();
        ordData.so_mstr so = new ordData.so_mstr(null, 
                String.valueOf(sonbr),
                 e.ov_billto,
                 shipto,
                 site,
                 OVData.getDefaultCurrency(),   
                 e.shipmethod,
                 "",
                 e.po,
                 e.duedate,
                 e.podate,
                 bsmf.MainFrame.dfdate.format(new Date()),
                 bsmf.MainFrame.userid,
                 getGlobalProgTag("open"),
                 "",   // order level allocation status (global variable) set by createDetRecord 
                 custinfo[4],
                 custinfo[0],
                 custinfo[1],
                 e.remarks,
                 "DISCRETE",
                 "", // tax
                "0", // isSourced
                "0", // isConfirmed
                "0" // isPlanned
                );
        // detail
        ArrayList<ordData.sod_det> detail = new ArrayList<ordData.sod_det>();
        String uom;
        for (int j = 0; j < e.getDetCount(); j++ ) {
           if (e.getDetUOM(j).isEmpty()) {
               uom = OVData.getUOMByItem(e.getDetItem(j));  
           } else { 
               uom = e.getDetUOM(j);
           }
        ordData.sod_det sod = new ordData.sod_det(null, 
                String.valueOf(sonbr),
                String.valueOf(j + 1),
                e.getDetItem(j), 
                e.getDetCustItem(j), 
                e.po,
                e.getDetQty(j).replace(defaultDecimalSeparator, '.'),
                uom,
                "", // allocation value
                e.getDetListPrice(j).replace(defaultDecimalSeparator, '.'),
                e.getDetDisc(j).replace(defaultDecimalSeparator, '.'),
                e.getDetNetPrice(j).replace(defaultDecimalSeparator, '.'),
                e.podate,
                e.duedate,   
                "0", // ship qty
                "", // status
                "", // wh
                "", // location
                "",  // desc
                "", // tax 
                site,  
                "", // bom
                shipto
                );  
                detail.add(sod);
        }
        
        m = addOrderTransaction(detail, so, null, null, null);
        if (m[0].equals("0")) {
            m[0] = "success";
            m[1] = m[1] + ": " + sonbr;
        } else {
            m[0] = "error";
        }
        return m;
    }
    
    public static void createShipperFrom945(edi945 e, String[] control) {
        
        int shipperid = 0;
        boolean error = false;
        
        String shipto = "";
       
            error = false;
             shipperid = OVData.getNextNbr("shipper");
             control[7] = String.valueOf(shipperid);
             EDData.writeEDILog(control, "INFO", "Load");
           //  control = ((edi850)e.get(i)).isaSenderID + "," + ((edi850)e.get(i)).doctype + "," + ((edi850)e.get(i)).isaCtrlNum + "," + ((edi850)e.get(i)).po ;
             
             if (e.ov_shipto.isEmpty())
                 shipto = OVData.CreateShipTo(e.ov_billto, e.st_name, e.st_line1, e.st_line2, e.st_line3, e.st_city, e.st_state, e.st_zip, e.st_country, e.shipto);
             else
                 shipto = e.ov_shipto; 
             
             error = EDData.CreateShipperHdrEDI(control, String.valueOf(shipperid), e.shipper, e.ov_billto, shipto, e.so, e.po, e.shipdate, e.podate, e.remarks, e.shipmethod);     
             if (! error) {  
             for (int j = 0; j < e.getDetCount(); j++ ) {
                 
                   // error trigger logic ...missing internal item
                   if (e.getDetItem(j).isEmpty())
                      error = true;
                   
                   // error trigger logic ...missing netprice = 0
                   if ( Double.valueOf(e.getDetNetPrice(j)) == 0)
                      error = true;
                  
               shpData.CreateShipperDet(String.valueOf(shipperid),
                       e.getDetItem(j), 
                       e.getDetCustItem(j), 
                       e.getDetSku(j),
                       e.so,
                       e.getDetPO(j), 
                       e.getDetQtyShp(j),
                       e.getDetUOM(j),
                       e.getDetListPrice(j), 
                       e.getDetDisc(j), 
                       e.getDetNetPrice(j),
                       e.getShipDate(), 
                       e.getDetDesc(j),
                       e.getDetLine(j),
                       e.getDetSite(j),
                       e.getDetWH(j),
                       e.getDetLoc(j),
                       "0"   // this is a holder for matltax which is not implemented in EDI yet
               ); 
               // System.out.println(((edi850)e.get(i)).getDetCustItem(j));
               }
             }
       
    
    }
      
    public static void createFOTDETFrom990(edi990 e, String[] control) {
             control[7] = e.order;
             EDData.writeEDILog(control, "INFO", "Load");
             EDData.CreateFOTDETFrom990i(control, e.order, e.scac, e.yesno, e.reasoncode);  
    
    }
     
    public static void createFOTDETFrom220(edi220 e, String[] control) {
             control[7] = e.order;
             EDData.writeEDILog(control, "INFO", "Load");
             EDData.CreateFOTDETFrom220i(control, e.order, e.scac, e.yesno, e.remarks, e.amount);   
    
    }
    
    public static String[] createCFOFrom204(edi204 e, String[] control) {
            String[] m = new String[]{"",""};
            
            String[] z = getCFOPrevious(e.custfo); // returns latest cfo_nbr, cfo_revision ...if exists 
            String cfokey = "";
            String revision = "";
            if (z[0].isBlank()) {
             cfokey = String.valueOf(OVData.getNextNbr("cfo"));
             revision = "1";
            } else {
             cfokey = z[0];
             revision = String.valueOf(Integer.valueOf(z[1]) + 1); // up the revision
            }
            String ratetype = "Flat Rate";
            double amt = 0.00;
            double miles = 0.00;
            double rate = 0.00;
            
            if (BlueSeerUtils.isParsableToDouble(e.getRate())) {
                rate = Double.valueOf(e.getRate());
            }
            if (BlueSeerUtils.isParsableToDouble(e.getMiles())) {
                miles = Double.valueOf(e.getMiles());
            }
            
            if (e.getRateType().equals("PM")) {
                ratetype = "Mileage Rate";
                amt = rate * miles;
            } else {
                ratetype = "Flat Rate";
                amt = rate;
            }
              
            
            
            frtData.cfo_mstr x = new frtData.cfo_mstr(null, 
                cfokey,
                e.getCust(),
                e.getCustFO(),
                revision, // revision
                "", // service type
                e.getEquipType(),
                "", //vehicle ID
                "", // trailer 
                "pending", // status
                "", // delivery status
                "", // driver
                "", // driver cell
                "", // fotype
                "", // broker
                "", // broker contact
                "", // broker cell
                ratetype, // rate type 
                BlueSeerUtils.currformatDoubleUS(rate), // rate
                String.valueOf(miles), // miles
                "", // driver rate
                "0", // driver standard toggle
                e.getWeight(), // weight
                BlueSeerUtils.convertDateFormat("yyyyMMdd", BlueSeerUtils.now().substring(0,8)),
                "", // confirm date
                e.getHazmat(), // is hazmat
                "0", // expenses
                "0", // charges
                BlueSeerUtils.currformatDoubleUS(amt), // total cost
                "", // bol
                e.getRemarks(), // remarks
                "0,0,0", // derived construct
                "", // logic
                "", // site
                "1", // is EDI
                "", // edi rejection reason..to be added
                "1" // revision is default? ...assumes latest always default when created via EDI
        );
            
            // now the detail
            ArrayList<frtData.cfo_det> list = new ArrayList<frtData.cfo_det>();
            String dettype = "";
            for (int j = 0; j < e.getDetCount(); j++ ) {
               System.out.println("detcount: " + j + "/" + e.getDetType(j));
                dettype = ediData.getEDIXvalue("204", "S5", "2", e.getDetType(j)); // three types supported... LD = Load; CU = Unload Complete; UL = Unload Partial
                if (dettype.isBlank()) {
                   m[0] = "1"; 
                   m[1] = "unknown S5 type: " + e.getDetType(j);
                   break;
                }
                frtData.cfo_det y = new frtData.cfo_det(null, 
                cfokey,
                revision, // revision 
                String.valueOf(j + 1),  //stopline
                "", // sequence
                dettype,  // type
                e.getDetAddrCode(j), // code
                e.getDetAddrName(j), 
                e.getDetAddrLine1(j), 
                e.getDetAddrLine2(j),
                "", //line3
                e.getDetAddrCity(j), 
                e.getDetAddrState(j),
                e.getDetAddrZip(j),
                e.getDetCountry(j), // country
                e.getDetPhone(j), // phone
                e.getDetEmail(j), // email
                e.getDetContact(j), // contacts
                "", // misc
                e.getDetRemarks(j), // remarks
                e.getDetRef(j), // reference
                "", // ordnum
                e.getDetWeight(j).replace(defaultDecimalSeparator, '.'), // weight
                e.getDetBoxes(j).replace(defaultDecimalSeparator, '.'), // pallets
                e.getDetUnits(j).replace(defaultDecimalSeparator, '.'), // quantity
                "0", // hazmat
                e.getDetDateType(j), // datetype
                e.getDetDate(j), // date
                e.getDetTimeType1(j),// timetype1
                e.getDetTime1(j), // time
                e.getDetTimeType1(j), // timetype2
                e.getDetTimeType2(j), // time2
                e.getDetTimeZone(j), // timezone
                e.getDetRate(j), // rate 
                e.getDetMiles(j) // miles
                );  
                list.add(y);
               } // for each det
            
            if (m[0].isBlank()) { // if not error
                m = addCFOTransaction(list, x, null, null);
            } 
            System.out.println("HERE: " + m[0] + "/" + m[1]);
            return m;
     }
     
    public static void createFOTDETFrom214(edi214 e, String[] control) {
             control[7] = e.order;
             EDData.writeEDILog(control, "INFO", "Load");
             EDData.CreateFOTDETFrom214i(control, e.order, e.scac, e.pronbr, e.status, e.remarks, e.lat, e.lon, e.equipmentnbr, e.equipmenttype, e.apptdate, e.appttime);   
    
    }
      
    public static void createOrderFromCSV(ArrayList e, String[] control) {
        
        int sonbr = 0;
        boolean error = false;
        String shipto = "";
        
       
        for (int i = 0; i < e.size(); i++) {
            error = false;
             sonbr = OVData.getNextNbr("order");
             control[7] = ((edi850) e.get(i)).po;
             EDData.writeEDILog(control, "INFO", "Load");
           //  control = ((edi850)e.get(i)).isaSenderID + "," + ((edi850)e.get(i)).doctype + "," + ((edi850)e.get(i)).isaCtrlNum + "," + ((edi850)e.get(i)).po ;
             
             if (((edi850) e.get(i)).ov_shipto.isEmpty())
                 shipto = OVData.CreateShipTo(((edi850) e.get(i)).ov_billto, ((edi850) e.get(i)).st_name, ((edi850) e.get(i)).st_line1, ((edi850) e.get(i)).st_line2, ((edi850) e.get(i)).st_line3, ((edi850) e.get(i)).st_city, ((edi850) e.get(i)).st_state, ((edi850) e.get(i)).st_zip, ((edi850) e.get(i)).st_country, ((edi850) e.get(i)).shipto);
             else
                 shipto = ((edi850) e.get(i)).ov_shipto;
             
             EDData.CreateSalesOrderHdr(control, String.valueOf(sonbr), ((edi850) e.get(i)).ov_billto, shipto, ((edi850) e.get(i)).po, ((edi850) e.get(i)).duedate, ((edi850) e.get(i)).podate, ((edi850) e.get(i)).remarks, ((edi850) e.get(i)).shipmethod);
               for (int j = 0; j < ((edi850) e.get(i)).getDetCount(); j++ ) {
                 
                   // error trigger logic ...missing internal item
                   if (((edi850) e.get(i)).getDetItem(j).isEmpty())
                      error = true;
                   
                   // error trigger logic ...missing netprice = 0
                   if ( Double.valueOf(((edi850) e.get(i)).getDetNetPrice(j)) == 0)
                      error = true;
                  
               EDData.CreateSalesOrderDet(String.valueOf(sonbr), 
                       ((edi850) e.get(i)).ov_billto,
                       ((edi850) e.get(i)).getDetItem(j), 
                       ((edi850) e.get(i)).getDetCustItem(j), 
                       ((edi850) e.get(i)).getDetSku(j), 
                       ((edi850) e.get(i)).getDetPO(j), 
                       ((edi850) e.get(i)).getDetQty(j),
                       ((edi850) e.get(i)).getDetListPrice(j), 
                       ((edi850) e.get(i)).getDetDisc(j), 
                       ((edi850) e.get(i)).getDetNetPrice(j),
                       ((edi850) e.get(i)).duedate, 
                       ((edi850) e.get(i)).getDetRef(j),
                       String.valueOf(j + 1)); 
               // System.out.println(((edi850)e.get(i)).getDetCustItem(j));
               }
       
        if (error)
            OVData.updateOrderStatusError(String.valueOf(sonbr));
        }
    
    }
     
    public static void createOrderFromXML(ArrayList e, String[] control) {
        
        int sonbr = 0;
        boolean error = false;
        String shipto = "";
       
        for (int i = 0; i < e.size(); i++) {
            error = false;
             sonbr = OVData.getNextNbr("order");
             control[7] = ((edi850) e.get(i)).po;
             EDData.writeEDILog(control, "INFO", "Load");
           //  control = ((edi850)e.get(i)).isaSenderID + "," + ((edi850)e.get(i)).doctype + "," + ((edi850)e.get(i)).isaCtrlNum + "," + ((edi850)e.get(i)).po ;
             
             if (((edi850) e.get(i)).ov_shipto.isEmpty())
                 shipto = OVData.CreateShipTo(((edi850) e.get(i)).ov_billto, ((edi850) e.get(i)).st_name, ((edi850) e.get(i)).st_line1, ((edi850) e.get(i)).st_line2, ((edi850) e.get(i)).st_line3, ((edi850) e.get(i)).st_city, ((edi850) e.get(i)).st_state, ((edi850) e.get(i)).st_zip, ((edi850) e.get(i)).st_country, ((edi850) e.get(i)).shipto);
             else
                 shipto = ((edi850) e.get(i)).ov_shipto;
             
             EDData.CreateSalesOrderHdr(control, String.valueOf(sonbr), ((edi850) e.get(i)).ov_billto, shipto, ((edi850) e.get(i)).po, ((edi850) e.get(i)).duedate, ((edi850) e.get(i)).podate, ((edi850) e.get(i)).remarks, ((edi850) e.get(i)).shipmethod);
               for (int j = 0; j < ((edi850) e.get(i)).getDetCount(); j++ ) {
                 
                   // error trigger logic ...missing internal item
                   if (((edi850) e.get(i)).getDetItem(j).isEmpty())
                      error = true;
                   
                   // error trigger logic ...missing netprice = 0
                   if ( Double.valueOf(((edi850) e.get(i)).getDetNetPrice(j)) == 0)
                      error = true;
                  
               EDData.CreateSalesOrderDet(String.valueOf(sonbr), 
                       ((edi850) e.get(i)).ov_billto,
                       ((edi850) e.get(i)).getDetItem(j), 
                       ((edi850) e.get(i)).getDetCustItem(j), 
                       ((edi850) e.get(i)).getDetSku(j), 
                       ((edi850) e.get(i)).getDetPO(j), 
                       ((edi850) e.get(i)).getDetQty(j),
                       ((edi850) e.get(i)).getDetListPrice(j), 
                       ((edi850) e.get(i)).getDetDisc(j), 
                       ((edi850) e.get(i)).getDetNetPrice(j),
                       ((edi850) e.get(i)).duedate, 
                       ((edi850) e.get(i)).getDetRef(j),
                       String.valueOf(j + 1)); 
               // System.out.println(((edi850)e.get(i)).getDetCustItem(j));
               }
       
        if (error)
            OVData.updateOrderStatusError(String.valueOf(sonbr));
        }
    
    }
     
      
    // outbound
    
    public static int Create856(String shipper)  {
        int errorcode = 0;
        // errorcode = 0 ... clean exit
        // errorcode = 1 ... no record found in getEDIXrefOut/getEDITPDefaults
        // errorcode = 2 ... any catch error below ...try running from command line to see trace dump
        // errorcode = 3 ... error in map...see edi log
        
        String billto = "";
        String doctype = "856";
        String map = "";
        ArrayList<String[]> messages = new ArrayList<String[]>();
         
        // lets determine the billto of this shipper
        billto = shpData.getShipperBillto(shipper);
        
        
        messages.add(new String[]{"info","exporting shipper: " + shipper + " for billto: " + billto});
        
        int comkey = OVData.getNextNbr("edilog");
        
        String[] c = initEDIControl();   
        
        c[1] = doctype;
        c[2] = map;
        c[3] = "";
        c[4] = "";
        c[5] = "";
        c[6] = "";
        c[7] = shipper;
        c[15] = "0"; // dir out
        c[12] = "0"; // is override
        c[22] = String.valueOf(comkey);
        c[28] = "DB";
        c[17] = "0";
        c[18] = "999999";
        c[19] = "0";
        c[20] = "999999";
        
        // get Delimiters from Cust Defaults
        
        String[] ids = EDData.getEDIXrefOut(billto, "ST");
        messages.add(new String[]{"info","edi_xref: " + ids[0] + "/" + ids[1] + "/" + ids[2] + "/" + ids[3] + "/" + ids[4]});
        
        String[] defaults = EDData.getEDITPDefaults(doctype, EDData.getEDIgsid(), ids[1]  ); //810, ourGS, theirsGS
        messages.add(new String[]{"info","edi_mstr (id,doc): " + defaults[18] + "/" + defaults[19]});
        messages.add(new String[]{"info","edi_mstr (sndISA/GS,rcvISA/GS): " + defaults[0] + "/" + defaults[2] + "/" + defaults[3] + "/" + defaults[5]});
        
        c[9] = defaults[7]; 
        c[10] = defaults[6]; 
        c[11] = defaults[8]; 
        
        c[0] = defaults[2];
        c[21] = defaults[5];
        
        int idxnbr = EDData.writeEDIIDX(c);
        c[16] = String.valueOf(idxnbr);
        
        
        
        
        messages.add(new String[]{"info","searching for map with: " + c[1] + "/" + defaults[2] + "/" + defaults[5]});
        map = EDData.getEDIMap(c[1], defaults[2], defaults[5]);
        
        if (map.isEmpty()) {
            errorcode = 1;
            messages.add(new String[]{"error","856: map String is empty for billto/gs02/gs03/doc: " + billto + "/" + defaults[2] + "/" + defaults[5] + " / " + c[1]});
            EDData.writeEDILogMulti(c, messages);
            messages.clear();  // clear message here
            return errorcode;
        } 
          
        if (! BlueSeerUtils.isEDIClassFile(map)) {
            errorcode = 1;
            messages.add(new String[]{"error","856: unable to locate compiled map (" + map + ") billto/gs02/gs03/doc: " + billto + "/" + defaults[2] + "/" + defaults[5] + " / " + c[1]});
            EDData.writeEDILogMulti(c, messages);
            messages.clear();  // clear message here
            return errorcode;
        }   
          
          
        messages.add(new String[]{"info","using map: " + map});
       
        
        // Mapdata method call below requires two parameters (ArrayList, String[]) ...doc and c
        ArrayList doc = new ArrayList();
        doc.add(shipper);
        
        
         // call map 
        try {
        Class cls = Class.forName(map);
        Object obj = cls.getDeclaredConstructor().newInstance();
        Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class, ArrayList.class);
        Object oc = method.invoke(obj, doc, c, messages);
        String[] oString = (String[]) oc;
        messages.add(new String[]{oString[0], oString[1]});
        EDData.updateEDIIDX(idxnbr, c); 
        if (oString[0].equals("error")) {
            errorcode = 3;
        }
        } catch (InvocationTargetException ex) {
        errorcode = 2;    
        if (c[12].isEmpty()) {
        messages.add(new String[]{"error", "invocation exception in map class " + map + "/" + c[0] + " / " + c[1]});    
        clearStaticVariables();
        }
        edilog(ex); 
        } catch (ClassNotFoundException ex) {
        errorcode = 2;    
        if (c[12].isEmpty()) {
        messages.add(new String[]{"error", "Map Class not found " + map + "/" + c[0] + " / " + c[1]});        
        }
        edilog(ex); 
        } catch (IllegalAccessException |
             InstantiationException | NoSuchMethodException ex
            ) {
        errorcode = 2;    
        if (c[12].isEmpty()) {
        messages.add(new String[]{"error", "IllegalAccess|Instantiation|NoSuchMethod " + map + "/" + c[0] + " / " + c[1]});        
       }
        edilog(ex);
       } finally {
          EDData.writeEDILogMulti(c, messages);
          messages.clear();  // clear message here...and at 997...and at end   
       }
         
       return errorcode; 
         
     }
    
    public static int Create855(String order)  {
        int errorcode = 0;
        // errorcode = 0 ... clean exit
        // errorcode = 1 ... no record found in getEDIXrefOut/getEDITPDefaults
        // errorcode = 2 ... any catch error below ...try running from command line to see trace dump
        // errorcode = 3 ... error in map...see edi log
        
        String billto = "";
        String doctype = "855";
        String map = "";
        ArrayList<String[]> messages = new ArrayList<String[]>();
         
        // lets determine the billto of this shipper
        billto = ordData.getSOOrderBillto(order);
        
        
        messages.add(new String[]{"info","exporting ack(855): " + order + " for billto: " + billto});
        
        int comkey = OVData.getNextNbr("edilog");
        
        String[] c = initEDIControl();   
        
        c[1] = doctype;
        c[2] = map;
        c[3] = "";
        c[4] = "";
        c[5] = "";
        c[6] = "";
        c[7] = order;
        c[15] = "0"; // dir out
        c[12] = "0"; // is override
        c[22] = String.valueOf(comkey);
        c[28] = "DB";
        c[17] = "0";
        c[18] = "999999";
        c[19] = "0";
        c[20] = "999999";
        
        // get Delimiters from Cust Defaults
        String[] ids = EDData.getEDIXrefOut(billto, "ST");
        messages.add(new String[]{"info","edi_xref: " + ids[0] + "/" + ids[1] + "/" + ids[2] + "/" + ids[3] + "/" + ids[4]});
        // tpid, gsid, tpaddr, ovaddr, type
        
        String[] defaults = EDData.getEDITPDefaults(doctype, EDData.getEDIgsid(), ids[1]  ); //810, ourGS, theirsGS
        messages.add(new String[]{"info","edi_mstr (id,doc): " + defaults[18] + "/" + defaults[19]});
        messages.add(new String[]{"info","edi_mstr (sndISA/GS,rcvISA/GS): " + defaults[0] + "/" + defaults[2] + "/" + defaults[3] + "/" + defaults[5]});
        
        c[9] = defaults[7]; 
        c[10] = defaults[6]; 
        c[11] = defaults[8]; 
        
        c[0] = defaults[2];
        c[21] = defaults[5];
        
        int idxnbr = EDData.writeEDIIDX(c);
        c[16] = String.valueOf(idxnbr);
        
        messages.add(new String[]{"info","searching for map with: " + c[1] + "/" + defaults[2] + "/" + defaults[5]});
        map = EDData.getEDIMap(c[1], defaults[2], defaults[5]);
        
        if (map.isEmpty()) {
            errorcode = 1;
            messages.add(new String[]{"error","855 map string is empty for billto/gs02/gs03/doc: " + billto + "/" + defaults[2] + "/" + defaults[5] + " / " + c[1]});
            EDData.writeEDILogMulti(c, messages);
            messages.clear();  // clear message here
            return errorcode;
        } 
          
         if (! BlueSeerUtils.isEDIClassFile(map)) {
            errorcode = 1;
            messages.add(new String[]{"error","855: unable to locate compiled map (" + map + ") billto/gs02/gs03/doc: " + billto + "/" + defaults[2] + "/" + defaults[5] + " / " + c[1]});
            EDData.writeEDILogMulti(c, messages);
            messages.clear();  // clear message here
            return errorcode;
        }     
          
        messages.add(new String[]{"info","using map: " + map});
       
        
        // Mapdata method call below requires two parameters (ArrayList, String[]) ...doc and c
        ArrayList doc = new ArrayList();
        doc.add(order);
        
        
         // call map 
        try {
        Class cls = Class.forName(map);
        Object obj = cls.getDeclaredConstructor().newInstance();
        Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class, ArrayList.class);
        Object oc = method.invoke(obj, doc, c, messages);
        String[] oString = (String[]) oc;
        messages.add(new String[]{oString[0], oString[1]});
        EDData.updateEDIIDX(idxnbr, c); 
        if (oString[0].equals("error")) {
            errorcode = 3;
        }
        } catch (InvocationTargetException ex) {
        errorcode = 2;    
        if (c[12].isEmpty()) {
        messages.add(new String[]{"error", "invocation exception in map class " + map + "/" + c[0] + " / " + c[1]});    
        clearStaticVariables();
        }
        edilog(ex); 
        } catch (ClassNotFoundException ex) {
        errorcode = 2;    
        if (c[12].isEmpty()) {
        messages.add(new String[]{"error", "Map Class not found " + map + "/" + c[0] + " / " + c[1]});        
        }
        edilog(ex); 
        } catch (IllegalAccessException |
             InstantiationException | NoSuchMethodException ex
            ) {
        errorcode = 2;    
        if (c[12].isEmpty()) {
        messages.add(new String[]{"error", "IllegalAccess|Instantiation|NoSuchMethod " + map + "/" + c[0] + " / " + c[1]});        
       }
        edilog(ex);
       } finally {
          EDData.writeEDILogMulti(c, messages);
          messages.clear();  // clear message here...and at 997...and at end   
       }
         
       return errorcode; 
         
     }
         
    public static int Create810(String shipper)  {
        int errorcode = 0;
        // errorcode = 0 ... clean exit
        // errorcode = 1 ... no record found in getEDIXrefOut/getEDITPDefaults
        // errorcode = 2 ... any catch error below ...try running from command line to see trace dump
        // errorcode = 3 ... error in map...see edi log
        
        String billto = "";
        String doctype = "810";
        String map = "";
        ArrayList<String[]> messages = new ArrayList<String[]>();
         
        // lets determine the billto of this shipper
        billto = shpData.getShipperBillto(shipper);
        
        
        messages.add(new String[]{"info","exporting invoice: " + shipper + " for billto: " + billto});
        
        int comkey = OVData.getNextNbr("edilog");
        
        String[] c = initEDIControl();   
        
        c[1] = doctype;
        c[2] = map;
        c[3] = "";
        c[4] = "";
        c[5] = "";
        c[6] = "";
        c[7] = shipper;
        c[15] = "0"; // dir out
        c[12] = "0"; // is override
        c[22] = String.valueOf(comkey);
        c[28] = "DB";
        c[17] = "0";
        c[18] = "999999";
        c[19] = "0";
        c[20] = "999999";
        
        // get Delimiters from Cust Defaults
        
        String[] ids = EDData.getEDIXrefOut(billto, "PY");
        messages.add(new String[]{"info","edi_xref: " + ids[0] + "/" + ids[1] + "/" + ids[2] + "/" + ids[3] + "/" + ids[4]});
        
        String[] defaults = EDData.getEDITPDefaults(doctype, EDData.getEDIgsid(), ids[1]  ); //810, ourGS, theirsGS
        messages.add(new String[]{"info","edi_mstr (id,doc): " + defaults[18] + "/" + defaults[19]});
        messages.add(new String[]{"info","edi_mstr (sndISA/GS,rcvISA/GS): " + defaults[0] + "/" + defaults[2] + "/" + defaults[3] + "/" + defaults[5]});
        
        c[9] = defaults[7]; 
        c[10] = defaults[6]; 
        c[11] = defaults[8]; 
        
        c[0] = defaults[2];
        c[21] = defaults[5];
        
        int idxnbr = EDData.writeEDIIDX(c);
        c[16] = String.valueOf(idxnbr);
        
        messages.add(new String[]{"info","searching for map with: " + c[1] + "/" + defaults[2] + "/" + defaults[5]});
        map = EDData.getEDIMap(c[1], defaults[2], defaults[5]);
        
          if (map.isEmpty()) {
            errorcode = 1;
            messages.add(new String[]{"error","810: map variable is empty for billto/gs02/gs03/doc: " + billto + "/" + defaults[2] + "/" + defaults[5] + " / " + c[1]});
            EDData.writeEDILogMulti(c, messages);
            messages.clear();  // clear message here
            return errorcode;
        } 
          
        if (! BlueSeerUtils.isEDIClassFile(map)) {
            errorcode = 1;
            messages.add(new String[]{"error","810: unable to locate compiled map (" + map + ") billto/gs02/gs03/doc: " + billto + "/" + defaults[2] + "/" + defaults[5] + " / " + c[1]});
            EDData.writeEDILogMulti(c, messages);
            messages.clear();  // clear message here
            return errorcode;
        }     
        messages.add(new String[]{"info","using map: " + map});
       
        
        // Mapdata method call below requires two parameters (ArrayList, String[]) ...doc and c
        ArrayList doc = new ArrayList();
        doc.add(shipper);
        
        
         // call map 
        try {
        Class cls = Class.forName(map);
        Object obj = cls.getDeclaredConstructor().newInstance();
        Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class, ArrayList.class);
        Object oc = method.invoke(obj, doc, c, messages);
        String[] oString = (String[]) oc;
        messages.add(new String[]{oString[0], oString[1]});
        EDData.updateEDIIDX(idxnbr, c); 
        if (oString[0].equals("error")) {
            errorcode = 3;
        }
        } catch (InvocationTargetException ex) {
        errorcode = 2;    
        if (c[12].isEmpty()) {
        messages.add(new String[]{"error", "invocation exception in map class " + map + "/" + c[0] + " / " + c[1]});    
        clearStaticVariables();
        }
        edilog(ex); 
        } catch (ClassNotFoundException ex) {
        errorcode = 2;    
        if (c[12].isEmpty()) {
        messages.add(new String[]{"error", "Map Class not found " + map + "/" + c[0] + " / " + c[1]});        
        }
        edilog(ex); 
        } catch (IllegalAccessException |
             InstantiationException | NoSuchMethodException ex
            ) {
        errorcode = 2;    
        if (c[12].isEmpty()) {
        messages.add(new String[]{"error", "IllegalAccess|Instantiation|NoSuchMethod " + map + "/" + c[0] + " / " + c[1]});        
       }
        edilog(ex);
       } finally {
          EDData.writeEDILogMulti(c, messages);
          messages.clear();  // clear message here...and at 997...and at end   
       }
         
       return errorcode; 
         
     }
    
    public static int Create850(String po)  {
        int errorcode = 0;
        // errorcode = 0 ... clean exit
        // errorcode = 1 ... no record found in getEDIXrefOut/getEDITPDefaults
        // errorcode = 2 ... any catch error below ...try running from command line to see trace dump
        // errorcode = 3 ... error in map...see edi log
        
        String vendor = "";
        String doctype = "850";
        String map = "";
        ArrayList<String[]> messages = new ArrayList<String[]>();
         
        // lets determine the billto of this shipper
        vendor = purData.getPOVendor(po);
        
        
        messages.add(new String[]{"info","exporting Purchase Order: " + po + " for billto: " + vendor});
        
        int comkey = OVData.getNextNbr("edilog");
        
        String[] c = initEDIControl();   
        
        c[1] = doctype;
        c[2] = map;
        c[3] = "";
        c[4] = "";
        c[5] = "";
        c[6] = "";
        c[7] = po;
        c[15] = "0"; // dir out
        c[12] = "0"; // is override
        c[22] = String.valueOf(comkey);
        c[28] = "DB";
        c[17] = "0";
        c[18] = "999999";
        c[19] = "0";
        c[20] = "999999";
        
        // get Delimiters from Cust Defaults
        
        String[] ids = EDData.getEDIXrefOut(vendor, "VN");
        messages.add(new String[]{"info","edi_xref: " + ids[0] + "/" + ids[1] + "/" + ids[2] + "/" + ids[3] + "/" + ids[4]});
        
        String[] defaults = EDData.getEDITPDefaults(doctype, EDData.getEDIgsid(), ids[1]  ); //810, ourGS, theirsGS
        messages.add(new String[]{"info","edi_mstr (id,doc): " + defaults[18] + "/" + defaults[19]});
        messages.add(new String[]{"info","edi_mstr (sndISA/GS,rcvISA/GS): " + defaults[0] + "/" + defaults[2] + "/" + defaults[3] + "/" + defaults[5]});
        
        c[9] = defaults[7]; 
        c[10] = defaults[6]; 
        c[11] = defaults[8]; 
        
        c[0] = defaults[2];
        c[21] = defaults[5];
        
        int idxnbr = EDData.writeEDIIDX(c);
        c[16] = String.valueOf(idxnbr);
        
        messages.add(new String[]{"info","searching for map with: " + c[1] + "/" + defaults[2] + "/" + defaults[5]});
        map = EDData.getEDIMap(c[1], defaults[2], defaults[5]);
        
          if (map.isEmpty()) {
            errorcode = 1;
            messages.add(new String[]{"error","850o: map variable is empty for billto/gs02/gs03/doc: " + vendor + "/" + defaults[2] + "/" + defaults[5] + " / " + c[1]});
            EDData.writeEDILogMulti(c, messages);
            messages.clear();  // clear message here
            return errorcode;
        } 
        
        if (! BlueSeerUtils.isEDIClassFile(map)) {
            errorcode = 1;
            messages.add(new String[]{"error","850o: unable to locate compiled map (" + map + ") billto/gs02/gs03/doc: " + vendor + "/" + defaults[2] + "/" + defaults[5] + " / " + c[1]});
            EDData.writeEDILogMulti(c, messages);
            messages.clear();  // clear message here
            return errorcode;
        }     
          
        messages.add(new String[]{"info","using map: " + map});
       
        
        // Mapdata method call below requires two parameters (ArrayList, String[]) ...doc and c
        ArrayList doc = new ArrayList();
        doc.add(po);
        
        
         // call map 
        try {
        Class cls = Class.forName(map);
        Object obj = cls.getDeclaredConstructor().newInstance();
        Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class, ArrayList.class);
        Object oc = method.invoke(obj, doc, c, messages);
        String[] oString = (String[]) oc;
        messages.add(new String[]{oString[0], oString[1]});
        EDData.updateEDIIDX(idxnbr, c); 
        if (oString[0].equals("error")) {
            errorcode = 3;
        }
        } catch (InvocationTargetException ex) {
        errorcode = 2;    
        if (c[12].isEmpty()) {
        messages.add(new String[]{"error", "invocation exception in map class " + map + "/" + c[0] + " / " + c[1]});    
        clearStaticVariables();
        }
        edilog(ex); 
        } catch (ClassNotFoundException ex) {
        errorcode = 2;    
        if (c[12].isEmpty()) {
        messages.add(new String[]{"error", "Map Class not found " + map + "/" + c[0] + " / " + c[1]});        
        }
        edilog(ex); 
        } catch (IllegalAccessException |
             InstantiationException | NoSuchMethodException ex
            ) {
        errorcode = 2;    
        if (c[12].isEmpty()) {
        messages.add(new String[]{"error", "IllegalAccess|Instantiation|NoSuchMethod " + map + "/" + c[0] + " / " + c[1]});        
       }
        edilog(ex);
       } finally {
          EDData.writeEDILogMulti(c, messages);
          messages.clear();  // clear message here...and at 997...and at end   
       }
         
       return errorcode; 
         
     }
    
    
    public static int Create940(String nbr)  {
        int errorcode = 0;
       
        // errorcode = 0 ... clean exit
        // errorcode = 1 ... no record found in wh_mstr for wh doctype
        // errorcode = 2 ... unable to retrieve wh from order
        // errorcode = 3 ... any catch error below ...try running from command line to see trace dump
        // errorcode = 4 ... shipper does not exist
        
        
        ArrayList<String> wh = new ArrayList();
        String doctype = "940";
        String map = "";
        String[] control = null;
        
        // lets determine the warehouse or warehouses for this order
        wh = OVData.getOrderWHSource(nbr);
        if (wh.size() == 0) {
            return 2;
        }
        
         
        for (String w : wh) {   
        
        map = EDData.getEDIMap(w, doctype, "");
        
         String[] c_in = initEDIControl();   // controlarray in this order : entity, doctype, map, filename, isacontrolnum, gsctrlnum, stctrlnum, ref ; 
       
        c_in[0] = w;
        c_in[1] = doctype;
        c_in[2] = map;
        c_in[3] = "";
        c_in[4] = "";
        c_in[5] = "";
        c_in[6] = "";
        c_in[7] = nbr;
        c_in[15] = "0"; // dir out
        c_in[12] = "0"; // is override
        
        // get Delimiters from Cust Defaults
        String[] defaults = EDData.getEDITPDefaults(doctype, w, "");
        c_in[9] = defaults[7]; 
        c_in[10] = defaults[6]; 
        c_in[11] = defaults[8]; 
        
        
        if (map.isEmpty()) {
            EDData.writeEDILog(c_in, "error", "no edi_mstr map for " + w + " / " + doctype);
            errorcode = 1;
                   continue;
        } 
        
         // Mapdata method call below requires two parameters (ArrayList, String[]) ...doc and c
        ArrayList doc = new ArrayList();
        doc.add(nbr);
                   
                      try {
                    Class cls = Class.forName(map);
                    Object obj = cls.getDeclaredConstructor().newInstance();
                    Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class, ArrayList.class);
               
                    
                    Object envelope = method.invoke(obj, doc, c_in); // envelope array holds in this order (isa, gs, ge, iea, filename, isactrlnum, gsctrlnum, stctrlnum)
                    String[] c_out = (String[])envelope;
                    
                    EDData.writeEDILog(c_out, "INFO", "Export"); 
                    EDData.CreateFreightEDIRecs(c_in, nbr);
                    

                    } catch (IllegalAccessException | ClassNotFoundException |
                             InstantiationException | NoSuchMethodException |
                            InvocationTargetException ex) {
                        EDData.writeEDILog(c_in, "error", "unable to find map class or invocation error for " + w + " / " + doctype);
                        clearStaticVariables();
                        errorcode = 3;
                        edilog(ex);
                    }
                    
                    
           
        }  // each warehouse to receive 940
        
        // now lets set the order issourced flag if errorcode still equals 0
        if (errorcode == 0) {
           // EDData.updateOrderSourceFlag(nbr);
        }
        
        return errorcode;
     }
     
    public static int Create219(String nbr, String type)  {
        int errorcode = 0;
       
        // errorcode = 0 ... clean exit
        // errorcode = 1 ... no record found in wh_mstr for wh doctype
        // errorcode = 2 ... unable to retrieve carriers from order
        // errorcode = 3 ... any catch error below ...try running from command line to see trace dump
        // errorcode = 4 ... shipper does not exist
        
        
        ArrayList<String> cars = new ArrayList();
        String doctype = "219";
        String map = "";
        
        // lets determine the list of carriers to quote for this freight order
        // first we need the proposed carrier or carrier list to send quote to
        cars = OVData.getfreightlist();
        if (cars.size() == 0) {
            return 2;
        }
        
         
        for (String ca : cars) {   
            
        
        map = EDData.getEDIMap(ca, doctype, ""); 
        String[] c_in = initEDIControl();   // controlarray in this order : entity, doctype, map, filename, isacontrolnum, gsctrlnum, stctrlnum, ref ; 
        
        c_in[0] = ca;
        c_in[1] = doctype;
        c_in[2] = map;
        c_in[3] = "";
        c_in[4] = "";
        c_in[5] = "";
        c_in[6] = "";
        c_in[7] = nbr;
        c_in[15] = "0"; // dir out
        c_in[12] = "0"; // is override
        
        // get Delimiters from Cust Defaults
        String[] defaults = EDData.getEDITPDefaults(ca, doctype, "");
        c_in[9] = defaults[7]; 
        c_in[10] = defaults[6]; 
        c_in[11] = defaults[8]; 
        
        if (map.isEmpty()) {
            EDData.writeEDILog(c_in, "error", "no edi_mstr map for " + ca + " / " + doctype);
            errorcode = 1;
                   continue;
        } 
        
         // Mapdata method call below requires two parameters (ArrayList, String[]) ...doc and c
        ArrayList doc = new ArrayList();
        doc.add(nbr);
        
                    try {
                     Class cls = Class.forName(map);
                    Object obj = cls.getDeclaredConstructor().newInstance();
                    Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class, ArrayList.class);
               
                    
                    Object envelope = method.invoke(obj, doc, c_in); // envelope array holds in this order (isa, gs, ge, iea, filename, isactrlnum, gsctrlnum, stctrlnum)
                    String[] c_out = (String[])envelope;
                    
                    EDData.writeEDILog(c_out, "INFO", "Export"); 
                    EDData.CreateFreightEDIRecs(c_in, nbr);

                    } catch (IllegalAccessException | ClassNotFoundException |
                             InstantiationException | NoSuchMethodException |
                            InvocationTargetException ex) {
                        EDData.writeEDILog(c_in, "error", "unable to find map class or invocation error for " + ca + " / " + doctype);
                        clearStaticVariables();
                        errorcode = 3;
                        edilog(ex);
                    }
        
                   
        
                   
           
        }  // each warehouse to receive 940
        
        // now lets set the order issourced flag if errorcode still equals 0
        if (errorcode == 0) {
           // EDData.updateOrderSourceFlag(nbr);
        }
        
        return errorcode;
     }
    
    public static int Create990(String nbr)  {
        int errorcode = 0;
        // errorcode = 0 ... clean exit
        // errorcode = 1 ... no record found in getEDIXrefOut/getEDITPDefaults
        // errorcode = 2 ... any catch error below ...try running from command line to see trace dump
        // errorcode = 3 ... error in map...see edi log
        
        String tp = "";
        String doctype = "990db";
        String map = "";
        ArrayList<String[]> messages = new ArrayList<String[]>();
         
        // lets determine the billto of this shipper
        tp = frtData.getCFOCust(nbr);
        
        
        messages.add(new String[]{"info","exporting 990 response: " + nbr + " for partner: " + tp});
        
        int comkey = OVData.getNextNbr("edilog");
        
        String[] c = initEDIControl();   
        
        c[1] = doctype;
        c[2] = map;
        c[3] = "";
        c[4] = "";
        c[5] = "";
        c[6] = "";
        c[7] = nbr;
        c[15] = "990x12"; // outdoctype
        c[12] = "0"; // is override
        c[22] = String.valueOf(comkey);
        c[28] = "DB";
        c[29] = "X12";
        c[17] = "0";
        c[18] = "999999";
        c[19] = "0";
        c[20] = "999999";
        
        // get Delimiters from Cust Defaults
        
        String[] ids = EDData.getEDIXrefOut(tp, "PY");
        messages.add(new String[]{"info","edi_xref: " + ids[0] + "/" + ids[1] + "/" + ids[2] + "/" + ids[3] + "/" + ids[4]});
        
        String[] defaults = EDData.getEDITPDefaults(doctype, EDData.getEDIgsid(), ids[1]  ); //990db, ourGS, theirsGS
        messages.add(new String[]{"info","edi_mstr (id,doc): " + defaults[18] + "/" + defaults[19]});
        messages.add(new String[]{"info","edi_mstr (sndISA/GS,rcvISA/GS): " + defaults[0] + "/" + defaults[2] + "/" + defaults[3] + "/" + defaults[5]});
        
        c[9] = defaults[7]; 
        c[10] = defaults[6]; 
        c[11] = defaults[8]; 
        
        c[0] = defaults[2];
        c[21] = defaults[5];
        
        int idxnbr = EDData.writeEDIIDX(c);
        c[16] = String.valueOf(idxnbr);
        
        messages.add(new String[]{"info","searching for map with: " + c[1] + "/" + defaults[2] + "/" + defaults[5]});
        map = EDData.getEDIMap(c[1], defaults[2], defaults[5]);
        
          if (map.isEmpty()) {
            errorcode = 1;
            messages.add(new String[]{"error","990: map variable is empty for partner/gs02/gs03/doc: " + tp + "/" + defaults[2] + "/" + defaults[5] + " / " + c[1]});
            EDData.writeEDILogMulti(c, messages);
            messages.clear();  // clear message here
            return errorcode;
        } 
          
        if (! BlueSeerUtils.isEDIClassFile(map)) {
            errorcode = 1;
            messages.add(new String[]{"error","990: unable to locate compiled map (" + map + ") billto/gs02/gs03/doc: " + tp + "/" + defaults[2] + "/" + defaults[5] + " / " + c[1]});
            EDData.writeEDILogMulti(c, messages);
            messages.clear();  // clear message here
            return errorcode;
        }     
        messages.add(new String[]{"info","using map: " + map});
       
        
        // Mapdata method call below requires two parameters (ArrayList, String[]) ...doc and c
        ArrayList doc = new ArrayList();
        doc.add(nbr);
        
        
         // call map 
        try {
        Class cls = Class.forName(map);
        Object obj = cls.getDeclaredConstructor().newInstance();
        Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class, ArrayList.class);
        Object oc = method.invoke(obj, doc, c, messages);
        String[] oString = (String[]) oc;
        messages.add(new String[]{oString[0], oString[1]});
        EDData.updateEDIIDX(idxnbr, c); 
        if (oString[0].equals("error")) {
            errorcode = 3;
        }
        } catch (InvocationTargetException ex) {
        errorcode = 2;    
        if (c[12].isEmpty()) {
        messages.add(new String[]{"error", "invocation exception in map class " + map + "/" + c[0] + " / " + c[1]});    
        clearStaticVariables();
        }
        edilog(ex); 
        } catch (ClassNotFoundException ex) {
        errorcode = 2;    
        if (c[12].isEmpty()) {
        messages.add(new String[]{"error", "Map Class not found " + map + "/" + c[0] + " / " + c[1]});        
        }
        edilog(ex); 
        } catch (IllegalAccessException |
             InstantiationException | NoSuchMethodException ex
            ) {
        errorcode = 2;    
        if (c[12].isEmpty()) {
        messages.add(new String[]{"error", "IllegalAccess|Instantiation|NoSuchMethod " + map + "/" + c[0] + " / " + c[1]});        
       }
        edilog(ex);
       } finally {
          EDData.writeEDILogMulti(c, messages);
          messages.clear();  // clear message here...and at 997...and at end   
       }
         
       return errorcode; 
         
     }
    
    
    public static int Create990o(String nbr, String response)  {
        int errorcode = 0;
       
        // errorcode = 0 ... clean exit
        // errorcode = 1 ... no record found in wh_mstr for wh doctype
        // errorcode = 2 ... unable to retrieve carriers from order
        // errorcode = 3 ... any catch error below ...try running from command line to see trace dump
        // errorcode = 4 ... shipper does not exist
        
        
        
        String doctype = "990";
        String map = "";
        
        
      
        
         String tp = OVData.getFreightOrderCarrierAssigned(nbr);
       
            
        
        map = EDData.getEDIMap(tp, doctype, ""); 
        
        String[] c_in = initEDIControl();   // controlarray in this order : entity, doctype, map, filename, isacontrolnum, gsctrlnum, stctrlnum, ref ; 
        
         c_in[0] = tp;
        c_in[1] = doctype;
        c_in[2] = map;
        c_in[3] = "";
        c_in[4] = "";
        c_in[5] = "";
        c_in[6] = "";
        c_in[7] = nbr;
        c_in[15] = "0"; // dir out
        c_in[12] = "0"; // is override
        
        // get Delimiters from Cust Defaults
        String[] defaults = EDData.getEDITPDefaults(tp, doctype, "");
        c_in[9] = defaults[7]; 
        c_in[10] = defaults[6]; 
        c_in[11] = defaults[8]; 
        
        if (tp.isEmpty()) {
            EDData.writeEDILog(c_in, "error", "no carrier for this freight order " + tp + " / " + doctype);
            errorcode = 1;
            return errorcode;
        } 
        
        if (map.isEmpty()) {
            EDData.writeEDILog(c_in, "error", "no edi_mstr map for " + tp + " / " + doctype);
            errorcode = 1;
            return errorcode;
        } 
        
        // Mapdata method call below requires two parameters (ArrayList, String[]) ...doc and c
        ArrayList doc = new ArrayList();
        doc.add(nbr);
        
                    try {
                   Class cls = Class.forName(map);
                    Object obj = cls.getDeclaredConstructor().newInstance();
                    Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class, ArrayList.class);
               
                    
                    Object envelope = method.invoke(obj, doc, c_in); // envelope array holds in this order (isa, gs, ge, iea, filename, isactrlnum, gsctrlnum, stctrlnum)
                    String[] c_out = (String[])envelope;
                    
                    EDData.writeEDILog(c_out, "INFO", "Export"); 
                    EDData.CreateFreightEDIRecs(c_out, nbr);

                    } catch (IllegalAccessException | ClassNotFoundException |
                             InstantiationException | NoSuchMethodException |
                            InvocationTargetException ex) {
                        EDData.writeEDILog(c_in, "error", "unable to find map class or invocation error for " + tp + " / " + doctype);
                        clearStaticVariables();
                        errorcode = 3;
                        edilog(ex);
                    }
           
     
        
        // now lets set the order issourced flag if errorcode still equals 0
        if (errorcode == 0) {
           // EDData.updateOrderSourceFlag(nbr);
        }
        
        return errorcode;
     }
      
    public static int Create204(String nbr)  {
        int errorcode = 0;
       
        // errorcode = 0 ... clean exit
        // errorcode = 1 ... no record found in wh_mstr for wh doctype
        // errorcode = 2 ... unable to retrieve carriers from order
        // errorcode = 3 ... any catch error below ...try running from command line to see trace dump
        // errorcode = 4 ... shipper does not exist
        
        
        
        String doctype = "204";
        String map = "";
      
        
         String ca = OVData.getFreightOrderCarrierAssigned(nbr);
       
            
        
        map = EDData.getEDIMap(ca, doctype, "");
        
        
         String[] c_in = initEDIControl();   // controlarray in this order : entity, doctype, map, filename, isacontrolnum, gsctrlnum, stctrlnum, ref ; 
        
        c_in[0] = ca;
        c_in[1] = doctype;
        c_in[2] = map;
        c_in[3] = "";
        c_in[4] = "";
        c_in[5] = "";
        c_in[6] = "";
        c_in[7] = nbr;
        c_in[15] = "0"; // dir out
        c_in[12] = "0"; // is override
        
        // get Delimiters from Cust Defaults
        String[] defaults = EDData.getEDITPDefaults(ca, doctype, "");
        c_in[9] = defaults[7]; 
        c_in[10] = defaults[6]; 
        c_in[11] = defaults[8]; 
        
        
        if (map.isEmpty()) {
            EDData.writeEDILog(c_in, "error", "no edi_mstr map for " + c_in + " / " + doctype);
            errorcode = 1;
            return errorcode;
        } 
        
         // Mapdata method call below requires two parameters (ArrayList, String[]) ...doc and c
        ArrayList doc = new ArrayList();
        doc.add(nbr);
        
        
                    try {
                    Class cls = Class.forName(map);
                    Object obj = cls.getDeclaredConstructor().newInstance();
                    Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class, ArrayList.class);
               
                    
                    Object envelope = method.invoke(obj, doc, c_in); // envelope array holds in this order (isa, gs, ge, iea, filename, isactrlnum, gsctrlnum, stctrlnum)
                    String[] c_out = (String[])envelope;
                    
                    EDData.writeEDILog(c_out, "INFO", "Export"); 
                    EDData.CreateFreightEDIRecs(c_out, nbr); 

                    } catch (IllegalAccessException | ClassNotFoundException |
                             InstantiationException | NoSuchMethodException |
                            InvocationTargetException ex) {
                        EDData.writeEDILog(c_in, "error", "unable to find map class or invocation error for " + c_in + " / " + doctype);
                        clearStaticVariables();
                        errorcode = 3;
                        edilog(ex);
                    }
           
     
        
        // now lets set the order issourced flag if errorcode still equals 0
        if (errorcode == 0) {
           // EDData.updateOrderSourceFlag(nbr);
        }
        
        return errorcode;
     }
       
      
    
     // miscellaneous 
      public static String[] generateEnvelope(String doctype, String sndid, String rcvid) {
        
        String [] envelope = new String[10];  // will hold 7 elements.... ISA, GS, GE,IEA, filename, isactrl, gsctrl, sd, ed, ud
        
        //  * @return Array with 0=ISA, 1=ISAQUAL, 2=GS, 3=BS_ISA, 4=BS_ISA_QUAL, 5=BS_GS, 6=ELEMDELIM, 7=SEGDELIM, 8=SUBDELIM, 9=FILEPATH, 10=FILEPREFIX, 11=FILESUFFIX,
        //  * @return 12=X12VERSION, 13=SUPPCODE, 14=DIRECTION
        
        String[] defaults = EDData.getEDITPDefaults(doctype, sndid, rcvid);
        ArrayList<String> attrs = EDData.getEDIAttributesList(doctype, sndid, rcvid); 
        Map<String, String> attrkeys = new HashMap<String, String>();
        for (String x : attrs) {
            String[] z = x.split(":", -1);
            if (z != null && ! z[0].isEmpty()) {
                attrkeys.put(z[0], z[1]);
            }
        }
        
        // get counter for ediout
        
       int envctrlnbr = 0;
       int grpctrlnbr = 0;
       if (attrkeys.containsKey("envctrlnbr")) {
          envctrlnbr = EDData.getEDIControlNbr(doctype, sndid, rcvid, "envctrlnbr");
       } 
       if (attrkeys.containsKey("grpctrlnbr")) {
          grpctrlnbr = EDData.getEDIControlNbr(doctype, sndid, rcvid, "grpctrlnbr");
       }
       
       if (envctrlnbr == 0) {
          envctrlnbr = OVData.getNextNbr("ediout"); 
       }
       if (grpctrlnbr == 0) {
          grpctrlnbr = envctrlnbr; 
       }
             
              
       defaults[7] = (defaults[7].isBlank() || defaults[7].equals("0")) ? "10" : defaults[7];
       defaults[6] = (defaults[6].isBlank() || defaults[6].equals("0")) ? "42" : defaults[6];
       defaults[8] = (defaults[8].isBlank() || defaults[8].equals("0")) ? "126" : defaults[8];
       int sdint = Integer.valueOf(defaults[7]);
       int edint = Integer.valueOf(defaults[6]);
       int udint = Integer.valueOf(defaults[8]);
         String sd = Character.toString((char) sdint );   // "\n"  or "\u001c"
         String ed = Character.toString((char) edint );
         String ud = Character.toString((char) udint );
            
        //  if default filename is empty..set as generic
         if (defaults[10].isEmpty()) {
             defaults[10] = "generic";
         }
         
         String filename = defaults[10] + "." + String.valueOf(envctrlnbr) + "." + defaults[11];
        
         //  if filepath is defined...use this for explicit file path relative to root
        /*  don't do this...TEV 20220505...done downstream
         if (! defaults[9].isEmpty()) {
             filename = defaults[9] + "/" + filename;
         }
         */
         
         //File f = new File(defaults[9] + defaults[10] + "." + String.valueOf(filenumber) + "." + defaults[11]);
         //BufferedWriter output;
         //output = new BufferedWriter(new FileWriter(f));
         DateFormat isadfdate = new SimpleDateFormat("yyMMdd");
         DateFormat gsdfdate = new SimpleDateFormat("yyyyMMdd");
         DateFormat isadftime = new SimpleDateFormat("HHmm");
         DateFormat gsdftime = new SimpleDateFormat("HHmm");
         Date now = new Date();
        
         
         String isa1 = "00";
         if (attrkeys.containsKey("ISA01")) {isa1 = String.format("%2s",attrkeys.get("ISA01"));}
         
         String isa2 = "          ";
         if (attrkeys.containsKey("ISA02")) {isa2 = String.format("%10s",attrkeys.get("ISA02"));}
         
         String isa3 = "00";
         if (attrkeys.containsKey("ISA03")) {isa3 = String.format("%2s",attrkeys.get("ISA03"));}
         
         String isa4 = "          ";
         if (attrkeys.containsKey("ISA04")) {isa4 = String.format("%10s",attrkeys.get("ISA04"));}
         
         String isa5 = String.format("%2s", defaults[4]);
         
         String isa6 = "";
         if (attrkeys.containsKey("ISA06")) {
             isa6 = String.format("%-15s",attrkeys.get("ISA06"));
         } else {
             isa6 = String.format("%-15s", defaults[0]); // turnaround receiver ID as sender 
         }
         
         String isa7 = String.format("%2s", defaults[1]);
         
         String isa8 = "";
         if (attrkeys.containsKey("ISA08")) {
             isa8 = String.format("%-15s",attrkeys.get("ISA08"));
         } else {
             isa8 = String.format("%-15s", defaults[3]); // turnaround receiver ID as sender 
         }
        
         String isa9 = isadfdate.format(now);
         String isa10 = isadftime.format(now);
         String isa11 = "U";
         if (attrkeys.containsKey("ISA11")) {isa11 = String.format("%1s",attrkeys.get("ISA11"));}
         
         String isa12 = "     ";  // must be 5 chars long
         if (defaults[12].length() > 4) {
         isa12 = defaults[12].substring(0,5);
         }
         if (attrkeys.containsKey("ISA12")) {isa12 = String.format("%1s",attrkeys.get("ISA12"));}
         
         String isa13 = String.format("%09d", envctrlnbr);
         String isa14 = "0";
         if (attrkeys.containsKey("ISA14")) {isa14 = String.format("%1s",attrkeys.get("ISA14"));}
         
         String isa15 = "P";
         if (attrkeys.containsKey("ISA15")) {isa15 = String.format("%1s",attrkeys.get("ISA15"));}
         
         String isa16 = ud;
         String gs1 = EDData.getEDIGSTypeFromBSDoc(defaults[14]);   // defaults[14] = outdoctype 
         
         String gs2 = defaults[2];
         if (attrkeys.containsKey("GS02")) {gs2 = attrkeys.get("GS02");}
         
         String gs3 = defaults[5];
         if (attrkeys.containsKey("GS03")) {gs3 = attrkeys.get("GS03");}
         
         String gs4 = gsdfdate.format(now);
         String gs5 = gsdftime.format(now);
         String gs6 = String.valueOf(grpctrlnbr);
         String gs7 = "X";
         if (attrkeys.containsKey("GS07")) {gs7 = attrkeys.get("GS07");}
         
         String gs8 = defaults[12];
         if (attrkeys.containsKey("GS08")) {gs8 = attrkeys.get("GS08");}
          
         
        
        
       
          
           envelope[0] = "ISA" + ed + isa1 + ed + isa2 + ed + isa3 + ed + isa4 + ed + isa5 + ed + isa6 + ed + isa7 + ed + isa8 + ed + isa9 + ed + 
                 isa10 + ed + isa11 + ed + isa12 + ed + isa13 + ed + isa14 + ed + isa15 + ed + isa16 ;
           envelope[0] = envelope[0].toUpperCase();
           envelope[1] = "GS" + ed + gs1 + ed + gs2 + ed + gs3 + ed + gs4 + ed + gs5 + ed + gs6 + ed + gs7 + ed + gs8;
            envelope[1] = envelope[1].toUpperCase();
           envelope[2] = "GE" + ed + "1" + ed + String.valueOf(grpctrlnbr);
            envelope[2] = envelope[2].toUpperCase();
           envelope[3] = "IEA" + ed + "1" + ed + String.format("%09d", envctrlnbr);
            envelope[3] = envelope[3].toUpperCase();
            
           envelope[4] = filename;
           envelope[5] = String.format("%09d", envctrlnbr);
           envelope[6] = String.valueOf(grpctrlnbr);
           envelope[7] = sd;
           envelope[8] = ed;
           envelope[9] = ud;
           
            return envelope;
      }
      
      public static String[] generateEnvelopeUNE(String doctype, String sndid, String rcvid) {
        
        String [] envelope = new String[]{"","","","","","","","","",""};  
        String[] defaults = EDData.getEDITPDefaults(doctype, sndid, rcvid);
        ArrayList<String> attrs = EDData.getEDIAttributesList(doctype, sndid, rcvid); 
        Map<String, String> attrkeys = new HashMap<String, String>();
        for (String x : attrs) {
            String[] z = x.split(":", -1);
            if (z != null && ! z[0].isEmpty()) {
                attrkeys.put(z[0], z[1]);
            }
        }
        
       int envctrlnbr = 0;
       int grpctrlnbr = 0;
       if (attrkeys.containsKey("envctrlnbr")) {
          envctrlnbr = EDData.getEDIControlNbr(doctype, sndid, rcvid, "envctrlnbr");
       } 
       if (attrkeys.containsKey("grpctrlnbr")) {
          grpctrlnbr = EDData.getEDIControlNbr(doctype, sndid, rcvid, "grpctrlnbr");
       }
       
       if (envctrlnbr == 0) {
          envctrlnbr = OVData.getNextNbr("ediout"); 
       }
       if (grpctrlnbr == 0) {
          grpctrlnbr = envctrlnbr; 
       }
       
       
       defaults[7] = (defaults[7].isBlank() || defaults[7].equals("0")) ? "39" : defaults[7];
       defaults[6] = (defaults[6].isBlank() || defaults[6].equals("0")) ? "43" : defaults[6];
       defaults[8] = (defaults[8].isBlank() || defaults[8].equals("0")) ? "58" : defaults[8];
       int sdint = Integer.valueOf(defaults[7]);
       int edint = Integer.valueOf(defaults[6]);
       int udint = Integer.valueOf(defaults[8]);
         String sd = Character.toString((char) sdint );   // "\n"  or "\u001c"
         String ed = Character.toString((char) edint );
         String ud = Character.toString((char) udint );
            
        //  if default filename is empty..set as generic
         if (defaults[10].isEmpty()) {
             defaults[10] = "generic";
         }
         
         String filename = defaults[10] + "." + String.valueOf(envctrlnbr) + "." + defaults[11];
        
         //  if filepath is defined...use this for explicit file path relative to root
        /*  don't do this...TEV 20220505...done downstream
         if (! defaults[9].isEmpty()) {
             filename = defaults[9] + "/" + filename;
         }
         */
         
         //File f = new File(defaults[9] + defaults[10] + "." + String.valueOf(filenumber) + "." + defaults[11]);
         //BufferedWriter output;
         //output = new BufferedWriter(new FileWriter(f));
         DateFormat isadfdate = new SimpleDateFormat("yyMMdd");
         DateFormat gsdfdate = new SimpleDateFormat("yyyyMMdd");
         DateFormat isadftime = new SimpleDateFormat("HHmm");
         DateFormat gsdftime = new SimpleDateFormat("HHmm");
         Date now = new Date();
        
         
         String unb1 = "";
         if (attrkeys.containsKey("UNB01")) {
             unb1 = attrkeys.get("UNB01");
         } else {
             unb1 = "UNOC";
         }
         
         
         String unb2 = defaults[0] + ud + defaults[4];
         
         String unb3 = defaults[3] + ud + defaults[1];
         
         String unb4 = isadfdate.format(now) + ud + isadftime.format(now);
         
         String unb5 = String.valueOf(envctrlnbr);
         
         
         String ung1 = EDData.getEDIGSTypeFromBSDoc(defaults[14]);   // defaults[14] = outdoctype 
         
         String ung2 = defaults[2];
         if (attrkeys.containsKey("UNG02")) {ung2 = attrkeys.get("UNG02");}
         
         String ung3 = defaults[5];
         if (attrkeys.containsKey("UNG03")) {ung3 = attrkeys.get("UNG03");}
         
         String ung4 = gsdfdate.format(now) + ud + gsdftime.format(now);
         
         String ung5 = String.valueOf(grpctrlnbr);
         
         String ung6 = "UN";
         
         String ung7 = defaults[12];
                
           
           envelope[0] = "UNB" + ed + unb1 + ed + unb2 + ed + unb3 + ed + unb4 + ed + unb5;
           if (defaults[22].equals("1")) { // prefix UNA if una checkbox is checked  
             envelope[0] = "UNA:+.? '" + envelope[0];  
           }
           envelope[0] = envelope[0].toUpperCase();
           
           if (defaults[23].equals("1")) {  // if ung checkbox is checked write UNG
           envelope[1] = "UNG" + ed + ung1 + ed + ung2 + ed + ung3 + ed + ung4 + ed + ung5 + ed + ung6 + ed + ung7;
            envelope[1] = envelope[1].toUpperCase();
           envelope[2] = "UNE" + ed + "1" + ed + String.valueOf(grpctrlnbr);
           envelope[2] = envelope[2].toUpperCase();
           }
           envelope[3] = "UNZ" + ed + "1" + ed + String.valueOf(envctrlnbr);
            envelope[3] = envelope[3].toUpperCase();
            
           envelope[4] = filename;
           envelope[5] = String.valueOf(envctrlnbr);
           envelope[6] = String.valueOf(grpctrlnbr);
           envelope[7] = sd;
           envelope[8] = ed;
           envelope[9] = ud;
           
            return envelope;
      }
      
      
      public static String[] generate997(ArrayList<String> docs, String[] c) {
        String[] m = new String[]{"",""};
        
        
        
        
        String sd = delimConvertIntToStr(c[9]);
        String ed = delimConvertIntToStr(c[10]);
        String ud = delimConvertIntToStr(c[11]);
        String[] _isa = c[13].split(EDI.escapeDelimiter(delimConvertIntToStr(c[10])), -1);
        String[] _gs = c[14].split(EDI.escapeDelimiter(delimConvertIntToStr(c[10])), -1);
         
         DateFormat isadfdate = new SimpleDateFormat("yyMMdd");
         DateFormat gsdfdate = new SimpleDateFormat("yyyyMMdd");
         DateFormat isadftime = new SimpleDateFormat("HHmm");
         DateFormat gsdftime = new SimpleDateFormat("HHmm");
         DateFormat tsdfdate = new SimpleDateFormat("yyMMddHHmmssSSS");
         Date now = new Date();
        int segcount = 0;
        int hdrsegcount = 0;
        String stctrl = "0001";
        int filenumber = OVData.getNextNbr("ediout");
        String filename = "997_" + c[0] + "_" + tsdfdate.format(now) + ".txt";
        
        String outdir = "";
        
        // create envelope segments
        
        // 997s are generated first without regards to specific 997 partner relationship setup
        // if relationship record found...this overrides filename, and various ISA and GS attributes as found
        String[] defaults = EDData.getEDITPDefaults("997x12", _gs[2], _gs[3]);
        ArrayList<String> attrs = EDData.getEDIAttributesList("997x12", _gs[2], _gs[3]); 
        Map<String, String> attrkeys = new HashMap<String, String>();
        for (String x : attrs) {
            String[] z = x.split(":", -1);
            if (z != null && ! z[0].isEmpty()) {
                attrkeys.put(z[0], z[1]);
            }
        }
         
        //  Override filename with defaults values if found for partner relationship
       
       if (! defaults[0].isBlank()) { 
       defaults[7] = (defaults[7].isBlank() || defaults[7].equals("0")) ? "10" : defaults[7];
       defaults[6] = (defaults[6].isBlank() || defaults[6].equals("0")) ? "42" : defaults[6];
       defaults[8] = (defaults[8].isBlank() || defaults[8].equals("0")) ? "126" : defaults[8];
       int sdint = Integer.valueOf(defaults[7]);
       int edint = Integer.valueOf(defaults[6]);
       int udint = Integer.valueOf(defaults[8]);
       sd = Character.toString((char) sdint );   // "\n"  or "\u001c"
       ed = Character.toString((char) edint );
       ud = Character.toString((char) udint );
       filename = defaults[10] + "." + String.valueOf(filenumber) + "." + defaults[11];
       outdir = cleanDirString(defaults[9]);
       
       }
        
         
         
         String isa1 = _isa[1];
         if (attrkeys.containsKey("ISA01")) {isa1 = String.format("%2s",attrkeys.get("ISA01"));}
         
         String isa2 = _isa[2];
         if (attrkeys.containsKey("ISA02")) {isa2 = String.format("%10s",attrkeys.get("ISA02"));}
         
         String isa3 = _isa[3];
         if (attrkeys.containsKey("ISA03")) {isa3 = String.format("%2s",attrkeys.get("ISA03"));}
         
         String isa4 = _isa[4];
         if (attrkeys.containsKey("ISA04")) {isa4 = String.format("%10s",attrkeys.get("ISA04"));}
         
         String isa5 = String.format("%2s", _isa[7]);
         
         String isa6 = "";
         if (attrkeys.containsKey("ISA06")) {
             isa6 = String.format("%-15s",attrkeys.get("ISA06"));
         } else {
             isa6 = String.format("%-15s", _isa[8]); // turnaround receiver ID as sender 
         }
         
         String isa7 = String.format("%2s", _isa[5]);
         
         String isa8 = "";
         if (attrkeys.containsKey("ISA08")) {
             isa8 = String.format("%-15s",attrkeys.get("ISA08"));
         } else {
             isa8 = String.format("%-15s", _isa[6]); // turnaround receiver ID as sender 
         }
         
         String isa9 = isadfdate.format(now);
         String isa10 = isadftime.format(now);
         String isa11 = _isa[11];
         if (attrkeys.containsKey("ISA11")) {isa11 = String.format("%1s",attrkeys.get("ISA11"));}
         
         String isa12 = _isa[12];  // must be 5 chars long
         if (attrkeys.containsKey("ISA12")) {isa12 = String.format("%1s",attrkeys.get("ISA12"));}
         
         String isa13 = String.format("%09d", filenumber);
         String isa14 = _isa[14];
         if (attrkeys.containsKey("ISA14")) {isa14 = String.format("%1s",attrkeys.get("ISA14"));}
         
         String isa15 = _isa[15];
         if (attrkeys.containsKey("ISA15")) {isa15 = String.format("%1s",attrkeys.get("ISA15"));}
         
         String isa16 = ud;
         
         String gs1 = EDData.getEDIGSTypeFromStds("997"); 
         
         
         String gs2 = _gs[3];
         if (attrkeys.containsKey("GS02")) {gs2 = attrkeys.get("GS02");}
         
         String gs3 = _gs[2];
         if (attrkeys.containsKey("GS03")) {gs3 = attrkeys.get("GS03");}
         
         String gs4 = gsdfdate.format(now);
         String gs5 = gsdftime.format(now);
         String gs6 = String.valueOf(filenumber);
         String gs7 = "X";
         if (attrkeys.containsKey("GS07")) {gs7 = attrkeys.get("GS07");}
         
         String gs8 = _gs[8];
         if (attrkeys.containsKey("GS08")) {gs8 = attrkeys.get("GS08");}
          
         
        
        
       
          
           String ISA = "ISA" + ed + isa1 + ed + isa2 + ed + isa3 + ed + isa4 + ed + isa5 + ed + isa6 + ed + isa7 + ed + isa8 + ed + isa9 + ed + 
                 isa10 + ed + isa11 + ed + isa12 + ed + isa13 + ed + isa14 + ed + isa15 + ed + isa16 ;
           
           String GS = "GS" + ed + gs1 + ed + gs2 + ed + gs3 + ed + gs4 + ed + gs5 + ed + gs6 + ed + gs7 + ed + gs8;
           
           String GE = "GE" + ed + "1" + ed + String.valueOf(filenumber);
           
           String IEA = "IEA" + ed + "1" + ed + String.format("%09d", filenumber);
            
            
        // now create body
        String ST = "ST" + ed + "997" + ed + stctrl + sd;
        hdrsegcount = 2; // "ST and SE" included
         
         String Header = "";
       
         ArrayList<String> S = new ArrayList();
         S.add("AK1" + ed + _gs[1] + ed + _gs[6]);
         hdrsegcount += 1;
         for (String d : docs) {
             S.add("AK2" + ed + getEDIDocTypeFromBSDoc(c[1]) + ed + d);
             S.add("AK5" + ed + "A");
             hdrsegcount += 2;
         }
         
         S.add("AK9" + ed + "A" + ed + docs.size() + ed + docs.size() + ed + docs.size());
         hdrsegcount += 1;
         
         // now cleanup
         for (String s : S) {
             Header += (EDI.trimSegment(s, ed).toUpperCase() + sd);
         }
         segcount = hdrsegcount;
         
         String SE = "SE" + ed + String.valueOf(segcount) + ed + stctrl + sd;
                         
         
                 
         // concat and send content to edi.writeFile
         String content = ISA + sd + GS + sd + ST + Header + SE + GE + sd + IEA + sd;
         
         // override outdir if specific path is empty
         if (outdir.isEmpty()) {
                    outdir = cleanDirString(EDData.getEDIOutDir());
         }
        
         
         // create batch file
          String batchfile = "S" + String.format("%07d", filenumber); 
          
          
               // update c for edi_idx
         String[] cc = c.clone();
         cc[0] = c[21];
         cc[1] = "997x12";
         cc[15] = "997x12";
         cc[8] = filename;
         cc[21] = c[0];
         cc[24] = batchfile;
         cc[25] = batchfile;
         cc[28] = "X12";
         cc[29] = "X12";
         cc[31] = "0";
         cc[32]  = "99999";
         cc[33] = "0";
         cc[34] = "99999";
         cc[35] = c[9];
         cc[36] = c[10];
         cc[37] = c[11];
         EDData.writeEDIIDX(cc);
         
        try {
            
            EDI.writeFile(content, outdir, filename);
            Files.copy(new File(outdir + filename).toPath(), new File(cleanDirString(EDData.getEDIBatchDir()) + batchfile).toPath(), StandardCopyOption.REPLACE_EXISTING);
           
        } catch (SmbException ex) {
            edilog(ex);
        } catch (IOException ex) {
            edilog(ex);
        }
         
         
         return m;
      }
      
      public static String[] generateCONTRL(ArrayList<String> docs, String[] c) {
        String[] m = new String[]{"",""};
        
        
        
        
        String sd = delimConvertIntToStr(c[9]);
        String ed = delimConvertIntToStr(c[10]);
        String ud = delimConvertIntToStr(c[11]);
        
       
        
        String[] _unb = c[13].split(EDI.escapeDelimiter(delimConvertIntToStr(c[10])), -1);
        
        String sender = _unb[2];
        String senderQ = "ZZ";
        if (_unb[2].contains(ud)) {
            sender = _unb[2].split(ud)[0];
            senderQ = _unb[2].split(ud)[1];
        }
        String receiver = _unb[3];
        String receiverQ = "ZZ";
        if (_unb[3].contains(ud)) {
            receiver = _unb[3].split(ud)[0];
            receiverQ = _unb[3].split(ud)[1];
        }
       // String[] _ung = c[14].split(EDI.escapeDelimiter(delimConvertIntToStr(c[10])), -1);
         
         DateFormat isadfdate = new SimpleDateFormat("yyMMdd");
         DateFormat gsdfdate = new SimpleDateFormat("yyyyMMdd");
         DateFormat isadftime = new SimpleDateFormat("HHmm");
         DateFormat gsdftime = new SimpleDateFormat("HHmm");
         DateFormat tsdfdate = new SimpleDateFormat("yyMMddHHmmssSSS");
         Date now = new Date();
        int segcount = 0;
        int hdrsegcount = 0;
        String stctrl = "0001";
        int filenumber = OVData.getNextNbr("ediout");
        String filename = "CONTRL_" + c[0] + "_" + tsdfdate.format(now) + ".txt";
        // create envelope segments
        
        // 997s are generated first without regards to specific 997 partner relationship setup
        // if relationship record found...this overrides filename, and various ISA and GS attributes as found
        
        String unb2 = receiver + ud + receiverQ;
        String unb3 = sender + ud + senderQ;
        
        String[] defaults = EDData.getEDITPDefaults("997une", sender, receiver);
        ArrayList<String> attrs = EDData.getEDIAttributesList("997une", sender, receiver); 
        Map<String, String> attrkeys = new HashMap<String, String>();
        for (String x : attrs) {
            String[] z = x.split(":", -1);
            if (z != null && ! z[0].isEmpty()) {
                attrkeys.put(z[0], z[1]);
            }
        }
         
        //  Override filename with defaults values if found for partner relationship
         if (! defaults[0].isBlank()) {
             filename = defaults[10] + "." + String.valueOf(filenumber) + "." + defaults[11];
             //  if filepath is defined...use this for explicit file path relative to root
             if (! defaults[9].isEmpty()) {
                 filename = defaults[9] + "/" + filename;
             }
         }
         
         String unb1 = "";
         if (attrkeys.containsKey("UNB01")) {
             unb1 = attrkeys.get("UNB01");
         } else {
             unb1 = "UNOC";
         }
         
         if (! defaults[0].isBlank()) {
         unb2 = defaults[0] + ud + defaults[4];
         unb3 = defaults[3] + ud + defaults[1];
         }
         
         String unb4 = isadfdate.format(now) + ud + isadftime.format(now);
         String unb5 = String.valueOf(filenumber);
         
         /*
         String ung1 = EDData.getEDIGSTypeFromBSDoc("997une");   // defaults[14] = outdoctype 
         
         String ung2 = defaults[2];
         if (attrkeys.containsKey("UNG02")) {ung2 = attrkeys.get("UNG02");}
         
         String ung3 = defaults[5];
         if (attrkeys.containsKey("UNG03")) {ung3 = attrkeys.get("UNG03");}
         
         String ung4 = gsdfdate.format(now) + ud + gsdftime.format(now);
         
         String ung5 = String.valueOf(filenumber);
         
         String ung6 = "UN";
         
         String ung7 = defaults[12];
         */
         
         
           String UNB = "";
           String UNG = "";
           String UNE = "";
           String UNZ = "";
          
           UNB = "UNB" + ed + unb1 + ed + unb2 + ed + unb3 + ed + unb4 + ed + unb5;
           if (defaults[22].equals("1")) { // prefix UNA if una checkbox is checked  
             UNB = "UNA:+.? '" + UNB;  
           }
           /*
           if (defaults[23].equals("1")) {  // if ung checkbox is checked write UNG
            UNG = "UNG" + ed + ung1 + ed + ung2 + ed + ung3 + ed + ung4 + ed + ung5 + ed + ung6 + ed + ung7;
            UNE = "UNE" + ed + "1" + ed + String.valueOf(filenumber);
           }
          */ 
            UNZ = "UNZ" + ed + "1" + ed + String.valueOf(filenumber);
            
            
        // now create body
        StringBuilder unhe02 = new StringBuilder();
           String[] unhe02_array = null;
           if (attrkeys.containsKey("UNHE02")) {
               unhe02_array = attrkeys.get("UNHE02").split(":",-1);
           }
           unhe02.append(EDData.getEDIGSTypeFromBSDoc("997une"));
           if (unhe02_array == null || unhe02_array.length == 0) {
             unhe02.append(":D:98A:UN"); // fallback if no EDI attributes found for key UNHE02
           } else {
               for (String ux : unhe02_array ) {
                   if (! ux.isBlank()) {
                     unhe02.append(ud);
                     unhe02.append(ux);
                   }
               }
           }
        
        
        String UNH = "UNH" + ed + stctrl + ed + unhe02;
        hdrsegcount = 2; // "ST and SE" included
         
         String Header = "";
       
         ArrayList<String> S = new ArrayList();
         S.add("UCI" + ed + c[4].trim() + ed + unb3 + ed + unb2 + ed + "8");
         hdrsegcount += 1;
         
         // S.add("UCM" + ed + "A" + ed + docs.size() + ed + docs.size() + ed + docs.size());
         // hdrsegcount += 1;
         
         // now cleanup
         for (String s : S) {
             Header += (EDI.trimSegment(s, ed).toUpperCase() + sd);
         }
         segcount = hdrsegcount;
         
         String UNT = "UNT" + ed + String.valueOf(segcount) + ed + stctrl;
                         
         
                 
         // concat and send content to edi.writeFile
        // String content = UNB + sd + UNG + sd + UNH + Header + UNT + UNE + sd + UNZ + sd;
         String content = UNB + sd + UNH + sd + Header + UNT + sd + UNZ + sd;
         
        
         
         // create batch file
          String batchfile = "S" + String.format("%07d", filenumber); 
          
          
               // update c for edi_idx
         String[] cc = c.clone();
         cc[0] = c[21];
         cc[1] = "997une";
         cc[15] = "997une";
         cc[8] = filename;
         cc[21] = c[0];
         cc[24] = batchfile;
         cc[25] = batchfile;
         cc[28] = "UNE";
         cc[29] = "UNE";
         cc[31] = "0";
         cc[32]  = "99999";
         cc[33] = "0";
         cc[34] = "99999";
         cc[35] = c[9];
         cc[36] = c[10];
         cc[37] = c[11];
         EDData.writeEDIIDX(cc);
         
        try {
            // write to file
            EDI.writeFile(content, cleanDirString(EDData.getEDIOutDir()), filename);
            Files.copy(new File(cleanDirString(EDData.getEDIOutDir()) + filename).toPath(), new File(cleanDirString(EDData.getEDIBatchDir()) + batchfile).toPath(), StandardCopyOption.REPLACE_EXISTING);
           
        } catch (SmbException ex) {
            edilog(ex);
        } catch (IOException ex) {
            edilog(ex);
        }
         
         
         return m;
      }
      
      
      public static String[] process997(ArrayList<String> docs, String[] c) {
         // used for inbound 997s to match outbound transmissions
         String[] _isa = c[13].split(EDI.escapeDelimiter(delimConvertIntToStr(c[10])), -1);
         String[] _gs = c[14].split(EDI.escapeDelimiter(delimConvertIntToStr(c[10])), -1);  
         ArrayList<String[]> docids = new ArrayList<String[]>();
         String groupid = "";
         String doctype = "";
         for (Object seg : docs) {
            String ea[] = seg.toString().split(EDI.escapeDelimiter(delimConvertIntToStr(c[10])), -1);
            // in practice...should be only 1 AK1 and.... 1 or more AK2s (or no AK2s i.e. all docs of group)
            if (ea[0] != null && ea.length > 2 && ea[0].equals("AK1")) {
                groupid = ea[2];
                doctype = EDData.getEDIDocTypeFromStds(ea[1]);
            }
            if (ea[0] != null && ea.length > 2 && ea[0].equals("AK2")) {
                String[] temp = new String[]{doctype, groupid, ea[2], c[24]};
               docids.add(temp);
            }
         }
         
         // acknowledge original envelope as a whole...ALL docs of envelope
         int count = EDData.updateEDIIDXAcksAllGroup(doctype, groupid, c[0].trim(), c[24]);
         // find edi_idx record of groupid, doctype, and docid ...for each element of docids
         // may do this later
         /*
         if (! docids.isEmpty()) {
            EDData.updateEDIIDXAcks(docids);
         } else {
             // some 997s do not send ST specific...AK2,AK5...but GS (group specific) only AK1...update all doc of GS group ID
            EDData.updateEDIIDXAcksAllGroup(doctype, groupid, c[0].trim(), c[24]);
         }
         */
         if (groupid.isEmpty() || count == 0) {
             return new String[]{"error","unrecognized gscntrlnum in AK1"};
         } else {
             return new String[]{"success","997 loaded"};
         }
         
      }
      
      public static String[] processCONTRL(ArrayList<String> docs, String[] c) {
         // used for inbound 997s to match outbound transmissions
         String[] _isa = c[13].split(EDI.escapeDelimiter(delimConvertIntToStr(c[10])), -1);
         String[] _gs = c[14].split(EDI.escapeDelimiter(delimConvertIntToStr(c[10])), -1);  
         ArrayList<String[]> docids = new ArrayList<String[]>();
         String controlnbr = "";
         for (Object seg : docs) {
            String ea[] = seg.toString().split(EDI.escapeDelimiter(delimConvertIntToStr(c[10])), -1);
            if (ea[0] != null && ea.length > 2 && ea[0].equals("UCI")) {
                controlnbr = ea[1];
            }
            /*
            if (ea[0] != null && ea.length > 2 && ea[0].equals("AK2")) {
                String[] temp = new String[]{doctype, groupid, ea[2], c[24]};
               docids.add(temp);
            }
            */
         }
        
         
          // acknowledge original envelope as a whole...ALL docs of envelope
         int count = EDData.updateEDIIDXAcksAllGroup(c[1], controlnbr, c[0].trim(), c[24]);
         // find edi_idx record of groupid, doctype, and docid ...for each element of docids
         // may do this later
         /*
         if (! docids.isEmpty()) {
            EDData.updateEDIIDXAcks(docids);
         } else {
             // some 997s do not send ST specific...AK2,AK5...but GS (group specific) only AK1...update all doc of GS group ID
            EDData.updateEDIIDXAcksAllGroup(doctype, groupid, c[0].trim(), c[24]);
         }
         */
         
         if (controlnbr.isEmpty() || count == 0) {
             return new String[]{"error","unrecognized gscntrlnum in UCI"};
         } else {
             return new String[]{"success","997une loaded"};
         }
         
      }
      
      
      public static String[] generate997Envelope(String[] in_isa, String[] in_gs) {
        
        
                    
        String [] envelope = new String[7];  // will hold 7 elements.... ISA, GS, GE,IEA, filename, isactrl, gsctrl
        
        // get counter for ediout
        int filenumber = OVData.getNextNbr("ediout");
        
        String[] defaults = EDData.getEDITPDefaults("997", in_isa[6].trim(), in_isa[8].trim());
        ArrayList<String> attrs = EDData.getEDIAttributesList("997", in_isa[6].trim(), in_isa[8].trim());
      
        Map<String, String> attrkeys = new HashMap<String, String>();
        for (String x : attrs) {
            String[] z = x.split(":", -1);
            if (z != null && ! z[0].isEmpty()) {
                attrkeys.put(z[0], z[1]);
            }
        }
        
        String sd = "\n";
        String ed = "*";
        String ud = ">";
        if (defaults[7] != null) {
         sd = delimConvertIntToStr(defaults[7]);  // "\n"  or "\u001c"
        }
        if (defaults[6] != null) {
         ed = delimConvertIntToStr(defaults[6]);
        } 
        if (defaults[8] != null) {
         ud = delimConvertIntToStr(defaults[8]);
        }
         
        if (defaults[10].isEmpty()) {  // if no filename provided in EDI Partner Master
            defaults[10] = "997" + "." + in_isa[6].trim();
        }
         String filename =  defaults[10] + "." + String.valueOf(filenumber) + "." + defaults[11];
       //  String filename = "997" + "." + in_isa[6].trim() + "." + String.valueOf(filenumber) + "." + "txt";
        
         //File f = new File(defaults[9] + defaults[10] + "." + String.valueOf(filenumber) + "." + defaults[11]);
         //BufferedWriter output;
         //output = new BufferedWriter(new FileWriter(f));
         DateFormat isadfdate = new SimpleDateFormat("yyMMdd");
         DateFormat gsdfdate = new SimpleDateFormat("yyyyMMdd");
         DateFormat isadftime = new SimpleDateFormat("HHmm");
         DateFormat gsdftime = new SimpleDateFormat("HHmm");
         Date now = new Date();
        
         
         String isa1 = String.format("%2s", in_isa[3]);
         if (attrkeys.containsKey("ISA01")) {isa1 = String.format("%2s",attrkeys.get("ISA01"));}
         
         String isa2 = String.format("%10s", in_isa[4]);
         if (attrkeys.containsKey("ISA02")) {isa2 = String.format("%10s",attrkeys.get("ISA02"));}
         
         String isa3 = String.format("%2s", in_isa[1]);
         if (attrkeys.containsKey("ISA03")) {isa3 = String.format("%2s",attrkeys.get("ISA03"));}
         
         String isa4 = String.format("%10s", in_isa[2]);
         if (attrkeys.containsKey("ISA04")) {isa4 = String.format("%10s",attrkeys.get("ISA04"));}
         
         String isa5 = String.format("%2s", in_isa[7]);
         String isa6 = String.format("%-15s", in_isa[8].trim());
         String isa7 = String.format("%2s", in_isa[5]);
         String isa8 = String.format("%-15s", in_isa[6].trim());
         String isa9 = isadfdate.format(now);
         String isa10 = isadftime.format(now);
         String isa11 = "U";
         if (attrkeys.containsKey("ISA11")) {isa11 = String.format("%1s",attrkeys.get("ISA11"));}
         
         String isa12 = in_gs[8].substring(0,5);
         if (attrkeys.containsKey("ISA12")) {isa12 = String.format("%5s",attrkeys.get("ISA12"));}
         
         String isa13 = String.format("%09d", filenumber);
         String isa14 = "0";
         if (attrkeys.containsKey("ISA14")) {isa14 = String.format("%1s",attrkeys.get("ISA14"));}
         
         String isa15 = "P";
         if (attrkeys.containsKey("ISA15")) {isa15 = String.format("%1s",attrkeys.get("ISA15"));}
         
         String isa16 = ud;
         
         String gs1 = "FA";
         
        
         
         String gs2 = in_gs[3];
         if (attrkeys.containsKey("GS02")) {gs2 = attrkeys.get("GS02");}
         
         String gs3 = in_gs[2];
         if (attrkeys.containsKey("GS03")) {gs3 = attrkeys.get("GS03");}
         
         String gs4 = gsdfdate.format(now);
         String gs5 = gsdftime.format(now);
         String gs6 = String.valueOf(filenumber);
         String gs7 = "X";
         if (attrkeys.containsKey("GS07")) {gs7 = attrkeys.get("GS07");}
         
         String gs8 = in_gs[8];
         if (attrkeys.containsKey("GS08")) {gs8 = attrkeys.get("GS08");}
          
        
        
       
          
           envelope[0] = "ISA" + ed + isa1 + ed + isa2 + ed + isa3 + ed + isa4 + ed + isa5 + ed + isa6 + ed + isa7 + ed + isa8 + ed + isa9 + ed + 
                 isa10 + ed + isa11 + ed + isa12 + ed + isa13 + ed + isa14 + ed + isa15 + ed + isa16 + sd ;
           envelope[0] = envelope[0].toUpperCase();
           envelope[1] = "GS" + ed + gs1 + ed + gs2 + ed + gs3 + ed + gs4 + ed + gs5 + ed + gs6 + ed + gs7 + ed + gs8 + sd;
            envelope[1] = envelope[1].toUpperCase();
           envelope[2] = "GE" + ed + "1" + ed + String.valueOf(filenumber) + sd;
            envelope[2] = envelope[2].toUpperCase();
           envelope[3] = "IEA" + ed + "1" + ed + String.format("%09d", filenumber) + sd;
            envelope[3] = envelope[3].toUpperCase();
            
          
           
            envelope[4] = filename;
            envelope[5] = String.format("%09d", filenumber);
            envelope[6] = String.valueOf(filenumber);
           
            return envelope;
      }
          
      public static String trimSegment(String segment, String delimiter) {
          String mystring = "";
          if ( delimiter.charAt(0) == segment.charAt(segment.length()- 1) ) {
              mystring = segment.substring(0, segment.length() - 1);
              if (mystring.length() > 0 && delimiter.charAt(0) == mystring.charAt(mystring.length()- 1) ) {
               return trimSegment(mystring, delimiter);
              }
          } else {
              mystring = segment;
          }
          return mystring;
      }
      
      public static String escapeDelimiter(String delim) {
      if (delim.equals("*")) {
            delim = "\\*";
        }
        if (delim.equals("^")) {
            delim = "\\^";
        }
        if (delim.equals("+")) {
            delim = "\\+";
        }
        if (delim.equals("\\")) {
            delim = "\\\\";
        }
        if (delim.equals("|")) {
            delim = "\\|";
        }
        if (delim.equals("?")) {
            delim = "\\?";
        }
        if (delim.equals("!")) {
            delim = "\\!";
        }
        return delim;
      }
      
      public static String delimConvertIntToStr(String intdelim) {
        String delim = "";
        int x = Integer.valueOf(intdelim);
        delim = String.valueOf(Character.toString((char) x));
        return delim;
      }
      
      
      public static Boolean isEnvelopeSegment(String seg) {
      
      return (seg.equals("ISA") || seg.equals("GS") || 
              seg.equals("ST") || seg.equals("SE") ||
               seg.equals("GE") || seg.equals("IEA") ) ? true : false ;
      }
      
     public static void writeFile(String filecontent, String dir, String filename) throws MalformedURLException, SmbException, IOException {
    
  // File folder = new File("smb://10.17.2.55/edi");
  // File[] listOfFiles = folder.listFiles();

    
 //   NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null, null, null);
     
    NtlmPasswordAuthentication auth = NtlmPasswordAuthentication.ANONYMOUS;
    // if samba is used filepath should be something akin to smb://10.10.1.1/somepath/somedir/ + filename
    
   // SmbFile folder = new SmbFile("smb://10.17.2.55/edi/", auth);
    
   // bsmf.MainFrame.show(dir + " -- " + filename);
    Path path = FileSystems.getDefault().getPath(dir + filename);
    Path archpath = FileSystems.getDefault().getPath(cleanDirString(EDData.getEDIOutArch()) + filename);
    
    
    BufferedWriter output;
    
         if (path.startsWith("smb")) {
         output = new BufferedWriter(new OutputStreamWriter(new SmbFileOutputStream(new SmbFile(path.toString(), auth))));
         output.write(filecontent);
         output.close();
         // now arch
         output = new BufferedWriter(new OutputStreamWriter(new SmbFileOutputStream(new SmbFile(archpath.toString(), auth))));
         output.write(filecontent);
         output.close();
         } else {
         output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path.toFile())));
         output.write(filecontent);
         output.close();  
         // now arch
         output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(archpath.toFile())));
         output.write(filecontent);
         output.close();   
         }
   // SmbFile[] listOfFiles = folder.listFiles();
         
        
    }
      
      public void writeFileCmdLine(String filecontent, String filename) throws MalformedURLException, SmbException, IOException {
    
//    File folder = new File("smb://10.17.2.55/edi");
  // File[] listOfFiles = folder.listFiles();

    
 //   NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null, null, null);
     
    NtlmPasswordAuthentication auth = NtlmPasswordAuthentication.ANONYMOUS;
    // if samba is used filepath should be something akin to smb://10.10.1.1/somepath/somedir/ + filename
    
   // SmbFile folder = new SmbFile("smb://10.17.2.55/edi/", auth);
    
   
    
    Path path = FileSystems.getDefault().getPath(filename);
    
    
    
    BufferedWriter output;
    
         if (path.startsWith("smb")) {
         output = new BufferedWriter(new OutputStreamWriter(new SmbFileOutputStream(new SmbFile(path.toString(), auth))));
         output.write(filecontent);
         output.close();
         } else {
         output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path.toFile(), true)));
         output.write(filecontent);
         output.close();  
         }
   // SmbFile[] listOfFiles = folder.listFiles();
         
        
    }
     
     
     public static class edi830 {
    
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String po = "";
    public String rlse = "";
    public String billto = "";
    public String shipto = "";
    public String ov_billto = "";
    public String ov_shipto = "";
    public String remarks = "";
    public String shipmethod = "";
    public String duedate = "";
    public String podate = "";
    public String st_name = "";
    public String st_line1 = "";
    public String st_line2 = "";
    public String st_line3 = "";
    public String st_city = "";
    public String st_state = "";
    public String st_zip = "";
    public String st_country = "";
    
       
    public ArrayList<String> detsku = new ArrayList<String>();
    public ArrayList<String> detcustitem = new ArrayList<String>();
    public ArrayList<String> detitem = new ArrayList<String>();
    public ArrayList<String> detqty = new ArrayList<String>();
    public ArrayList<String> detref = new ArrayList<String>();
    public ArrayList<String> detpo = new ArrayList<String>();
    public ArrayList<String> detline = new ArrayList<String>();
    public ArrayList<String> detlistprice = new ArrayList<String>();
    public ArrayList<String> detnetprice = new ArrayList<String>();
    public ArrayList<String> detdisc = new ArrayList<String>();
    public ArrayList<String> dettype = new ArrayList<String>();
    public ArrayList<String> detdate = new ArrayList<String>();
    
    
    public ArrayList<String> FSTloopqty = new ArrayList<String>();
    public ArrayList<String> FSTloopdate = new ArrayList<String>();
    public ArrayList<String> FSTlooptype = new ArrayList<String>();
    public ArrayList<String> FSTloopref = new ArrayList<String>();
    
    public Map<Integer, ArrayList<String>> map = new HashMap<Integer, ArrayList<String>>();
    
    
    
        public edi830() {
            
        }
        public edi830(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
       
        public void setFSTMap(Integer i, ArrayList<String> a) {
            this.map.put(i, a);
        }
        
        public void setFSTLoopDate(String v) {
           this.FSTloopdate.add(v);
        }
        public void setFSTLoopQty(String v)  {
           this.FSTloopqty.add(v);
        }
        public void setFSTLoopType(String v) {
           this.FSTlooptype.add(v);
        }
        public void setFSTLoopRef(String v) {
           this.FSTloopref.add(v);
        }
        
        public void setDetDate(String v) {
           this.detdate.add(v);
        }
        public void setDetType(String v) {
           this.dettype.add(v);
        }
        public void setDetItem(String v) {
           this.detitem.add(v);
        }
        public void setDetCustItem(String v) {
           this.detcustitem.add(v);
        }
        public void setDetSku(String v) {
           this.detsku.add(v);
        }
        public void setDetRef(String v) {
           this.detref.add(v);
        }
        public void setDetPO(String v) {
           this.detpo.add(v);
        }
        public void setDetLine(String v) {
           this.detline.add(v);
        }
        public void setDetQty(String v) {
           this.detqty.add(v);
        }
        public void setDetListPrice(String v) {
           this.detlistprice.add(v);
        }
        public void setDetNetPrice(String v) {
           this.detnetprice.add(v);
        }
        public void setDetDisc(String v) {
           this.detdisc.add(v);
        }
       
        public void setRlse(String v) {
           this.rlse = v;
        }  
        public void setPO(String v) {
           this.po = v;
        }
        public void setPODate(String v) {
           this.podate = v;
        }
        public void setDueDate(String v) {
           this.duedate = v;
        }
        
        public void setRemarks(String v) {
           this.remarks = v;
        }
         public void setShipVia(String v) {
           this.shipmethod = v;
        }
        
        
        public void setShipTo(String v) {
           this.shipto = v;
        }
        public void setShipToName(String v) {
           this.st_name = v;
        }
        public void setShipToLine1(String v) {
           this.st_line1 = v;
        }
        public void setShipToLine2(String v) {
           this.st_line2 = v;
        }
        public void setShipToLine3(String v) {
           this.st_line3 = v;
        }
        public void setShipToCity(String v) {
           this.st_city = v;
        }
        public void setShipToState(String v) {
           this.st_state = v;
        }
        public void setShipToZip(String v) {
           this.st_zip = v;
        }
        public void setShipToCountry(String v) {
           this.st_country = v;
        }
        public void setBillTo(String v) {
           this.billto = v;
        }
        public void setOVShipTo(String v) {
            this.ov_shipto = v;
        }
        public void setOVBillTo(String v) {
            this.ov_billto = v;
        }
        public String getPO() {
           return this.po;
        }
        public Map getFSAMap(int i) {
           return this.map;
        }
        public String getRlse() {
           return this.rlse;
        }
        public String getPODate() {
           return this.podate;
        }
        public String getShipTo() {
           return this.shipto;
        }
        public String getShipToName() {
           return this.st_name;
        }
        public String getShipToLine1() {
           return this.st_line1;
        }
        public String getShipToLine2() {
           return this.st_line2;
        }
        public String getShipToLine3() {
           return this.st_line3;
        }
        public String getShipToCity() {
           return this.st_city;
        }
        public String getShipToState() {
           return this.st_state;
        }
        public String getShipToZip() {
           return this.st_zip;
        }
        public String getShipToCountry() {
           return this.st_country;
        }
        public String getBillTo() {
           return this.billto;
        }
        public String getOVShipTo() {
           return this.ov_shipto;
        }
        public String getOVBillTo() {
           return this.ov_billto;
        }
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }

        public String getIsaSenderID() {
            return isaSenderID;
        }

        public String getFSTLoopDate(int i) {
            return FSTloopdate.get(i);
        }
        public String getFSTLoopQty(int i) {
            return FSTloopqty.get(i);
        }
        public String getFSTLoopType(int i) {
            return FSTlooptype.get(i);
        }
         public String getFSTLoopRef(int i) {
            return FSTloopref.get(i);
        }
        
        public String getDetDate(int i) {
            return detdate.get(i);
        }
        public String getDetType(int i) {
            return dettype.get(i);
        }
        public String getDetItem(int i) {
            return detitem.get(i);
        }
        public String getDetCustItem(int i) {
           return detcustitem.get(i);
        }
        public String getDetSku(int i) {
          return detsku.get(i);
        }
        public String getDetRef(int i) {
           return detref.get(i);
        }
        public String getDetPO(int i) {
           return detpo.get(i);
        }
        public String getDetLine(int i) {
           return detline.get(i);
        }
        public String getDetQty(int i) {
           return detqty.get(i);
        }
        public String getDetListPrice(int i) {
           return detlistprice.get(i);
        }
        public String getDetNetPrice(int i) {
           return detnetprice.get(i);
        }
        public String getDetDisc(int i) {
           return detdisc.get(i);
        }
                
        public int getDetCount() {
            return detitem.size();
        }
       
        public int getFCSCount() {
            return FSTloopdate.size();
        }
        
    }
     
    public static class edi850 {
    // Header fields
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String po = "";
    public String billto = "";
    public String shipto = "";
    public String ov_billto = "";
    public String ov_shipto = "";
    public String remarks = "";
    public String shipmethod = "";
    public String duedate = "";
    public String podate = "";
    public String st_name = "";
    public String st_line1 = "";
    public String st_line2 = "";
    public String st_line3 = "";
    public String st_city = "";
    public String st_state = "";
    public String st_zip = "";
    public String st_country = "";
    
    // Detail fields      
    public ArrayList<String> detsku = new ArrayList<String>();
    public ArrayList<String> detcustitem = new ArrayList<String>();
    public ArrayList<String> detitem = new ArrayList<String>();
    public ArrayList<String> detuom = new ArrayList<String>();
    public ArrayList<String> detqty = new ArrayList<String>();
    public ArrayList<String> detref = new ArrayList<String>();
    public ArrayList<String> detpo = new ArrayList<String>();
    public ArrayList<String> detline = new ArrayList<String>();
    public ArrayList<String> detlistprice = new ArrayList<String>();
    public ArrayList<String> detnetprice = new ArrayList<String>();
    public ArrayList<String> detdisc = new ArrayList<String>();
        
        public edi850() {
            
        }
        
        public edi850(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
        
        
        public void addDetail() {
            this.detitem.add("");
            this.detuom.add("");
            this.detcustitem.add("");
            this.detsku.add("");
            this.detref.add("");
            this.detpo.add("");
            this.detline.add("");
            this.detqty.add("0");
            this.detlistprice.add("0");
            this.detnetprice.add("0");
            this.detdisc.add("0");
        }
        
        public void setDetItem(int i, String v) {
           this.detitem.set(i,v);
        }
        public void setDetUOM(int i, String v) {
           this.detuom.set(i,v);
        }
        public void setDetCustItem(int i, String v) {
           this.detcustitem.set(i,v);
        }
        public void setDetSku(int i, String v) {
           this.detsku.set(i,v);
        }
        public void setDetRef(int i, String v) {
           this.detref.set(i,v);
        }
        public void setDetPO(int i, String v) {
           this.detpo.set(i,v);
        }
        public void setDetLine(int i, String v) {
           this.detline.set(i,v);
        }
        public void setDetQty(int i, String v) {
           this.detqty.set(i,v);
        }
        public void setDetListPrice(int i, String v) {
           this.detlistprice.set(i,v);
        }
         public void setDetNetPrice(int i, String v) {
           this.detnetprice.set(i,v);
        }
          public void setDetDisc(int i, String v) {
           this.detdisc.set(i,v);
        }
        public void setPO(String v) {
           this.po = v;
        }
        public void setPODate(String v) {
           this.podate = v;
        }
         public void setDueDate(String v) {
           this.duedate = v;
        }
        public void setShipTo(String v) {
           this.shipto = v;
        }
         public void setShipVia(String v) {
           this.shipmethod = v;
        }
          public void setRemarks(String v) {
           this.remarks = v;
        }
        public void setShipToName(String v) {
           this.st_name = v;
        }
        public void setShipToLine1(String v) {
           this.st_line1 = v;
        }
        public void setShipToLine2(String v) {
           this.st_line2 = v;
        }
        public void setShipToLine3(String v) {
           this.st_line3 = v;
        }
        public void setShipToCity(String v) {
           this.st_city = v;
        }
        public void setShipToState(String v) {
           this.st_state = v;
        }
        public void setShipToZip(String v) {
           this.st_zip = v;
        }
        public void setShipToCountry(String v) {
           this.st_country = v;
        }
        public void setBillTo(String v) {
           this.billto = v;
        }
        public void setOVShipTo(String v) {
            this.ov_shipto = v;
        }
        public void setOVBillTo(String v) {
            this.ov_billto = v;
        }
        public String getPO() {
           return this.po;
        }
        public String getPODate() {
           return this.podate;
        }
        public String getShipTo() {
           return this.shipto;
        }
          public String getRemarks() {
           return this.remarks;
        }
            public String getShipVia() {
           return this.shipmethod;
        }
        public String getShipToName() {
           return this.st_name;
        }
        public String getShipToLine1() {
           return this.st_line1;
        }
        public String getShipToLine2() {
           return this.st_line2;
        }
        public String getShipToLine3() {
           return this.st_line3;
        }
        public String getShipToCity() {
           return this.st_city;
        }
        public String getShipToState() {
           return this.st_state;
        }
        public String getShipToZip() {
           return this.st_zip;
        }
        public String getShipToCountry() {
           return this.st_country;
        }
        public String getBillTo() {
           return this.billto;
        }
        public String getOVShipTo() {
           return this.ov_shipto;
        }
        public String getOVBillTo() {
           return this.ov_billto;
        }
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }
         public String getHdrDueDate() {
           return this.duedate;
        }

        public String getIsaSenderID() {
            return isaSenderID;
        }

        public String getDetItem(int i) {
            return detitem.get(i);
        }
        public String getDetUOM(int i) {
            return detuom.get(i);
        }
        public String getDetCustItem(int i) {
           return detcustitem.get(i);
        }
        public String getDetSku(int i) {
          return detsku.get(i);
        }
        public String getDetRef(int i) {
           return detref.get(i);
        }
        public String getDetPO(int i) {
           return detpo.get(i);
        }
        public String getDetLine(int i) {
           return detline.get(i);
        }
        public String getDetQty(int i) {
           return detqty.get(i);
        }
        public String getDetListPrice(int i) {
           return detlistprice.get(i);
        }
        public String getDetNetPrice(int i) {
           return detnetprice.get(i);
        }
        public String getDetDisc(int i) {
           return detdisc.get(i);
        }
        
        
        public int getDetCount() {
            return detitem.size();
        }
       
        
    }
    
     public static class edi945 {
    // Header fields
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String po = "";
    public String so = "";
    public String shipper = "";
    public String billto = "";
    public String shipto = "";
    public String ov_billto = "";
    public String ov_shipto = "";
    public String remarks = "";
    public String shipmethod = "";
    public String duedate = "";
    public String shipdate = "";
    public String podate = "";
    public String st_name = "";
    public String st_line1 = "";
    public String st_line2 = "";
    public String st_line3 = "";
    public String st_city = "";
    public String st_state = "";
    public String st_zip = "";
    public String st_country = "";
    
    // Detail fields     
    public ArrayList<String[]> detailArray = new ArrayList<String[]>();
    public int DetFieldsCount945 = 17;
    public String[] initDetailArray(String[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = "";
        }        
        return a;
    }
   
        
        public edi945() {
            
        }
        
        public edi945(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
        
        // setters for detail
        public void setDetLine(int i, String v) {
          this.detailArray.get(i)[0] = v;
        }
        public void setDetItem(int i, String v) {
         this.detailArray.get(i)[1] = v;
        }
        public void setDetCustItem(int i, String v) {
          this.detailArray.get(i)[2] = v;
        }
        public void setDetDesc(int i, String v) {
           this.detailArray.get(i)[3] = v;
        }
        public void setDetSite(int i, String v) {
           this.detailArray.get(i)[4] = v;
        }
        public void setDetQtyOrd(int i, String v) {
          this.detailArray.get(i)[5] = v;
        }
        public void setDetQtyShp(int i, String v) {
           this.detailArray.get(i)[6] = v;
        }
        public void setDetListPrice(int i, String v) {
           this.detailArray.get(i)[7] = v;
        }
         public void setDetNetPrice(int i, String v) {
           this.detailArray.get(i)[8] = v;
        }
          public void setDetNetWt(int i, String v) {
           this.detailArray.get(i)[9] = v;
        }
        public void setDetWH(int i, String v) {
           this.detailArray.get(i)[10] = v;
        }
        public void setDetLoc(int i, String v) {
          this.detailArray.get(i)[11] = v;
        }
        public void setDetUOM(int i, String v) {
          this.detailArray.get(i)[12] = v;
        }        
        public void setDetSku(int i, String v) {
           this.detailArray.get(i)[13] = v;
        }
        public void setDetRef(int i, String v) {
         this.detailArray.get(i)[14] = v;
        }
        public void setDetPO(int i, String v) {
          this.detailArray.get(i)[15] = v;
        }
        public void setDetDisc(int i, String v) {
           this.detailArray.get(i)[16] = v;
        }
        
        
       // getters for detail
         public String getDetLine(int i) {
            return detailArray.get(i)[0];
        }
          public String getDetItem(int i) {
            return detailArray.get(i)[1];
        }
         public String getDetCustItem(int i) {
           return detailArray.get(i)[2];
        }
         public String getDetDesc(int i) {
            return detailArray.get(i)[3];
        }
          public String getDetSite(int i) {
            return detailArray.get(i)[4];
        }
          public String getDetQtyOrd(int i) {
           return detailArray.get(i)[5];
        }
        public String getDetQtyShp(int i) {
           return detailArray.get(i)[6];
        }
        public String getDetListPrice(int i) {
           return detailArray.get(i)[7];
        }
        public String getDetNetPrice(int i) {
           return detailArray.get(i)[8];
        }
        public String getDetNetWt(int i) {
           return detailArray.get(i)[9];
        }
           public String getDetWH(int i) {
            return detailArray.get(i)[10];
        }
            public String getDetLoc(int i) {
            return detailArray.get(i)[11];
        }
        public String getDetUOM(int i) {
            return detailArray.get(i)[12];
        }
        public String getDetSku(int i) {
          return detailArray.get(i)[13];
        }
        public String getDetRef(int i) {
           return detailArray.get(i)[14];
        }
        public String getDetPO(int i) {
           return detailArray.get(i)[15];
        }
        public String getDetDisc(int i) {
           return detailArray.get(i)[16];
        } 
        
        
        
        
        
       // header setters 
        
        public void setPO(String v) {
           this.po = v;
        }
        public void setSO(String v) {
           this.so = v;
        }
        public void setShipper(String v) {
           this.shipper = v;
        }
        public void setPODate(String v) {
           this.podate = v;
        }
         public void setDueDate(String v) {
           this.duedate = v;
        }
          public void setShipDate(String v) {
           this.shipdate = v;
        }
        public void setShipTo(String v) {
           this.shipto = v;
        }
         public void setShipVia(String v) {
           this.shipmethod = v;
        }
          public void setRemarks(String v) {
           this.remarks = v;
        }
        public void setShipToName(String v) {
           this.st_name = v;
        }
        public void setShipToLine1(String v) {
           this.st_line1 = v;
        }
        public void setShipToLine2(String v) {
           this.st_line2 = v;
        }
        public void setShipToLine3(String v) {
           this.st_line3 = v;
        }
        public void setShipToCity(String v) {
           this.st_city = v;
        }
        public void setShipToState(String v) {
           this.st_state = v;
        }
        public void setShipToZip(String v) {
           this.st_zip = v;
        }
        public void setShipToCountry(String v) {
           this.st_country = v;
        }
        public void setBillTo(String v) {
           this.billto = v;
        }
        public void setOVShipTo(String v) {
            this.ov_shipto = v;
        }
        public void setOVBillTo(String v) {
            this.ov_billto = v;
        }
        
        // header getters
        public String getPO() {
           return this.po;
        }
        public String getSO() {
           return this.so;
        }
        public String getShipper() {
           return this.shipper;
        }
        public String getPODate() {
           return this.podate;
        }
        public String getShipTo() {
           return this.shipto;
        }
          public String getRemarks() {
           return this.remarks;
        }
            public String getShipVia() {
           return this.shipmethod;
        }
        public String getShipToName() {
           return this.st_name;
        }
        public String getShipToLine1() {
           return this.st_line1;
        }
        public String getShipToLine2() {
           return this.st_line2;
        }
        public String getShipToLine3() {
           return this.st_line3;
        }
        public String getShipToCity() {
           return this.st_city;
        }
        public String getShipToState() {
           return this.st_state;
        }
        public String getShipToZip() {
           return this.st_zip;
        }
        public String getShipToCountry() {
           return this.st_country;
        }
        public String getBillTo() {
           return this.billto;
        }
        public String getOVShipTo() {
           return this.ov_shipto;
        }
        public String getOVBillTo() {
           return this.ov_billto;
        }
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }
         public String getHdrDueDate() {
           return this.duedate;
        }
          public String getShipDate() {
           return this.shipdate;
        }

        public String getIsaSenderID() {
            return isaSenderID;
        }

      
        
        
        public int getDetCount() {
            return detailArray.size();
        }
       
        
    }
    
      public static class edi204 {
    // Header fields
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String custfo = "";
    public String carrier = "";
    public String equiptype = "";
    public String cust = "";
    public String tpid = "";
    public String ref = "";
    public String remarks = "";
    public String shipmethod = "";
    public String fodate = "";
    public String bol = "";
    public String weight = "";
    public String rate = "";
    public String ratetype = "";
    public String miles = "";
    public String hazmat = "";
    
    // Detail fields     
    public ArrayList<String[]> detailArray = new ArrayList<String[]>();
    public int DetFieldsCount204i = 31;
    public String[] initDetailArray(String[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = "";
        }        
        return a;
    }
   
        
        public edi204() {
            
        }
        
        public edi204(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
        
        // setters for detail
        public void setDetLine(int i, String v) {
          this.detailArray.get(i)[0] = v;
        }
        public void setDetType(int i, String v) {
         this.detailArray.get(i)[1] = v;
        }
        public void setDetShipper(int i, String v) {
          this.detailArray.get(i)[2] = v;
        }
        public void setDetDate(int i, String v) {
           this.detailArray.get(i)[3] = v;
        }
        public void setDetTime1(int i, String v) {
           this.detailArray.get(i)[4] = v;
        }
        public void setDetTimeType1(int i, String v) {
          this.detailArray.get(i)[5] = v;
        }
        public void setDetTime2(int i, String v) {
           this.detailArray.get(i)[6] = v;
        }
        public void setDetRef(int i, String v) {
           this.detailArray.get(i)[7] = v;
        }
         public void setDetAddrCode(int i, String v) {
           this.detailArray.get(i)[8] = v;
        }
          public void setDetAddrName(int i, String v) {
           this.detailArray.get(i)[9] = v;
        }
        public void setDetAddrLine1(int i, String v) {
           this.detailArray.get(i)[10] = v;
        }
        public void setDetAddrLine2(int i, String v) {
          this.detailArray.get(i)[11] = v;
        }
        public void setDetAddrCity(int i, String v) {
          this.detailArray.get(i)[12] = v;
        }        
        public void setDetAddrState(int i, String v) {
           this.detailArray.get(i)[13] = v;
        }
        public void setDetAddrZip(int i, String v) {
         this.detailArray.get(i)[14] = v;
        }
        public void setDetAddrContact(int i, String v) {
          this.detailArray.get(i)[15] = v;
        }
        public void setDetAddrPhone(int i, String v) {
           this.detailArray.get(i)[16] = v;
        }
        public void setDetUnits(int i, String v) {
           this.detailArray.get(i)[17] = v;
        }
        public void setDetBoxes(int i, String v) {
           this.detailArray.get(i)[18] = v;
        }
        public void setDetWeight(int i, String v) {
           this.detailArray.get(i)[19] = v;
        }
        public void setDetWeightUOM(int i, String v) {
           this.detailArray.get(i)[20] = v;
        }
        public void setDetRemarks(int i, String v) {
           this.detailArray.get(i)[21] = v;
        }
        public void setDetDateType(int i, String v) {
           this.detailArray.get(i)[22] = v;
        }
        public void setDetTimeType2(int i, String v) {
           this.detailArray.get(i)[23] = v;
        }
        public void setDetRate(int i, String v) {
           this.detailArray.get(i)[24] = v;
        }
        public void setDetMiles(int i, String v) {
           this.detailArray.get(i)[25] = v;
        }
        public void setDetTimeZone(int i, String v) {
           this.detailArray.get(i)[26] = v;
        }
        public void setDetCountry(int i, String v) {
           this.detailArray.get(i)[27] = v;
        }
        public void setDetPhone(int i, String v) {
           this.detailArray.get(i)[28] = v;
        }
        public void setDetEmail(int i, String v) {
           this.detailArray.get(i)[29] = v;
        }
        public void setDetContact(int i, String v) {
           this.detailArray.get(i)[30] = v;
        }
        
        
       // getters for detail
         public String getDetLine(int i) {
            return detailArray.get(i)[0];
        }
          public String getDetType(int i) {
            return detailArray.get(i)[1];
        }
         public String getDetShipper(int i) {
           return detailArray.get(i)[2];
        }
         public String getDetDate(int i) {
            return detailArray.get(i)[3];
        }
          public String getDetTime1(int i) {
            return detailArray.get(i)[4];
        }
          public String getDetTimeType1(int i) {
           return detailArray.get(i)[5];
        }
        public String getDetTime2(int i) {
           return detailArray.get(i)[6];
        }
        public String getDetRef(int i) {
           return detailArray.get(i)[7];
        }
        public String getDetAddrCode(int i) {
           return detailArray.get(i)[8];
        }
        public String getDetAddrName(int i) {
           return detailArray.get(i)[9];
        }
           public String getDetAddrLine1(int i) {
            return detailArray.get(i)[10];
        }
            public String getDetAddrLine2(int i) {
            return detailArray.get(i)[11];
        }
        public String getDetAddrCity(int i) {
            return detailArray.get(i)[12];
        }
        public String getDetAddrState(int i) {
          return detailArray.get(i)[13];
        }
        public String getDetAddrZip(int i) {
           return detailArray.get(i)[14];
        }
        public String getDetAddrContact(int i) {
           return detailArray.get(i)[15];
        }
        public String getDetAddrPhone(int i) {
           return detailArray.get(i)[16];
        } 
        public String getDetUnits(int i) {
           return detailArray.get(i)[17];
        }
        public String getDetBoxes(int i) {
           return detailArray.get(i)[18];
        }
        public String getDetWeight(int i) {
           return detailArray.get(i)[19];
        }
        public String getDetWeightUOM(int i) {
           return detailArray.get(i)[20];
        }
        public String getDetRemarks(int i) {
           return detailArray.get(i)[21];
        }
        public String getDetDateType(int i) {
           return detailArray.get(i)[22];
        }
        public String getDetTimeType2(int i) {
           return detailArray.get(i)[23];
        }
        public String getDetRate(int i) {
           return detailArray.get(i)[24];
        }
        public String getDetMiles(int i) {
           return detailArray.get(i)[25];
        }
        public String getDetTimeZone(int i) {
           return detailArray.get(i)[26];
        }
        public String getDetCountry(int i) {
           return detailArray.get(i)[27];
        }
        public String getDetPhone(int i) {
           return detailArray.get(i)[28];
        }
        public String getDetEmail(int i) {
           return detailArray.get(i)[29];
        }
        public String getDetContact(int i) {
           return detailArray.get(i)[30];
        }
              
// header setters 
   
    
        public void setCustFO(String v) {
           this.custfo = v;
        }
        public void setCarrier(String v) {
           this.carrier = v;
        }
        public void setEquipType(String v) {
           this.equiptype = v;
        }
        public void setCust(String v) {
           this.cust = v;
        }
        public void setTPID(String v) {
           this.tpid = v;
        }
         public void setRef(String v) {
           this.ref = v;
        }
          public void setRemarks(String v) {
           this.remarks = v;
        }
        public void setShipMethod(String v) {
           this.shipmethod = v;
        }
         public void setFODate(String v) {
           this.fodate = v;
        }
          public void setBOL(String v) {
           this.bol = v;
        }
          public void setWeight(String v) {
           this.weight = v;
        }  
        public void setRate(String v) {
           this.rate = v;
        }  
        public void setMiles(String v) {
           this.miles = v;
        }  
        public void setHazmat(String v) {
           this.hazmat = v;
        }  
        public void setRateType(String v) {
           this.ratetype = v;
        }  
        
 
        // header getters
        public String getCustFO() {
           return this.custfo;
        }
        public String getCarrier() {
           return this.carrier;
        }
        public String getEquipType() {
           return this.equiptype;
        }
        public String getCust() {
           return this.cust;
        }
        public String getTPID() {
           return this.tpid;
        }
        public String getRef() {
           return this.ref;
        }
          public String getRemarks() {
           return this.remarks;
        }
            public String getShipMethod() {
           return this.shipmethod;
        }
      
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }
         public String getFODate() {
           return this.fodate;
        }
          public String getBOL() {
           return this.bol;
        }
        public String getWeight() {
           return this.weight;
        }
        public String getRate() {
           return this.rate;
        }
        public String getMiles() {
           return this.miles;
        }
        public String getHazmat() {
           return this.hazmat;
        }
        public String getRateType() {
           return this.ratetype;
        }
        

      
        
        
        public int getDetCount() {
            return detailArray.size();
        }
       
        
    }
     
     public static class edi990 {
    // Header fields
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String order = "";
    public String yesno = "";
    public String reasoncode = "";
    public String scac = "";
    
  
   
        
        public edi990() {
            
        }
        
        public edi990(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
        
     
        
        
        
       // header setters 
        
        public void setOrder(String v) {
           this.order = v;
        }
        public void setYESNO(String v) {
           this.yesno = v;
        }
        public void setReasonCode(String v) {
           this.reasoncode = v;
        }
        public void setSCAC(String v) {
           this.scac = v;
        }
      
        
        // header getters
        public String getOrder() {
           return this.order;
        }
        public String getYESNO() {
           return this.yesno;
        }
        public String getReasonCode() {
           return this.reasoncode;
        }
        public String getSCAC() {
           return this.scac;
        }
       
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }
     

        public String getIsaSenderID() {
            return isaSenderID;
        }

      
        
        
     
       
        
    }
     
     public static class edi997i {
    // Header fields
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String gsCtrlNum = "";
    public String stCtrlNum = "";
    
  
    // Detail fields     
    public ArrayList<String[]> detailArray = new ArrayList<String[]>();
    public int DetFieldsCount997i = 1;
    public String[] initDetailArray(String[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = "";
        }        
        return a;
    }
        
        public edi997i() {
            
        }
        
        public edi997i(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String gsctrlnum, String isadate, 
                      String doctype) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaCtrlNum = gsctrlnum; 
            this.isaDate = isadate;
            this.doctype = doctype;
        }
        
      // detail setters
          public void setSTCtrlNum(int i, String v) {
          this.detailArray.get(i)[0] = v;
        }
      // detail getters 
           public String getSTCtrlNum(int i) {
            return detailArray.get(i)[0];
        }
        
     
        
       
      
        
        // header getters
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getGSCtrlNum() {
           return this.gsCtrlNum;
        }
         public String getISACtrlNum() {
           return this.gsCtrlNum;
        }
       

         public int getDetCount() {
            return detailArray.size();
        }
        
        
     
       
        
    }
     
      public static class edi220 {
    // Header fields
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String order = "";
    public String yesno = "";
    public String remarks = "";
    public String scac = "";
    public String amount = "";
    
  
   
        
        public edi220() {
            
        }
        
        public edi220(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
        
     
        
        
        
       // header setters 
        
        public void setOrder(String v) {
           this.order = v;
        }
        public void setAmount(String v) {
           this.amount = v;
        }
        public void setYESNO(String v) {
           this.yesno = v;
        }
        public void setRemarks(String v) {
           this.remarks = v;
        }
        public void setSCAC(String v) {
           this.scac = v;
        }
      
        
        // header getters
        public String getOrder() {
           return this.order;
        }
        public String getAmount() {
           return this.amount;
        }
        public String getYESNO() {
           return this.yesno;
        }
        public String getRemarks() {
           return this.remarks;
        }
        public String getSCAC() {
           return this.scac;
        }
       
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }
     

        public String getIsaSenderID() {
            return isaSenderID;
        }

      
        
        
     
       
        
    }
     
      public static class edi214 {
    // Header fields
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String order = "";
    public String pronbr = "";
    public String status = "";
    public String remarks = "";
    public String scac = "";
    public String lat = "";
    public String lon = "";
    public String apptdate = "";
    public String appttime = "";
    public String equipmentnbr = "";
    public String equipmenttype = "";
    
  
   
        
        public edi214() {
            
        }
        
        public edi214(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
        
     
        
        
        
       // header setters 
        
        public void setOrder(String v) {
           this.order = v;
        }
        public void setProNbr(String v) {
           this.pronbr = v;
        }
        public void setStatus(String v) {
           this.status = v;
        }
        public void setLat(String v) {
           this.lat = v;
        }
        public void setLon(String v) {
           this.lon = v;
        }
        public void setEquipmentNbr(String v) {
           this.equipmentnbr = v;
        }
        public void setEquipmentType(String v) {
           this.equipmenttype = v;
        }
        public void setApptDate(String v) {
           this.apptdate = v;
        }
        public void setApptTime(String v) {
           this.appttime = v;
        }
        public void setRemarks(String v) {
           this.remarks = v;
        }
        public void setSCAC(String v) {
           this.scac = v;
        }
      
        
        // header getters
        public String getOrder() {
           return this.order;
        }
        public String getProNbr() {
           return this.pronbr;
        }
        public String getStatus() {
           return this.status;
        }
        public String getRemarks() {
           return this.remarks;
        }
        public String getSCAC() {
           return this.scac;
        }
         public String getLat() {
           return this.lat;
        }
        public String getLon() {
           return this.lon;
        }
        public String getEquipmentType() {
           return this.equipmenttype;
        }
        public String getEquipmentNbr() {
           return this.equipmentnbr;
        }
        public String getApptDate() {
           return this.apptdate;
        }
        public String getApptTime() {
           return this.appttime;
        }
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }
     

        public String getIsaSenderID() {
            return isaSenderID;
        }

      
        
        
     
       
        
    } 
      
     public static class edi862 {
    
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String po = "";
    public String rlse = "";
    public String billto = "";
    public String shipto = "";
    public String ov_billto = "";
    public String ov_shipto = "";
    public String remarks = "";
    public String shipmethod = "";
    public String duedate = "";
    public String podate = "";
    public String st_name = "";
    public String st_line1 = "";
    public String st_line2 = "";
    public String st_line3 = "";
    public String st_city = "";
    public String st_state = "";
    public String st_zip = "";
    public String st_country = "";
    
       
    public ArrayList<String> detsku = new ArrayList<String>();
    public ArrayList<String> detcustitem = new ArrayList<String>();
    public ArrayList<String> detitem = new ArrayList<String>();
    public ArrayList<String> detqty = new ArrayList<String>();
    public ArrayList<String> detref = new ArrayList<String>();
    public ArrayList<String> detpo = new ArrayList<String>();
    public ArrayList<String> detline = new ArrayList<String>();
    public ArrayList<String> detlistprice = new ArrayList<String>();
    public ArrayList<String> detnetprice = new ArrayList<String>();
    public ArrayList<String> detdisc = new ArrayList<String>();
    public ArrayList<String> dettype = new ArrayList<String>();
    public ArrayList<String> detdate = new ArrayList<String>();
    
    
    public ArrayList<String> FSTloopqty = new ArrayList<String>();
    public ArrayList<String> FSTloopdate = new ArrayList<String>();
    public ArrayList<String> FSTlooptype = new ArrayList<String>();
    public ArrayList<String> FSTloopref = new ArrayList<String>();
    
    public Map<Integer, ArrayList<String>> map = new HashMap<Integer, ArrayList<String>>();
    
    
    
        public edi862() {
            
        }
        public edi862(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
       
        public void setFSTMap(Integer i, ArrayList<String> a) {
            this.map.put(i, a);
        }
        
        public void setFSTLoopDate(String v) {
           this.FSTloopdate.add(v);
        }
        public void setFSTLoopQty(String v)  {
           this.FSTloopqty.add(v);
        }
        public void setFSTLoopType(String v) {
           this.FSTlooptype.add(v);
        }
        public void setFSTLoopRef(String v) {
           this.FSTloopref.add(v);
        }
        
        public void setDetDate(String v) {
           this.detdate.add(v);
        }
        public void setDetType(String v) {
           this.dettype.add(v);
        }
        public void setDetItem(String v) {
           this.detitem.add(v);
        }
        public void setDetCustItem(String v) {
           this.detcustitem.add(v);
        }
        public void setDetSku(String v) {
           this.detsku.add(v);
        }
        public void setDetRef(String v) {
           this.detref.add(v);
        }
        public void setDetPO(String v) {
           this.detpo.add(v);
        }
        public void setDetLine(String v) {
           this.detline.add(v);
        }
        public void setDetQty(String v) {
           this.detqty.add(v);
        }
        public void setDetListPrice(String v) {
           this.detlistprice.add(v);
        }
         public void setDetNetPrice(String v) {
           this.detnetprice.add(v);
        }
          public void setDetDisc(String v) {
           this.detdisc.add(v);
        }
       
        public void setRlse(String v) {
           this.rlse = v;
        }  
        public void setPO(String v) {
           this.po = v;
        }
        public void setPODate(String v) {
           this.podate = v;
        }
         public void setDueDate(String v) {
           this.duedate = v;
        }
        public void setShipTo(String v) {
           this.shipto = v;
        }
        public void setShipToName(String v) {
           this.st_name = v;
        }
        public void setShipToLine1(String v) {
           this.st_line1 = v;
        }
        public void setShipToLine2(String v) {
           this.st_line2 = v;
        }
        public void setShipToLine3(String v) {
           this.st_line3 = v;
        }
        public void setShipToCity(String v) {
           this.st_city = v;
        }
        public void setShipToState(String v) {
           this.st_state = v;
        }
        public void setShipToZip(String v) {
           this.st_zip = v;
        }
        public void setShipToCountry(String v) {
           this.st_country = v;
        }
        public void setBillTo(String v) {
           this.billto = v;
        }
        public void setOVShipTo(String v) {
            this.ov_shipto = v;
        }
        public void setOVBillTo(String v) {
            this.ov_billto = v;
        }
        public String getPO() {
           return this.po;
        }
        public Map getFSAMap(int i) {
           return this.map;
        }
        public String getRlse() {
           return this.rlse;
        }
        public String getPODate() {
           return this.podate;
        }
        public String getShipTo() {
           return this.shipto;
        }
        public String getShipToName() {
           return this.st_name;
        }
        public String getShipToLine1() {
           return this.st_line1;
        }
        public String getShipToLine2() {
           return this.st_line2;
        }
        public String getShipToLine3() {
           return this.st_line3;
        }
        public String getShipToCity() {
           return this.st_city;
        }
        public String getShipToState() {
           return this.st_state;
        }
        public String getShipToZip() {
           return this.st_zip;
        }
        public String getShipToCountry() {
           return this.st_country;
        }
        public String getBillTo() {
           return this.billto;
        }
        public String getOVShipTo() {
           return this.ov_shipto;
        }
        public String getOVBillTo() {
           return this.ov_billto;
        }
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }

        public String getIsaSenderID() {
            return isaSenderID;
        }

        public String getFSTLoopDate(int i) {
            return FSTloopdate.get(i);
        }
        public String getFSTLoopQty(int i) {
            return FSTloopqty.get(i);
        }
        public String getFSTLoopType(int i) {
            return FSTlooptype.get(i);
        }
         public String getFSTLoopRef(int i) {
            return FSTloopref.get(i);
        }
        
        public String getDetDate(int i) {
            return detdate.get(i);
        }
        public String getDetType(int i) {
            return dettype.get(i);
        }
        public String getDetItem(int i) {
            return detitem.get(i);
        }
        public String getDetCustItem(int i) {
           return detcustitem.get(i);
        }
        public String getDetSku(int i) {
          return detsku.get(i);
        }
        public String getDetRef(int i) {
           return detref.get(i);
        }
        public String getDetPO(int i) {
           return detpo.get(i);
        }
        public String getDetLine(int i) {
           return detline.get(i);
        }
        public String getDetQty(int i) {
           return detqty.get(i);
        }
        public String getDetListPrice(int i) {
           return detlistprice.get(i);
        }
        public String getDetNetPrice(int i) {
           return detnetprice.get(i);
        }
        public String getDetDisc(int i) {
           return detdisc.get(i);
        }
                
        public int getDetCount() {
            return detitem.size();
        }
       
        public int getFCSCount() {
            return FSTloopdate.size();
        }
        
    }
    
    @Retention(RUNTIME)
    @Target(value = METHOD)
    public @interface AnnoDoc {
      /**
       * This provides description when generating docs.
       */
      public String[] desc();
      /**
       * This provides params when generating docs.
       */
      public String[] params();
    }
     
}

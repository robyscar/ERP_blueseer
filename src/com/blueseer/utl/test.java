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

package com.blueseer.utl;

import bsmf.MainFrame;
import static bsmf.MainFrame.db;
import static bsmf.MainFrame.pass;
import static bsmf.MainFrame.url;
import static bsmf.MainFrame.user;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jcifs.smb.SmbException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRResultSetDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;
import net.sourceforge.barbecue.BarcodeFactory;

/**
 *
 * @author vaughnte
 */
public class test extends javax.swing.JPanel {

    /**
     * Creates new form test
     */
    public test() {
        initComponents();
    }

    public void initvars() {
        
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btjasper = new javax.swing.JButton();
        btlabelprint = new javax.swing.JButton();
        testmail = new javax.swing.JButton();
        btjobticket = new javax.swing.JButton();
        btautoclock = new javax.swing.JButton();

        btjasper.setText("jasper");
        btjasper.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btjasperActionPerformed(evt);
            }
        });

        btlabelprint.setText("labelprinttest");
        btlabelprint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btlabelprintActionPerformed(evt);
            }
        });

        testmail.setText("testmail");
        testmail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testmailActionPerformed(evt);
            }
        });

        btjobticket.setText("Jobticket");
        btjobticket.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btjobticketActionPerformed(evt);
            }
        });

        btautoclock.setText("AutoClock");
        btautoclock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btautoclockActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(152, 152, 152)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btlabelprint)
                            .addComponent(btjasper)
                            .addComponent(btjobticket))
                        .addContainerGap(148, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btautoclock)
                            .addComponent(testmail))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(39, 39, 39)
                .addComponent(btautoclock)
                .addGap(18, 18, 18)
                .addComponent(testmail)
                .addGap(18, 18, 18)
                .addComponent(btjasper)
                .addGap(18, 18, 18)
                .addComponent(btlabelprint)
                .addGap(18, 18, 18)
                .addComponent(btjobticket)
                .addContainerGap(45, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btjasperActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btjasperActionPerformed
       
  try {
            Connection con = DriverManager.getConnection(url + db, user, pass);
            

                HashMap hm = new HashMap();
                hm.put("REPORT_TITLE", "SHIPPER");
                hm.put("myid",  "00808522");
                hm.put("myid2",  "00808530");
                hm.put("imagepath", "images/avmlogo.png");
               // res = st.executeQuery("select shd_id, sh_cust, shd_po, shd_item, shd_qty, shd_netprice, cm_code, cm_name, cm_line1, cm_line2, cm_city, cm_state, cm_zip, concat(cm_city, \" \", cm_state, \" \", cm_zip) as st_citystatezip, site_desc from ship_det inner join ship_mstr on sh_id = shd_id inner join cm_mstr on cm_code = sh_cust inner join site_mstr on site_site = sh_site where shd_id = '1848' ");
               // JRResultSetDataSource jasperReports = new JRResultSetDataSource(res);
                File mytemplate = new File("jasper/ps_generic_multi.jasper");
                JasperPrint jasperPrint = JasperFillManager.fillReport(mytemplate.getPath(), hm, con );
                JasperExportManager.exportReportToPdfFile(jasperPrint,"temp/b1.pdf");
         
            JasperViewer jasperViewer = new JasperViewer(jasperPrint, false);
            jasperViewer.setVisible(true);
                
                
            con.close();
        } catch (Exception e) {
            MainFrame.bslog(e);
        }


    }//GEN-LAST:event_btjasperActionPerformed

    private void btlabelprintActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btlabelprintActionPerformed
        try {

Socket soc = null;
DataOutputStream dos= null;
String ZPLPrinterIPAddress="10.17.4.99";
int ZPLPrinterPort =9100;
            
            
  //          FileOutputStream fos = new FileOutputStream("10.17.4.99");
 //           PrintStream ps = new PrintStream(fos);

BufferedReader fsr = new BufferedReader(new FileReader(new File("zebra/address.prn")));
String line = "";
String concatline = "";
while ((line = fsr.readLine()) != null) {
    concatline += line;
}
fsr.close();
// fos.write(concatline.getBytes());

concatline = concatline.replace("$ADDRNAME", "TEST USER");



 String commands = "^XA~TA000~JSN^LT0^MMT^MCY^MNW^MTT^PON^PMN^LH0,0^JMA^PR4,4^MD0^JUS^LRN^CI0^XZ" +
"^XA^LL1218" +
"^BY2,3,106^FT406,949^B3B,N,,N,N" +
"^FD661400C01000^FS" +
"^FT585,970^A0B,65,60^FH\\^FDPC3332^FS" +
"^FT168,1198^A0B,133,122^FH\\^FD661400C01000^FS" +
"^FT578,1094^A0B,28,28^FH\\^FDPART:^FS" +
"^PQ1,0,1,Y^XZ";


 soc = new Socket(ZPLPrinterIPAddress, ZPLPrinterPort);
        dos= new DataOutputStream(soc.getOutputStream());
        dos.writeBytes(concatline);

 dos.close();
 soc.close();
 

 //ps.println(commands);
 //                   ps.print("\f");
 //                   ps.close();


/*
try {
        
        Process p = Runtime.getRuntime().exec("sleep 5");
    }
    catch (Exception ex) {
        ex.printStackTrace();
    }
*/


//printer.println(concatline);
//printer.flush();

// close and free the device
// printer.close();
//fos.close();
} catch (Exception e) {
MainFrame.bslog(e);
}
    }//GEN-LAST:event_btlabelprintActionPerformed

    private void testmailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testmailActionPerformed
        OVData.sendEmailByID("vaughnte", "test", "testbody", "");
    }//GEN-LAST:event_testmailActionPerformed

    private void btjobticketActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btjobticketActionPerformed
         try {
            Connection con = DriverManager.getConnection(url + db, user, pass);
            
                HashMap hm = new HashMap();
                hm.put("REPORT_TITLE", "MASTER TICKET");
                 hm.put("SUBREPORT_DIR", "jasper/");
                hm.put("myid",  "1");
                //hm.put("imagepath", "images/avmlogo.png");
               // res = st.executeQuery("select shd_id, sh_cust, shd_po, shd_item, shd_qty, shd_netprice, cm_code, cm_name, cm_line1, cm_line2, cm_city, cm_state, cm_zip, concat(cm_city, \" \", cm_state, \" \", cm_zip) as st_citystatezip, site_desc from ship_det inner join ship_mstr on sh_id = shd_id inner join cm_mstr on cm_code = sh_cust inner join site_mstr on site_site = sh_site where shd_id = '1848' ");
               // JRResultSetDataSource jasperReports = new JRResultSetDataSource(res);
                File mytemplate = new File("jasper/jobticket.jasper");
                JasperPrint jasperPrint = JasperFillManager.fillReport(mytemplate.getPath(), hm, con );
                JasperExportManager.exportReportToPdfFile(jasperPrint,"temp/jobticket.pdf");
         
            JasperViewer jasperViewer = new JasperViewer(jasperPrint, false);
            jasperViewer.setVisible(true);
                
                
           con.close();
        } catch (Exception e) {
            MainFrame.bslog(e);
        }

    }//GEN-LAST:event_btjobticketActionPerformed

    private void btautoclockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btautoclockActionPerformed
        OVData.autoclock(-7);
        bsmf.MainFrame.show("clock complete");
    }//GEN-LAST:event_btautoclockActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btautoclock;
    private javax.swing.JButton btjasper;
    private javax.swing.JButton btjobticket;
    private javax.swing.JButton btlabelprint;
    private javax.swing.JButton testmail;
    // End of variables declaration//GEN-END:variables
}

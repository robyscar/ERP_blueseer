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

package com.blueseer.fgl;

import bsmf.MainFrame;
import static bsmf.MainFrame.db;
import com.blueseer.utl.OVData;
import com.blueseer.utl.BlueSeerUtils;
import java.awt.Color;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import static bsmf.MainFrame.driver;
import static bsmf.MainFrame.ds;
import static bsmf.MainFrame.mydialog;
import static bsmf.MainFrame.pass;
import static bsmf.MainFrame.tags;
import static bsmf.MainFrame.url;
import static bsmf.MainFrame.user;
import static com.blueseer.utl.BlueSeerUtils.bsParseDouble;
import static com.blueseer.utl.BlueSeerUtils.currformatDouble;
import static com.blueseer.utl.BlueSeerUtils.getGlobalColumnTag;
import static com.blueseer.utl.BlueSeerUtils.getMessageTag;
import java.sql.Connection;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author vaughnte
 */
public class TrialBalanceRpt extends javax.swing.JPanel {
 
     public Map<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
     
    javax.swing.table.DefaultTableModel mymodel = new javax.swing.table.DefaultTableModel(new Object[][]{},
                        new String[]{getGlobalColumnTag("detail"),
                            getGlobalColumnTag("account"),
                            getGlobalColumnTag("type"),
                            getGlobalColumnTag("currency"),
                            getGlobalColumnTag("description"),
                            getGlobalColumnTag("site"),
                            getGlobalColumnTag("debits"),
                            getGlobalColumnTag("credits")})
            {
                      @Override  
                      public Class getColumnClass(int col) {  
                        if (col == 0  )       
                            return ImageIcon.class;  
                        else return String.class;  //other columns accept String values  
                      }  
                        };
    
    
    javax.swing.table.DefaultTableModel mymodelCC = new javax.swing.table.DefaultTableModel(new Object[][]{},
                        new String[]{getGlobalColumnTag("detail"),
                            getGlobalColumnTag("account"),
                            getGlobalColumnTag("type"),
                            getGlobalColumnTag("currency"),
                            getGlobalColumnTag("description"),
                            getGlobalColumnTag("site"), 
                            getGlobalColumnTag("costcenter"), 
                            getGlobalColumnTag("debits"),
                            getGlobalColumnTag("credits")})
            {
                      @Override  
                      public Class getColumnClass(int col) {  
                        if (col == 0  )       
                            return ImageIcon.class;  
                        else return String.class;  //other columns accept String values  
                      }  
                        };
                
    javax.swing.table.DefaultTableModel modeldetail = new javax.swing.table.DefaultTableModel(new Object[][]{},
                        new String[]{getGlobalColumnTag("account"),
                            getGlobalColumnTag("costcenter"), 
                            getGlobalColumnTag("site"), 
                            getGlobalColumnTag("reference"), 
                            getGlobalColumnTag("type"),
                            getGlobalColumnTag("effectivedate"),
                            getGlobalColumnTag("description"),
                            getGlobalColumnTag("amount")});
    
    
   
    
     class ButtonRenderer extends JButton implements TableCellRenderer {

        public ButtonRenderer() {
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(Color.blue);
                setBackground(UIManager.getColor("Button.background"));
            }
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
    
   
     private static class myHeaderRenderer implements TableCellRenderer {
      DefaultTableCellRenderer renderer;
      int horAlignment;
      public myHeaderRenderer(JTable table, int horizontalAlignment) {
        horAlignment = horizontalAlignment;
        renderer = (DefaultTableCellRenderer)table.getTableHeader()
            .getDefaultRenderer();
      }
      public Component getTableCellRendererComponent(JTable table, Object value,
          boolean isSelected, boolean hasFocus, int row, int col) {
        Component c = renderer.getTableCellRendererComponent(table, value,
          isSelected, hasFocus, row, col);
        JLabel label = (JLabel)c;
        label.setHorizontalAlignment(horAlignment);
        return label;
      }
}

    
    
    
    /**
     * Creates new form ScrapReportPanel
     */
    public TrialBalanceRpt() {
        initComponents();
        setLanguageTags(this);
    }

    public void getdetail(String acct, String site, String year, String period) {
      
         modeldetail.setNumRows(0);
         double total = 0;
         ArrayList<Date> actdatearray = fglData.getGLCalForPeriod(year, period);  
                String datestart = String.valueOf(actdatearray.get(0));
                String dateend = String.valueOf(actdatearray.get(1));
                
                tabledetail.getColumnModel().getColumn(7).setCellRenderer(BlueSeerUtils.NumberRenderer.getCurrencyRenderer()); 
        
        try {

            Connection con = null;
            if (ds != null) {
            con = ds.getConnection();
            } else {
              con = DriverManager.getConnection(url + db, user, pass);  
            }
            Statement st = con.createStatement();
            ResultSet res = null;
            try {
                int i = 0;
                String blanket = "";
                res = st.executeQuery("select glh_acct, glh_cc, glh_site, glh_type, glh_ref, glh_doc, glh_effdate, glh_desc, glh_base_amt from gl_hist " +
                        " where glh_acct = " + "'" + acct + "'" + " AND " + 
                        " glh_site = " + "'" + site + "'" + " AND " +
                        " glh_effdate >= " + "'" + datestart + "'" + " AND " +
                        " glh_effdate <= " + "'" + dateend + "'" + ";");
                while (res.next()) {
                    total = total + res.getDouble("glh_base_amt");
                   modeldetail.addRow(new Object[]{ 
                      res.getString("glh_acct"), 
                       res.getString("glh_cc"),
                       res.getString("glh_site"),
                      res.getString("glh_ref"), 
                      res.getString("glh_type"), 
                      res.getString("glh_effdate"),
                      res.getString("glh_desc"),
                      bsParseDouble(currformatDouble(res.getDouble("glh_base_amt")))  });
                }
               
                tabledetail.setModel(modeldetail);
                this.repaint();

            } catch (SQLException s) {
                MainFrame.bslog(s);
                bsmf.MainFrame.show(getMessageTag(1016,Thread.currentThread().getStackTrace()[1].getMethodName()));
            } finally {
                if (res != null) {
                    res.close();
                }
                if (st != null) {
                    st.close();
                }
                con.close();
            }
        } catch (Exception e) {
            MainFrame.bslog(e);
        }

    }
    
    public void getdetailCC(String acct, String cc, String site, String year, String period) {
      
         modeldetail.setNumRows(0);
         double total = 0;
         ArrayList<Date> actdatearray = fglData.getGLCalForPeriod(year, period);  
                String datestart = String.valueOf(actdatearray.get(0));
                String dateend = String.valueOf(actdatearray.get(1));
        tabledetail.getColumnModel().getColumn(7).setCellRenderer(BlueSeerUtils.NumberRenderer.getCurrencyRenderer());
        try {

            Connection con = null;
            if (ds != null) {
            con = ds.getConnection();
            } else {
              con = DriverManager.getConnection(url + db, user, pass);  
            }
            Statement st = con.createStatement();
            ResultSet res = null;
            try {
                int i = 0;
                String blanket = "";
                res = st.executeQuery("select glh_acct, glh_cc, glh_site, glh_ref, glh_doc, glh_effdate, glh_desc, glh_base_amt from gl_hist " +
                        " where glh_acct = " + "'" + acct + "'" + " AND " + 
                        " glh_cc = " + "'" + cc + "'" + " AND " +
                        " glh_site = " + "'" + site + "'" + " AND " +
                        " glh_effdate >= " + "'" + datestart + "'" + " AND " +
                        " glh_effdate <= " + "'" + dateend + "'" + ";");
                while (res.next()) {
                    total = total + res.getDouble("glh_base_amt");
                   modeldetail.addRow(new Object[]{ 
                      res.getString("glh_acct"), 
                       res.getString("glh_cc"),
                       res.getString("glh_site"),
                      res.getString("glh_ref"), 
                      res.getString("glh_doc"), 
                      res.getString("glh_effdate"),
                      res.getString("glh_desc"),
                      bsParseDouble(currformatDouble(res.getDouble("glh_base_amt")))});
                }
               
              
                tabledetail.setModel(modeldetail);
                this.repaint();

            } catch (SQLException s) {
                MainFrame.bslog(s);
                bsmf.MainFrame.show(getMessageTag(1016,Thread.currentThread().getStackTrace()[1].getMethodName()));
            } finally {
                if (res != null) {
                    res.close();
                }
                if (st != null) {
                    st.close();
                }
                con.close();
            }
        } catch (Exception e) {
            MainFrame.bslog(e);
        }

    }
    
    public void setLanguageTags(Object myobj) {
       JPanel panel = null;
        JTabbedPane tabpane = null;
        JScrollPane scrollpane = null;
        if (myobj instanceof JPanel) {
            panel = (JPanel) myobj;
        } else if (myobj instanceof JTabbedPane) {
           tabpane = (JTabbedPane) myobj; 
        } else if (myobj instanceof JScrollPane) {
           scrollpane = (JScrollPane) myobj;    
        } else {
            return;
        }
       Component[] components = panel.getComponents();
       for (Component component : components) {
           if (component instanceof JPanel) {
                    if (tags.containsKey(this.getClass().getSimpleName() + ".panel." + component.getName())) {
                       ((JPanel) component).setBorder(BorderFactory.createTitledBorder(tags.getString(this.getClass().getSimpleName() +".panel." + component.getName())));
                    } 
                    setLanguageTags((JPanel) component);
                }
                if (component instanceof JLabel ) {
                    if (tags.containsKey(this.getClass().getSimpleName() + ".label." + component.getName())) {
                       ((JLabel) component).setText(tags.getString(this.getClass().getSimpleName() +".label." + component.getName()));
                    }
                }
                if (component instanceof JButton ) {
                    if (tags.containsKey("global.button." + component.getName())) {
                       ((JButton) component).setText(tags.getString("global.button." + component.getName()));
                    }
                }
                if (component instanceof JCheckBox) {
                    if (tags.containsKey(this.getClass().getSimpleName() + ".label." + component.getName())) {
                       ((JCheckBox) component).setText(tags.getString(this.getClass().getSimpleName() +".label." + component.getName()));
                    } 
                }
                if (component instanceof JRadioButton) {
                    if (tags.containsKey(this.getClass().getSimpleName() + ".label." + component.getName())) {
                       ((JRadioButton) component).setText(tags.getString(this.getClass().getSimpleName() +".label." + component.getName()));
                    } 
                }
       }
    }
    
     
    public void initvars(String[] arg) {
        
        lbldebits.setText("0");
        lblcredits.setText("0");
       
        
        java.util.Date now = new java.util.Date();
        DateFormat dfdate = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat dfyear = new SimpleDateFormat("yyyy");
        DateFormat dfperiod = new SimpleDateFormat("M");
        
        mymodel.setNumRows(0);
         mymodelCC.setNumRows(0);
        modeldetail.setNumRows(0);
        tablereport.setModel(mymodel);
        tabledetail.setModel(modeldetail);
        
        tablereport.getTableHeader().setReorderingAllowed(false);
        tabledetail.getTableHeader().setReorderingAllowed(false);
         
        /*
        BlueSeerUtils.clickheader = new ImageIcon(getClass().getResource("/images/flag.png")); 
        BlueSeerUtilsclickprint = new ImageIcon(getClass().getResource("/images/print.png")); 
        clickdetail = new ImageIcon(getClass().getResource("/images/basket.png")); 
       */
          
         
                //          ReportPanel.TableReport.getColumn("CallID").setCellEditor(
                    //       new ButtonEditor(new JCheckBox()));
        
        
        
        
        btdetail.setEnabled(false);
        detailpanel.setVisible(false);
        
        ddsite.removeAllItems();
        ArrayList sites = OVData.getSiteList();
        for (Object site : sites) {
            ddsite.addItem(site);
        }
         ddsite.setSelectedItem(OVData.getDefaultSite());
        
        
        ddyear.removeAllItems();
        for (int i = 1967 ; i < 2222; i++) {
            ddyear.addItem(String.valueOf(i));
        }
        ddyear.setSelectedItem(dfyear.format(now));
            
        ddperiod.removeAllItems();
        for (int i = 1 ; i <= 12; i++) {
            ddperiod.addItem(String.valueOf(i));
        }
        String[] fromdatearray = fglData.getGLCalForDate(dfdate.format(now));
        //int fromdateperiod = Integer.valueOf(fromdatearray.get(1).toString());
        ddperiod.setSelectedItem(fromdatearray[1].toString());
        ArrayList startend = fglData.getGLCalForPeriod(ddyear.getSelectedItem().toString(), ddperiod.getSelectedItem().toString());
        datelabel.setText(startend.get(0).toString() + " To " + startend.get(1).toString());
       
        
          
          
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        tablepanel = new javax.swing.JPanel();
        summarypanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablereport = new javax.swing.JTable();
        detailpanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tabledetail = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        btdetail = new javax.swing.JButton();
        btRun = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        cbzero = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        ddperiod = new javax.swing.JComboBox();
        ddyear = new javax.swing.JComboBox();
        datelabel = new javax.swing.JLabel();
        ddsite = new javax.swing.JComboBox();
        cbcc = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        lblcredits = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        lbldebits = new javax.swing.JLabel();

        setBackground(new java.awt.Color(0, 102, 204));

        jPanel1.setName("panelmain"); // NOI18N

        tablepanel.setLayout(new javax.swing.BoxLayout(tablepanel, javax.swing.BoxLayout.LINE_AXIS));

        summarypanel.setLayout(new java.awt.BorderLayout());

        tablereport.setAutoCreateRowSorter(true);
        tablereport.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        tablereport.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tablereportMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tablereport);

        summarypanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        tablepanel.add(summarypanel);

        detailpanel.setLayout(new java.awt.BorderLayout());

        tabledetail.setAutoCreateRowSorter(true);
        tabledetail.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(tabledetail);

        detailpanel.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        tablepanel.add(detailpanel);

        btdetail.setText("Hide Detail");
        btdetail.setName("bthidedetail"); // NOI18N
        btdetail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btdetailActionPerformed(evt);
            }
        });

        btRun.setText("Run");
        btRun.setName("btrun"); // NOI18N
        btRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btRunActionPerformed(evt);
            }
        });

        jLabel5.setText("Site");
        jLabel5.setName("lblsite"); // NOI18N

        cbzero.setText("Supress Zeros");
        cbzero.setName("cbsuppresszeros"); // NOI18N

        jLabel3.setText("Period");
        jLabel3.setName("lblperiod"); // NOI18N

        jLabel2.setText("Year");
        jLabel2.setName("lblyear"); // NOI18N

        ddperiod.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" }));
        ddperiod.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                ddperiodItemStateChanged(evt);
            }
        });

        ddyear.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                ddyearItemStateChanged(evt);
            }
        });

        cbcc.setText("CostCenter");
        cbcc.setName("cbcostcenter"); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(datelabel, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(443, 443, 443))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ddyear, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(ddperiod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addGap(4, 4, 4)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(ddsite, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(btRun)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btdetail))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(cbcc)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbzero)))
                        .addGap(202, 202, 202))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ddyear, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(btRun)
                    .addComponent(btdetail)
                    .addComponent(ddsite, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ddperiod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(cbcc)
                    .addComponent(cbzero, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(datelabel, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jLabel8.setText("Total Credits");
        jLabel8.setName("lbltotalcredits"); // NOI18N

        lblcredits.setText("0");

        jLabel7.setText("Total Debits");
        jLabel7.setName("lbltotaldebits"); // NOI18N

        lbldebits.setText("0");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel8)
                    .addComponent(jLabel7))
                .addGap(27, 27, 27)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblcredits, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbldebits, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbldebits, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblcredits, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addGap(36, 36, 36))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 205, Short.MAX_VALUE)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(tablepanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(22, 22, 22)
                .addComponent(tablepanel, javax.swing.GroupLayout.DEFAULT_SIZE, 397, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btRunActionPerformed

    
try {
            Connection con = null;
            if (ds != null) {
            con = ds.getConnection();
            } else {
              con = DriverManager.getConnection(url + db, user, pass);  
            }
            Statement st = con.createStatement();
            ResultSet res = null;
            Statement st2 = con.createStatement();
            ResultSet res2 = null;
            try {
                
                int qty = 0;
                double dol = 0;
                int j = 0;
               
               mymodel.setNumRows(0);
               mymodelCC.setNumRows(0);
                
                 
          if (cbcc.isSelected()) {    
              tablereport.setModel(mymodelCC);
          tablereport.getColumnModel().getColumn(7).setCellRenderer(BlueSeerUtils.NumberRenderer.getCurrencyRenderer());
          tablereport.getColumnModel().getColumn(8).setCellRenderer(BlueSeerUtils.NumberRenderer.getCurrencyRenderer());
        //  tablereport.getColumnModel().getColumn(0).setCellRenderer(new ButtonRenderer());
         tablereport.getColumnModel().getColumn(0).setMaxWidth(100);
          } else {
              tablereport.setModel(mymodel);
          tablereport.getColumnModel().getColumn(6).setCellRenderer(BlueSeerUtils.NumberRenderer.getCurrencyRenderer());
          tablereport.getColumnModel().getColumn(7).setCellRenderer(BlueSeerUtils.NumberRenderer.getCurrencyRenderer()); 
        //  tablereport.getColumnModel().getColumn(0).setCellRenderer(new ButtonRenderer());
         tablereport.getColumnModel().getColumn(0).setMaxWidth(100);
          }
          
          
          for (int i = 0 ; i < tablereport.getColumnCount(); i++){ 
              if (cbcc.isSelected()) {
                  if (i == 7 || i == 8 ) {
                 tablereport.getTableHeader().getColumnModel().getColumn(i)
                 .setHeaderRenderer(new myHeaderRenderer(tablereport, JLabel.RIGHT));
                  } else {
                  tablereport.getTableHeader().getColumnModel().getColumn(i)
                 .setHeaderRenderer(new myHeaderRenderer(tablereport, JLabel.LEFT));    
                  }
              } else {
                   if (i == 6 || i == 7 ) {
                 tablereport.getTableHeader().getColumnModel().getColumn(i)
                 .setHeaderRenderer(new myHeaderRenderer(tablereport, JLabel.RIGHT));
                  } else {
                  tablereport.getTableHeader().getColumnModel().getColumn(i)
                 .setHeaderRenderer(new myHeaderRenderer(tablereport, JLabel.LEFT));    
                  }
              }
          }
          
          
          
                 DateFormat dfdate = new SimpleDateFormat("yyyy-MM-dd");
                
                
                 
                 int period = Integer.valueOf(ddperiod.getSelectedItem().toString());
                 int year = Integer.valueOf(ddyear.getSelectedItem().toString());
                 int prioryear = 0;
                 double begbal = 0;
                 double activity = 0;
                 double endbal = 0;
                 double totbegbal = 0;
                 double totactivity = 0;
                 double totendbal = 0;
                 double totaldebits = 0;
                 double totalcredits = 0;
                 double preact = 0;
                 double postact = 0;
                 Date p_datestart = null;
                 Date p_dateend = null;
                 
                 ArrayList<String> ccamts = new ArrayList<String>();
                 
                 ArrayList<String[]> accounts = fglData.getGLAcctListRangeWCurrTypeDesc("", "");
                // ArrayList<String> accounts = OVData.getGLAcctList();
                 
                 ArrayList<String> ccs = fglData.getGLCCList();
                 
                  totbegbal = 0;
                  totactivity = 0;
                  totendbal = 0;
                 
                 prioryear = year - 1;
                 String site = ddsite.getSelectedItem().toString(); 
                 String acctid = "";
                 String accttype = "";
                 String acctdesc = "";
                 String acctcurr = "";
                 String cc = "";
                 
                 
                 if (cbcc.isSelected()) {
                 
                 ACCTS:    for (String[] account : accounts) {
                     
                     
                 
                  acctid = account[0];
                  acctcurr = account[1];
                  accttype = account[2];
                  acctdesc = account[3];
                  
                 
                  
                  begbal = 0;
                  activity = 0;
                  endbal = 0;
                  preact = 0;
                  postact = 0;
                
                  
                  
                 // calculate all acb_mstr records for whole periods < fromdateperiod
                    // begbal += OVData.getGLAcctBalSummCC(account.toString(), String.valueOf(fromdateyear), String.valueOf(p));
                  if (accttype.equals("L") || accttype.equals("A")) {
                      //must be type balance sheet
                  res = st.executeQuery("select acb_cc, sum(acb_amt) as sum from acb_mstr where " +
                        " acb_acct = " + "'" + acctid + "'" + " AND " +
                        " acb_site = " + "'" + site + "'" + " AND " +
                        " (( acb_year = " + "'" + year + "'" + " AND acb_per <= " + "'" + period + "'" + " ) OR " +
                        "  ( acb_year <= " + "'" + prioryear + "'" + " )) " +
                        " group by acb_cc ;");
                
                       while (res.next()) {
                          endbal = 0;
                          activity = 0;
                          begbal = 0;
                          begbal = res.getDouble("sum");
                          
                           // now activity
                                      res2= st2.executeQuery("select sum(acb_amt) as sum from acb_mstr where acb_year = " +
                                "'" + String.valueOf(year) + "'" + 
                                " AND acb_per = " +
                                "'" + String.valueOf(period) + "'" +
                                " AND acb_acct = " +
                                "'" + acctid + "'" +
                                " AND acb_cc = " +
                                "'" + res.getString("acb_cc") + "'" +
                                " AND acb_site = " + "'" + site + "'" +
                                " ;");
                               while (res2.next()) {
                                  activity = res2.getDouble(("sum"));
                               }
                            
                               begbal = begbal - activity;
                               endbal = begbal + activity;
                           
                             if (accttype.equals("L") || accttype.equals("O")) {
                                   totalcredits = totalcredits + (-1 * endbal);
                                   mymodelCC.addRow(new Object[]{BlueSeerUtils.clickbasket, acctid, accttype, acctcurr,
                                acctdesc,
                                site,
                                res.getString("acb_cc"),
                                0,
                                bsParseDouble(currformatDouble((-1 * endbal)))
                            });
                               } else {
                                   totaldebits = totaldebits + endbal ;
                                   mymodelCC.addRow(new Object[]{BlueSeerUtils.clickbasket, acctid, accttype, acctcurr,
                                acctdesc,
                                site,
                                res.getString("acb_cc"),
                                bsParseDouble(currformatDouble(endbal)),
                                0
                            });
                               }
                 totendbal = totendbal + endbal;
                 totbegbal = totbegbal + begbal;
                 totactivity = totactivity + activity;
                            
                       }
                  } else {
                     // must be income statement
                      res = st.executeQuery("select acb_cc, sum(acb_amt) as sum from acb_mstr where " +
                        " acb_acct = " + "'" + acctid + "'" + " AND " +
                        " acb_site = " + "'" + site + "'" + " AND " +
                        " ( acb_year = " + "'" + year + "'" + " AND acb_per <= " + "'" + period + "'" + ")" +
                        " group by acb_cc ;");
                
                       while (res.next()) {
                          endbal = 0;
                          activity = 0;
                          begbal = 0;
                          
                       
                          begbal = res.getDouble("sum");
                        
                                    // now activity
                                      res2= st2.executeQuery("select sum(acb_amt) as sum from acb_mstr where acb_year = " +
                                "'" + String.valueOf(year) + "'" + 
                                " AND acb_per = " +
                                "'" + String.valueOf(period) + "'" +
                                " AND acb_acct = " +
                                "'" + acctid + "'" +
                                " AND acb_cc = " +
                                "'" + res.getString("acb_cc") + "'" +
                                " AND acb_site = " + "'" + site + "'" +
                                "  ;");
                               while (res2.next()) {
                                  activity = res2.getDouble(("sum"));
                               }
                            
                               begbal = begbal - activity;
                               endbal = begbal + activity;
                               if (accttype.equals("I") ) {
                                   totalcredits = totalcredits + (-1 * endbal);
                                   mymodelCC.addRow(new Object[]{BlueSeerUtils.clickbasket, acctid, accttype, acctcurr,
                                acctdesc,
                                site,
                                res.getString("acb_cc"),
                                0,
                                bsParseDouble(currformatDouble((-1 * endbal)))
                            });
                               } else {
                                   totaldebits = totaldebits + endbal ;
                                   mymodelCC.addRow(new Object[]{BlueSeerUtils.clickbasket, acctid, accttype, acctcurr,
                                acctdesc,
                                site,
                                res.getString("acb_cc"),
                                bsParseDouble(currformatDouble(endbal)),
                                0
                            });
                               }
                            
                            
                                  
                 totendbal = totendbal + endbal;
                 totbegbal = totbegbal + begbal;
                 totactivity = totactivity + activity;
                            
                       }
                 
                       
                  }
                  
                  /* 
                   // calculate period(s) activity defined by date range 
                  // activity += OVData.getGLAcctBalSummCC(account.toString(), String.valueOf(fromdateyear), String.valueOf(p));
                       res = st.executeQuery("select acb_cc, sum(acb_amt) as sum from acb_mstr where acb_year = " +
                        "'" + String.valueOf(year) + "'" + 
                        " AND acb_per = " +
                        "'" + String.valueOf(period) + "'" +
                        " AND acb_acct = " +
                        "'" + acctid + "'" +
                        " AND acb_site = " + "'" + site + "'" +
                        " group by acb_cc ;");
                       while (res.next()) {
                         // activity += res.getDouble(("sum"));
                         // ccamts.add(res.getString("acb_cc") + "," + "activity" + "," + res.getString("sum"));
                       }
                 
                  */
                
                 } // Accts
                               
                   
                 // now sum for the total labels display
                 
                
                
                 } else {    // else if not CC included
                     
                  
                 ACCTS:    for (String account[] : accounts) {
                     
                     
                  
                  acctid = account[0];
                  acctcurr = account[1];
                  accttype = account[2];
                  acctdesc = account[3];
               
                  
                  begbal = 0;
                  activity = 0;
                  endbal = 0;
                  preact = 0;
                  postact = 0;
                  
                
                  
                  
                 // calculate all acb_mstr records for whole periods < fromdateperiod
                    // begbal += OVData.getGLAcctBalSummCC(account.toString(), String.valueOf(fromdateyear), String.valueOf(p));
                  if (accttype.equals("L") || accttype.equals("A")) {
                      //must be type balance sheet
                  res = st.executeQuery("select sum(acb_amt) as sum from acb_mstr where " +
                        " acb_acct = " + "'" + acctid + "'" + " AND " +
                        " acb_site = " + "'" + site + "'" + " AND " +
                        " (( acb_year = " + "'" + year + "'" + " AND acb_per < " + "'" + period + "'" + " ) OR " +
                        "  ( acb_year <= " + "'" + prioryear + "'" + " )) " +
                        ";");
                
                       while (res.next()) {
                          begbal += res.getDouble("sum");
                       }
                  } else {
                     // must be income statement
                      res = st.executeQuery("select sum(acb_amt) as sum from acb_mstr where " +
                        " acb_acct = " + "'" + acctid + "'" + " AND " +
                        " acb_site = " + "'" + site + "'" + " AND " +
                        " ( acb_year = " + "'" + year + "'" + " AND acb_per < " + "'" + period + "'" + ")" +
                        ";");
                
                       while (res.next()) {
                          begbal += res.getDouble("sum");
                       }
                  }
                  
                   
                   // calculate period(s) activity defined by date range 
                  // activity += OVData.getGLAcctBalSummCC(account.toString(), String.valueOf(fromdateyear), String.valueOf(p));
               
                  
                 
                       res = st.executeQuery("select sum(acb_amt) as sum from acb_mstr where acb_year = " +
                        "'" + String.valueOf(year) + "'" + 
                        " AND acb_per = " +
                        "'" + String.valueOf(period) + "'" +
                        " AND acb_acct = " +
                        "'" + acctid + "'" +
                        " AND acb_site = " + "'" + site + "'" +
                        ";");
               
                  
                
                       while (res.next()) {
                          activity += res.getDouble(("sum"));
                       }
                 
                       
                
                 
                               
                 endbal = begbal + activity;
                 
                 if (cbzero.isSelected() && begbal == 0 && endbal == 0 && activity == 0) {
                     continue ACCTS;
                 }
                 
                 // now sum for the total labels display
                 totendbal = totendbal + endbal;
                 totbegbal = totbegbal + begbal;
                 totactivity = totactivity + activity;
                 
               //  if (begbal == 0 && endbal == 0 && activity == 0)
               //      bsmf.MainFrame.show(account);
               
                 if (accttype.equals("A") || accttype.equals("E")) {  // Debits
                     totaldebits = totaldebits + endbal;
                     mymodel.addRow(new Object[]{BlueSeerUtils.clickbasket, acctid, accttype, acctcurr,
                                acctdesc,
                                site,
                                bsParseDouble(currformatDouble(endbal)),
                                0
                            });
                 } else {  // credits
                     totalcredits = totalcredits + (-1 * endbal);
                    mymodel.addRow(new Object[]{BlueSeerUtils.clickbasket, acctid, accttype, acctcurr,
                                acctdesc,
                                site,
                                0,
                                bsParseDouble(currformatDouble((-1 * endbal)))  // reverse sign of credit column for trial balance
                            }); 
                 }
                    
               
             
                   
                } // Accts   
                     
                     
                 } // else of cc is not included
                 
                 
                 
              
                lbldebits.setText(currformatDouble(totaldebits));
                lblcredits.setText(currformatDouble(totalcredits));
            } catch (SQLException s) {
                MainFrame.bslog(s);
                bsmf.MainFrame.show(getMessageTag(1016,Thread.currentThread().getStackTrace()[1].getMethodName()));
            } finally {
                if (res != null) {
                    res.close();
                }
                if (res2 != null) {
                    res2.close();
                }
                if (st != null) {
                    st.close();
                }
                if (st2 != null) {
                    st2.close();
                }
                con.close();
            }
        } catch (Exception e) {
            MainFrame.bslog(e);
        }
       
    }//GEN-LAST:event_btRunActionPerformed

    private void btdetailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btdetailActionPerformed
       detailpanel.setVisible(false);
       btdetail.setEnabled(false);
    }//GEN-LAST:event_btdetailActionPerformed

    private void tablereportMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tablereportMouseClicked
        
        int row = tablereport.rowAtPoint(evt.getPoint());
        int col = tablereport.columnAtPoint(evt.getPoint());
        if ( col == 0) {
               if (tablereport.getColumnCount() == 8) {
                getdetail(tablereport.getValueAt(row, 1).toString(), tablereport.getValueAt(row, 5).toString(), ddyear.getSelectedItem().toString(), ddperiod.getSelectedItem().toString());
                btdetail.setEnabled(true);
                detailpanel.setVisible(true);
               } else {
                 getdetailCC(tablereport.getValueAt(row, 1).toString(), tablereport.getValueAt(row, 6).toString(), tablereport.getValueAt(row, 5).toString(), ddyear.getSelectedItem().toString(), ddperiod.getSelectedItem().toString());
                btdetail.setEnabled(true);
                detailpanel.setVisible(true);  
               }
        }
    }//GEN-LAST:event_tablereportMouseClicked

    private void ddyearItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_ddyearItemStateChanged
       if (ddyear.getItemCount() > 0 && ddperiod.getItemCount() > 0) {
        ArrayList fromdatearray = fglData.getGLCalForPeriod(ddyear.getSelectedItem().toString(), ddperiod.getSelectedItem().toString());
        if (fromdatearray.size() == 2)
        datelabel.setText(fromdatearray.get(0).toString() + " To " + fromdatearray.get(1).toString());
       }
    }//GEN-LAST:event_ddyearItemStateChanged

    private void ddperiodItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_ddperiodItemStateChanged
        if (ddperiod.getItemCount() > 0 && ddyear.getItemCount() > 0) {
        ArrayList fromdatearray = fglData.getGLCalForPeriod(ddyear.getSelectedItem().toString(), ddperiod.getSelectedItem().toString());
        if (fromdatearray.size() == 2)
        datelabel.setText(fromdatearray.get(0).toString() + " To " + fromdatearray.get(1).toString());
        }
    }//GEN-LAST:event_ddperiodItemStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btRun;
    private javax.swing.JButton btdetail;
    private javax.swing.JCheckBox cbcc;
    private javax.swing.JCheckBox cbzero;
    private javax.swing.JLabel datelabel;
    private javax.swing.JComboBox ddperiod;
    private javax.swing.JComboBox ddsite;
    private javax.swing.JComboBox ddyear;
    private javax.swing.JPanel detailpanel;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblcredits;
    private javax.swing.JLabel lbldebits;
    private javax.swing.JPanel summarypanel;
    private javax.swing.JTable tabledetail;
    private javax.swing.JPanel tablepanel;
    private javax.swing.JTable tablereport;
    // End of variables declaration//GEN-END:variables
}

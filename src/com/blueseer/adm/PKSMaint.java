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

package com.blueseer.adm;

import bsmf.MainFrame;
import static bsmf.MainFrame.bslog;
import static bsmf.MainFrame.tags;
import static com.blueseer.adm.admData.addPksMstr;
import static com.blueseer.adm.admData.deletePksMstr;
import static com.blueseer.adm.admData.getPKSStoreFileName;
import static com.blueseer.adm.admData.getPKSStorePWD;
import static com.blueseer.adm.admData.getPksMstr;
import static com.blueseer.adm.admData.isValidPKSStore;
import com.blueseer.adm.admData.pks_mstr;
import static com.blueseer.adm.admData.updatePksMstr;
import com.blueseer.edi.apiUtils;
import static com.blueseer.edi.apiUtils.createKeyStore;
import static com.blueseer.edi.apiUtils.createKeyStoreWithNewKeyPair;
import static com.blueseer.edi.apiUtils.createNewKeyPair;
import static com.blueseer.edi.apiUtils.generateSSHCert;
import static com.blueseer.edi.apiUtils.getPublicKeyAsOPENSSH;
import static com.blueseer.edi.apiUtils.getPublicKeyAsPEM;
import com.blueseer.utl.BlueSeerUtils;
import static com.blueseer.utl.BlueSeerUtils.callDialog;
import com.blueseer.utl.BlueSeerUtils.dbaction;
import static com.blueseer.utl.BlueSeerUtils.getClassLabelTag;
import static com.blueseer.utl.BlueSeerUtils.getMessageTag;
import static com.blueseer.utl.BlueSeerUtils.isFile;
import static com.blueseer.utl.BlueSeerUtils.luModel;
import static com.blueseer.utl.BlueSeerUtils.luTable;
import static com.blueseer.utl.BlueSeerUtils.lual;
import static com.blueseer.utl.BlueSeerUtils.ludialog;
import static com.blueseer.utl.BlueSeerUtils.luinput;
import static com.blueseer.utl.BlueSeerUtils.luml;
import static com.blueseer.utl.BlueSeerUtils.lurb1;
import static com.blueseer.utl.BlueSeerUtils.lurb2;
import com.blueseer.utl.OVData;
import com.blueseer.utl.DTData;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import com.blueseer.utl.IBlueSeerT;
import static com.blueseer.utl.OVData.exportCertToFile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import org.apache.commons.io.Charsets;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.util.encoders.Base64;

/**
 *
 * @author vaughnte
 */
public class PKSMaint extends javax.swing.JPanel implements IBlueSeerT {

    // global variable declarations
        boolean isLoad = false;
        public static pks_mstr x = null;
    
    
                
   // global datatablemodel declarations    
                
                
    public PKSMaint() {
        initComponents();
        setLanguageTags(this);
    }

    
    
    // interface functions implemented
    public void executeTask(dbaction x, String[] y) { 
      
        class Task extends SwingWorker<String[], Void> {
       
          String type = "";
          String[] key = null;
          
          public Task(dbaction type, String[] key) { 
              this.type = type.name();
              this.key = key;
          } 
           
        @Override
        public String[] doInBackground() throws Exception {
            String[] message = new String[2];
            message[0] = "";
            message[1] = "";
            
            
             switch(this.type) {
                case "add":
                    message = addRecord(key);
                    break;
                case "update":
                    message = updateRecord(key);
                    break;
                case "delete":
                    message = deleteRecord(key);    
                    break;
                case "get":
                    message = getRecord(key);    
                    break;    
                default:
                    message = new String[]{"1", "unknown action"};
            }
            
            return message;
        }
 
        
       public void done() {
            try {
            String[] message = get();
           
            BlueSeerUtils.endTask(message);
           if (this.type.equals("delete")) {
             initvars(null);  
           } else if (this.type.equals("get")) {
             updateForm();
             tbkey.requestFocus();
           } else {
             initvars(null);  
           }
           
            
            } catch (Exception e) {
                MainFrame.bslog(e);
            } 
           
        }
    }  
      
       BlueSeerUtils.startTask(new String[]{"","Running..."});
       Task z = new Task(x, y); 
       z.execute(); 
       
    }
   
    public void setPanelComponentState(Object myobj, boolean b) {
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
        
        if (panel != null) {
        panel.setEnabled(b);
        Component[] components = panel.getComponents();
        
            for (Component component : components) {
                if (component instanceof JLabel || component instanceof JTable ) {
                    continue;
                }
                if (component instanceof JPanel) {
                    setPanelComponentState((JPanel) component, b);
                }
                if (component instanceof JTabbedPane) {
                    setPanelComponentState((JTabbedPane) component, b);
                }
                if (component instanceof JScrollPane) {
                    setPanelComponentState((JScrollPane) component, b);
                }
                
                component.setEnabled(b);
            }
        }
            if (tabpane != null) {
                tabpane.setEnabled(b);
                Component[] componentspane = tabpane.getComponents();
                for (Component component : componentspane) {
                    if (component instanceof JLabel || component instanceof JTable ) {
                        continue;
                    }
                    if (component instanceof JPanel) {
                        setPanelComponentState((JPanel) component, b);
                    }
                    
                    component.setEnabled(b);
                    
                }
            }
            if (scrollpane != null) {
                scrollpane.setEnabled(b);
                JViewport viewport = scrollpane.getViewport();
                Component[] componentspane = viewport.getComponents();
                for (Component component : componentspane) {
                    if (component instanceof JLabel || component instanceof JTable ) {
                        continue;
                    }
                    component.setEnabled(b);
                }
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
    
    public void setComponentDefaultValues() {
       isLoad = true;
       
        tbkey.setText("");
        tbdesc.setText("");
        tbuser.setText("");
        tbpass.setText("");
        tbfile.setText("");
        ddsigalgo.setSelectedIndex(2); // SHA-256
        tbstorepass.setText("");
        taoutput.setText("");
        ddtype.setSelectedIndex(0);
        
        ddparent.removeAllItems();
        admData.getPKSStores().stream().forEach((s) -> ddparent.addItem(s)); 
        ddparent.insertItemAt("", 0);
        ddparent.setSelectedIndex(0);
        
        ddstandard.removeAllItems();
        ddstandard.addItem("X.509");
        ddstandard.addItem("openPGP");
        ddstandard.insertItemAt("", 0);
        ddstandard.setSelectedIndex(0);
        
       isLoad = false;
    }
    
    public void newAction(String x) {
       setPanelComponentState(this, true);
        setComponentDefaultValues();
        BlueSeerUtils.message(new String[]{"0",BlueSeerUtils.addRecordInit});
        btupdate.setEnabled(false);
        btdelete.setEnabled(false);
        btnew.setEnabled(false);
        tbkey.setEditable(true);
        tbkey.setForeground(Color.blue);
        ddtype.setSelectedIndex(0); // custom override to trigger field enable logic
        if (! x.isEmpty()) {
          tbkey.setText(String.valueOf(OVData.getNextNbr(x)));  
          tbkey.setEditable(false);
        } 
        tbkey.requestFocus();
    }
    
    public void setAction(String[] x) {
        String[] m = new String[2];
        if (x[0].equals("0")) {
                   setPanelComponentState(this, true);
                   // override for unique use case of ddtype field inits
                   ddtype.setSelectedIndex(ddtype.getSelectedIndex());
                   btadd.setEnabled(false);
                   tbkey.setEditable(false);
                   tbkey.setForeground(Color.blue);
        } else {
                   tbkey.setForeground(Color.red); 
        }
    }
    
    public boolean validateInput(dbaction x) {
        boolean b = true;
                               
                              
                if (tbkey.getText().isEmpty()) {
                    b = false;
                    bsmf.MainFrame.show(getMessageTag(1024));
                    tbkey.requestFocus();
                    return b;
                }
                
                if (x.name().equals("add") && ddtype.getSelectedItem().toString().equals("keypair") && ddparent.getSelectedItem().toString().isEmpty()) {
                    b = false;
                    bsmf.MainFrame.show("Parent Store cannot be empty when generating keypair cert");
                    ddparent.requestFocus();
                    return b;
                }
                
                
                if (x.name().equals("add") && ddtype.getSelectedItem().toString().equals("keypair") && tbuser.getText().isBlank()) {
                    b = false;
                    bsmf.MainFrame.show("User (alias) cannot be empty when generating keypair cert");
                    tbuser.requestFocus();
                    return b;
                }
                
                if (x.name().equals("add") && ddtype.getSelectedItem().toString().equals("keypair") && String.valueOf(tbpass.getPassword()).isBlank()) {
                    b = false;
                    bsmf.MainFrame.show("User password cannot be empty when generating keypair cert");
                    tbpass.requestFocus();
                    return b;
                }
               
                if ( (x.name().equals("add") || x.name().equals("update")) && (ddtype.getSelectedItem().toString().equals("publickey") || ddtype.getSelectedItem().toString().equals("privatekey")) && ! isFile(tbfile.getText())) {
                    b = false;
                    bsmf.MainFrame.show("pem file does not exist");
                    tbfile.requestFocus();
                    return b;
                }
               
        return b;
    }
    
    public void initvars(String[] arg) {
       
       setPanelComponentState(this, false); 
       setComponentDefaultValues();
        btnew.setEnabled(true);
        btlookup.setEnabled(true);
        
        if (arg != null && arg.length > 0) {
            executeTask(dbaction.get,arg);
        } else {
            tbkey.setEnabled(true);
            tbkey.setEditable(true);
            tbkey.requestFocus();
        }
    }
    
    public String[] addRecord(String[] x) {
     if (ddtype.getSelectedItem().toString().equals("store")) {
         if (! createKeyStore(String.valueOf(tbstorepass.getPassword()), 
                 tbfile.getText())) {
             return new String[]{BlueSeerUtils.ErrorBit, "Unable to generate new Store"};
         }
     }
     if (ddtype.getSelectedItem().toString().equals("keypair")) {
         if (! createNewKeyPair(tbuser.getText(), 
                 String.valueOf(tbpass.getPassword()), 
                 getPKSStorePWD(ddparent.getSelectedItem().toString()), 
                 getPKSStoreFileName(ddparent.getSelectedItem().toString()),
                 ddencalgo.getSelectedItem().toString(),
                 ddsigalgo.getSelectedItem().toString(),
                 Integer.valueOf(ddstrength.getSelectedItem().toString()),
                 Integer.valueOf(ddyears.getSelectedItem().toString())
                 )) {
             return new String[]{BlueSeerUtils.ErrorBit, "Unable to generate new User / keypair"};
         }
     }
     String[] m = addPksMstr(createRecord()); 
         return m;
     }
     
    public String[] updateRecord(String[] x) {
     String[] m = updatePksMstr(createRecord());
         return m;
     }
     
    public String[] deleteRecord(String[] x) {
     String[] m = new String[2];
        boolean proceed = bsmf.MainFrame.warn(getMessageTag(1004));
        if (proceed) {
         m = deletePksMstr(createRecord()); 
         initvars(null);
        } else {
           m = new String[] {BlueSeerUtils.ErrorBit, BlueSeerUtils.deleteRecordCanceled}; 
        }
         return m;
     }
      
    public String[] getRecord(String[] key) {
       x = getPksMstr(key);
        return x.m();
    }
    
    public pks_mstr createRecord() { 
        String userpass = bsmf.MainFrame.PassWord("0", tbpass.getPassword());
        String storepass = bsmf.MainFrame.PassWord("0", tbstorepass.getPassword());
        pks_mstr x = new pks_mstr(null, 
                tbkey.getText(),
                tbdesc.getText(),
                ddtype.getSelectedItem().toString(),
                tbuser.getText(),
                userpass,
                tbfile.getText(),
                "",  // old store user (n/a/)...available for other field
                storepass,
                "", // expire
                "", //create
                ddparent.getSelectedItem().toString(),
                ddstandard.getSelectedItem().toString(),
                String.valueOf(BlueSeerUtils.boolToInt(cbexternal.isSelected()))
                );
        return x;
    }
    
    public void lookUpFrame() {
        
       
        luinput.removeActionListener(lual);
        lual = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
        if (lurb1.isSelected()) {  
         luModel = DTData.getPksBrowseUtil(luinput.getText(),0, "pks_id");
        } else if (lurb2.isSelected()) {
         luModel = DTData.getPksBrowseUtil(luinput.getText(),0, "pks_type");   
        } else {
         luModel = DTData.getPksBrowseUtil(luinput.getText(),0, "pks_user");   
        }
        luTable.setModel(luModel);
        luTable.getColumnModel().getColumn(0).setMaxWidth(50);
        if (luModel.getRowCount() < 1) {
            ludialog.setTitle(getMessageTag(1001));
        } else {
            ludialog.setTitle(getMessageTag(1002, String.valueOf(luModel.getRowCount())));
        }
        }
        };
        luinput.addActionListener(lual);
        
        luTable.removeMouseListener(luml);
        luml = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JTable target = (JTable)e.getSource();
                int row = target.getSelectedRow();
                int column = target.getSelectedColumn();
                if ( column == 0) {
                ludialog.dispose();
                initvars(new String[]{target.getValueAt(row,1).toString()});
                }
            }
        };
        luTable.addMouseListener(luml);
      
        callDialog(getClassLabelTag("lblcode", this.getClass().getSimpleName()), 
                getClassLabelTag("lbltype", this.getClass().getSimpleName()),
                getClassLabelTag("lbluser", this.getClass().getSimpleName()));
        
        
    }

    public void updateForm() {
        tbkey.setText(x.pks_id());
        tbdesc.setText(x.pks_desc());
        ddtype.setSelectedItem(x.pks_type());
        tbfile.setText(x.pks_file());
        tbuser.setText(x.pks_user());
        ddparent.setSelectedItem(x.pks_parent());
        tbpass.setText(bsmf.MainFrame.PassWord("1", x.pks_pass().toCharArray()));
        tbstorepass.setText(bsmf.MainFrame.PassWord("1", x.pks_storepass().toCharArray()));
        ddstandard.setSelectedItem(x.pks_standard());
        cbexternal.setSelected(BlueSeerUtils.ConvertStringToBool(x.pks_external()));
        setAction(x.m());
    }
    
    // custom functions
    
    public File decryptFile() {
        taoutput.removeAll();
        File file = null;
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = fc.showOpenDialog(this);
        java.util.Date now = new java.util.Date();
        DateFormat dfdate = new SimpleDateFormat("yyyyMMddHHmmss");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
            file = fc.getSelectedFile();
            
            Path inpath = FileSystems.getDefault().getPath(file.getAbsolutePath());
            
            String myfile = file.getName() + "." + dfdate.format(now) + ".dec";
            Path outpath = FileSystems.getDefault().getPath(bsmf.MainFrame.temp + "/" + myfile);
            
            byte[] indata = Files.readAllBytes(inpath);
            
            byte[] outdata = apiUtils.decryptData(indata, apiUtils.getPrivateKey(tbkey.getText()) );
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outpath.toFile()));
            bos.write(outdata);
            bos.flush();
            bos.close();   
           
            taoutput.append(new String(outdata));
            }
            catch (Exception ex) {
            ex.printStackTrace();
            }
           
        } else {
           System.out.println("cancelled");
        }
        return file;
    }
    
    public File encryptFile() {
        taoutput.removeAll();
        File file = null;
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = fc.showOpenDialog(this);
        java.util.Date now = new java.util.Date();
        DateFormat dfdate = new SimpleDateFormat("yyyyMMddHHmmss");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
            file = fc.getSelectedFile();
            Path inpath = FileSystems.getDefault().getPath(file.getAbsolutePath());
            
            String myfile = file.getName() + "." + dfdate.format(now) + ".enc";
            Path outpath = FileSystems.getDefault().getPath(bsmf.MainFrame.temp + "/" + myfile);
            
            byte[] indata = Files.readAllBytes(inpath);
            byte[] outdata = apiUtils.encryptData(indata, apiUtils.getPublicKeyAsCert(tbkey.getText()), "" );
           
         //   Path path = FileSystems.getDefault().getPath("temp" + "/" + "beforefile");
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outpath.toFile()));
            bos.write(outdata);
            bos.flush();
            bos.close();
         
            taoutput.append(new String(outdata));
            taoutput.append("\n");
           
            }
            catch (Exception ex) {
            ex.printStackTrace();
            }
           
        } else {
           System.out.println("cancelled");
        }
        return file;
    }
    
    public void test() throws CertificateException, NoSuchProviderException, CertificateEncodingException, CMSException, IOException {
        taoutput.removeAll();
        Path path = FileSystems.getDefault().getPath(bsmf.MainFrame.temp + "/" + "beforefile");
        byte[] data = Files.readAllBytes(path);
          
            /*
            byte[] data = APIMaint.encryptData(jb.toString().getBytes(Charsets.UTF_8), apiUtils.getCert("terrycer.cer") );
            String datastring = new String(Base64.encode(data));
            taoutput.append("--now encrypted" + "\n");
            taoutput.append(datastring);
            taoutput.append("\n");
            taoutput.append("--now decrypted" + "\n");
            */
          //  byte[] data2 = APIMaint.decryptData(java.util.Base64.getDecoder().decode(datastring), apiUtils.getPrivateKey("terry") );
           //   byte[] data2 = APIMaint.decryptData(jb.toString().getBytes(Charsets.UTF_8), apiUtils.getPrivateKey("terry") );
            byte[] data2 = apiUtils.decryptData(data, apiUtils.getPrivateKey("terry") );
           String datastring2 = new String(data2);
            taoutput.append(datastring2);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTextField1 = new javax.swing.JTextField();
        fc = new javax.swing.JFileChooser();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        tbdesc = new javax.swing.JTextField();
        btdelete = new javax.swing.JButton();
        btadd = new javax.swing.JButton();
        btupdate = new javax.swing.JButton();
        tbkey = new javax.swing.JTextField();
        tbuser = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        btnew = new javax.swing.JButton();
        btclear = new javax.swing.JButton();
        btlookup = new javax.swing.JButton();
        tbfile = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        ddtype = new javax.swing.JComboBox<>();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        tbstorepass = new javax.swing.JPasswordField();
        tbpass = new javax.swing.JPasswordField();
        jScrollPane1 = new javax.swing.JScrollPane();
        taoutput = new javax.swing.JTextArea();
        jLabel9 = new javax.swing.JLabel();
        btpublickey = new javax.swing.JButton();
        ddyears = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        ddstrength = new javax.swing.JComboBox<>();
        jLabel11 = new javax.swing.JLabel();
        ddsigalgo = new javax.swing.JComboBox<>();
        ddparent = new javax.swing.JComboBox<>();
        btexport = new javax.swing.JButton();
        ddencalgo = new javax.swing.JComboBox<>();
        jLabel12 = new javax.swing.JLabel();
        ddformat = new javax.swing.JComboBox<>();
        cbexternal = new javax.swing.JCheckBox();
        ddstandard = new javax.swing.JComboBox<>();
        jLabel13 = new javax.swing.JLabel();

        jTextField1.setText("jTextField1");

        setBackground(new java.awt.Color(0, 102, 204));

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("PKS Maintenance"));
        jPanel1.setName("panelmain"); // NOI18N

        jLabel1.setText("Key Code:");
        jLabel1.setName("lblcode"); // NOI18N

        jLabel2.setText("Description:");
        jLabel2.setName("lbldesc"); // NOI18N

        btdelete.setText("delete");
        btdelete.setName("btdelete"); // NOI18N
        btdelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btdeleteActionPerformed(evt);
            }
        });

        btadd.setText("add");
        btadd.setName("btadd"); // NOI18N
        btadd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btaddActionPerformed(evt);
            }
        });

        btupdate.setText("update");
        btupdate.setName("btupdate"); // NOI18N
        btupdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btupdateActionPerformed(evt);
            }
        });

        tbkey.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbkeyActionPerformed(evt);
            }
        });

        jLabel3.setText("User:");
        jLabel3.setName("lbluser"); // NOI18N

        jLabel5.setText("Pass:");
        jLabel5.setName("lblpass"); // NOI18N

        jLabel6.setText("StorePass:");
        jLabel6.setName("lblstorepass"); // NOI18N

        btnew.setText("New");
        btnew.setName("btnew"); // NOI18N
        btnew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnewActionPerformed(evt);
            }
        });

        btclear.setText("Clear");
        btclear.setName("btclear"); // NOI18N
        btclear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btclearActionPerformed(evt);
            }
        });

        btlookup.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/lookup.png"))); // NOI18N
        btlookup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btlookupActionPerformed(evt);
            }
        });

        jLabel4.setText("File:");
        jLabel4.setName("lblfile"); // NOI18N

        ddtype.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "keypair", "store", "publickey", "privatekey" }));
        ddtype.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ddtypeActionPerformed(evt);
            }
        });

        jLabel7.setText("Type:");
        jLabel7.setName("lbltype"); // NOI18N

        jLabel8.setText("Sign Algorithm:");
        jLabel8.setName("lblsigalgo"); // NOI18N

        taoutput.setColumns(20);
        taoutput.setRows(5);
        jScrollPane1.setViewportView(taoutput);

        jLabel9.setText("ParentStore:");

        btpublickey.setText("View Public Key");
        btpublickey.setName("btpublickey"); // NOI18N
        btpublickey.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btpublickeyActionPerformed(evt);
            }
        });

        ddyears.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5" }));

        jLabel10.setText("Years Valid:");
        jLabel10.setName("lblyears"); // NOI18N

        ddstrength.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1024", "2048" }));

        jLabel11.setText("Key Size:");
        jLabel11.setName("lblstrength"); // NOI18N

        ddsigalgo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "MD-5", "SHA-1", "SHA-256", "SHA-512" }));

        btexport.setText("Export");
        btexport.setName("btexport"); // NOI18N
        btexport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btexportActionPerformed(evt);
            }
        });

        ddencalgo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "RSA", "DSA", "ED25519" }));

        jLabel12.setText("Encrypt Algorithm");

        ddformat.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { ".cer", "ssh2" }));

        cbexternal.setText("External");

        jLabel13.setText("Standard:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(60, 60, 60)
                        .addComponent(btadd)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btdelete)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btupdate)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel5)
                            .addComponent(jLabel6)
                            .addComponent(jLabel4)
                            .addComponent(jLabel7)
                            .addComponent(jLabel8)
                            .addComponent(jLabel9)
                            .addComponent(jLabel10)
                            .addComponent(jLabel11)
                            .addComponent(jLabel12)
                            .addComponent(jLabel13))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(tbkey, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btlookup, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(13, 13, 13)
                                .addComponent(btnew)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btclear)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(tbstorepass, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(tbpass, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(tbfile)
                                        .addComponent(tbdesc, javax.swing.GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE)
                                        .addComponent(tbuser)
                                        .addComponent(ddtype, 0, 233, Short.MAX_VALUE))
                                    .addComponent(ddyears, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(ddstrength, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(ddencalgo, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(ddsigalgo, javax.swing.GroupLayout.Alignment.LEADING, 0, 111, Short.MAX_VALUE))
                                    .addComponent(cbexternal)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(ddstandard, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(ddparent, javax.swing.GroupLayout.Alignment.LEADING, 0, 111, Short.MAX_VALUE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 32, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(btpublickey)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btexport))
                                    .addComponent(ddformat, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 291, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(27, 27, 27))))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(tbkey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel1))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnew)
                        .addComponent(btclear))
                    .addComponent(btlookup))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(tbdesc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(ddtype, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(ddparent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(ddstandard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel13))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbexternal)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(tbfile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4))
                        .addGap(10, 10, 10)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(tbuser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(tbpass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(tbstorepass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(ddencalgo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel12))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(ddsigalgo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ddyears, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(ddstrength, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel11)))
                    .addComponent(jScrollPane1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btadd)
                    .addComponent(btdelete)
                    .addComponent(btupdate)
                    .addComponent(ddformat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btexport)
                    .addComponent(btpublickey))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        add(jPanel1);
    }// </editor-fold>//GEN-END:initComponents

    private void btaddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btaddActionPerformed
       if (! validateInput(dbaction.add)) {
           return;
       }
        setPanelComponentState(this, false);
        executeTask(dbaction.add, new String[]{tbkey.getText()});
    }//GEN-LAST:event_btaddActionPerformed

    private void btupdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btupdateActionPerformed
       if (! validateInput(dbaction.update)) {
           return;
       }
        setPanelComponentState(this, false);
        executeTask(dbaction.update, new String[]{tbkey.getText()});
    }//GEN-LAST:event_btupdateActionPerformed

    private void btdeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btdeleteActionPerformed
       if (! validateInput(dbaction.delete)) {
           return;
       }
        setPanelComponentState(this, false);
        executeTask(dbaction.delete, new String[]{tbkey.getText()});   
     
    }//GEN-LAST:event_btdeleteActionPerformed

    private void btnewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnewActionPerformed
      newAction("");
    }//GEN-LAST:event_btnewActionPerformed

    private void tbkeyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbkeyActionPerformed
       executeTask(dbaction.get, new String[]{tbkey.getText()});
    }//GEN-LAST:event_tbkeyActionPerformed

    private void btclearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btclearActionPerformed
        BlueSeerUtils.messagereset();
        initvars(null);
    }//GEN-LAST:event_btclearActionPerformed

    private void btlookupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btlookupActionPerformed
        lookUpFrame();
    }//GEN-LAST:event_btlookupActionPerformed

    private void btpublickeyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btpublickeyActionPerformed
            taoutput.setText("");
            if (ddformat.getSelectedItem().toString().equals(".cer")) {
                /*
                taoutput.append("-----BEGIN CERTIFICATE-----\n");
                taoutput.append(new String(Base64.encode(apiUtils.getPublicKeyAsCert(tbkey.getText()).getEncoded())).replaceAll("(.{64})", "$1\n"));
                taoutput.append("\n-----END CERTIFICATE-----\n");
                */
                taoutput.append(getPublicKeyAsPEM(tbkey.getText()));
            }
            if (ddformat.getSelectedItem().toString().equals("ssh2")) {
                taoutput.append(getPublicKeyAsOPENSSH(tbkey.getText()));
            }
    }//GEN-LAST:event_btpublickeyActionPerformed

    private void ddtypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ddtypeActionPerformed
      if (! isLoad) {
        switch (ddtype.getSelectedItem().toString()) {
            case "keypair" :
                ddyears.setEnabled(true);
                ddstrength.setEnabled(true);
                ddsigalgo.setEnabled(true);
                tbuser.setEnabled(true);
                tbpass.setEnabled(true);
                tbstorepass.setEnabled(false);
                tbfile.setEnabled(false);
                ddparent.setEnabled(true);
                btexport.setEnabled(true);
                btpublickey.setEnabled(true);
            break;
            
            case "privatekey" :
                ddyears.setEnabled(true);
                ddstrength.setEnabled(true);
                ddsigalgo.setEnabled(true);
                tbuser.setEnabled(true);
                tbpass.setEnabled(true);
                tbstorepass.setEnabled(false);
                tbfile.setEnabled(true);
                ddparent.setEnabled(true);
                btexport.setEnabled(true);
                btpublickey.setEnabled(true);
            break;
            
            case "store" :
                ddyears.setEnabled(false);
                ddstrength.setEnabled(false);
                ddsigalgo.setEnabled(false);
                tbuser.setEnabled(false);
                tbpass.setEnabled(false);
                tbstorepass.setEnabled(true);
                tbfile.setEnabled(true);
                ddparent.setEnabled(false);
                btexport.setEnabled(false);
                btpublickey.setEnabled(false);
            break; 
            
            case "publickey" :
                ddyears.setEnabled(false);
                ddstrength.setEnabled(false);
                ddsigalgo.setEnabled(false);
                tbuser.setEnabled(false);
                tbpass.setEnabled(false);
                tbstorepass.setEnabled(false);
                tbfile.setEnabled(true);
                ddparent.setEnabled(false);
                btexport.setEnabled(false);
                btpublickey.setEnabled(false);
            break; 
            
            default:
        } 
      } 
    }//GEN-LAST:event_ddtypeActionPerformed

    private void btexportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btexportActionPerformed
        StringBuilder s = new StringBuilder();
        try {
         s.append("-----BEGIN CERTIFICATE-----\n");
         s.append(new String(Base64.encode(apiUtils.getPublicKeyAsCert(tbkey.getText()).getEncoded())).replaceAll("(.{64})", "$1\n"));
         s.append("\n-----END CERTIFICATE-----\n");
        exportCertToFile(s.toString(), tbkey.getText());
        } catch (CertificateEncodingException ex) {
                bsmf.MainFrame.show("cannot export PEM formatted public key");
        }
    }//GEN-LAST:event_btexportActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btadd;
    private javax.swing.JButton btclear;
    private javax.swing.JButton btdelete;
    private javax.swing.JButton btexport;
    private javax.swing.JButton btlookup;
    private javax.swing.JButton btnew;
    private javax.swing.JButton btpublickey;
    private javax.swing.JButton btupdate;
    private javax.swing.JCheckBox cbexternal;
    private javax.swing.JComboBox<String> ddencalgo;
    private javax.swing.JComboBox<String> ddformat;
    private javax.swing.JComboBox<String> ddparent;
    private javax.swing.JComboBox<String> ddsigalgo;
    private javax.swing.JComboBox<String> ddstandard;
    private javax.swing.JComboBox<String> ddstrength;
    private javax.swing.JComboBox<String> ddtype;
    private javax.swing.JComboBox<String> ddyears;
    private javax.swing.JFileChooser fc;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextArea taoutput;
    private javax.swing.JTextField tbdesc;
    private javax.swing.JTextField tbfile;
    private javax.swing.JTextField tbkey;
    private javax.swing.JPasswordField tbpass;
    private javax.swing.JPasswordField tbstorepass;
    private javax.swing.JTextField tbuser;
    // End of variables declaration//GEN-END:variables
}

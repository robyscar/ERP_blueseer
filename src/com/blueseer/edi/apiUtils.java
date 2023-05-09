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

import static bsmf.MainFrame.bslog;
import com.blueseer.adm.admData;
import com.blueseer.adm.admData.pks_mstr;
import static com.blueseer.edi.APIMaint.apidm;
import static com.blueseer.edi.AS2Maint.certs;
import com.blueseer.edi.ediData.api_det;
import com.blueseer.edi.ediData.api_mstr;
import static com.blueseer.edi.ediData.getKeyStoreByUser;
import static com.blueseer.edi.ediData.getKeyStorePass;
import static com.blueseer.edi.ediData.getKeyUserPass;
import static com.blueseer.utl.BlueSeerUtils.cleanDirString;
import com.blueseer.utl.EDData;
import static com.blueseer.utl.EDData.updateAS2LogMDNFile;
import static com.blueseer.utl.EDData.writeAS2Log;
import static com.blueseer.utl.EDData.writeAS2LogDetail;
import com.blueseer.utl.OVData;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.activation.DataHandler;
import javax.crypto.BadPaddingException;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.KeyTransRecipientInformation;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 *
 * @author terryva
 */
public class apiUtils {
    
    public static String[] runAPICall(api_mstr api, api_det apid, Path destinationpath, Path sourcepath) {
        String[] r = new String[]{"0",""};
       
        int k = 0;
        String method = "";
        String verb = "";
        String value = "";
        String urlstring = "";
        String port = "";
        HttpURLConnection conn = null;
        
        if (api.m()[0].equals("1") || api.api_id().isBlank()) {
           r[0] = "1";
           r[1] = "unknown api_mstr id";
           return r;
        }
        
        if (apid.m()[0].equals("1") || apid.apid_id().isBlank()) {
           r[0] = "1";
           r[1] = "unknown api_det id or api_det method";
           return r;
        }
        
        if (api.api_url().isBlank() || api.api_protocol().isBlank()) {
           r[0] = "1";
           r[1] = "api url and/or protocol is blank";
           return r;
        }
        
        
            try {
                
                
                if (! apid.apid_value().isBlank()) {
                    ArrayList<String[]> list = apidm.get(apid.apid_method());
                if (list != null) {
                    value = "?";
                    for (String[] s : list) {
                        value = value + s[0] + "=" + s[1] + "&";
                    }
                    if (value.endsWith("&")) {
                        value = value.substring(0, value.length() - 1);
                    }
                }
                }
                if (api.api_port().isBlank()) {  
                   port = ""; 
                } else {
                   port = ":" + api.api_port();
                }
                
                if (apid.apid_verb().equals("NONE")) {
                    urlstring = api.api_protocol() + "://" + api.api_url() + port + api.api_path() + value;
                } else {
                    urlstring = api.api_protocol() + "://" + api.api_url() + port + api.api_path() + apid.apid_verb().toLowerCase() ;
                }
                
                URL url = new URL(urlstring);
                
                if (destinationpath == null) {
                    destinationpath = FileSystems.getDefault().getPath(apid.apid_destination());
                }
                if (sourcepath == null) {
                    sourcepath = FileSystems.getDefault().getPath(apid.apid_source());
                }
             
                // sourcepath api 'push' unfinished
                
		conn = (HttpURLConnection) url.openConnection();
		if (! apid.apid_verb().equals("NONE")) {
                conn.setRequestMethod(verb);
                }
		conn.setRequestProperty("Accept", "application/json");

                BufferedReader br = null;
		if (conn.getResponseCode() != 200) {
                        r[0] = "1";
                        r[1] = conn.getResponseCode() + ": " + conn.getResponseMessage();
		} else {
                    br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
                }

                BufferedWriter outputfile = null;
                outputfile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destinationpath.toFile())));
		String output;
                if (br != null) {
                    while ((output = br.readLine()) != null) {
                             outputfile.write(output);
                    }
                    br.close();
                }
                outputfile.close(); 
                if (r[0].equals("0")) {
                r[1] = "api call to id: " + api.api_id() + " with urlstring: " + urlstring;
                }
                
                } catch (MalformedURLException e) {
                r[0] = "1";    
                r[1] = ("MalformedURLException: " + urlstring + "\n" + e + "\n");
                } catch (UnknownHostException ex) {
                    r[0] = "1";   
                    r[1] = ("UnknownHostException: " + urlstring + "\n" + ex + "\n");
                } catch (IOException ex) {
                    r[0] = "1";   
                    r[1] = ("IOException: " + urlstring + "\n" + ex + "\n");
                } catch (Exception ex) {
                    r[0] = "1";   
                    r[1] = ("Exception: " + urlstring + "\n" + ex + "\n");
                } finally {
                   if (conn != null) {
                    conn.disconnect();
                   }
                }
        
        return r;
    }
    
    
    public static PrivateKey getPrivateKey(String user)  {
        PrivateKey key = null;
        FileInputStream fis = null;
        try {
            String[] k = getKeyStoreByUser(user); // store, storeuser, storepass, user, pass
            k[2] = bsmf.MainFrame.PassWord("1", k[2].toCharArray());
            k[4] = bsmf.MainFrame.PassWord("1", k[4].toCharArray());
            char[] keyPassword = k[4].toCharArray();
            KeyStore keystore = KeyStore.getInstance("PKCS12");
             fis = new FileInputStream(FileSystems.getDefault().getPath(k[0]).toString());
            keystore.load(fis, k[2].toCharArray());
            
            key = (PrivateKey) keystore.getKey(k[3], keyPassword);
            
            //System.out.println("key-->");
            //System.out.println(key);
        } catch (KeyStoreException ex) {
            bslog(ex);
        } catch (FileNotFoundException ex) {
            bslog(ex);
        } catch (IOException ex) {
            bslog(ex);
        } catch (NoSuchAlgorithmException ex) {
            bslog(ex);
        } catch (CertificateException ex) {
            bslog(ex);
        } catch (UnrecoverableKeyException ex) {
            bslog(ex);
        } finally {
          if (fis != null ) {
              try {
                  fis.close();
              } catch (IOException ex) {
                  bslog(ex);
              }
          }
          
        }
        return key;
    }
    
    public static PublicKey getPublicKey(String user)  {
        X509Certificate cert = null;
        FileInputStream fis = null;
        pks_mstr pks = admData.getPksMstr(new String[]{user});
        try {
        String[] k = getKeyStoreByUser(user); // store, storeuser, storepass, user, pass
        k[2] = bsmf.MainFrame.PassWord("1", k[2].toCharArray());
        k[4] = bsmf.MainFrame.PassWord("1", k[4].toCharArray());
        KeyStore keystore = KeyStore.getInstance("PKCS12");
         fis = new FileInputStream(FileSystems.getDefault().getPath(k[0]).toString());
        keystore.load(fis, k[2].toCharArray());
        cert = (X509Certificate) keystore.getCertificate(pks.pks_user());
        fis.close();
        } catch (KeyStoreException ex) {
            bslog(ex);
        } catch (FileNotFoundException ex) {
            bslog(ex);
        } catch (IOException ex) {
            bslog(ex);
        } catch (NoSuchAlgorithmException ex) {
            bslog(ex);
        } catch (CertificateException ex) {
            bslog(ex);
        } 
        
        return cert.getPublicKey();
    }
    
    public static String getPublicKeyAsOPENSSH(String user)  {
        String s = "";
        X509Certificate cert = null;
        FileInputStream fis = null;
        pks_mstr pks = admData.getPksMstr(new String[]{user});
        try {
        String[] k = getKeyStoreByUser(user); // store, storeuser, storepass, user, pass
        k[2] = bsmf.MainFrame.PassWord("1", k[2].toCharArray());
        k[4] = bsmf.MainFrame.PassWord("1", k[4].toCharArray());
        KeyStore keystore = KeyStore.getInstance("PKCS12");
         fis = new FileInputStream(FileSystems.getDefault().getPath(k[0]).toString());
        keystore.load(fis, k[2].toCharArray());
        cert = (X509Certificate) keystore.getCertificate(pks.pks_user());
        fis.close();
        
        PublicKey pubkey = cert.getPublicKey();
        AsymmetricKeyParameter bpuv = PublicKeyFactory.createKey(pubkey.getEncoded());
        byte[] opuv = OpenSSHPublicKeyUtil.encodePublicKey(bpuv);
        
        // s = new String(Base64.encode(opuv));
        
        StringWriter writer = new StringWriter();
        PemWriter pemWriter = new PemWriter(writer);
        pemWriter.writeObject(new PemObject("SSH2 PUBLIC KEY", opuv));
        pemWriter.flush();
        pemWriter.close();
        s = writer.toString();
        writer.close();
        
        
        } catch (KeyStoreException ex) {
            bslog(ex);
        } catch (FileNotFoundException ex) {
            bslog(ex);
        } catch (IOException ex) {
            bslog(ex);
        } catch (NoSuchAlgorithmException ex) {
            bslog(ex);
        } catch (CertificateException ex) {
            bslog(ex);
        } 
        
        return s;  
    }
    
    
    public static String generateSSHCert(String certype) throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
        Security.addProvider(new BouncyCastleProvider());
        String newstring = "";
        KeyPairGenerator generator;
         generator = KeyPairGenerator.getInstance("ED25519","BC");
       // generator = KeyPairGenerator.getInstance("RSA","BC");
       // generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        AsymmetricKeyParameter bprv = PrivateKeyFactory.createKey(pair.getPrivate().getEncoded());
        AsymmetricKeyParameter bpuv = PublicKeyFactory.createKey(pair.getPublic().getEncoded());
        
        
        byte[] oprv = OpenSSHPrivateKeyUtil.encodePrivateKey(bprv);
        byte[] opuv = OpenSSHPublicKeyUtil.encodePublicKey(bpuv);
        /*
        PemWriter w = new PemWriter(new OutputStreamWriter(System.out));
        w.writeObject(new PemObject("OPENSSH PRIVATE KEY", oprv)); 
        w.close();
        */
        
        
        if (certype.equals("public")) {
        newstring = new String(Base64.encode(opuv));
        } else {
        newstring = new String(Base64.encode(oprv));    
        }
        
        StringWriter writer = new StringWriter();
        PemWriter pemWriter = new PemWriter(writer);
        pemWriter.writeObject(new PemObject("OPENSSH PUBLIC KEY", opuv));
        pemWriter.flush();
        pemWriter.close();
        newstring = writer.toString();
        writer.close();
        
        
        return newstring;
    }
    
    public static X509Certificate getPublicKeyAsCert(String user)  {
        X509Certificate cert = null;
        FileInputStream fis = null;
        pks_mstr pks = admData.getPksMstr(new String[]{user});
        try {
            // File type
            if (pks.pks_type().equals("external pem") ) {
                Path certfilepath = FileSystems.getDefault().getPath(pks.pks_file());
                if (! Files.exists(certfilepath)) {
                     // throw new RuntimeException("bad path to cert file: " + certfile);
                     return cert; // return null
                }
               // System.out.println("here->" + certfilepath.toString());
                Security.addProvider(new BouncyCastleProvider());
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509", "BC");
                try (FileInputStream fiscert = new FileInputStream(certfilepath.toFile())) {
                    cert = (X509Certificate) certFactory.generateCertificate(fiscert);
                    return cert;
                } catch (IOException ex) {
                    bslog(ex);
                }
            }
            
            if (pks.pks_type().equals("keypair") ) {
            String[] k = getKeyStoreByUser(user); // store, storeuser, storepass, user, pass
            k[2] = bsmf.MainFrame.PassWord("1", k[2].toCharArray());
            k[4] = bsmf.MainFrame.PassWord("1", k[4].toCharArray());
            KeyStore keystore = KeyStore.getInstance("PKCS12");
             fis = new FileInputStream(FileSystems.getDefault().getPath(k[0]).toString());
            keystore.load(fis, k[2].toCharArray());
            cert = (X509Certificate) keystore.getCertificate(pks.pks_user());
            fis.close();
            return cert;
            }
            
            //System.out.println("here-->" + cert.getSerialNumber());
        } catch (KeyStoreException ex) {
            bslog(ex);
        } catch (FileNotFoundException ex) {
            bslog(ex);
        } catch (IOException ex) {
            bslog(ex);
        } catch (NoSuchAlgorithmException ex) {
            bslog(ex);
        } catch (CertificateException ex) {
            bslog(ex);
        } catch (NoSuchProviderException ex) {
            bslog(ex);
        } 
        
        return cert;
    }
    
    public static String getPublicKeyAsPEM(String user)  {
        X509Certificate cert = null;
        String s = "";
        FileInputStream fis = null;
        pks_mstr pks = admData.getPksMstr(new String[]{user});
        try {
            // File type
            if (pks.pks_type().equals("external pem") ) {
                Path certfilepath = FileSystems.getDefault().getPath(pks.pks_file());
                if (! Files.exists(certfilepath)) {
                     // throw new RuntimeException("bad path to cert file: " + certfile);
                     return ""; // return null
                }
               // System.out.println("here->" + certfilepath.toString());
                Security.addProvider(new BouncyCastleProvider());
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509", "BC");
                try (FileInputStream fiscert = new FileInputStream(certfilepath.toFile())) {
                    cert = (X509Certificate) certFactory.generateCertificate(fiscert);
                    PublicKey pubkey = cert.getPublicKey();
                    AsymmetricKeyParameter bpuv = PublicKeyFactory.createKey(pubkey.getEncoded());
                    byte[] opuv = OpenSSHPublicKeyUtil.encodePublicKey(bpuv);
                    StringWriter writer = new StringWriter();
                    PemWriter pemWriter = new PemWriter(writer);
                    pemWriter.writeObject(new PemObject("CERTIFICATE", cert.getEncoded()));
                    pemWriter.flush();
                    pemWriter.close();
                    s = writer.toString();
                    writer.close();
                    return s;
                } catch (IOException ex) {
                    bslog(ex);
                }
            }
            
            if (pks.pks_type().equals("keypair") ) {
            String[] k = getKeyStoreByUser(user); // store, storeuser, storepass, user, pass
            k[2] = bsmf.MainFrame.PassWord("1", k[2].toCharArray());
            k[4] = bsmf.MainFrame.PassWord("1", k[4].toCharArray());
            KeyStore keystore = KeyStore.getInstance("PKCS12");
             fis = new FileInputStream(FileSystems.getDefault().getPath(k[0]).toString());
            keystore.load(fis, k[2].toCharArray());
            cert = (X509Certificate) keystore.getCertificate(pks.pks_user());
            fis.close();
            PublicKey pubkey = cert.getPublicKey();
                    AsymmetricKeyParameter bpuv = PublicKeyFactory.createKey(pubkey.getEncoded());
                    byte[] opuv = OpenSSHPublicKeyUtil.encodePublicKey(bpuv);
                    StringWriter writer = new StringWriter();
                    PemWriter pemWriter = new PemWriter(writer);
                    pemWriter.writeObject(new PemObject("CERTIFICATE", cert.getEncoded()));
                    pemWriter.flush();
                    pemWriter.close();
                    s = writer.toString();
                    writer.close();
                    return s;
            }
            
            //System.out.println("here-->" + cert.getSerialNumber());
        } catch (KeyStoreException ex) {
            bslog(ex);
        } catch (FileNotFoundException ex) {
            bslog(ex);
        } catch (IOException ex) {
            bslog(ex);
        } catch (NoSuchAlgorithmException ex) {
            bslog(ex);
        } catch (CertificateException ex) {
            bslog(ex);
        } catch (NoSuchProviderException ex) {
            bslog(ex);
        } 
        
        return s;
    }
    
        
    
    public static PrivateKey readPrivateKeyFromPem(File file) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    KeyFactory factory = KeyFactory.getInstance("RSA");
    try (FileReader keyReader = new FileReader(file);
      PemReader pemReader = new PemReader(keyReader)) {
        PemObject pemObject = pemReader.readPemObject();
        byte[] content = pemObject.getContent();
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
        return (PrivateKey) factory.generatePrivate(privKeySpec);
    }
    
}
    
    public static X509Certificate readPublicKeyFromPem(File file) throws IOException {
    try (FileReader keyReader = new FileReader(file)) {
        PEMParser pemParser = new PEMParser(keyReader);
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(pemParser.readObject());
        return (X509Certificate) converter.getPublicKey(publicKeyInfo);
    }
}
    
    public static boolean createKeyStoreWithNewKeyPair(String alias, String userpass, String passphrase, String filename, String sigalgo, int strength, int years) {
        
        Security.addProvider(new BouncyCastleProvider());
        // --- generate a key pair (you did this already it seems)
        KeyPairGenerator rsaGen;
        try {
        rsaGen = KeyPairGenerator.getInstance("RSA", "BC");
        rsaGen.initialize(strength, new SecureRandom());
        
        final KeyPair pair = rsaGen.generateKeyPair();

        // --- create the self signed cert
        Certificate cert = createSelfSigned(sigalgo, pair, years);

        // --- create a new pkcs12 key store in memory
        KeyStore pkcs12 = KeyStore.getInstance("PKCS12");
        pkcs12.load(null, null);

        // --- create entry in PKCS12
        pkcs12.setKeyEntry(alias, pair.getPrivate(), userpass.toCharArray(), new Certificate[] {cert});

         // --- store PKCS#12 as file
        Path filepath = FileSystems.getDefault().getPath(filename);
        try (FileOutputStream p12 = new FileOutputStream(filepath.toFile())) {
            pkcs12.store(p12, passphrase.toCharArray());
        }

        } catch (NoSuchAlgorithmException ex) {
            bslog(ex);
            return false;
        } catch (OperatorCreationException ex) {
            bslog(ex);
            return false;
        } catch (CertIOException ex) {
            bslog(ex);
            return false;
        } catch (CertificateException ex) {
            bslog(ex);
            return false;
        } catch (KeyStoreException ex) {
            bslog(ex);
            return false;
        } catch (IOException ex) {
            bslog(ex);
            return false;
        } catch (NoSuchProviderException ex) {
            bslog(ex);
            return false;
        }
        
        return true;
    }
    
    public static boolean createKeyStore(String passphrase, String filename) {
       
        try {
        // --- create a new pkcs12 key store in memory
        KeyStore pkcs12 = KeyStore.getInstance("PKCS12");
        pkcs12.load(null, null);

         // --- store PKCS#12 as file
        Path filepath = FileSystems.getDefault().getPath(filename);
        try (FileOutputStream p12 = new FileOutputStream(filepath.toFile())) {
            pkcs12.store(p12, passphrase.toCharArray());
        }

        } catch (NoSuchAlgorithmException ex) {
            bslog(ex);
            return false;
        } catch (CertIOException ex) {
            bslog(ex);
            return false;
        } catch (CertificateException ex) {
            bslog(ex);
            return false;
        } catch (KeyStoreException ex) {
            bslog(ex);
            return false;
        } catch (IOException ex) {
            bslog(ex);
            return false;
        } 
        
        return true;
    }
    
    
    public static boolean createNewKeyPair(String alias, String userpass, String passphrase, String filename, String algo, String sigalgo, int strength, int years) {
        
        Security.addProvider(new BouncyCastleProvider());
        // --- generate a key pair (you did this already it seems)
        KeyPairGenerator rsaGen;
        try {
        rsaGen = KeyPairGenerator.getInstance(algo, "BC");
        rsaGen.initialize(strength, new SecureRandom());
        
        final KeyPair pair = rsaGen.generateKeyPair();

        // --- create the self signed cert
        Certificate cert = createSelfSigned(sigalgo, pair, years);

        KeyStore keystore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(FileSystems.getDefault().getPath(filename).toString()))  {
            keystore.load(fis, passphrase.toCharArray());
            keystore.setKeyEntry(alias, pair.getPrivate(), userpass.toCharArray(), new Certificate[] {cert});
        }
        
         // --- now save back to .p12 file
        Path filepath = FileSystems.getDefault().getPath(filename);
        try (FileOutputStream p12 = new FileOutputStream(filepath.toFile())) {
            keystore.store(p12, passphrase.toCharArray());
        }
      
        } catch (NoSuchAlgorithmException ex) {
            bslog(ex);
            return false;
        } catch (OperatorCreationException ex) {
            bslog(ex);
            return false;
        } catch (CertIOException ex) {
            bslog(ex);
            return false;
        } catch (CertificateException ex) {
            bslog(ex);
            return false;
        } catch (KeyStoreException ex) {
            bslog(ex);
            return false;
        } catch (IOException ex) {
            bslog(ex);
            return false;
        } catch (NoSuchProviderException ex) {
            bslog(ex);
            return false;
        }
        
        return true;
    }
    
    
    public static X509Certificate createSelfSigned(String sigalgo, KeyPair pair, int years) throws OperatorCreationException, CertIOException, CertificateException {
        String x = "";
        
        if (sigalgo.equals("SHA-256")) {
            x = "SHA256WithRSA";
        } else if (sigalgo.equals("SHA-1")) {
            x = "SHA1WithRSA";
        } else if (sigalgo.equals("MD-5")) {
            x = "MD5WithRSA";    
        } else {
           x = "SHA256WithRSA";  
        }
        
        KeyPairGenerator rsaGen;
        
        String[] siteinfo = OVData.getSiteAddressArray(OVData.getDefaultSite());
        X500Name dnName = new X500Name("CN=BlueSeer Software"); 
        BigInteger certSerialNumber = new BigInteger(Long.toString(System.currentTimeMillis()));

        Date startDate = new Date(); // now
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.YEAR, years);
        Date endDate = calendar.getTime();

        X500NameBuilder builder = new X500NameBuilder(RFC4519Style.INSTANCE);
        
        builder.addRDN(RFC4519Style.c, siteinfo[8]);  // site country
        builder.addRDN(RFC4519Style.o, siteinfo[1]);  // site ID
        builder.addRDN(RFC4519Style.l, siteinfo[5]);  // site city
        builder.addRDN(RFC4519Style.st, siteinfo[6]);  // site state
        
        ContentSigner contentSigner = new JcaContentSignerBuilder(x).build(pair.getPrivate());
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(dnName, certSerialNumber, startDate, endDate, builder.build(), pair.getPublic());

        return new JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner));
    }
    
    public static String hashdigest(byte[] indata, String algo) {
        String x;
        
        MessageDigest messageDigest = null;
                    try {
                        messageDigest = MessageDigest.getInstance(algo);  // SHA-1, etc
                    } catch (NoSuchAlgorithmException ex) {
                        bslog(ex);
                    }
        byte[] hashedbytes = messageDigest.digest(indata);
        x = new String(Base64.encode(hashedbytes));
        return x;
    }
    
    public static byte[] encryptData(byte[] data, X509Certificate encryptionCertificate, String algo) throws CertificateEncodingException, CMSException, IOException {
        ASN1ObjectIdentifier x = null;
        if (algo.equals("AES128_CBC")) {
            x = CMSAlgorithm.AES128_CBC;
        } else if (algo.equals("AES192_CBC")) {
            x = CMSAlgorithm.AES192_CBC;
        } else if (algo.equals("AES256_CBC")) {
            x = CMSAlgorithm.AES256_CBC;
        } else if (algo.equals("DES_CBC")) {
            x = CMSAlgorithm.DES_CBC;  
        } else if (algo.equals("DES_EDE3_CBC")) {
            x = CMSAlgorithm.DES_EDE3_CBC;    
        } else {
           x = CMSAlgorithm.AES128_CBC;  
        }
        byte[] encryptedData = null;
        if (null != data && null != encryptionCertificate) {
            CMSEnvelopedDataGenerator cmsEnvelopedDataGenerator
              = new CMSEnvelopedDataGenerator();

            JceKeyTransRecipientInfoGenerator jceKey 
              = new JceKeyTransRecipientInfoGenerator(encryptionCertificate);
            cmsEnvelopedDataGenerator.addRecipientInfoGenerator(jceKey);
            CMSTypedData msg = new CMSProcessableByteArray(data);
            OutputEncryptor encryptor = new JceCMSContentEncryptorBuilder(x)
              .setProvider("BC").build();
            CMSEnvelopedData cmsEnvelopedData = cmsEnvelopedDataGenerator
              .generate(msg,encryptor);
            encryptedData = cmsEnvelopedData.getEncoded();
        }
			    return encryptedData;
	
}
    
    public static byte[] decryptData(byte[] encryptedData,PrivateKey decryptionKey)throws CMSException {
            byte[] decryptedData = null;
            if (null != encryptedData && null != decryptionKey) {
                CMSEnvelopedData envelopedData = new CMSEnvelopedData(encryptedData);
                Collection<RecipientInformation> recipients = envelopedData.getRecipientInfos().getRecipients();
                KeyTransRecipientInformation recipientInfo = (KeyTransRecipientInformation) recipients.iterator().next();
                JceKeyTransRecipient recipient = new JceKeyTransEnvelopedRecipient(decryptionKey);
                try {
                decryptedData = recipientInfo.getContent(recipient);
                } catch (Exception ex) {
                bslog(ex);
                }
            }
            return decryptedData;
}
    
    public static boolean verifySignature(final byte[] plaintext, final byte[] signedData)  {
        boolean x = false;
        
        if (plaintext == null || signedData == null) {
            return x;
        }
        
        try {
            CMSSignedData s = new CMSSignedData(new CMSProcessableByteArray(plaintext), signedData);
            Store certstore = s.getCertificates();
            SignerInformationStore signers = s.getSignerInfos();
            Collection<SignerInformation> c = signers.getSigners();
            SignerInformation signer = c.iterator().next();
            Collection<X509CertificateHolder> certCollection = certstore.getMatches(signer.getSID());
            Iterator<X509CertificateHolder> certIt = certCollection.iterator();
            if (! certIt.hasNext()) {
                return x;
            }
            X509CertificateHolder certHolder = certIt.next();
            x = signer.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certHolder));
        } catch ( CMSException | OperatorCreationException | CertificateException ex) {
            bslog(ex);
        }
        return x;
}
    
  
     public static MimeBodyPart signData(
          byte[] data, 
          X509Certificate signingCertificate,
          PrivateKey signingKey, String filename, String[] tp, String contenttype) throws Exception {
            List<X509Certificate> certList = new ArrayList<X509Certificate>();
            certList.add(signingCertificate);
            certs = new JcaCertStore(certList);

            SMIMESignedGenerator sGen = new SMIMESignedGenerator(false ? SMIMESignedGenerator.RFC3851_MICALGS : SMIMESignedGenerator.RFC5751_MICALGS);
            JcaSimpleSignerInfoGeneratorBuilder jSig = new JcaSimpleSignerInfoGeneratorBuilder().setProvider("BC");
            SignerInfoGenerator sig = jSig.build("SHA1withRSA", signingKey, signingCertificate);
            sGen.addSignerInfoGenerator(sig);
            sGen.addCertificates(certs);
            MimeBodyPart dataPart = new MimeBodyPart();
            dataPart.removeHeader("Content-Type");
            dataPart.removeHeader("Content-Disposition");
            
            InputStream targetStream = new ByteArrayInputStream(data);
            //InputStream targetStream = new FileInputStream(new File("c:\\junk\\item_costs.pdf"));
            ByteArrayDataSource ds = new ByteArrayDataSource(targetStream, contenttype); 
            dataPart.setDataHandler(new DataHandler(ds));
            
            // dataPart.setText(new String(data, StandardCharsets.UTF_8), "UTF-8");
            dataPart.setHeader("Content-Type", contenttype + "; file=" + filename);
            dataPart.setHeader("Content-Disposition", "attachment; filename=" + filename);
            dataPart.setHeader("Content-Transfer-Encoding", "binary");
            /*
            ArrayList<String> list = EDData.getAS2AttributesList(tp[0], "httpheader");
            for (String x : list) {
                String[] h = x.split(":",-1);
                if (h != null && h.length > 1) {
                 dataPart.setHeader(h[0], h[1]);
                }
            }
            */
            
            MimeMultipart signedData = sGen.generate(dataPart);
            MimeBodyPart tmpBody = new MimeBodyPart();
            tmpBody.setContent(signedData);
            tmpBody.setHeader("Content-Type", signedData.getContentType());
            return tmpBody;
	}

    public static boolean isEncrypted(byte[] encryptedData) {
            CMSEnvelopedData envelopedData;
            String x = null;
                    try {
                        envelopedData = new CMSEnvelopedData(encryptedData);
                        x = envelopedData.getEncryptionAlgOID();
                    } catch (CMSException ex) {
                        x = null;
                    }
            return ! (x == null);
    }
    
    public static X509Certificate getCert(String certfile) throws CertificateException, NoSuchProviderException {
        X509Certificate certificate = null;
        Path certfilepath = FileSystems.getDefault().getPath("edi/certs/" + certfile);
        if (! Files.exists(certfilepath)) {
             // throw new RuntimeException("bad path to cert file: " + certfile);
             return certificate; // return null
        }
       // System.out.println("here->" + certfilepath.toString());
        Security.addProvider(new BouncyCastleProvider());
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509", "BC");
        try (FileInputStream fis = new FileInputStream(certfilepath.toFile())) {
            certificate = (X509Certificate) certFactory.generateCertificate(fis);
        } catch (IOException ex) {
            bslog(ex);
        }
        
        return certificate;
    }
    
    public static String setMessageID() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return "<BLUESEER-" + now + "." + Long.toHexString(System.currentTimeMillis()) + "@Blueseer Software>";
    }
    
    public static String setBoundary() {
        return "BSPart_" + Long.toHexString(System.currentTimeMillis());
    }
    
    public static String getPackagedBoundary(MimeBodyPart mbp) throws MessagingException {
        
        String[] mb = mbp.getContentType().split(";");
        for (String s : mb) {
            if (s.contains("boundary=")) {
                String[] mbs = s.split("=", 2);
                return mbs[1].trim().replace("\"", "");
            }
        }
        return null;
    }
    
    public static String postAS2( String as2id, boolean isDebug) throws MalformedURLException, URISyntaxException, IOException, CertificateException, NoSuchProviderException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateEncodingException, CMSException, SMIMEException, Exception  {
        
        StringBuilder r = new StringBuilder();
        String  now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        ArrayList<String[]> logdet = new ArrayList<String[]>(); 
        Security.addProvider(new BouncyCastleProvider());
        
        
        
     //   Path as2filepath = FileSystems.getDefault().getPath(as2file);
     //   if (! Files.exists(as2filepath)) {
     //      return "source file does not exist"; 
      //  }
       
       
        // gather pertinent info for this AS2 ID / Partner
        String[] tp = ediData.getAS2Info(as2id);
        String url = tp[15] + "://" + tp[1] + ":" + tp[2] + "/" + tp[3];
        String as2To = tp[4];
        String as2From = tp[5];
        String internalURL = tp[6];
        String sourceDir = tp[16];
        String signkeyid = tp[7];  // was tp[7]
        String contenttype = tp[21];
        
        
        int parent = writeAS2Log(new String[]{"0",as2id,"out",""," Init as2 outbound for partner: " + as2id + "/" + as2From + "/" + as2To,now,""}); 
        String parentkey = String.valueOf(parent);
        logdet.add(new String[]{parentkey, "info", "processing as2 for relationship " + as2From + "/" + as2To});
        logdet.add(new String[]{parentkey, "info", "Sending to URL / Port / Path = " + url});
        logdet.add(new String[]{parentkey, "info", "Source Directory: " + sourceDir});
        logdet.add(new String[]{parentkey, "info", "Encryption Cert file: " + tp[11]});
        logdet.add(new String[]{parentkey, "info", "Signing Key ID: " + signkeyid});
        
       
       // System.out.println("here->" + as2To + "/" +  as2From + "/" + internalURL + "/" + sourceDir + "/" + signkeyid);
        
        X509Certificate encryptcertificate = getPublicKeyAsCert(tp[11]);
        if (encryptcertificate == null) {
          logdet.add(new String[]{parentkey, "error", "Unable to retrieve encryption cert for " + tp[11]}); 
          writeAS2LogDetail(logdet);
          return "Unable to retrieve encryption cert for " + tp[11];
        }
        
        logdet.add(new String[]{parentkey, "info", "Encryption with: " + encryptcertificate.getIssuerX500Principal().getName() + "/" + encryptcertificate.getSigAlgName()});
        logdet.add(new String[]{parentkey, "info", "Encryption Serial#: " + encryptcertificate.getSerialNumber().toString(16)});
        logdet.add(new String[]{parentkey, "info", "Encryption Expiration Window: " + encryptcertificate.getNotBefore() + "/" + encryptcertificate.getNotAfter()});
        
        pks_mstr pks = admData.getPksMstr(new String[]{signkeyid});
        String[] k = new String[]{"","","","",""};
        X509Certificate signcertificate = null; 
        PrivateKey key = null;
        
        if ( pks.pks_type().equals("store") || pks.pks_type().equals("external pem") ) {
          logdet.add(new String[]{parentkey, "error", "Using non-user signing key " + signkeyid}); 
          writeAS2LogDetail(logdet);
          return "Using non-user signing key  " + signkeyid; 
        } else {
        k = getKeyStoreByUser(signkeyid); // store, storeuser, storepass, user, pass
        k[2] = bsmf.MainFrame.PassWord("1", k[2].toCharArray());
        k[4] = bsmf.MainFrame.PassWord("1", k[4].toCharArray());
       
        char[] keyPassword = k[4].toCharArray();  
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        
        FileInputStream fis = new FileInputStream(FileSystems.getDefault().getPath(k[0]).toString());
        if (k[2].isBlank()) {
            keystore.load(fis, null);
        } else {
            keystore.load(fis, k[2].toCharArray());
        }
        fis.close();
        
        key = (PrivateKey) keystore.getKey(k[3], keyPassword);
        signcertificate = (X509Certificate) keystore.getCertificate(k[3]);
    }
        
        if (key == null) {
          logdet.add(new String[]{parentkey, "error", "Unable to retrieve private key for signing " + signkeyid}); 
          writeAS2LogDetail(logdet);
          return "Unable to retrieve private key for signing " + signkeyid;
        }
        
        if (signcertificate == null) {
          logdet.add(new String[]{parentkey, "error", "Unable to retrieve signing cert " + k[3]}); 
          writeAS2LogDetail(logdet);
          return "Unable to retrieve signing cert " + k[3];
        }
        
        logdet.add(new String[]{parentkey, "info", "Signing with: " + signcertificate.getIssuerX500Principal().getName() + "/" + signcertificate.getSigAlgName()});
        logdet.add(new String[]{parentkey, "info", "Signing Serial#: " + signcertificate.getSerialNumber().toString(16)});
        logdet.add(new String[]{parentkey, "info", "Signing Expiration Window: " + signcertificate.getNotBefore() + "/" + signcertificate.getNotAfter()});
        
        
        
        Path as2filepath = null;
        File folder = new File(sourceDir);
        File[] listOfFiles = folder.listFiles();
        boolean isSuccess = false;
        
        if (listOfFiles == null || listOfFiles.length == 0) {
            logdet.add(new String[]{parentkey, "passive", "No Files in output directory " + sourceDir}); 
            writeAS2LogDetail(logdet);
            return "No Files in output directory " + sourceDir;
        }
        for (int i = 0; i < listOfFiles.length; i++) {
            
            isSuccess = false;
            
            if (! listOfFiles[i].isFile()) {
            continue;
            }
            
            as2filepath = FileSystems.getDefault().getPath(sourceDir + "/" + listOfFiles[i].getName()); 
            logdet.add(new String[]{parentkey, "info", "Transmitting file: " + listOfFiles[i].getName()});
       
                    
        String messageid = setMessageID();
        
        CloseableHttpClient client = HttpClients.createDefault();
           
        byte[] filecontent;
        // String filecontent;
        try {
            filecontent = Files.readAllBytes(as2filepath);
            
           // filecontent = Files.readString(as2filepath);
        } catch (IOException ex) {
            bslog(ex);
            continue;
        }
        
        MimeBodyPart mbp;
        byte[] signedAndEncrypteddata = null;
        
        
        boolean isSignedAndEncrypted = true;
        // need signed, signed+enc, enc, none ....condition logic here
        if (filecontent != null) {    
                try {
                    // mbp = signData(filecontent.getBytes(StandardCharsets.UTF_8),signcertificate,key,listOfFiles[i].getName());
                    mbp = signData(filecontent,signcertificate,key,listOfFiles[i].getName(),tp,contenttype);
                    
                } catch (Exception ex) {
                    bslog(ex);
                    continue;
                }
        } else {
           bslog("file content is null in AS2Post");
           continue; 
        }
        
        MimeBodyPart mbp2 = new MimeBodyPart();
        MimeMultipart mp = new MimeMultipart();
        String newboundary = getPackagedBoundary(mbp);  
        if (isSignedAndEncrypted) {
         // mbp.addHeader("Content-Type", "multipart/signed; protocol=\"application/pkcs7-signature\"; boundary=" + "\"" + newboundary + "\"" + "; micalg=sha1");
         // mbp.addHeader("Content-Disposition", "attachment; filename=smime.p7m");
           /*
           mbp2 = encryptDataSMIME(mbp.getInputStream().readAllBytes(), encryptcertificate);
           Enumeration<?> list = mbp2.getAllHeaders();
            while (list.hasMoreElements()) {
                javax.mail.Header head = (javax.mail.Header) list.nextElement();
                System.out.println(head.getName() + ": " + head.getValue());
            }
           */ 
          mp.addBodyPart(mbp);
          mbp2.setContent(mp);
          mbp2.addHeader("Content-Type", "multipart/signed; protocol=\"application/pkcs7-signature\"; boundary=" + "\"" + newboundary + "\"" + "; micalg=sha1");
          mbp2.addHeader("Content-Disposition", "attachment; filename=smime.p7m");
        
          if (isDebug) { 
            String debugfile = "debugAS2post." + now + "." + Long.toHexString(System.currentTimeMillis());
            Path pathinput = FileSystems.getDefault().getPath("temp" + "/" + debugfile);
            try (FileOutputStream stream = new FileOutputStream(pathinput.toFile())) {
            stream.write(mbp2.getInputStream().readAllBytes());
            }
        }
          
          signedAndEncrypteddata = encryptData(mbp2.getInputStream().readAllBytes(), encryptcertificate, tp[18]);
          
        }
        
        
       // BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path.toFile())));
       // output.write(new String(Base64.encode(signedAndEncrypteddata)));
       // output.close();
        /*
        Path path = FileSystems.getDefault().getPath("temp" + "/" + "beforefile");
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path.toFile()));
        bos.write(signedAndEncrypteddata);
        bos.flush();
        bos.close();
        */
        
        URL urlObj = new URL(url);
        RequestBuilder rb = RequestBuilder.post();
        rb.setUri(urlObj.toURI());
        
        
        
        if (! isSignedAndEncrypted) {
        rb.addHeader("User-Agent", "java/app (BlueSeer Software; +http://www.blueseer.com/)"); 
        rb.addHeader("AS2-To", as2To);
        rb.addHeader("AS2-From", as2From); 
        rb.addHeader("AS2-Version", "1.2"); 
        rb.addHeader("Mime-Version", "1.0");
        rb.addHeader("Subject", "as2");
        rb.addHeader("Accept-Encoding", "deflate, gzip, x-gzip, compress, x-compress");
        rb.addHeader("Disposition-Notification-Options", "signed-receipt-protocol=optional, pkcs7-signature; signed-receipt-micalg=optional, sha1");
        rb.addHeader("Disposition-Notification-To", internalURL);
        rb.addHeader("Message-ID", messageid);
        rb.addHeader("Recipient-Address", url.toString());
        rb.addHeader("EDIINT-Features", "CEM, multiple-attachments, AS2-Reliability");
        rb.addHeader("Content-Type", "multipart/signed; protocol=\"application/pkcs7-signature\"; boundary=" + "\"" + newboundary + "\"" + "; micalg=sha1");
        rb.addHeader("Content-Disposition", "attachment; filename=smime.p7m");
        } else {
        rb.addHeader("User-Agent", "java/app (BlueSeer Software; +http://www.blueseer.com/)"); 
        rb.addHeader("AS2-To", as2To);
        rb.addHeader("AS2-From", as2From); 
        rb.addHeader("AS2-Version", "1.2"); 
        rb.addHeader("Mime-Version", "1.0");
        rb.addHeader("Subject", "as2");
        rb.addHeader("Accept-Encoding", "deflate, gzip, x-gzip, compress, x-compress");
        rb.addHeader("Disposition-Notification-Options", "signed-receipt-protocol=optional, pkcs7-signature; signed-receipt-micalg=optional, sha1");
        rb.addHeader("Disposition-Notification-To", internalURL);
        rb.addHeader("Message-ID", messageid);
        rb.addHeader("Recipient-Address", url.toString());
        rb.addHeader("EDIINT-Features", "CEM, multiple-attachments, AS2-Reliability");
        rb.addHeader("Content-Type", "application/pkcs7-mime; smime-type=enveloped-data; name=smime.p7m");
        rb.addHeader("Content-Transfer-Encoding", "binary");
        rb.addHeader("Content-Disposition", "attachment; filename=smime.p7m");
        }
        
        // add custom headers
        ArrayList<String> list = EDData.getAS2AttributesList(tp[0], "httpheader");
        for (String x : list) {
                String[] h = x.split(":",-1);
                if (h != null && h.length > 1) {
                 rb.addHeader(h[0], h[1]);
                }
            }
        
        InputStreamEntity ise = new InputStreamEntity(new ByteArrayInputStream(signedAndEncrypteddata));
          
          rb.setEntity(new BufferedHttpEntity(ise));
          HttpUriRequest request = rb.build();
        
        if (isDebug) { 
            String debugfile = "debugAS2http." + now + "." + Long.toHexString(System.currentTimeMillis());
            Path pathinput = FileSystems.getDefault().getPath("temp" + "/" + debugfile);
            Header[] headers = request.getAllHeaders();
            try (FileOutputStream stream = new FileOutputStream(pathinput.toFile())) {
                for (Header x : headers) {
                    String h = x.getName() + ": " + x.getValue() + "\n";
                    stream.write(h.getBytes());
                }
            }  
        }
        
        try (CloseableHttpResponse response = client.execute(request)) {
        if (response.getStatusLine().getStatusCode() != 200) {
                r.append(response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase());
                //throw new RuntimeException("Failed : HTTP error code : "
                //		+ conn.getResponseCode());
        } else {
            r.append("SUCCESS: " + response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase() + "\n");
            if (isDebug) { 
            String debugfile = "debugAS2responseHeaders." + now + "." + Long.toHexString(System.currentTimeMillis());
            Path pathinput = FileSystems.getDefault().getPath("temp" + "/" + debugfile);
            Header[] headers = response.getAllHeaders();
             try (FileOutputStream stream = new FileOutputStream(pathinput.toFile())) {
                for (Header x : headers) {
                    String h = x.getName() + ": " + x.getValue() + "\n";
                    stream.write(h.getBytes());
                }
             }  
            }
        }
        
        HttpEntity entity = response.getEntity();
        byte[] indata = EntityUtils.toByteArray(entity);
        String result = new String(indata); 
        
        
        // save MDN file if present
        try {
        MimeMultipart mpr  = new MimeMultipart(new ByteArrayDataSource(indata, entity.getContentType().getValue()));
        for (int z = 0; z < mpr.getCount(); z++) {
            MimeBodyPart mbpr = (MimeBodyPart) mpr.getBodyPart(z);
            if (mbpr.getContentType().contains("disposition")) {
                String filename = "mdn." + now + "." + Long.toHexString(System.currentTimeMillis());
                Path path = FileSystems.getDefault().getPath("edi/mdn" + "/" + filename);
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path.toFile())));
                String datastring = new String(mbpr.getInputStream().readAllBytes());   
                output.write(datastring);
                output.close();
                
                // log mdn filename into parent log entry
                updateAS2LogMDNFile(parentkey, filename);
                
                Pattern p = Pattern.compile("Disposition:.*(error|failed).*");
		Matcher m = p.matcher(datastring);
		if (m.find()) {
                    logdet.add(new String[]{parentkey, "error", "MDN error: " + filename});
                } else {
                   logdet.add(new String[]{parentkey, "info", "MDN processed: " + filename}); 
                   isSuccess = true;
                }
            }
        }
        } catch (MessagingException ex) {
           logdet.add(new String[]{parentkey, "error", " Messaging error; Bad MDN Boundary"}); 
          writeAS2LogDetail(logdet);
          return "Messaging error; Bad MDN Boundary"; 
        }   
        
        
        r.append(result);
        } catch (HttpHostConnectException | ConnectTimeoutException  ex) {
          logdet.add(new String[]{parentkey, "error", " Connection refused or timeout from server "}); 
          writeAS2LogDetail(logdet);
          return "Connection refused or timeout from server ";
        } catch ( UnknownHostException ex) {
          logdet.add(new String[]{parentkey, "error", " Unknown host server " + request.getURI()}); 
          writeAS2LogDetail(logdet);
          return " Unknown host server " + request.getURI();
        } catch ( SocketException ex) {
          logdet.add(new String[]{parentkey, "error", " Socket exception connection reset " + request.getURI()}); 
          writeAS2LogDetail(logdet);
          return " Socket exception connection reset " + request.getURI();
        }
        
      // remove file if successful
      if (isSuccess) {
        Files.deleteIfExists(as2filepath);
        r.append("\n").append("status__pass");
      } 
        
    } // for each file
        writeAS2LogDetail(logdet);
        return r.toString();
    }
    
    
    public static MimeMultipart bundleit(String z, String receiver, String messageid, String mic, String status) {
        MimeBodyPart mbp = new MimeBodyPart();
        MimeBodyPart mbp2 = new MimeBodyPart();
        MimeMultipart mp = new MimeMultipart();
        try {
            mbp.setText(z);
            mbp.setHeader("Content-Type", "text/plain; charset=us-ascii");
            mbp.setHeader("Content-Transfer-Encoding", "7bit");
            
            String y = """
                       Reporting-UA: BlueSeer Software
                       Original-Recipient: rfc822; %s
                       Final-Recipient: rfc822; %s
                       Original-Message-ID: %s
                       Disposition: automatic-action/MDN-sent-automatically; %s
                       Received-Content-MIC: %s, sha
                       """.formatted(receiver, receiver, messageid, status, mic);
            
            mbp2.setText(y);
            mbp2.setHeader("Content-Type", "message/disposition-notification");
            mbp2.setHeader("Content-Transfer-Encoding", "7bit");
            
            mp.addBodyPart(mbp);
            mp.addBodyPart(mbp2);
            
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        return mp;
    }
    /*
    public static MimeMultipart code1000(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String z = """
                The message <%s> with subject <%s> has been received.  
                Message was sent from: <%s>  to:  <%s>
                Message was received at <%s>
                Note: The origin and integrity of the message have been verified.
                """.formatted(filename, subject, sender, receiver, now);
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "processed");
           ContentType ct = new ContentType(mpInner.getContentType());
           String boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
           //  mbp.setHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
            
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return mp;
    }
    */
    public static mmpx code1000(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        String boundary = "";
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String z = """
                The message <%s> with subject <%s> has been received.  
                Message was sent from: <%s>  to:  <%s>
                Message was received at <%s>
                Note: The origin and integrity of the message have been verified.
                """.formatted(filename, subject, sender, receiver, now);
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "processed");
           ContentType ct = new ContentType(mpInner.getContentType());
           boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
           //  mbp.setHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
            
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return new mmpx(mp, boundary);
    }
    
    
    public static mmpx code2000(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        String boundary = "";
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String z = """
                The message <%s> sent from: <%s> to: <%s>
                at %s was not signed.
                """.formatted(filename, sender, receiver, now);
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "failed");
           ContentType ct = new ContentType(mpInner.getContentType());
           boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return new mmpx(mp, boundary);
    }
    
    
    public static mmpx code2005(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        String boundary = "";
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String z = """
                The message <%s> sent from: <%s> to: <%s>
                at %s failed.
                Error: MimeMultipart is incomplete   
                """.formatted(filename, sender, receiver, now);
                   
               
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "failed");
           ContentType ct = new ContentType(mpInner.getContentType());
           boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return new mmpx(mp, boundary);
    }
    
    public static mmpx code2010(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        String boundary = "";
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String z = """
                The message <%s> sent from: <%s> to: <%s>
                at %s failed.
                   Error: unable to retrieve contents of File
                   Error:  FileBytesRead is null
                """.formatted(filename, sender, receiver, now);
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "failed");
           ContentType ct = new ContentType(mpInner.getContentType());
           boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return new mmpx(mp, boundary);
    }
    
    public static mmpx code2015(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        String boundary = "";
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String z = """
                The message <%s> sent from: <%s> to: <%s>
                at %s failed.
                   Error: Signature content is null
                """.formatted(filename, sender, receiver, now);
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "failed");
           ContentType ct = new ContentType(mpInner.getContentType());
           boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return new mmpx(mp, boundary);
    }
    
    public static mmpx code2020(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        String boundary = "";
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String z = """
                The message <%s> sent from: <%s> to: <%s>
                at %s failed.
                   Error: Invalid Signature
                """.formatted(filename, sender, receiver, now);
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "failed");
           ContentType ct = new ContentType(mpInner.getContentType());
           boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return new mmpx(mp, boundary);
    }
    
    public static mmpx code3000(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        String boundary = "";
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String z = """
                The message transmitted at <%s> was transmitted with null content.
                """.formatted(now);
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "failed");
           ContentType ct = new ContentType(mpInner.getContentType());
           boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return new mmpx(mp, boundary);
    }
     
    public static mmpx code3003(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        String boundary = "";
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String z = """
                Unable to decrypt message transmitted at <%s>.  Potential bad public key.
                """.formatted(now);
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "failed");
           ContentType ct = new ContentType(mpInner.getContentType());
           boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return new mmpx(mp, boundary);
    }
    
    
    public static mmpx code3005(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        String boundary = "";
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String z = """
                The message transmitted at <%s> had unrecognizable HTTP headers.
                """.formatted(now);
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "failed");
           ContentType ct = new ContentType(mpInner.getContentType());
           boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return new mmpx(mp, boundary);
    }
    
    public static mmpx code3007(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        String boundary = "";
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String z = """
                The message transmitted at <%s> had zero HTTP headers.
                """.formatted(now);
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "failed");
           ContentType ct = new ContentType(mpInner.getContentType());
           boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return new mmpx(mp, boundary);
    }
   
    public static mmpx code3100(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        String boundary = "";
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String z = """
                The message <%s> transmitted at <%s> was transmitted to unknown receiver ID.
                """.formatted(filename, now);
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "failed");
           ContentType ct = new ContentType(mpInner.getContentType());
           boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return new mmpx(mp, boundary);
    }
    
    public static mmpx code3200(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        String boundary = "";
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String z = """
                The message <%s> transmitted at <%s> was transmitted by unknown sender ID.
                """.formatted(filename, now);
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "failed");
           ContentType ct = new ContentType(mpInner.getContentType());
           boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
           
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return new mmpx(mp, boundary);
    }
    
    public static mmpx code3300(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        String boundary = "";
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String z = """
                The message <%s> transmitted at <%s>... unable to determine sender / receiver keys.
                """.formatted(filename, now);
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "failed");
           ContentType ct = new ContentType(mpInner.getContentType());
           boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return new mmpx(mp, boundary);
    }
    
    public static mmpx code3400(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        String boundary = "";
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String z = """
                The message <%s> transmitted at <%s>... encryption is required.
                """.formatted(filename, now);
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "failed");
           ContentType ct = new ContentType(mpInner.getContentType());
           boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return new mmpx(mp, boundary);
    }
    
    
    public static mmpx code9999(String sender, String receiver, String subject, String filename, String messageid, String mic) {
        MimeBodyPart mbp = new MimeBodyPart();
        String boundary = "";
        MimeMultipart mp = new MimeMultipart();
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_DATE);
        String z = """
                The message <%s> sent to <%s>
                on %s with Subject <%s> failed and has been rejected.
                The message was transmitted by <%s>.
                Internal server case 9999.
                """.formatted(filename, receiver, now, subject, sender);
        try {
           // mbp.setText(z);
           MimeMultipart mpInner = bundleit(z, receiver, messageid, mic, "failed");
           ContentType ct = new ContentType(mpInner.getContentType());
           boundary = ct.getParameter("boundary");
            mbp.setContent(mpInner);
            mbp.addHeader("Content-Type", "multipart/report; report-type=disposition-notification; boundary=" + "\"" + boundary + "\"");
            mp.addBodyPart(mbp);
            
        } catch (MessagingException ex) {
            bslog(ex);
        }
        
        return new mmpx(mp, boundary);
    }
    
    
    public static mdn createMDN(String code, String[] e, HashMap<String, String> headers, boolean isDebug) throws IOException, MessagingException {
        mdn x = null;
        MimeBodyPart mbp = new MimeBodyPart();
        
        String z;
        LocalDateTime localDateTime = LocalDateTime.now();
        String now = localDateTime.format(DateTimeFormatter.ISO_DATE);
        String boundary = "";
        mmpx mymmpx = null;
        
        switch (code) {
            case "1000" :
          //  mbp.setContent(code1000(e[0], e[1], e[2], e[3], e[4], e[5]));
           mymmpx = code1000(e[0], e[1], e[2], e[3], e[4], e[5]);
           mbp.setContent(mymmpx.mmp());
           boundary = mymmpx.boundary();
            break;     
            
            case "2000" :
            // mbp.setContent(code2000(e[0], e[1], e[2], e[3], e[4], e[5]));
           mymmpx = code2000(e[0], e[1], e[2], e[3], e[4], e[5]);
           mbp.setContent(mymmpx.mmp());
           boundary = mymmpx.boundary();
            break;
            
            case "3000" :
            mymmpx = code3000(e[0], e[1], e[2], e[3], e[4], e[5]);
           mbp.setContent(mymmpx.mmp());
           boundary = mymmpx.boundary();
            break;
            
            case "3003" :
            mymmpx = code3003(e[0], e[1], e[2], e[3], e[4], e[5]);
           mbp.setContent(mymmpx.mmp());
           boundary = mymmpx.boundary();
            break;
            
            case "3005" :
            mymmpx = code3005(e[0], e[1], e[2], e[3], e[4], e[5]);
           mbp.setContent(mymmpx.mmp());
           boundary = mymmpx.boundary();
            break;
           
            case "3007" :
            mymmpx = code3007(e[0], e[1], e[2], e[3], e[4], e[5]);
           mbp.setContent(mymmpx.mmp());
           boundary = mymmpx.boundary();
            break;
            
            case "3100" :
            mymmpx = code3100(e[0], e[1], e[2], e[3], e[4], e[5]);
           mbp.setContent(mymmpx.mmp());
           boundary = mymmpx.boundary();
            break;
            
            case "3200" :
            mymmpx = code3200(e[0], e[1], e[2], e[3], e[4], e[5]);
           mbp.setContent(mymmpx.mmp());
           boundary = mymmpx.boundary();
            break;
            
            case "3300" :
            mymmpx = code3300(e[0], e[1], e[2], e[3], e[4], e[5]);
           mbp.setContent(mymmpx.mmp());
           boundary = mymmpx.boundary();
            break;
            
            case "3400" :
            mymmpx = code3400(e[0], e[1], e[2], e[3], e[4], e[5]);
           mbp.setContent(mymmpx.mmp());
           boundary = mymmpx.boundary();
            break;
            
            case "2005" :
            //  mbp.setContent(code2005(e[0], e[1], e[2], e[3], e[4], e[5]));
            mymmpx = code2005(e[0], e[1], e[2], e[3], e[4], e[5]);
            mbp.setContent(mymmpx.mmp());
            boundary = mymmpx.boundary();
            break;
            
            case "2010" :
            mymmpx = code2010(e[0], e[1], e[2], e[3], e[4], e[5]);
           mbp.setContent(mymmpx.mmp());
           boundary = mymmpx.boundary();
            break;
            
            case "2015" :
            mymmpx = code2015(e[0], e[1], e[2], e[3], e[4], e[5]);
           mbp.setContent(mymmpx.mmp());
           boundary = mymmpx.boundary();
            break;
            
            case "2020" :
            mymmpx = code2020(e[0], e[1], e[2], e[3], e[4], e[5]);
           mbp.setContent(mymmpx.mmp());
           boundary = mymmpx.boundary();
            break;
                        
            default:
            mymmpx = code9999(e[0], e[1], e[2], e[3], e[4], e[5]);
            mbp.setContent(mymmpx.mmp());
            boundary = mymmpx.boundary();
            
        }        
        
        
        
        if (mbp != null) {
            x = new mdn(HttpServletResponse.SC_OK, headers, new String(mbp.getInputStream().readAllBytes()), boundary);
        } else {
            x = new mdn(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, "problem creating MIME structure for MDN");
        }
        
         if (isDebug && mbp != null) { 
            String debugfile = "debugMDN." + now + "." + Long.toHexString(System.currentTimeMillis());
            Path pathinput = FileSystems.getDefault().getPath("temp" + "/" + debugfile);
            try (FileOutputStream stream = new FileOutputStream(pathinput.toFile())) {
            stream.write(mbp.getInputStream().readAllBytes());
            }
        }
        
        return x; 
    }
    
    public record mdn(int status, HashMap<String, String> headers, String message, String boundary) {
        public mdn(int i, HashMap<String, String> hm, String bs) {
            this(i, hm, "", bs); 
        }
    }
    
    public record mmpx(MimeMultipart mmp, String boundary) {
     
    }
    
    
    
    
    
}

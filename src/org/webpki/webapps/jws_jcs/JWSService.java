/*
 *  Copyright 2006-2018 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.webapps.jws_jcs;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyStoreReader;
import org.webpki.crypto.MACAlgorithms;
import org.webpki.crypto.SignatureAlgorithms;
import org.webpki.util.ArrayUtil;
import org.webpki.util.Base64;
import org.webpki.webutil.InitPropertyReader;

public class JWSService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(JWSService.class.getName());

    static KeyHolder clientkey_rsa;

    static KeyHolder clientkey_ec;

    static String logotype;
    
    static String sampleSignature;
    
    static String keyDeclarations;

    class KeyDeclaration {
        
        StringBuilder decl = new StringBuilder("var ");
        StringBuilder after = new StringBuilder();
        String name;
        String last;
        String base;
        
        KeyDeclaration(String name, String base) {
            this.name = name;
            this.base = base;
            decl.append(name)
                .append(" = {");
        }

        KeyDeclaration addKey(SignatureAlgorithms alg, String fileOrNull) throws IOException {
            String algId = alg.getAlgorithmId(AlgorithmPreferences.JOSE);
            if (fileOrNull == null) {
                after.append(name)
                     .append('.')
                     .append(algId)
                     .append(" = ")
                     .append(name)
                     .append('.')
                     .append(last)
                     .append(";\n");
                     
            } else {
                if (last != null) {
                    decl.append(',');
                }
                decl.append("\n    ")
                    .append(algId)
                    .append(": '")
                    .append(HTML.javaScript(getEmbeddedResourceString(fileOrNull + base).trim()))
                    .append('\'');
                last = algId;
            }
            return this;
        }
        
        public String toString() {
            return decl.append("\n};\n").append(after).toString();
        }
    }
    class KeyHolder {

        X509Certificate[] certificatePath;
        KeyPair keyPair;

        public KeyHolder(String keyName, String key_password) throws KeyStoreException, IOException, GeneralSecurityException {
            KeyStore ks = KeyStoreReader.loadKeyStore(getResource(getPropertyString(keyName)), key_password);
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isKeyEntry(alias)) {
                    Vector<X509Certificate> certificates = new Vector<X509Certificate>();
                    for (Certificate certificate : ks.getCertificateChain(alias)) {
                        certificates.add((X509Certificate) certificate);
                    }
                    certificatePath = certificates.toArray(new X509Certificate[0]);
                    keyPair = new KeyPair(certificatePath[0].getPublicKey(),
                            (PrivateKey) ks.getKey(alias, key_password.toCharArray()));
                    return;
                }
            }
        }

        StringBuilder getPrivateKeyPEM() {
            return createPEM("PRIVATE KEY", keyPair.getPrivate().getEncoded());
        }

        StringBuilder getCertificatePathPEM() {
            StringBuilder collection = new StringBuilder();
            boolean next = false;
            for (X509Certificate certificate : certificatePath) {
                if (next) {
                    collection.append('\n');
                }
                next = true;
                try {
                    collection.append(createPEM("CERTIFICATE", certificate.getEncoded()));
                } catch (GeneralSecurityException e) {
                    new RuntimeException(e);
                }
            }
            return collection;
        }

        private StringBuilder createPEM(String header, byte[] binary) {
            return new StringBuilder("-----BEGIN ")
                .append(header)
                .append("-----\n")
                .append(new Base64().getBase64StringFromBinary(binary))
                .append("\n-----END ")
                .append(header)
                .append("-----");
        }
    }

    InputStream getResource(String name) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(name);
        if (is == null) {
            throw new IOException("Resource fail for: " + name);
        }
        return is;
    }
    
    String getEmbeddedResourceString(String name) throws IOException {
        return new String(
                ArrayUtil
                .getByteArrayFromInputStream(getResource(name)),
        "UTF-8");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        initProperties(event);
        try {
            // //////////////////////////////////////////////////////////////////////////////////////////
            // Logotype
            // //////////////////////////////////////////////////////////////////////////////////////////
            logotype = getEmbeddedResourceString("webpki-logo.svg");

            // //////////////////////////////////////////////////////////////////////////////////////////
            // Sample signature for verification
            // //////////////////////////////////////////////////////////////////////////////////////////
            sampleSignature = getEmbeddedResourceString("sample-signature.json");

            // //////////////////////////////////////////////////////////////////////////////////////////
            // Keys
            // //////////////////////////////////////////////////////////////////////////////////////////
            CustomCryptoProvider
                    .forcedLoad(getPropertyBoolean("bouncycastle_first"));
            String key_password = getPropertyString("key_password");
            clientkey_rsa = new KeyHolder("clientkey_rsa", key_password);
            clientkey_ec = new KeyHolder("clientkey_ec", key_password);
            
            
            
            keyDeclarations = 
                    new KeyDeclaration("privateKeys", "privatekey.pem")
                          .addKey(AsymSignatureAlgorithms.ECDSA_SHA256, "p256")
                          .addKey(AsymSignatureAlgorithms.ECDSA_SHA384, "p384")
                          .addKey(AsymSignatureAlgorithms.ECDSA_SHA512, "p521")
                          .addKey(AsymSignatureAlgorithms.RSA_SHA256,   "r2048")
                          .addKey(AsymSignatureAlgorithms.RSA_SHA384,   null)
                          .addKey(AsymSignatureAlgorithms.RSA_SHA512,   null).toString() +
                    new KeyDeclaration("certificates", "certpath.pem")
                          .addKey(AsymSignatureAlgorithms.ECDSA_SHA256, "p256")
                          .addKey(AsymSignatureAlgorithms.ECDSA_SHA384, "p384")
                          .addKey(AsymSignatureAlgorithms.ECDSA_SHA512, "p521")
                          .addKey(AsymSignatureAlgorithms.RSA_SHA256,   "r2048")
                          .addKey(AsymSignatureAlgorithms.RSA_SHA384,   null)
                          .addKey(AsymSignatureAlgorithms.RSA_SHA512,   null).toString() +
                    new KeyDeclaration("symmetricKeys", "bitkey.hex")
                          .addKey(MACAlgorithms.HMAC_SHA256,            "a256")
                          .addKey(MACAlgorithms.HMAC_SHA384,            "a384")
                          .addKey(MACAlgorithms.HMAC_SHA512,            "a512").toString();
            logger.info(keyDeclarations);

            logger.info("JWS-JWS Demo Successfully Initiated");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage()
                    + "\n********", e);
        }
    }
}

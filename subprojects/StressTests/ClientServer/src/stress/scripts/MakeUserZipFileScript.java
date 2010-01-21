/*
 * MakeUserZipFileScript.java
 *
 * Created on January 28, 2008, 9:33 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package stress.scripts;

import java.io.File;
import org.tranche.users.MakeUserZipFileTool;
import org.tranche.users.UserZipFile;

/**
 *
 * @author tranche
 */
public class MakeUserZipFileScript {
    
    public static void main(String args[]) {
        
        File f = new File("/home/tranche/Desktop/stress_user.zip.encrypted");
        
        try {
            
            f.createNewFile();
            
            MakeUserZipFileTool maker = new MakeUserZipFileTool();
            maker.setName("Stress Test User");
            maker.setPassphrase("stress");
            maker.setUserFile(f);
            maker.setValidDays(356*50);
            
//        // Don't let user get away with just a signer cert or a private key
//        if ((isSCert && !isSKey) || (!isSCert && isSKey)) {
//            throw new Exception("To sign a user, must provide a certificate and a key.");
//        }
//
//        // Set signer if available
//        if (isSCert && isSKey) {
//            maker.setSignerCertificate(signerCert);
//            maker.setSignerPrivateKey(signerKey);
//        }
            
            UserZipFile zip = (UserZipFile) maker.makeCertificate();
            
            // Set user permissions as admin (server needs user registered or it will
            // throw a SecurityException on attempted file upload)
//        if (isAdmin) {
//            zip.setFlags((new SecurityUtil()).getProteomeCommonsAdmin().getFlags());
//        }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }
    
}

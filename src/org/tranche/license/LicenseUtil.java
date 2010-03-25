/*
 *    Copyright 2005 The Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tranche.license;

import java.io.*;
import java.util.*;
import org.tranche.hash.BigHash;
import org.tranche.util.*;

/**
 * A helper class in generating a license file.
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class LicenseUtil {

    /**
     * <p>The standard and recommended file name for the license included in the Tranche upload.</p>
     */
    public static final String RECOMMENDED_LICENSE_FILE_NAME = "tranche-license.txt";

    /**
     * <p>Build a license file based on the parameters.</p>
     * @param outputFile File for the output. Note that if file does not exist, will be created.
     * @param license The License object representing the selected license or waiver.
     * @param additionalAgreements Any addition required statements of agreement. Can be null.
     * @param optionalNotes Any additional optional notes. Can be null.
     * @return The file with the license information. (The same file as parameter outputFile.)
     * @throws java.lang.Exception
     */
    public static File buildLicenseFile(File outputFile, License license, List<String> additionalAgreements, Map<String, String> optionalNotes) throws Exception {
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(outputFile));

            // title
            writer.write(license.getTitle());
            writer.newLine();
            writer.newLine();

            // text
            if (license.getDescription() != null && !license.getDescription().trim().equals("")) {
                writer.write(license.getDescription());
            } else if (license.getShortDescription() != null && !license.getShortDescription().trim().equals("")) {
                writer.write(license.getShortDescription());
            }

            // Add additional aggreements, if any
            if (additionalAgreements != null && additionalAgreements.size() > 0) {
                writer.newLine();
                writer.write("------------------------------------------------");
                writer.newLine();
                writer.write("The uploader agreed to the following:");
                writer.newLine();
                for (String statement : additionalAgreements) {
                    writer.write(" - " + statement);
                    writer.newLine();
                }
                writer.newLine();
            }

            // Add optional notes, if any
            if (optionalNotes != null && optionalNotes.size() > 0) {
                writer.newLine();
                writer.write("------------------------------------------------");
                writer.newLine();
                writer.write("Additional Notes:");
                writer.newLine();
                for (String licenseFieldName : optionalNotes.keySet()) {
                    writer.write(" " + licenseFieldName + ": " + optionalNotes.get(licenseFieldName));
                    writer.newLine();
                }
            }
        } finally {
            IOUtil.safeClose(writer);
        }

        return outputFile;
    }
    
    /**
     * 
     * @param outputFile
     * @param license
     * @param additionalAgreements
     * @param optionalNotes
     * @return
     * @throws java.lang.Exception
     */
    public static File buildLicenseMultiLicenseExplanationFile(File outputFile, Map<String,BigHash> pathsToParentHash) throws Exception {
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(outputFile));

            // title
            writer.write("This data set is created from files from multiple sources, which might use different licenses. For license information for each file, please check with the license file associated with each of the following hashes:");
            writer.newLine();
            
            for (String path : pathsToParentHash.keySet()) {
                BigHash parentHash = pathsToParentHash.get(path);
                writer.write("  * "+path+": "+parentHash);
                writer.newLine();
            }
            
            writer.newLine();
        } finally {
            IOUtil.safeClose(writer);
        }

        return outputFile;
    }
}

/*
 * StressTest.java
 *
 * Created on October 6, 2007, 9:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package stress.util;

import org.tranche.util.Text;


/**
 *
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class StressTest {
    
    private int clientCount = 0;
    private int files = 0;
    private long maxFileSize = 0;
    
    // If user sets these things, of couse
    private long minProjectSize = 0;
    private long maxProjectSize = Long.MAX_VALUE;
    
    private boolean isClientCountSet = false,
            isFilesSet = false,
            isMaxFileSizeSet = false,
            isRandomlyDeleteChunks = false;
    
    public int getClientCount() {
        return clientCount;
    }
    
    public void setClientCount(int clientCount) throws Exception {
        if (this.isClientCountSet)
            throw new RuntimeException("Client count already set!");
        this.clientCount = clientCount;
        this.isClientCountSet = true;
    }
    
    public int getFiles() {
        return files;
    }
    
    public void setFiles(int files) throws Exception {
        if (this.isFilesSet)
            throw new RuntimeException("Files count already set!");
        this.files = files;
        this.isFilesSet = true;
    }
    
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    public void setMaxFileSize(long maxFileSize) throws Exception {
        if (this.isMaxFileSizeSet)
            throw new RuntimeException("Max file size already set!");
        this.maxFileSize = maxFileSize;
        this.isMaxFileSizeSet = true;
    }
    
    /**
     * Returns true if properly configured, else false.
     */
    public boolean isCorrect() {
        return this.isClientCountSet && this.isFilesSet && this.isMaxFileSizeSet;
    }
    
    /**
     * Mostly for testing.
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        
//        buffer.append("-- --- --- TEST (Correct? = "+isCorrect()+") --- --- --"+Text.getNewLine());
//        buffer.append("Number clients: "+this.clientCount+Text.getNewLine());
//        buffer.append("Number of files/project: "+this.files+Text.getNewLine());
//        buffer.append("Max file size: "+this.maxFileSize+Text.getNewLine());
//        buffer.append("Using raw chunks?: "+this.isRandomlyDeleteChunks);
//        buffer.append("--- --- --- --- --- --- --- --- --- --- --- --- --- ---"+Text.getNewLine());
        
//        client_count=9
//        files=10
//        max_file_size=32768
//        delete_chunks_randomly=true
        
        buffer.append("test {"+Text.getNewLine());
        buffer.append("  client_count="+this.clientCount+Text.getNewLine());
        buffer.append("  files="+this.files+Text.getNewLine());
        buffer.append("  max_file_size="+this.maxFileSize+Text.getNewLine());
        buffer.append("  delete_chunks_randomly="+this.isRandomlyDeleteChunks+" (optional argument) "+Text.getNewLine());
        buffer.append("  min_project_size="+this.getMinProjectSize()+" (optional argument) "+Text.getNewLine());
        buffer.append("  max_project_size="+this.getMaxProjectSize()+" (optional argument) "+Text.getNewLine());
        
        buffer.append("}"+Text.getNewLine());
        
        return buffer.toString();
    }
    
    public boolean isRandomlyDeleteChunks() {
        return isRandomlyDeleteChunks;
    }
    
    /**
     * Set whether to randomly delete chunks (data and meta), as well as verify deletions. This will obviously affect performance. By default, deletions are turned off.
     */
    public void setIsRandomlyDeleteChunks(boolean isRandomlyDeleteChunks) {
        this.isRandomlyDeleteChunks = isRandomlyDeleteChunks;
    }

    public long getMinProjectSize() {
        return minProjectSize;
    }

    public void setMinProjectSize(long minProjectSize) {
        this.minProjectSize = minProjectSize;
    }

    public long getMaxProjectSize() {
        return maxProjectSize;
    }

    public void setMaxProjectSize(long maxProjectSize) {
        this.maxProjectSize = maxProjectSize;
    }
}

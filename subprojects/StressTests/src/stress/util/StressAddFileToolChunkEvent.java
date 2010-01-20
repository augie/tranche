package stress.util;

import java.util.ArrayList;
import java.util.List;
import org.tranche.hash.BigHash;

public class StressAddFileToolChunkEvent {
    // reference the parent AddFileTool
    public StressAddFileTool aft;
    
    // the file's relativeName
    public String fileRelativeName;
    
    // reference the chunk's hash
    public BigHash hash;
    public boolean isMetaData;
    
    // get the number of servers to upload to
    protected int serversToUploadTo = -1;
    
    // the list of servers suggested by ServerUtil
    List<String> serverList = new ArrayList();
    
    // the set of servers that don't have the data'
    List<String> notUsed = new ArrayList();
    
    // the set of servers that do have the data
    List<String> uploadedTo = new ArrayList();
    // list of servers that were skipped
    List<String> skipped = new ArrayList();
    
    // sockets that errored
    List<String> errors = new ArrayList();
    
    // the number of files uploaded
    int filesUploaded = 0;
    
    // flag for if thigns succeeded
    public boolean uploadSucceeded = false;
    
    
    
    // the data that was uploaded
    byte[] bytesToUpload = null;
    
    public StressAddFileToolChunkEvent(StressAddFileTool aft, BigHash hash, String fileRelativeName, boolean isMetaData) {
        this.aft = aft;
        this.hash = hash;
        this.fileRelativeName = fileRelativeName;
        this.isMetaData = isMetaData;
    }
    
    public int getServersToUploadTo() {
        return serversToUploadTo;
    }
    
}

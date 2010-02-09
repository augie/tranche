/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.tranche.get;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.tranche.hash.BigHash;
import org.tranche.server.PropagationExceptionWrapper;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class GetFileToolFailedChunksListener implements GetFileToolListener {
    
    private final Map<BigHash,Set<BigHash>> missingDataChunks;
    private final Set<BigHash> missingMetaDataChunks;
    
    public GetFileToolFailedChunksListener() {
        this.missingDataChunks = new HashMap();
        this.missingMetaDataChunks = new HashSet();
    }
    
    /**
     * <p>Notification of any other event occurence.</p>
     * @param msg
     */
    public void message(String msg) {
        System.out.println(msg);
    }

    /**
     * <p>Notification that a meta data chunk is starting to be downloaded.</p>
     * @param event
     */
    public void startedMetaData(GetFileToolEvent event) {

    }

    /**
     * <p>Notification that a meta data chunk is trying to be downloaded from a server.</p>
     * @param event
     */
    public void tryingMetaData(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a meta data chunk could not be downloaded.</p>
     * @param event
     */
    public void failedMetaData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        this.missingMetaDataChunks.add(event.getFileHash());
        System.out.println("Meta data chunk failed: " + event.getFileHash());
    }

    /**
     * <p>Notification that a meta data chunk has been downloaded.</p>
     * @param event
     */
    public void finishedMetaData(GetFileToolEvent event) {
    }

    /**
     * <p>Notificatin that a data chunk is starting to be downloaded.</p>
     * @param event
     */
    public void startedData(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a data chunk is trying to be downloaded from a server.</p>
     * @param event
     */
    public void tryingData(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a data chunk could not be downloaded.</p>
     * @param event
     */
    public void failedData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        Set<BigHash> dataChunkSet = this.missingDataChunks.get(event.getFileHash());
        
        if (dataChunkSet == null) {
            dataChunkSet = new HashSet();
            this.missingDataChunks.put(event.getFileHash(), dataChunkSet);
        }
        
        dataChunkSet.add(event.getChunkHash());
        
        System.out.println("Data chunk from file <name: "+event.getFileName()+"; hash:" + event.getFileHash() + "> failed: " + event.getChunkHash());
    }

    /**
     * <p>Notification that a data chunk has been downloaded.</p>
     * @param event
     */
    public void finishedData(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a file is starting to be downloaded.</p>
     * @param event
     */
    public void startingFile(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a file download has started.</p>
     * @param event
     */
    public void startedFile(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a file downloaded has been skipped.</p>
     * @param event
     */
    public void skippedFile(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a file download has finished.</p>
     * @param event
     */
    public void finishedFile(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a file download has failed.</p>
     * @param event
     */
    public void failedFile(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
    }

    /**
     * <p>Notification that a directory download is starting.</p>
     * @param event
     */
    public void startingDirectory(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a directory download has started.</p>
     * @param event
     */
    public void startedDirectory(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a directory download has finished.</p>
     * @param event
     */
    public void finishedDirectory(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a directory download has failed.</p>
     * @param event
     */
    public void failedDirectory(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
    }

    /**
     * <p>Return all the data chunks reported by the tool as missing in an unmodifiable container.</p>
     * <p>Note that the map's key set are the meta data chunks that are missing corresponding data chunks; the associated value set of hashes are the missing data chunks.</p>
     * <p>If a meta data is not missing any associated data chunks, it will not be in this collection.</p>
     * @return
     */
    public Map<BigHash,Set<BigHash>> getMissingDataChunks() {
        return Collections.unmodifiableMap(this.missingDataChunks);
    }
    
    /**
     * <p>Return all the meta data chunks reported by the tool as missing in an unmodifiable container.</p>
     * @return
     */
    public Set<BigHash> getMissingMetaDataChunks() {
        return Collections.unmodifiableSet(this.missingMetaDataChunks);
    }
}

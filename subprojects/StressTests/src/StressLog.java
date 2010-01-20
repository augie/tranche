import java.util.ArrayList;
import java.util.List;
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

/**
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class StressLog {
    
    // Simple protocol for logging. Makes easier, efficient.
    final int GET_CONFIG = 0,
            GET_NONCE = 1,
            GET_HASHES = 2,
            SET_DATA = 3,
            SET_META = 4,
            HAS_DATA = 5,
            HAS_META = 6,
            DELETE_DATA = 7,
            DELETE_META = 8,
            GET_DATA = 9,
            GET_META = 10;
    
    // Attempts
    int numGetConfigurations = 0,
            numGetNonce = 0,
            numGetProjectHashes = 0,
            numSetData = 0,
            numSetMetaData = 0,
            numHasData = 0,
            numHasMetaData = 0,
            numDeleteData = 0,
            numDeleteMetaData = 0,
            numGetData = 0,
            numGetMetaData = 0;
    
    // Fails
    int failGetConfigurations = 0,
            failGetNonce = 0,
            failGetProjectHashes = 0,
            failSetData = 0,
            failSetMetaData = 0,
            failHasData = 0,
            failHasMetaData = 0,
            failDeleteData = 0,
            failDeleteMetaData = 0,
            failGetData = 0,
            failGetMetaData = 0;
    
    // Store execution times for each (milliseconds)
    List<Long> timeGetConfigurations,
            timeGetNonce,
            timeGetProjectHashes,
            timeSetData,
            timeSetMetaData,
            timeHasData,
            timeHasMetaData,
            timeDeleteData,
            timeDeleteMetaData,
            timeGetData,
            timeGetMetaData;
    
    // Total execution time of stress test
    private long start, finish;
    
    // Number of connections logging for stress test
    int connections;
    
    // Just a horizontal rule
    final String HR = "=========================================================";
    
    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    
    public StressLog(int connections) {
        this.connections = connections;
        
        // Build up lists for all the times
        timeGetConfigurations = new ArrayList();
        timeGetNonce = new ArrayList();
        timeGetProjectHashes = new ArrayList();
        timeSetData = new ArrayList();
        timeSetMetaData = new ArrayList();
        timeHasData = new ArrayList();
        timeHasMetaData = new ArrayList();
        timeDeleteData = new ArrayList();
        timeDeleteMetaData = new ArrayList();
        timeGetData = new ArrayList();
        timeGetMetaData = new ArrayList();
    }
    
    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    
    /**
     * Set start time.
     */
    public void start() {
        this.start = System.currentTimeMillis();
    }
    
    /**
     * Set finish time and report findings.
     */
    public void finish() {
        
        this.finish = System.currentTimeMillis();
        
        long total = this.finish - this.start;
        
        long hours, minutes, seconds, milliseconds;
        
        hours = total / (1000*60*60);
        total %= (1000*60*60);
        
        minutes = total / (1000*60);
        total %= (1000*60);
        
        seconds = total / (1000);
        total %= (1000);
        
        milliseconds = total; //!
        
        System.out.println(HR);
        System.out.println("=== Stress test complete");
        System.out.println("=== Ran for " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds, " + milliseconds + " millseconds.");
        System.out.println(HR);
        System.out.println("* Number of connections: " + this.connections);
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // Get nonce
        
        long avgGetNonce = 0;
        for (Long next : this.timeGetNonce)
            avgGetNonce += next.longValue();
        
        if (this.numGetNonce != 0)
            avgGetNonce /= this.numGetNonce;
        
        System.out.println("* GET NONCE:");
        System.out.println("  - Attempts..... " + this.numGetNonce);
        System.out.println("  - fails........ " + this.failGetNonce);
        if (this.numGetNonce != 0)
            System.out.println("  - Avg time..... " + avgGetNonce + " ms.");
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // Get config
        
        long avgGetConfigurations = 0;
        for (Long next : this.timeGetConfigurations)
            avgGetConfigurations += next.longValue();
        
        if (this.numGetConfigurations != 0)
            avgGetConfigurations /= this.numGetConfigurations;
        
        System.out.println("* GET CONFIGURATIONS:");
        System.out.println("  - Attempts..... " + this.numGetConfigurations);
        System.out.println("  - fails........ " + this.failGetConfigurations);
        if (this.numGetConfigurations != 0)
            System.out.println("  - Avg time..... " + avgGetConfigurations + " ms.");
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // Set data
        
        long avgSetData = 0;
        for (Long next : this.timeSetData)
            avgSetData += next.longValue();
        
        if (this.numSetData != 0)
            avgSetData /= this.numSetData;
        
        System.out.println("* SET DATA:");
        System.out.println("  - Attempts..... " + this.numSetData);
        System.out.println("  - fails........ " + this.failSetData);
        if (this.numSetData != 0)
            System.out.println("  - Avg time..... " + avgSetData + " ms.");
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // Has data
        
        long avgHasData = 0;
        for (Long next : this.timeHasData)
            avgHasData += next.longValue();
        
        if (this.numHasData != 0)
            avgHasData /= this.numHasData;
        
        System.out.println("* HAS DATA:");
        System.out.println("  - Attempts..... " + this.numHasData);
        System.out.println("  - fails........ " + this.failHasData);
        if (this.numHasData != 0)
            System.out.println("  - Avg time..... " + avgHasData + " ms.");
        
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // Get data
        
        long avgGetData = 0;
        for (Long next : this.timeGetData)
            avgGetData += next.longValue();
        
        if (this.numGetData != 0)
            avgGetData /= this.numGetData;
        
        System.out.println("* GET DATA:");
        System.out.println("  - Attempts..... " + this.numGetData);
        System.out.println("  - fails........ " + this.failGetData);
        if (this.numGetData != 0)
            System.out.println("  - Avg time..... " + avgGetData + " ms.");
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // Set meta
        
        long avgSetMetaData = 0;
        for (Long next : this.timeSetMetaData)
            avgSetMetaData += next.longValue();
        
        if (this.numSetMetaData != 0)
            avgSetMetaData /= this.numSetMetaData;
        
        System.out.println("* SET META DATA:");
        System.out.println("  - Attempts..... " + this.numSetMetaData);
        System.out.println("  - fails........ " + this.failSetMetaData);
        if (this.numSetMetaData != 0)
            System.out.println("  - Avg time..... " + avgSetMetaData + " ms.");
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // Has meta
        
        long avgHasMetaData = 0;
        for (Long next : this.timeHasMetaData)
            avgHasMetaData += next.longValue();
        
        if (this.numHasMetaData != 0)
            avgHasMetaData /= this.numHasMetaData;
        
        System.out.println("* HAS META DATA:");
        System.out.println("  - Attempts..... " + this.numHasMetaData);
        System.out.println("  - fails........ " + this.failHasMetaData);
        if (this.numHasMetaData != 0)
            System.out.println("  - Avg time..... " + avgHasMetaData + " ms.");
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // Get meta
        
        long avgGetMetaData = 0;
        for (Long next : this.timeGetMetaData)
            avgGetMetaData += next.longValue();
        
        if (this.numGetMetaData != 0)
            avgGetMetaData /= this.numGetMetaData;
        
        System.out.println("* GET META DATA:");
        System.out.println("  - Attempts..... " + this.numGetMetaData);
        System.out.println("  - fails........ " + this.failGetMetaData);
        if (this.numGetMetaData != 0)
            System.out.println("  - Avg time..... " + avgGetMetaData + " ms.");
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // Delete data
        
        long avgDeleteData = 0;
        for (Long next : this.timeDeleteData)
            avgDeleteData += next.longValue();
        
        if (this.numDeleteData != 0)
            avgDeleteData /= this.numDeleteData;
        
        System.out.println("* DELETE DATA:");
        System.out.println("  - Attempts..... " + this.numDeleteData);
        System.out.println("  - fails........ " + this.failDeleteData);
        if (this.numDeleteData != 0)
            System.out.println("  - Avg time..... " + avgDeleteData + " ms.");
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // Delete meta
        
        long avgDeleteMetaData = 0;
        for (Long next : this.timeDeleteMetaData)
            avgDeleteMetaData += next.longValue();
        
        if (this.numDeleteMetaData != 0)
            avgDeleteMetaData /= this.numDeleteMetaData;
        
        System.out.println("* DELETE META DATA:");
        System.out.println("  - Attempts..... " + this.numDeleteMetaData);
        System.out.println("  - fails........ " + this.failDeleteMetaData);
        if (this.numDeleteMetaData != 0)
            System.out.println("  - Avg time..... " + avgDeleteMetaData + " ms.");
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // Get hashes
        
        long avgGetProjectHashes = 0;
        for (Long next : this.timeGetProjectHashes)
            avgGetProjectHashes += next.longValue();
        
        if (this.numGetProjectHashes != 0)
            avgGetProjectHashes /= this.numGetProjectHashes;
        
        System.out.println("* GET HASHES:");
        System.out.println("  - Attempts..... " + this.numGetProjectHashes);
        System.out.println("  - fails........ " + this.failGetProjectHashes);
        if (this.numGetProjectHashes != 0)
            System.out.println("  - Avg time..... " + avgGetProjectHashes + " ms.");
        
        
        System.out.println("\n\n");
        
        // Any errors?
        if (
                this.failDeleteData == 0 && 
                this.failDeleteMetaData == 0 && 
                this.failGetConfigurations == 0 && 
                this.failGetData == 0 &&
                this.failGetMetaData == 0 && 
                this.failGetNonce == 0 &&
                this.failGetProjectHashes == 0 &&
                this.failHasData == 0 &&
                this.failHasMetaData == 0 &&
                this.failSetData == 0 &&
                this.failSetMetaData == 0
                ) {
            System.out.println("CONGRADULATIONS! NO ERRORS");
        }
        
        else
            System.out.println("ERRORS FOUND, SEE ABOVE");
        
        System.out.println(HR + "\n\n\n");
    }
    
    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    /**
     * Log functions
     */
    
    /**
     * Every attempt at server must be recorded
     */
    public synchronized void attempt(final int TOKEN) {
        
        switch(TOKEN) {
            
            case GET_CONFIG:
                this.numGetConfigurations++;
                break;
                
            case GET_NONCE:
                this.numGetNonce++;
                break;
                
            case GET_HASHES:
                this.numGetProjectHashes++;
                break;
                
            case SET_DATA:
                this.numSetData++;
                break;
                
            case SET_META:
                this.numSetMetaData++;
                break;
                
            case HAS_DATA:
                this.numHasData++;
                break;
                
            case HAS_META:
                this.numHasMetaData++;
                break;
                
            case DELETE_DATA:
                this.numDeleteData++;
                break;
                
            case DELETE_META:
                this.numDeleteMetaData++;
                break;
                
            case GET_DATA:
                this.numGetData++;
                break;
                
            case GET_META:
                this.numGetMetaData++;
                break;
                
            default:
                System.err.println("Unknown token " + TOKEN +". Exiting.");
                System.exit(1);
        }
    }
    
    /**
     * Record failure and time to failure.
     */
    
    public synchronized void fail(final int TOKEN, long time) {
        switch(TOKEN) {
            
            case GET_CONFIG:
                this.failGetConfigurations++;
                this.timeGetConfigurations.add(new Long(time));
                break;
                
            case GET_NONCE:
                this.failGetNonce++;
                this.timeGetNonce.add(new Long(time));
                break;
                
            case GET_HASHES:
                this.failGetProjectHashes++;
                this.timeGetProjectHashes.add(new Long(time));
                break;
                
            case SET_DATA:
                this.failSetData++;
                this.timeSetData.add(new Long(time));
                break;
                
            case SET_META:
                this.failSetMetaData++;
                this.timeSetMetaData.add(new Long(time));
                break;
                
            case HAS_DATA:
                this.failHasData++;
                this.timeHasData.add(new Long(time));
                break;
                
            case HAS_META:
                this.failHasMetaData++;
                this.timeHasMetaData.add(new Long(time));
                break;
                
            case DELETE_DATA:
                this.failDeleteData++;
                this.timeDeleteData.add(new Long(time));
                break;
                
            case DELETE_META:
                this.failDeleteMetaData++;
                this.timeDeleteMetaData.add(new Long(time));
                break;
                
            case GET_DATA:
                this.failGetData++;
                this.timeGetData.add(new Long(time));
                break;
                
            case GET_META:
                this.failGetMetaData++;
                this.timeGetMetaData.add(new Long(time));
                break;
                
                
            default:
                System.err.println("Unknown token " + TOKEN +". Exiting.");
                System.exit(1);
        }
    }
    
    /**
     * If pass, just store the completion time.
     */
    public synchronized void pass(final int TOKEN, long time) {
        switch(TOKEN) {
            
            case GET_CONFIG:
                this.timeGetConfigurations.add(new Long(time));
                break;
                
            case GET_NONCE:
                this.timeGetNonce.add(new Long(time));
                break;
                
            case GET_HASHES:
                this.timeGetProjectHashes.add(new Long(time));
                break;
                
            case SET_DATA:
                this.timeSetData.add(new Long(time));
                break;
                
            case SET_META:
                this.timeSetMetaData.add(new Long(time));
                break;
                
            case HAS_DATA:
                this.timeHasData.add(new Long(time));
                break;
                
            case HAS_META:
                this.timeHasMetaData.add(new Long(time));
                break;
                
            case DELETE_DATA:
                this.timeDeleteData.add(new Long(time));
                break;
                
            case DELETE_META:
                this.timeDeleteMetaData.add(new Long(time));
                break;
                
            case GET_DATA:
                this.timeGetData.add(new Long(time));
                break;
                
            case GET_META:
                this.timeGetMetaData.add(new Long(time));
                break;
                
            default:
                System.err.println("Unknown token " + TOKEN +". Exiting.");
                System.exit(1);
        }
    }
}

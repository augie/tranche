/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.tranche.util;

import org.tranche.commons.RandomUtil;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import org.tranche.configuration.Configuration;
import org.tranche.hash.*;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.logs.activity.*;
import org.tranche.network.StatusTable;
import org.tranche.security.Signature;
import org.tranche.server.PropagationReturnWrapper;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class FailingFlatFileTrancheServer extends FlatFileTrancheServer {
    
    private final double failureProbability;

    public FailingFlatFileTrancheServer(String dirPath, X509Certificate cert, PrivateKey key, double failureProbability) {
        
        super(dirPath, cert, key);
        if (failureProbability < 0.0 || failureProbability > 1.0) {
            throw new RuntimeException("Failure probabilty must be a value between 0.0 and 1.0, inclusive. Instead, found: "+failureProbability);
        }
        this.failureProbability = failureProbability;
    }

    public double getFailureProbability() {
        return failureProbability;
    }
    
    private int failureCount = 0;
    
    private void failOccassionally() throws Exception {
        final double dice = RandomUtil.getDouble();
        if (dice <= this.failureProbability) {
            synchronized(this) {
                failureCount++;
            }
            throw new Exception("Dice rolled "+dice+" (less than or equal to "+this.failureProbability+"), so fails.");
        }
    }

    @Override()
    public StatusTable getNetworkStatusPortion(String startHost, String endHost) throws Exception {
        failOccassionally();
        return super.getNetworkStatusPortion(startHost, endHost);
    }

    @Override()
    public PropagationReturnWrapper getNonces(String[] hosts, int count) throws Exception {
        failOccassionally();
        return super.getNonces(hosts, count);
    }

    @Override()
    public boolean[] hasData(BigHash[] hashes) throws Exception {
        failOccassionally();
        return super.hasData(hashes);
    }

    @Override()
    public PropagationReturnWrapper getData(BigHash[] hashes, boolean propagateRequest) throws Exception {
        failOccassionally();
        return super.getData(hashes, propagateRequest);
    }

    @Override()
    public PropagationReturnWrapper setData(BigHash hash, byte[] data, Signature sig, String[] hosts) throws Exception {
        failOccassionally();
        return super.setData(hash, data, sig, hosts);
    }

    @Override()
    public PropagationReturnWrapper deleteData(BigHash hash, Signature[] sigs, byte[][] nonces, String[] hosts) throws Exception {
        failOccassionally();
        return super.deleteData(hash, sigs, nonces, hosts);
    }

    @Override()
    public boolean[] hasMetaData(BigHash[] hashes) throws Exception {
        failOccassionally();
        return super.hasMetaData(hashes);
    }

    @Override()
    public PropagationReturnWrapper getMetaData(BigHash[] hashes, boolean propagateRequest) throws Exception {
        failOccassionally();
        return super.getMetaData(hashes, propagateRequest);
    }

    @Override()
    public PropagationReturnWrapper setMetaData(boolean merge, BigHash hash, byte[] data, Signature sig, String[] hosts) throws Exception {
        failOccassionally();
        return super.setMetaData(merge, hash, data, sig, hosts);
    }

    @Override()
    public PropagationReturnWrapper deleteMetaData(BigHash hash, String uploaderName, Long uploadTimestamp, String relativePathInDataSet, Signature[] sigs, byte[][] nonces, String[] hosts) throws Exception {
        failOccassionally();
        return super.deleteMetaData(hash, uploaderName, uploadTimestamp, relativePathInDataSet, sigs, nonces, hosts);
    }

    @Override()
    public BigHash[] getDataHashes(BigInteger offset, BigInteger length) throws Exception {
        failOccassionally();
        return super.getDataHashes(offset, length);
    }

    @Override()
    public BigHash[] getMetaDataHashes(BigInteger offset, BigInteger length) throws Exception {
        failOccassionally();
        return super.getMetaDataHashes(offset, length);
    }

    @Override()
    public BigHash[] getProjectHashes(BigInteger offset, BigInteger length) throws Exception {
        failOccassionally();
        return super.getProjectHashes(offset, length);
    }

    @Override()
    public Configuration getConfiguration(Signature sig, byte[] nonce) throws Exception {
        failOccassionally();
        return super.getConfiguration(sig, nonce);
    }

    @Override()
    public void setConfiguration(byte[] data, Signature sig, byte[] nonce) throws Exception {
        failOccassionally();
        super.setConfiguration(data, sig, nonce);
    }

    @Override()
    public void registerServer(String url) throws Exception {
        failOccassionally();
        super.registerServer(url);
    }

    @Override()
    public void requestShutdown(Signature sig, byte[] nonce) throws Exception {
        failOccassionally();
        super.requestShutdown(sig, nonce);
    }

    @Override()
    public Activity[] getActivityLogEntries(long startTimestamp, long finishTimestamp, int limit, byte mask) throws Exception {
        failOccassionally();
        return super.getActivityLogEntries(startTimestamp, finishTimestamp, limit, mask);
    }

    @Override()
    public int getActivityLogEntriesCount(long startTimestamp, long stopTimestamp, byte mask) throws Exception {
        failOccassionally();
        return super.getActivityLogEntriesCount(startTimestamp, stopTimestamp, mask);
    }

    public int getFailureCount() {
        return failureCount;
    }
}

/*
 * StressGetFileTool.java
 *
 * Created on October 6, 2007, 7:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package stress.util;

/**
 * Modified GFT to download using 1 RemoteTrancheServer per client thread.
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class StressGetFileTool {
    
//    // Flag to show summary at end. Can be set here and by API.
//    private boolean showDownloadSummary = true;
//    
//    // static lock to prevent disk thrashing
//    protected static Object staticLock = new Object();
//    
//    // Number of concurrent download threads to execute simultaneously
//    private final static int CONCURRENT_THREAD_COUNT = 10;
//    
//    // the cache of the existing meta-data
//    MetaData metaData = null;
//    // the directory download executor (if used)
//    private ThreadPoolExecutor directoryDownloadExecutor = null;
//    // if the content's signatures should be checked
//    private boolean validate = false;
//    // the hash of the content to download
//    private BigHash hash = null;
//    // the file to save this as
//    private File saveAs = null;
//    // the dfs to connect to?
//    private List<String> serversToUse = new ArrayList();
//    // the certificates to trust
//    HashMap<String, X509Certificate> certs = new HashMap<String, X509Certificate>();
//    // the passphrase to use
//    private String passphrase = null;
//    // the padding to use
//    private byte[] padding = new byte[0];
//    // the regular expression to use
//    private String regex = new String(".");
//    private Pattern pattern = Pattern.compile(regex);
//    
//    // Need a global reference to the project root directory, if one
//    private File projectRootDir = null;
//    
//    // keep track of listeners
//    private ArrayList<GetFileToolListener> listeners = new ArrayList();
//    
//    // track the bytes downloaded
//    private BigInteger dataToDownload = BigInteger.ZERO;
//    private BigInteger dataDownloaded = BigInteger.ZERO;
//    
//    // flag for if unexpected files should be purged or not
//    private boolean purgeUnexpectedFiles = true;
//    // flag for if hashes should be recalculated on downloaded files
//    private boolean recalculateHashes = false;
//    
//    // lazy load servers
//    boolean lazyLoadServers = true;
//    
//    // track the errors
//    private final ArrayList<ProjectFilePart> errors = new ArrayList();
//    
//    // the map of currently downloaded trunks
//    private static final HashSet filesBeingDownloaded = new HashSet();
//    
//    // Keep project file around
//    public ProjectFile projectFile = null;
//    
//    // Total number of files to download
//    private static long numFilesToDownload = 0;
//    
//    // Flag used to tell whether get directory was called
//    private static boolean isDirectoryDownload = false;
//    
//    // Some files need to be trashed at end. Should be few -- don't want too many file handles
//    private Set<File> filesToDelete = new HashSet();
//    
//    private static Set<ProjectFilePart> projectParts = null;
//    
//    public Thread caller = null;
//    public StressGetFileTool(Thread caller) {
//        this.caller = caller;
//    }
//    
//    public int getSimultaneousFileDownloadCount() {
//        
//        if (directoryDownloadExecutor != null)
//            return directoryDownloadExecutor.getMaximumPoolSize();
//        else
//            return 1;
//    }
//    
//    // for pausing
//    private List <GetFileTool> gftList = new ArrayList <GetFileTool> ();
//    private boolean isPaused = false;
//    public void setPaused(boolean isPaused) {
//        this.isPaused = isPaused;
//        for (GetFileTool gft : gftList) {
//            gft.setPaused(isPaused);
//        }
//    }
//    public boolean isPaused() {
//        return isPaused;
//    }
//    
//    // for stopping
//    private boolean stop = false;
//    public void stop() {
//        stop = true;
//        for (GetFileTool gft : gftList) {
//            gft.stop();
//        }
//    }
//    
//    public BigInteger getDataToDownload() {
//        return dataToDownload;
//    }
//    
//    public BigInteger getDataDownloaded() {
//        return dataDownloaded;
//    }
//    
//    private synchronized void fireFailedChunk(BigHash fileHash, BigHash chunkHash) {
////        for (GetFileToolListener gftl : listeners) {
////            try {gftl.failedChunk(this, fileHash, chunkHash); } catch (Exception e){ LogUtil.error(e); }
////        }
//    }
//    private synchronized void fireStartedChunkDownload(BigHash fileHash, BigHash chunkHash) {
////        for (GetFileToolListener gftl : listeners) {
////            try { gftl.startedChunkDownload(this, fileHash, chunkHash); } catch (Exception e){ LogUtil.error(e); }
////        }
//    }
//    private synchronized void fireFinishedChunkDownload(BigHash fileHash, BigHash chunkHash) {
////        // inc bytes downloaded by the right amount
////        dataDownloaded = dataDownloaded.add(BigInteger.valueOf(chunkHash.getLength()));
////        // fire all of the listeners
////        for (GetFileToolListener gftl : listeners) {
////            try { gftl.finishedChunkDownload(this, fileHash, chunkHash); } catch (Exception e){ LogUtil.error(e); }
////        }
//    }
//    
//    private synchronized void fireFailedFile(BigHash hash) {
////        for (GetFileToolListener gftl : listeners) {
////            try { gftl.failedFile(this, hash); } catch (Exception e){ LogUtil.error(e); }
////        }
//    }
//    private synchronized void firedSkippedFileDownload(BigHash hash) {
////        // inc bytes downloaded
////        dataDownloaded = dataDownloaded.add(BigInteger.valueOf(hash.getLength()));
////        
////        for (GetFileToolListener gftl : listeners) {
////            try { gftl.skippedFileDownload(this, hash); } catch (Exception e){ LogUtil.error(e); }
////        }
//    }
//    private synchronized void fireStartedFileDownload(BigHash hash, BigHash encodedHash, String relativeName) {
////        
////        if (!this.filePassesFilter(relativeName)) {
////            return;
////        }
////        // adjust by the size of the encoded versus real file
////        if (isDirectoryDownload)
////            dataToDownload = dataToDownload.add(BigInteger.valueOf(encodedHash.getLength())).subtract(BigInteger.valueOf(hash.getLength()));
////        // Single file download
////        else
////            dataToDownload = dataToDownload.add(BigInteger.valueOf(encodedHash.getLength()));
////        
////        // fire all of the listeners
////        for (GetFileToolListener gftl : listeners) {
////            try { gftl.startedFileDownload(this, hash, encodedHash, relativeName); } catch (Exception e){ LogUtil.error(e); }
////        }
//    }
//    private synchronized void fireFinishedFileDownload(BigHash hash) {
////        for (GetFileToolListener gftl : listeners) {
////            try { gftl.finishedFileDownload(this, hash); } catch (Exception e){ LogUtil.error(e); }
////        }
//    }
//    private synchronized void fireStartedDirectoryDownload(BigHash projectFileHash, ProjectFile projectFile) {
////        for (GetFileToolListener gftl : listeners) {
////            try { gftl.startedDirectoryDownload(this, projectFileHash, projectFile); } catch (Exception e){ LogUtil.error(e); }
////        }
//    }
//    private synchronized void fireFinishedDirectoryDownload(BigHash projectFileHash) {
////        for (GetFileToolListener gftl : listeners) {
////            try { gftl.finishedDirectoryDownload(this, projectFileHash); } catch (Exception e){ LogUtil.error(e); }
////        }
//    }
//    
////    /**
////     *Hook for non-Java programs to invoke. This is primarily intended for non-Java programmers to have something that they can script in order to get Tranche data.
////     */
////    public static void main(String[] args) {
////        boolean debug = args.length > 1;
////        // register the bouncy castle code
////        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
////        
////        // check arg
////        if (args.length < 1) {
////            System.out.println("Usage: java -jar GetFileTool.jar <Hash>");
////            System.out.println("        e.g: java -jar GetFileTool.jar -file <file/directory to save as name> <Hash>");
////            System.out.println("        e.g: java -jar GetFileTool.jar -tempDir <temporary directory> <Hash>");
////            System.out.println("        e.g: java -jar GetFileTool.jar -server <server> <Hash>");
////            System.out.println("        e.g: java -jar GetFileTool.jar -server <server> -file <file/directory to save as or into> -validate <true/false> <Hash>");
////            System.out.println("");
////            System.out.println("Options:");
////            System.out.println("  -cert      The file location of a X509Ceriticate to use as a trusted authority. Multiple '-cert' commands may be used. Only content signed by one of the given certificate's will be downloaded.");
////            System.out.println("  -file      The file or directory that the Tranche code should save the data to.");
////            System.out.println("  -validate  A boolean value that denotes whether or not the data should be validated once downloaded.");
////            System.out.println("  -server    The particular server to use.");
////            System.out.println("  -tempDir   A temporary directory to use while downloading the data. When download is complete, the data will be moved to its final destination.");
////            System.out.println("  -regex     The regular expression to select only particular files for download.");
////            return;
////        }
////        
////        try {
////            // make a new file util
////            GetFileTool gft = new GetFileTool();
////            
////            // enable exception logging
////            LogUtil.setEnabled(true);
////            
////            // make a file to save to
////            File saveTo = null;
////            
////            // get the arguments
////            for (int i=0;i<args.length - 1;i+=2) {
////                // if the data's signatures should be checked
////                if (args[i].equals("-validate")) {
////                    try {
////                        gft.setValidate(Boolean.parseBoolean(args[i+1]));
////                    } catch (Exception e){}
////                    continue;
////                }
////                // the destination to download the file/directory to
////                if (args[i].equals("-file")) {
////                    // set any params
////                    saveTo = new File(args[i+1]);
////                    continue;
////                }
////                // the passphrase to use when decrypting the data -- if it is encrypted
////                if (args[i].equals("-passphrase")) {
////                    gft.setPassphrase(args[i+1]);
////                    continue;
////                }
////                // specify the temp directory
////                if (args[i].equals("-tempDir")){
////                    TempFileUtil.setTemporaryDirectory(args[i+1]);
////                    continue;
////                }
////                // specify a particular server to use
////                if (args[i].equals("-server")){
////                    gft.getServersToUse().add(args[i+1]);
////                    continue;
////                }
////                // a X.509 certificate that should be considered trusted
////                if (args[i].equals("-cert")) {
////                    File file = new File(args[i+1]);
////                    if (!file.exists()) {
////                        System.out.println("Can't load X.509 certificate "+file.getAbsolutePath()+". Exiting.");
////                        return;
////                    }
////                    // get the certificate
////                    FileInputStream fis = new FileInputStream(file);
////                    try {
////                        X509Certificate cert = SecurityUtil.getCertificate(fis);
////                        gft.addTrustedCertificate(cert);
////                    } catch (Exception e) {
////                        System.out.println("Can't load X.509 certificate "+file.getAbsolutePath()+". Exiting.");
////                        return;
////                    }
////                    fis.close();
////                }
////                // specify a regular expression for only specific files to download
////                if (args[i].equals("-regex")){
////                    gft.setRegex(args[i+1]);
////                    continue;
////                }
////            }
////            
////            // Set hash
////            gft.setHash(BigHash.createHashFromString(args[args.length-1]));
////            
////            // bootstrap the core servers
////            ServerUtil.waitForStartup();
////            
////            // add a command-line listener
////            gft.getListeners().add(new CommandLineGetFileToolListener(System.out));
////            
////            // if the file is null, set to the default
////            if (saveTo == null) {
////                MetaData md = gft.getMetaData();
////                // if it is a project file, save as such
////                if (md.isProjectFile()) {
////                    saveTo = new File("tranche-download");
////                    saveTo.mkdirs();
////                } else {
////                    saveTo = new File(md.getName());
////                }
////            }
////            
////            // Save single request to the specified file
////            if (saveTo.isDirectory()) {
////                gft.getDirectory(saveTo);
////            } else {
////                gft.getFile(saveTo);
////            }
////            
////            if (gft.getErrors().size() > 0) {
////                System.out.println("A total of " + gft.getErrors().size() + " occurred while downloading the project.");
////                System.out.println("Note that some of the project may have been downloaded, but the entire project was not downloaded.\n");
////                // Message if redirected standard error
////                System.err.println("A total of " + gft.getErrors().size() + " errors occurred while downloading project: " + gft.getHash());
////                
////                // Aware of JUnit tests calling GetFileTool.main
////                if (!TestUtil.isTesting()) {
////                    System.exit(1);
////                }
////            }
////            
////            else {
////                System.out.println("Finished.\n");
////                // Aware of JUnit tests calling GetFileTool.main
////                if (!TestUtil.isTesting()) {
////                    System.exit(0);
////                }
////            }
////            
////            
////        }
////        
////        catch (RuntimeException re) {
////            System.out.println("GetFileTool failed with the following message: " + re.getMessage());
////            // Log it? Probably not if human error.
////            System.exit(1);
////        }
////        
////        // handle can't connect to url exceptions
////        catch (FileNotFoundException ce) {
////            System.out.println("Can't find the file. Please check that it exists.");
////            LogUtil.error(ce);
////            System.exit(1);
////        } catch (Exception e) {
////            System.out.println("Program error. Please run again with the '-debug' flag and send the error to jfalkner@umich.edu");
////            System.out.println(e.getMessage());
////            LogUtil.error(e);
////            System.exit(1);
////        }
////    } // Main
//    
//    private List<String> getServersToTry(final BigHash partHash) {
//        // if lazy load, do so
//        if (lazyLoadServers && !TestUtil.isTesting()) {
//            ServerUtil.waitForStartup();
//        }
//        // if the user didn't specify any, fall back on all
//        if (serversToUse.size() == 0) {
//            // start by adding the known server
//            List<String> dfsUrlsToTry = new ArrayList();
//            // add all specified servers
//            dfsUrlsToTry.addAll(serversToUse);
//            // add all servers with spans
//            dfsUrlsToTry.addAll(ServerUtil.getServersWithHash(partHash));
//            // fall back on all servers
//            List<ServerInfo> allServers = ServerUtil.getServers();
//            for (ServerInfo si : allServers) {
//                dfsUrlsToTry.add(si.getUrl());
//            }
//            return dfsUrlsToTry;
//        }
//        // if the user specified some servers, try to use just them
//        else {
//            // load a list of servers to try
//            List<String> servers = new ArrayList();
//            for (String s : serversToUse) {
//                // register the servers
//                boolean isServerOnline = ServerUtil.isServerOnline(s);
//                if (isServerOnline) {
//                    servers.add(ServerUtil.getServerInfo(s).getUrl());
//                }
//            }
//            return servers;
//        }
//    }
//    
//    /**
//     * <p>Download a project. Set the regular expression if want part of a project, or use getFile to get a single file.</p>
//     * @param saveAs A directory to hold the project files.
//     */
//    public void getDirectory(final File saveAs) throws Exception {
//        
//        final long start = System.currentTimeMillis();
//        
//        // Set the reference to the project root directory. This is important
//        // for getProjectFile
//        this.setProjectRootDir(saveAs);
//        
//        // Flag directory download (to help w/ firing events & estimating download sizes)
//        isDirectoryDownload = true;
//        
//        try {
//            // Grab the project file
//            ProjectFile pf = getProjectFile();
//            // set the data to download
//            if (regex.equals(".")) {
//                dataToDownload = pf.getSize();
//            }
//            // Need to calculate the data to download for filtered project
//            for (ProjectFilePart pfp : pf.getParts()) {
//                if (this.filePassesFilter(pfp.getRelativeName())) {
//                    dataToDownload = dataToDownload.add(BigInteger.valueOf(pfp.getHash().getLength()));
//                }
//            }
//            
//            // fire the start of the project download
//            fireStartedDirectoryDownload(getHash(), pf);
//            
//            // get all the parts
//            Set<ProjectFilePart> parts = pf.getParts();
//            
//            // fire up a downloading executor service
//            ArrayBlockingQueue queue = new ArrayBlockingQueue(100);
//            directoryDownloadExecutor = new ThreadPoolExecutor(CONCURRENT_THREAD_COUNT, CONCURRENT_THREAD_COUNT, 10000, TimeUnit.MILLISECONDS, queue);
//            
//            // get each part
//            for(final ProjectFilePart pfp : parts) {
//                if (filePassesFilter(pfp.getRelativeName())) {
//                    // Make a callable task for every file in project.
//                    Callable task = makeFileDownloadCallable(getErrors(), pfp, saveAs);
//                    boolean submitted = false;
//                    while (!submitted) {
//                        try {
//                            // add to the queue
//                            directoryDownloadExecutor.submit(task);
//                            submitted = true;
//                        } catch (RejectedExecutionException ree) {
//                            // if the queue is full, wait a few
//                            try {
//                                Thread.sleep(100);
//                            } catch (InterruptedException ex) {
//                                // noop
//                            }
//                        }
//                    }
//                }
//            }
//            
//            // shutdown the executor service
//            directoryDownloadExecutor.shutdown();
//            // wait for the shutdown
//            while (!directoryDownloadExecutor.isTerminated()) {
//                directoryDownloadExecutor.awaitTermination(100, TimeUnit.MILLISECONDS);
//            }
//            
//            // list how many are being retried
//            if (getErrors().size() > 0) {
//                System.out.println("Couldn't download "+getErrors().size()+" files. Retrying to download them.");
//                // list the errors to retry
//                ProjectFilePart[] errorsToRetry = getErrors().toArray(new ProjectFilePart[0]);
//                // clear the old queue
//                getErrors().clear();
//                for (ProjectFilePart errorPart : errorsToRetry) {
//                    try {
//                        // make the callable
//                        Callable call = this.makeFileDownloadCallable(getErrors(), errorPart, saveAs);
//                        call.call();
//                    } catch (Exception e){
//                        // noop
//                        System.err.println("*** Error on download retry ***");
//                        System.err.println(e.getMessage());
//                    }
//                }
//            }
//            
//            // fail the remaining parts
//            for (ProjectFilePart errorPart : errors) {
//                System.out.println("Couldn't download "+errorPart.getRelativeName()+", hash: "+errorPart.getHash());
//                this.fireFailedFile(errorPart.getHash());
//            }
//            
//            // fire finished
//            fireFinishedDirectoryDownload(getHash());
//            
//        } finally {
//            // Delete the disk-backed set (if is an instance)
//            try {
//                safeDestroyProjectPartFiles();
//            } catch(Exception e) { /* continue */ }
//            
//            for (File f : filesToDelete)
//                IOUtil.safeDelete(f);
//            
//            // Make sure no confusion if object reused
//            projectFile = null;
//            this.setProjectRootDir(null);
//            isDirectoryDownload = false;
//            
//            if (showDownloadSummary) {
//                System.out.println("Total download time: " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis()-start));
//            }
//        }
//    }
//    
//    private Callable makeFileDownloadCallable(final ArrayList<ProjectFilePart> errors, final ProjectFilePart pfp, final File saveAs) throws FileNotFoundException {
//        // make a new callable
//        Callable task = new Callable() {
//            public Object call() throws Exception {
//                // get the file
//                File part = new File(saveAs, pfp.getRelativeName());
//                // check if the file is already downloaded
//                if (part.exists()) {
//                    // flag for validated
//                    boolean isValid = false;
//                    if (isRecalculateHashes()) {
//                        // make a check hash
//                        BigHash checkHash = new BigHash(part, padding);
//                        // set the flag
//                        isValid = checkHash.compareTo(pfp.getHash()) == 0;
//                    }
//                    // else just check the size
//                    else {
//                        // flag true if the file exists and has the right size
//                        isValid = part.exists() && part.length() == pfp.getPaddingAdjustedLength();
//                    }
//                    // see if the hash matches the unencoded data
//                    if (isValid) {
//                        // notify listeners that the file has been downloaded
//                        firedSkippedFileDownload(pfp.getHash());
//                        
//                        // exit the Callable code
//                        return null;
//                    }
//                }
//                
//                // flag for successfully
//                LinkedList<Exception> localErrors = new LinkedList();
//                // 3 max download attempts
//                int maxDownloadAttempts = 3;
//                for (int downloadAttempts=0;downloadAttempts<maxDownloadAttempts;downloadAttempts++) {
//                    try {
//                        // fall bakc on the GetFileTool to get the file
//                        StressGetFileTool gft = new StressGetFileTool(caller);
//                        if (!isValidate()) {
//                            gft.setValidate(false);
//                        } else {
//                            gft.setValidate(isValidate());
//                            gft.setTrustedCertificates(getTrustedCertificates());
//                        }
//                        // keep the same set of servers to use
//                        gft.setServersToUse(getServersToUse());
//                        gft.setHash(pfp.getHash());
//                        gft.setPadding(pfp.getPadding());
//                        // The passphrase resets the padding!
//                        gft.setPassphrase(getPassphrase());
//                        gft.setProjectRootDir(projectRootDir);
//                        gft.setProjectFile(getProjectFile());
//                        // add a listener to relay chunk events
//                        gft.addListener(new GetFileToolListener(){
//                            boolean firedFile = false;
//                            public void startedChunkDownload(GetFileTool gft, BigHash fileHash, BigHash chunkHash) {
//                                StressGetFileTool.this.fireStartedChunkDownload(fileHash, chunkHash);
//                            }
//                            public void skippedFileDownload(GetFileTool gft, BigHash hash) {}
//                            public void finishedFileDownload(GetFileTool gft, BigHash hash) {}
//                            public void finishedDirectoryDownload(GetFileTool gft, BigHash projectFileHash) {}
//                            public void finishedChunkDownload(GetFileTool gft, BigHash fileHash, BigHash chunkHash) {
//                                StressGetFileTool.this.fireFinishedChunkDownload(fileHash, chunkHash);
//                            }
//                            
//                            public void failedFile(GetFileTool gft, BigHash hash) {}
//                            public void failedChunk(GetFileTool gft, BigHash fileHash, BigHash chunkHash) {
//                                StressGetFileTool.this.fireFailedChunk(fileHash, chunkHash);
//                            }
//                            public void startedDirectoryDownload(GetFileTool gft, BigHash projectFileHash, ProjectFile projectFile) {}
//                            public void startedFileDownload(GetFileTool gft, BigHash hash, BigHash encodedHash, String relativeName) {
//                                if (firedFile) return;
//                                firedFile = true;
//                                StressGetFileTool.this.fireStartedFileDownload(hash, encodedHash, pfp.getRelativeName());
//                            }
//                            public void skippedChunkDownload(GetFileTool gft, BigHash fileHash, BigHash chunkHash) {}
//                        });
//                        
////                        // pause
////                        gftList.add(gft);
////                        gft.setPaused(isPaused);
//                        
//                        // get the file
//                        gft.getFile(part);
//                        
//                        // break out
//                        break;
//                    } catch (Exception e) {
//                        // buffer the error
//                        localErrors.add(e);
//                    }
//                }
//                
//                // check if all were errors
//                if (localErrors.size() >= maxDownloadAttempts) {
//                    // add the part
//                    System.out.println("*** After "+maxDownloadAttempts+" can't download file: "+pfp.getRelativeName()+", hash: "+pfp.getHash().toString()+" ***");
//                    // dump out the errors
//                    for (int errorCount=0;errorCount<localErrors.size();errorCount++) {
//                        System.out.println("Attempt "+errorCount+" Error ");
//                        localErrors.get(errorCount).printStackTrace(System.out);
//                    }
//                    // add as an error
//                    errors.add(pfp);
//                    
//                    // fire failed event
//                    fireFailedFile(pfp.getHash());
//                }
//                // else fire file finished
//                else {
//                    fireFinishedFileDownload(pfp.getHash());
//                }
//                return null;
//            }
//        };
//        return task;
//    }
//    
//    /**
//     * Use if you set a single hash.
//     */
//    public void getFile(final File saveAs) throws Exception {
//        // get the hash being downlaoded
//        BigHash hashBeingDownloaded = getHash();
//        //filesBeingDownloaded
//        while (true) {
//            // synch on the hash
//            synchronized (filesBeingDownloaded) {
//                // if the hash isn't being used, download it
//                if (!filesBeingDownloaded.contains(hashBeingDownloaded)) {
//                    filesBeingDownloaded.add(hashBeingDownloaded);
//                    numFilesToDownload++;
//                    // break out
//                    break;
//                }
//            }
//            
//            // wait a little while in hopes that it clears
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException ex) {
//                // noop
//            }
//        }
//        
//        // make sure to remove the hash -- no matter what
//        try {
//            // set the saveAs
//            this.setSaveAs(saveAs);
//            // check that a URI is present
//            if (getHash() == null) {
//                throw new Exception("A hash is required before downloading a file.");
//            }
//            
//            // reference for the meta-data
//            MetaData md = null;
//            
//            // start by adding the known server
//            List<String> dfsUrlsToTry = this.getServersToTry(getHash());
//            
//            // try all the servers
//            for (String si : dfsUrlsToTry) {
//                // skip off-line servers
//                if (!ServerUtil.isServerOnline(si)) {
//                    continue;
//                }
//                TrancheServer dfs = null;
//                try {
//                    dfs = StressIOUtil.connect(si,caller);
//                    if (!dfs.hasMetaData(getHash())) continue;
//                    // get the meta-data
//                    byte[] metaData = dfs.getMetaData(getHash());
//                    md = MetaDataUtil.read(new ByteArrayInputStream(metaData));
//                    // break out of here
//                    break;
//                }
//                // skip fnf exceptions
//                catch (FileNotFoundException e){
//                    // noop
//                } catch (Exception e) {
//                    ServerUtil.setServerOnlineStatus(si, false);
//                } finally {
//                    IOUtil.safeClose(dfs);
//                }
//            }
//            
//            // if the meta-data is null, raise an exception
//            if (md == null) {
//                throw new RuntimeException("Can't locate meta-data for "+getHash());
//            }
//            
//            // get the encodings
//            List<FileEncoding> encodings = md.getEncodings();
//            final FileEncoding firstEncoding = encodings.get(0);
//            final FileEncoding lastEncoding = encodings.get(encodings.size()-1);
//            
//            // fire the start of the file
//            fireStartedFileDownload(firstEncoding.getHash(), lastEncoding.getHash(), md.getName());
//            boolean fileSuccessfullyDownloaded = false;
//            
//            // check if the file exists
//            try {
//                // check if the file is already downloaded
//                if (saveAs.exists()) {
//                    // make a check hash
//                    BigHash checkHash = new BigHash(saveAs, padding);
//                    // see if the hash matches the unencoded data
//                    if (checkHash.compareTo(firstEncoding.getHash()) == 0) {
////                        firedSkippedFileDownload(firstEncoding.getHash());
//                        // Since already fired start of file download, should
//                        // use the last encoding (already adjusted)
//                        firedSkippedFileDownload(lastEncoding.getHash());
//                        fileSuccessfullyDownloaded = true;
//                        return;
//                    }
//                    
//                    // Doesn't match, delete
//                    IOUtil.safeDelete(saveAs);
//                }
//                
//                /**
//                 * Following three scenarios handled separately:
//                 * 1) Tarball: downloaded in memory one-at-a-time
//                 * 2) Less than a MB: downloading in memory
//                 * 3) Greater than/eq MB: downloaded and buffered to disk
//                 */
//                boolean isTarball = false;
//                for (FileEncoding enc : encodings) {
//                    if (enc.getName().equals(FileEncoding.TARBALL)) {
//                        isTarball = true;
//                        break;
//                    }
//                }
//                
//                if (isTarball) {
//                    downloadAndDecodeTarballQueue(saveAs,encodings,firstEncoding,lastEncoding,md);
//                } else if (lastEncoding.getHash().getLength() < DataBlockUtil.ONE_MB) {
//                    // download and decode the file using a disk-based backing. handles files of any size.
//                    downloadAndDecodeInMemory(saveAs, encodings, firstEncoding, lastEncoding, md);
//                } else {
//                    // download and decode the file using a disk-based backing. handles files of any size.
//                    downloadAndDecodeFileBacked(saveAs, encodings, firstEncoding, lastEncoding, md);
//                }
//                
//                // verify that the contents match
//                BigHash verifyHash = new BigHash(saveAs, padding);
//                
//                if (verifyHash.compareTo(firstEncoding.getHash()) != 0) {
//                    // Only bother if failed w/ padding
//                    BigHash noPaddingHash = new BigHash(saveAs);
//                    
//                    // For files in a tarball, there may not have been individual padding, but
//                    // there may have been padding on the tarball. If matches w/o padding,
//                    // then there wasn't a problem
//                    if (noPaddingHash.compareTo(firstEncoding.getHash()) != 0) {
//                        System.out.println("Verification of file failed: ");
//                        System.out.println(" - Expecting......... " + firstEncoding.getHash());
//                        System.out.println(" - Found............. " + verifyHash);
//                        System.out.println(" - Without padding... " + noPaddingHash);
//                        
//                        throw new RuntimeException("Decoded file does not match the expected file!\n  Expected: "+getHash()+"\n  Found: "+verifyHash);
//                    } // Hash doesn't match (w/o padding)
//                } // Hash doesn't match (w/ padding)
//                
//                // if not validation, get the content
//                if (!isValidate()) {
//                    // fire that the file has finished downloading
//                    fireFinishedFileDownload(firstEncoding.getHash());
//                    fileSuccessfullyDownloaded = true;
//                    return;
//                }
//                
//                // the good signature
//                Signature goodSignature = null;
//                X509Certificate goodCertificate = null;
//                
//                // check each sig
//                for (Signature sig : md.getSignatures()) {
//                    // make the MD5 name
//                    String name = SecurityUtil.getMD5Name(sig.getCert());
//                    // try to grab that cert
//                    X509Certificate cert = getTrustedCertificate(name);
//                    // if the cert isn't known, skip
//                    if (cert != null) {
//                        goodSignature = sig;
//                        goodCertificate = cert;
//                        break;
//                    }
//                }
//                
//                // if not found, throw an exception
//                if (goodSignature == null || goodCertificate == null) {
//                    // fire the failed
//                    throw new IOException("Can't validate signature!");
//                }
//                
//                // verify the file's contents
//                FileInputStream verify = null;
//                try {
//                    // get the unencoded data
//                    verify = new FileInputStream(saveAs);
//                    // verify that it matches what is expected
//                    if(!SecurityUtil.verify(verify, goodSignature.getBytes(), goodSignature.getAlgorithm(), goodCertificate.getPublicKey())) {
//                        // pitch the exception
//                        throw new CantVerifySignatureException();
//                    }
//                } finally {
//                    IOUtil.safeClose(verify);
//                }
//                
//                // fire that the file has finished
//                fireFinishedFileDownload(firstEncoding.getHash());
//                fileSuccessfullyDownloaded = true;
//            } finally {
//                // check if the file was a success
//                if (!fileSuccessfullyDownloaded) {
//                    fireFailedFile(firstEncoding.getHash());
//                }
//            }
//            
//        } finally {
//            // remove that hash
//            filesBeingDownloaded.remove(hashBeingDownloaded);
//        }
//    }
//    
//    /**
//     * Helper method to synchronize downloads for tarball
//     */
//    public void downloadAndDecodeTarballQueue(final File saveAs, final List<FileEncoding> encodings, final FileEncoding firstEncoding, final FileEncoding lastEncoding, final MetaData md) throws Exception {
//        
//        // Synchronize so only one download at a time
//        synchronized(staticLock) {
//            
//            // Recheck whether files exists now lock was released
//            if (saveAs.exists()) {
//                // make a check hash
//                BigHash checkHash = new BigHash(saveAs, padding);
//                // see if the hash matches the unencoded data
//                if (checkHash.compareTo(firstEncoding.getHash()) == 0) {
//                    // Since already fired start of file download, should
//                    // use the last encoding (already adjusted)
//                    firedSkippedFileDownload(lastEncoding.getHash());
//                    return;
//                }
//                
//                // Doesn't match, delete
//                IOUtil.safeDelete(saveAs);
//            }
//            
//            /**
//             * Tarball should be less than 1MB, but just in case
//             */
//            if (lastEncoding.getHash().getLength() < DataBlockUtil.ONE_MB) {
//                // download and decode the file using a disk-based backing. handles files of any size.
//                downloadAndDecodeInMemory(saveAs, encodings, firstEncoding, lastEncoding, md);
//            } else {
//                // download and decode the file using a disk-based backing. handles files of any size.
//                downloadAndDecodeFileBacked(saveAs, encodings, firstEncoding, lastEncoding, md);
//            }
//        }
//    }
//    
//    /**
//     * A helper method that handles the slightly ugly task of downloading files of any size and using temp files on the hard disk for intermediate steps. This is relatively slow for small files compared to the size of data being moved around, which is why the downloadAndDecodeInMemory() method is available.
//     */
//    private void downloadAndDecodeFileBacked(final File saveAs, final List<FileEncoding> encodings, final FileEncoding firstEncoding, final FileEncoding lastEncoding, final MetaData md) throws Exception {
//        // make a temp file for the encoded file -- recycle any old ones
//        final File tempFile;
//        
//        if (saveAs.exists()) {
//            // File already in location, skip
//            return;
//        } else {
//            tempFile = TempFileUtil.createTemporaryFile(lastEncoding.getHash());
//        }
//        
//        try {
//            
//            // a reference for the expected length
//            final long expectedLength = lastEncoding.getHash().getLength();
//            
//            // fire up a downloading executor service
//            ArrayBlockingQueue queue = new ArrayBlockingQueue(10);
//            ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 3, Long.MAX_VALUE, TimeUnit.MILLISECONDS, queue);
//            
//            // a queue for the parts to write
//            final int partsToDownload = md.getParts().size();
//            final ArrayBlockingQueue<ChunkToDownload> chunksToSaveQueue = new ArrayBlockingQueue(10);
//            final boolean[] runFileSavingThread = new boolean[1];
//            runFileSavingThread[0] = true;
//            // spawn a thread to handle all of the downloads
//            Thread fileSavingThread = new Thread() {
//                public void run() {
//                    // open a file for writing
//                    RandomAccessFile raf = null;
//                    try {
//                        raf = new RandomAccessFile(tempFile, "rw");
//                        // set the size
//                        raf.setLength(expectedLength);
//                        // save all of the parts, taking care to place them appropriately
//                        for (int i=0;chunksToSaveQueue.size() > 0 || runFileSavingThread[0];i++) {
//                            ChunkToDownload ctd = null;
//                            while (ctd==null) {
//                                synchronized (chunksToSaveQueue) {
//                                    ctd = chunksToSaveQueue.poll();
//                                }
//                                // if null, sleep a little
//                                if (ctd == null) {
//                                    if (!runFileSavingThread[0] && chunksToSaveQueue.size() == 0) {
//                                        return;
//                                    } else {
//                                        Thread.sleep(100);
//                                    }
//                                }
//                            }
//                            
//                            // write to the file
//                            raf.seek(ctd.offset);
//                            raf.write(ctd.bytes);
//                        }
//                    } catch (Exception e){
//                        System.err.println("*** Can't download file's chunks! ***");
//                        e.printStackTrace(System.err);
//                    } finally {
//                        // close
//                        try {raf.close();} catch (Exception e){}
//                    }
//                }
//            };
//            // set as daemon
//            fileSavingThread.setName("GetFileTool File Saving Thread: "+firstEncoding.getName());
//            fileSavingThread.setDaemon(true);
//            fileSavingThread.start();
//            
//            // download each of the required chunks
//            ArrayList<BigHash> parts = md.getParts();
//            // queue of the parts to download
//            ArrayList<ChunkToDownload> chunksToDownload = new ArrayList();
//            // read through the existing file -- recycle any existing chunks
//            FileInputStream fis = null;
//            BufferedInputStream bis = null;
//            try {
//                // make the streams
//                fis = new FileInputStream(tempFile);
//                bis = new BufferedInputStream(fis);
//                
//                int partsHandled = 0;
//                // the bytes to read
//                int bytesToRead = DataBlockUtil.ONE_MB;
//                if (expectedLength < bytesToRead) bytesToRead = (int)expectedLength;
//                byte[] buffer = new byte[bytesToRead];
//                // get the bytes
//                int bytesRead = IOUtil.getBytes(bis, buffer);
//                while (partsHandled < parts.size()) {
//                    // get the expected hash
//                    final BigHash partHash = parts.get(partsHandled);
//                    
//                    // check that the data matches the hash
//                    BigHash partCheck = new BigHash(buffer);
//                    // if they don't match, download this chunk
//                    if (partCheck.compareTo(partHash) != 0) {
//                        // make the part handle id
//                        final int partID = partsHandled;
//                        
//                        // queue up the chunk
//                        ChunkToDownload ctd = new ChunkToDownload(partID*DataBlockUtil.ONE_MB, null, partHash);
//                        chunksToDownload.add(ctd);
//                    }
//                    
//                    // inc the parts handled
//                    partsHandled++;
//                }
//            } finally {
//                IOUtil.safeClose(bis);
//                IOUtil.safeClose(fis);
//            }
//            
//            // track the failed chunks
//            final ArrayList<ChunkToDownload> failedChunks = new ArrayList();
//            
//            // download all of the parts that need to be
//            while (chunksToDownload.size() > 0) {
//                // get the chunk
//                final ChunkToDownload ctd = chunksToDownload.remove(0);
//                // make a thread that can be used to download the chunk
//                Thread t = makeChunkDownloadThread(failedChunks, firstEncoding, ctd, chunksToSaveQueue);
//                // add the part for download
//                boolean executed = false;
//                while (!executed) {
//                    try {
//                        executor.submit(t);
//                        executed = true;
//                    } catch (RejectedExecutionException ree) {
//                        try {
//                            // sleep a little
//                            Thread.sleep(100);
//                        } catch (InterruptedException ex) {
//                            //noop
//                        }
//                    }
//                }
//            }
//            
//            // shutdown the service -- it'll finish all the current threads
//            executor.shutdown();
//            // wait for the shutdown to complete
//            while (!executor.isTerminated()) {
//                executor.awaitTermination(100, TimeUnit.MILLISECONDS);
//            }
//            
//            // go back and try to get each of the chunks again
//            ChunkToDownload[] tryAgainChunks = failedChunks.toArray(new ChunkToDownload[0]);
//            // clear out the error queue
//            failedChunks.clear();
//            for (ChunkToDownload ctd : tryAgainChunks) {
//                // make a thread that can be used to download the chunk
//                Thread t = makeChunkDownloadThread(failedChunks, firstEncoding, ctd, chunksToSaveQueue);
//                // start the thread
//                t.start();
//                // wait for it to complete
//                t.join();
//            }
//            // purge the chunks
//            tryAgainChunks = null;
//            
//            // join the file saving thread as long as it has work to do
//            runFileSavingThread[0] = false;
//            fileSavingThread.join();
//            
//            // if there are any more errors, report them
//            if (failedChunks.size() > 0) {
//                System.out.println("*** Failed to find the following chunks on-line ***");
//                for (ChunkToDownload ctd : failedChunks) {
//                    System.out.println(ctd.hash+", offset: "+ctd.offset+", length: "+ctd.hash.getLength());
//                }
//                throw new Exception("Can't download file. Missing "+failedChunks.size()+" chunks in the file.");
//            }
//            
//            // synchronized helper method to decode files
//            decodeFile(tempFile, firstEncoding, saveAs, encodings);
//        } finally {
//            IOUtil.safeDelete(tempFile);
//        }
//    }
//    
//    /**
//     * A helper method that handles the slightly ugly task of downloading files of any size and using temp files on the hard disk for intermediate steps. This is relatively slow for small files compared to the size of data being moved around, which is why the downloadAndDecodeInMemory() method is available.
//     */
//    private void downloadAndDecodeInMemory(final File saveAs, final List<FileEncoding> encodings, final FileEncoding firstEncoding, final FileEncoding lastEncoding, final MetaData md) throws Exception {
//        
//        // download each of the required chunks
//        ArrayList<BigHash> parts = md.getParts();
//        // download each part
//        for (BigHash hash : parts) {
//            // fire the start of the chunk
//            fireStartedChunkDownload(firstEncoding.getHash(), hash);
//            
//            // the bytes to download
//            byte[] bytes = null;
//            
//            if (saveAs.exists()) {
//                // File in correct place, done
//                return;
//            } else {
//                // get the ordered set of servers to try
//                List<String> servers = getServersToTry(hash);
//                // try each of the servers
//                for (String si : servers) {
//                    
//                    // pause
//                    while (isPaused) {
//                        Thread.sleep(500);
//                    }
//                    
//                    // download the data
//                    try {
//                        // stop
//                        if (stop) {
//                            Thread.currentThread().interrupt();
//                            return;
//                        }
//                        
//                        // make a reference to the DFS
//                        TrancheServer dfs = null;
//                        try {
//                            // connect to the DFS
//                            dfs = StressIOUtil.connect(si,caller);
//                            
//                            // get the data
//                            bytes = dfs.getData(hash);
//                            
//                            // pause
//                            while (isPaused) {
//                                Thread.sleep(500);
//                            }
//                            
//                            // stop
//                            if (stop) {
//                                Thread.currentThread().interrupt();
//                                return;
//                            }
//                            
//                            // finished
//                            fireFinishedChunkDownload(firstEncoding.getHash(), hash);
//                            
//                            // break out
//                            break;
//                        } finally {
//                            IOUtil.safeClose(dfs);
//                        }
//                    } catch (Exception e) {
//                        // skip errors, they might be meaningless here
//                    }
//                }
//            }
//            
//            // buffer all of the bytes
//            if (bytes == null) {
//                // if here, the chunk wasn't downloaded
//                fireFailedChunk(firstEncoding.getHash(), hash);
//                // pitch an exception
//                throw new Exception("Can't download file "+hash);
//            }
//            
//            // decode in memory
//            decodeInMemory(bytes, firstEncoding, saveAs, encodings);
//        }
//    }
//    
//    // synchronized to prevent disk thrashing
//    private synchronized void decodeInMemory(byte[] bytes, final FileEncoding firstEncoding, final File saveAs, final List<FileEncoding> encodings) throws RuntimeException, IOException, FileNotFoundException, GeneralSecurityException, Exception {
//        // reverse so things are decoded in the correct order
//        Collections.reverse(encodings);
//        
//        // decode the file
//        for (FileEncoding fe : encodings) {
//            // If auto tarballed, helper method will do all the work
//            if (fe.getName().equals(FileEncoding.TARBALL)) {
//                getFilesFromTarballBytes(bytes,saveAs);
//                return;
//            }
//            // handle the encodings
//            if (fe.getName().equals(FileEncoding.GZIP)) {
//                // save to a temp file
//                bytes = CompressionUtil.gzipDecompress(bytes);
//            }
//            // handle the encodings
//            if (fe.getName().equals(FileEncoding.LZMA)) {
//                // file for storing the decoded bytes
//                File temp = TempFileUtil.createTemporaryFile();
//                // file for storing the encoded bytes
//                File tempFile = TempFileUtil.createTemporaryFile();
//                try {
//                    FileOutputStream fos = new FileOutputStream(tempFile);
//                    try {
//                        fos.write(bytes);
//                    } finally {
//                        IOUtil.safeClose(fos);
//                    }
//                    
//                    // manually decompress the data
//                    LzmaAlone.main(new String[] {"d", tempFile.getCanonicalPath(), temp.getCanonicalPath()});
//                    
//                    // get the bytes back in memory
//                    bytes = IOUtil.getBytes(temp);
//                } finally {
//                    IOUtil.safeDelete(temp);
//                    IOUtil.safeDelete(tempFile);
//                }
//                
//            }
//            // if it is AES, decrypt it
//            if (fe.getName().equals(FileEncoding.AES)) {
//                bytes = unencodeAESBytes(bytes,fe);
//            }
//        }
//        
//        // lastly, trim off any padding
//        if (padding.length > 0) {
//            // make the trimmed array
//            byte[] trimmedBytes = new byte[bytes.length-padding.length];
//            System.arraycopy(bytes, 0, trimmedBytes, 0, trimmedBytes.length);
//            // swap the references
//            bytes = trimmedBytes;
//        }
//        
//        // finally, save the decoded bytes to the expected file
//        IOUtil.setBytes(bytes, saveAs);
//    }
//    
//    private synchronized void decodeFile(File tempFile, final FileEncoding firstEncoding, final File saveAs, final List<FileEncoding> encodings) throws RuntimeException, IOException, FileNotFoundException, GeneralSecurityException, Exception {
//        // reverse so things are decoded in the correct order
//        Collections.reverse(encodings);
//        
//        // decode the file
//        for (FileEncoding fe : encodings) {
//            // If auto tarballed, helper method will do all the work
//            if (fe.getName().equals(FileEncoding.TARBALL)) {
//                getFilesFromTarball(tempFile,saveAs);
//                return;
//            }
//            
//            // handle the encodings
//            if (fe.getName().equals(FileEncoding.GZIP)) {
//                // save to a temp file
//                File temp = CompressionUtil.gzipDecompress(tempFile);
//                
//                // keep a reference to the old file
//                File todelete = tempFile;
//                // swap references
//                tempFile = temp;
//                // safely delete the old -- this is requried because the code seems to occasionally error here on IOUtil.renameFallBackCopy()
//                IOUtil.safeDelete(todelete);
//            }
//            // handle the encodings
//            if (fe.getName().equals(FileEncoding.LZMA)) {
//                // save to a temp file
//                File temp = TempFileUtil.createTemporaryFile();
//                
//                // manually decompress the data
//                LzmaAlone.main(new String[] {"d", tempFile.getCanonicalPath(), temp.getCanonicalPath()});
//                
//                // keep a reference to the old file
//                File todelete = tempFile;
//                // swap references
//                tempFile = temp;
//                // safely delete the old -- this is requried because the code seems to occasionally error here on IOUtil.renameFallBackCopy()
//                IOUtil.safeDelete(todelete);
//            }
//            // if it is AES, decrypt it
//            if (fe.getName().equals(FileEncoding.AES)) {
//                tempFile = unencodeAESFile(tempFile,fe);
//            }
//        }
//        
//        // lastly, trim off any padding
//        if (padding.length > 0) {
//            RandomAccessFile raf = null;
//            try {
//                raf = new RandomAccessFile(tempFile, "rw");
//                raf.setLength(tempFile.length()-padding.length);
//            } catch (IOException ex) {
//                // try to close the raf
//                try { raf.close(); } catch (Exception e){}
//                // fallback on manually copying -- shouldn't ever make it here
//                FileInputStream fis = null;
//                FileOutputStream fos = null;
//                try {
//                    fis = new FileInputStream(tempFile);
//                    File trimTempFile = TempFileUtil.createTemporaryFile();
//                    fos = new FileOutputStream(trimTempFile);
//                    IOUtil.getBytes(fos, fis, tempFile.length()-padding.length);
//                    // update the references
//                    IOUtil.safeDelete(tempFile);
//                    tempFile = trimTempFile;
//                } catch (IOException exx) {
//                    throw new RuntimeException("Can't trim the decoded file!?", exx);
//                } finally {
//                    IOUtil.safeClose(fos);
//                    IOUtil.safeClose(fis);
//                }
//            } finally {
//                try { raf.close(); } catch (Exception e){}
//            }
//        }
//        
//        // finally, rename the decoded file to the expected one
//        IOUtil.renameFallbackCopy(tempFile, saveAs);
//    }
//    
//    /**
//     * Sets file from tarball to correct location.
//     */
//    
//    private void getFilesFromTarball(File tarball, final File desiredFile) throws Exception {
//        
//        synchronized(staticLock) {
//            
//            // Just got lock. If file exists, tarball already exploded, nothing to do!
//            if (desiredFile.exists()) {
//                IOUtil.safeDelete(tarball);
//                return;
//            }
//            
//            if (!tarball.exists()) {
//                return;
//            }
//            
//            // If padding, need to adjust file so doesn't contain the padded bytes
//            if (padding.length > 0) {
//                byte[] tarballBytes = IOUtil.getBytes(tarball);
//                // make the trimmed array
//                byte[] trimmedBytes = new byte[tarballBytes.length-padding.length];
//                System.arraycopy(tarballBytes, 0, trimmedBytes, 0, trimmedBytes.length);
//                // Should clobber old content
//                IOUtil.setBytes(trimmedBytes, tarball);
//            }
//            
//            // Must guarentee file ends w/ tar.gz
//            if (!tarball.getName().endsWith("tar.gz") && !tarball.getName().endsWith(".tgz")) {
//                File temp = new File(tarball.getAbsolutePath() + ".tar.gz");
//                boolean renamed = tarball.renameTo(temp);
//                
//                // Only if rename doesn't work
//                if (!renamed || !temp.exists()) {
//                    byte[] tarBytes = IOUtil.getBytes(tarball);
//                    IOUtil.setBytes(tarBytes,temp);
//                    IOUtil.safeDelete(tarball);
//                    tarball = temp;
//                }
//                // Make sure reference is swapped no matter what. This may happen
//                // if the tarball has already been downloaded by another thread.
//                else if (!tarball.getName().equals(temp.getName())) {
//                    tarball = temp;
//                }
//            }
//            
//            List<File> explodedFiles = ArchiveUtil.explodeTarball(tarball);
//            
//            // No longer need the tarball
//            IOUtil.safeDelete(tarball);
//            
//            // The search map. If found, removed from collection. Any remaining
//            // entries are deleted (likely scenario: part of download but
//            // GFT has a regex filter running)
//            Map<BigHash,File> explodedHashesWithFiles = new HashMap();
//            
//            // Going to explode each file to correct download location.
//            for (File f : explodedFiles) {
//                // Check whether file matches the filter
//                if (!filePassesFilter(f.getName())) {
//                    IOUtil.safeDelete(f);
//                    continue;
//                }
//                
//                /**
//                 * Need the hash to match. So that the hash is the same w/ or w/o
//                 * auto tarballing, padding is accounted for by project file part.
//                 * As a result, if there is padding, must add padding to get
//                 * expected hash.
//                 */
//                BigHash foundFileHash;
//                if (padding.length > 0) {
//                    byte[] fileBytes = IOUtil.getBytes(f);
//                    byte[] withPaddingBytes = new byte[fileBytes.length+padding.length];
//                    System.arraycopy(fileBytes,0,withPaddingBytes,0,fileBytes.length);
//                    System.arraycopy(padding,0,withPaddingBytes,fileBytes.length,padding.length);
//                    foundFileHash = new BigHash(withPaddingBytes);
//                } else {
//                    foundFileHash = new BigHash(f);
//                }
//                
//                explodedHashesWithFiles.put(foundFileHash,f);
//            } // For each exploded file
//            
//            // Match the file against project file parts.
//            // (Iterate disk-backed set to keep disk accesses lower.)
//            File nextFile = null;
//            BigHash nextHash = null;
//            for (ProjectFilePart part : this.getProjectFile(true).getParts()) {
//                
//                // Break out of loop if finished
//                if (explodedHashesWithFiles.isEmpty()) break;
//                
//                // Reset the iterator vars so no confusion
//                nextFile = null;
//                nextHash = null;
//                
//                // Try and find matching hash
//                for (BigHash h : explodedHashesWithFiles.keySet()) {
//                    if (h.equals(part.getHash())) {
//                        // Store reference to big hash
//                        nextHash = h;
//                        nextFile = explodedHashesWithFiles.get(nextHash);
//                        break;
//                    }
//                }
//                
//                // If found, put the file in download location and remove from
//                // search map
//                if (nextFile != null) {
//                    
//                    // Unfortunately, can't remove entry from map in loop above
//                    // or get ConcurrentModificationException, so do it here
//                    explodedHashesWithFiles.remove(nextHash);
//                    
//                    // Place in correct location
//                    File dest;
//                    
//                    // The preferred scenario is that the root dir is set
//                    if (this.projectRootDir != null)
//                        dest = new File(this.projectRootDir,part.getRelativeName());
//                    // The proj root dir may not be set (e.g., single file download).
//                    // Use parent of the saveAs file
//                    else
//                        dest = new File(desiredFile.getParent(),part.getRelativeName());
//                    
//                    // Files are under 1MB, no need to buffer
//                    byte[] bytes = IOUtil.getBytes(nextFile);
//                    IOUtil.setBytes(bytes,dest);
//                    
//                    // TODO If this project is a SVN project, two copies are kept,
//                    // meaning one isn't found. However, if put file back, very
//                    // long loop conditions.
//                }
//            }
//            
//            // If any files left, delete
//            for (BigHash h : explodedHashesWithFiles.keySet()) {
//                IOUtil.safeDelete(explodedHashesWithFiles.get(h));
//            }
//            
//        } // static lock
//    } // getFilesFromTarball
//    
//    /**
//     * Extracts file's bytes from tarball, caches in ExplodedFilePool.
//     * Synchronized to avoid duplicated work.
//     */
//    private void getFilesFromTarballBytes(byte[] tarballBytes,final File desiredFile) throws Exception {
//        
//        File tarball = TempFileUtil.createTemporaryFile(".tar.gz");
//        IOUtil.setBytes(tarballBytes,tarball);
//        
//        getFilesFromTarball(tarball,desiredFile);
//    }
//    
//    private byte[] unencodeAESBytes(byte[] bytes, FileEncoding fe) throws IOException, GeneralSecurityException {
//        // get the passphrase
//        String passphrase = getPassphrase();
//        if (passphrase == null) {
//            passphrase = fe.getProperty(FileEncoding.PROP_PASSPHRASE);
//        }
//        // check for passphrase
//        if (passphrase == null) {
//            throw new RuntimeException("Can't decrypt the file! No passphrase specified.");
//        }
//        // save to a temp file
//        return SecurityUtil.decrypt(passphrase, bytes);
//    }
//    
//    /**
//     * Returns the unencoded AES file. Also deletes file of encoded file.
//     */
//    private File unencodeAESFile(File file, FileEncoding fe) throws IOException,GeneralSecurityException {
//        String passphrase = getPassphrase();
//        if (passphrase == null) {
//            passphrase = fe.getProperty(FileEncoding.PROP_PASSPHRASE);
//        }
//        // check for passphrase
//        if (passphrase == null) {
//            throw new RuntimeException("Can't decrypt the file! No passphrase specified.");
//        }
//        // save to a temp file
//        File decrypted = SecurityUtil.decrypt(passphrase, file);
//        IOUtil.safeDelete(file);
//        
//        return decrypted;
//    }
//    
//    private Thread makeChunkDownloadThread(final ArrayList<ChunkToDownload> failedChunks, final FileEncoding firstEncoding, final ChunkToDownload ctd, final ArrayBlockingQueue<ChunkToDownload> chunksToSaveQueue) {
//        // spawn a new thread to get the chunk
//        Thread t = new Thread("GetFileTool ChunkDownloadThread: "+firstEncoding.getName()) {
//            public void run() {
//                // fire the start of the chunk
//                fireStartedChunkDownload(firstEncoding.getHash(), ctd.hash);
//                // get the ordered set of servers to try
//                List<String> servers = getServersToTry(ctd.hash);
//                // try each of the servers
//                for (String si : servers) {
//                    
//                    // pause
//                    while (isPaused) {
//                        try {
//                            Thread.sleep(500);
//                        } catch (Exception e) {}
//                    }
//                    
//                    // download the data
//                    try {
//                        // stop
//                        if (stop) {
//                            Thread.currentThread().interrupt();
//                            return;
//                        }
//                        
//                        // make a reference to the DFS
//                        TrancheServer dfs = null;
//                        try {
//                            // connect to the DFS
//                            dfs = StressIOUtil.connect(si,caller);
//                            
//                            // get the data
//                            ctd.bytes = dfs.getData(ctd.hash);
//                            // queue the data for writing
//                            synchronized (chunksToSaveQueue) {
//                                chunksToSaveQueue.add(ctd);
//                            }
//                            
//                            // pause
//                            while (isPaused) {
//                                Thread.sleep(500);
//                            }
//                            
//                            // stop
//                            if (stop) {
//                                Thread.currentThread().interrupt();
//                                return;
//                            }
//                            
//                            // finished
//                            fireFinishedChunkDownload(firstEncoding.getHash(), ctd.hash);
//                            
//                            // return
//                            return;
//                        } finally {
//                            IOUtil.safeClose(dfs);
//                        }
//                    } catch (Exception e) {
//                        // skip errors, they might be meaningless here
//                    }
//                }
//                // if here, the chunk wasn't downloaded
//                fireFailedChunk(firstEncoding.getHash(), ctd.hash);
//                
//                // if we made it here, queue the error
//                synchronized(failedChunks) {
//                    failedChunks.add(ctd);
//                }
//            }
//        };
//        return t;
//    }
//    
//    /**
//     * Returns meta data for project or file.
//     */
//    public synchronized MetaData getMetaData() throws IOException {
//        return this.getMetaData(getHash());
//    }
//    
//    private synchronized MetaData getMetaData(BigHash meta) throws IOException {
//        // check if the meta-data is null
//        if (metaData != null) {
//            return metaData;
//        }
//        
//        // try each of the urls
//        List<String> dfsUrlsToTry = getServersToTry(meta);
//        
//        // remove urls that are known to be bad.
//        for (Iterator<String> it = dfsUrlsToTry.iterator();it.hasNext();) {
//            // check the URL
//            String s = it.next();
//            if (!ServerUtil.isServerOnline(s)) {
//                it.remove();
//            }
//        }
//        
//        // try each url until the file is found or none can find it
//        for (String toTry : dfsUrlsToTry) {
//            if (metaData != null) break;
//            
//            // reopen it
//            TrancheServer dfs = null;
//            try {
//                dfs = StressIOUtil.connect(toTry,caller);
//                // check for meta-data
//                if (!dfs.hasMetaData(meta)) continue;
//                // get the meta-data bytes
//                byte[] metaDataBytes = dfs.getMetaData(meta);
//                // convert to an object
//                metaData = MetaDataUtil.read(new ByteArrayInputStream(metaDataBytes));
//                // the file
//                break;
//            } catch (FileNotFoundException e) {
//                // ignore
//            } catch (Exception e) {
//                // try the next
//                ServerUtil.setServerOnlineStatus(toTry, false);
//                continue;
//            } finally {
//                IOUtil.safeClose(dfs);
//            }
//        }
//        
//        // if no meta-data can be found, throw an exception
//        if(metaData == null){
//            LogUtil.errorBackground(new IOException("can't find metadata for " + meta));
//            throw new IOException("can't find metadata for " + meta);
//        }
//        
//        // return the cached meta-data
//        return metaData;
//    }
//    
//    public X509Certificate[] getTrustedCertificates() {
//        return certs.values().toArray(new X509Certificate[0]);
//    }
//    
//    public X509Certificate getTrustedCertificate(String name) {
//        return certs.get(name);
//    }
//    
//    public void addTrustedCertificate(X509Certificate cert) {
//        String name = SecurityUtil.getMD5Name(cert);
//        certs.put(name, cert);
//        setValidate(true);
//    }
//    
//    public void setTrustedCertificates(X509Certificate[] trustedCertificates) {
//        // index each cert to match throws etrusted certificate
//        for (X509Certificate c : trustedCertificates) {
//            String name = SecurityUtil.getMD5Name(c);
//            certs.put(name, c);
//        }
//        setValidate(true);
//    }
//    
//    public boolean isValidate() {
//        return validate;
//    }
//    
//    public void setValidate(boolean validate) {
//        this.validate = validate;
//    }
//    
//    public String getPassphrase() {
//        return passphrase;
//    }
//    
//    public void setPassphrase(String passphrase) {
//        this.passphrase = passphrase;
//        
//        // set the default padding
//        if (passphrase != null) {
//            padding = new BigHash(passphrase.getBytes()).toByteArray();
//        }
//    }
//    
//    public BigHash getHash() {
//        return hash;
//    }
//    
//    /**
//     * Download one file/project.
//     */
//    public void setHash(BigHash hash) {
//        
//        this.hash = hash;
//    }
//    
//    /**
//     * Download one file/project.
//     */
//    public void setHash(String hash) {
//        
//        this.hash = BigHash.createFromBytes(Base64.decode(hash));
//    }
//    
//    public File getSaveAs() {
//        return saveAs;
//    }
//    
//    public void setSaveAs(File saveAs) {
//        this.saveAs = saveAs;
//    }
//    
//    public List<String> getServersToUse() {
//        return serversToUse;
//    }
//    
//    public void setServersToUse(List<String> serversToUse) {
//        this.serversToUse = serversToUse;
//    }
//    
//    public ArrayList<GetFileToolListener> getListeners() {
//        return listeners;
//    }
//    
//    public void addListener(GetFileToolListener listener) {
//        
//        if (this.listeners == null)
//            this.listeners = new ArrayList();
//        
//        this.listeners.add(listener);
//    }
//    
//    public void setListeners(ArrayList<GetFileToolListener> listeners) {
//        this.listeners = listeners;
//    }
//    
//    public boolean isPurgeUnexpectedFiles() {
//        return purgeUnexpectedFiles;
//    }
//    
//    public void setPurgeUnexpectedFiles(boolean purgeUnexpectedFiles) {
//        this.purgeUnexpectedFiles = purgeUnexpectedFiles;
//    }
//    
//    public boolean isRecalculateHashes() {
//        return recalculateHashes;
//    }
//    
//    public void setRecalculateHashes(boolean recalculateHashes) {
//        this.recalculateHashes = recalculateHashes;
//    }
//    
//    public ArrayList<ProjectFilePart> getErrors() {
//        return errors;
//    }
//    
//    public byte[] getPadding() {
//        return padding;
//    }
//    
//    public void setPadding(byte[] padding) {
//        this.padding = padding;
//    }
//    
//    public String getRegex() {
//        return regex;
//    }
//    
//    public void setRegex(String r) throws Exception {
//        this.regex = r;
//        try {
//            //
//            this.setPattern(Pattern.compile(r));
//        } catch (Exception e) {
//            // handle the bad regular expression by setting the regex to anything
//            setRegex(".");
//            System.out.println("Invalid regular expression -- ignoring.");
//        }
//    }
//    
//    public Pattern getPattern() {
//        return pattern;
//    }
//    
//    public void setPattern(Pattern pattern) {
//        this.pattern = pattern;
//    }
//    
//    public static long getNumFilesToDownload() {
//        return numFilesToDownload;
//    }
//    
//    protected void setProjectFile(ProjectFile projectFile) {
//        this.projectFile = projectFile;
//    }
//    
//    /**
//     * Helper method lazy loads the ProjectFile, avoids duplicate work.
//     */
//    private ProjectFile getProjectFile() throws Exception {
//        return getProjectFile(false);
//    }
//    
//    /**
//     * Returns project parts. Avoids too much work.
//     */
//    private Set<ProjectFilePart> getProjectPartFiles() throws Exception {
//        return getProjectFile().getParts();
//    }
//    
//    /**
//     * Destroy the project parts, if exists. House keeping.
//     */
//    private void safeDestroyProjectPartFiles() throws Exception {
//        if (projectFile != null) {
//            Set<ProjectFilePart> parts = projectFile.getParts();
//            if (parts instanceof DiskBackedProjectFilePartSet) {
//                ((DiskBackedProjectFilePartSet)parts).destroy();
//            }
//        }
//        if (projectParts != null && projectParts instanceof DiskBackedProjectFilePartSet) {
//            ((DiskBackedProjectFilePartSet)projectParts).destroy();
//        }
//    }
//    
//    
//    /**
//     * <p>Get a project file object, with the option to create a clone.</p>
//     * <p>If caller clones, please clean up files when done!</p>
//     * @param useSafeClone Create a new copy to avoid disk-backed collisions when iterating parts.
//     * @return ProjectFile object.
//     */
//    private ProjectFile getProjectFile(boolean useSafeClone) throws Exception  {
//        
//        // If done successfully before, don't do again
//        if (this.projectFile != null && !useSafeClone)
//            return this.projectFile;
//        
//        // If no project root dir set, use temp file
////        File pf;
////        if (this.projectRootDir == null)
////            pf = new File(TempFileUtil.getTemporaryDirectory(), ProjectFile.DEFAULT_PROJECT_FILE_NAME);
////        else
////            pf = new File(this.projectRootDir, ProjectFile.DEFAULT_PROJECT_FILE_NAME);
////
////        // delete old files
////        if (pf.exists() && !useSafeClone) {
////            if (!pf.delete()) {
////                throw new Exception("Can't overwrite old project file: "+pf);
////            }
////        }
////
////        // If pf still exists, must be a clone, don't download
////        if (!pf.exists()) {
////            getFile(pf);
////        }
//        
//        File pf = null;
//        
//        // Parse the project file in memory
//        FileInputStream fis = null;
//        BufferedInputStream bis = null;
//        ProjectFile projectFileCopy = null;
//        try {
//            
//            pf =  TempFileUtil.createTemporaryFile(".pf");
//            getFile(pf);
//            
//            // open the streams
//            fis = new FileInputStream(pf);
//            bis = new BufferedInputStream(fis);
//            // set the project file
//            projectFileCopy = ProjectFileUtil.read(bis);
//            
//            // Don't replace unless doesn't exist. This may be a clone!
//            if (this.projectFile == null)
//                this.projectFile = projectFileCopy;
//            
//        } finally {
//            IOUtil.safeClose(bis);
//            IOUtil.safeClose(fis);
//            IOUtil.safeDelete(pf);
//        }
//        
//        return projectFileCopy;
//    }
//    
//    /**
//     * <p>Set a project root directory, or the directory where files are stored from a project. If using getDirectory, this will be set automatically.</p>
//     * <p>This is generally useful for single file downloads into existing partial projects. This is also used internally withing the GFT.</p>
//     */
//    public void setProjectRootDir(File projectRootDir) {
//        this.projectRootDir = projectRootDir;
//    }
//    
//    /**
//     * <p>Filters a file using the regex. If no regex was supplied, will always return true.</p>
//     * @param relativeName The relative name for the file, or just the simple file name.
//     * @return True if file passes the regex filter, else false.
//     */
//    private boolean filePassesFilter(String relativeName) {
//        return getPattern().matcher(relativeName).find();
//    }
//    
//    public void setShowDownloadSummary(boolean showDownloadSummary) {
//        this.showDownloadSummary = showDownloadSummary;
//    }
//    
//    /**
//     * Helper method. Returns true if project is tarball heavy.
//     */
//    private boolean isTarballDownload(ProjectFile pf) {
//        // Download up to 3 meta data. If any are tarballs, return true.
//        int downloadCount = 0;
//        for (ProjectFilePart pfp : pf.getParts()) {
//            if (downloadCount >= 3) {
//                break;
//            }
//            if (pfp.getHash().LENGTH_LENGTH > Math.pow(2,20)) {
//                continue;
//            }
//            try {
//                GetFileTool gft = new GetFileTool();
//                gft.setServersToUse(getServersToUse());
//                gft.setHash(pfp.getHash());
//                MetaData md = gft.getMetaData();
//                for (FileEncoding fe : md.getEncodings()) {
//                    if (fe.getName().equals(FileEncoding.TARBALL)) {
//                        return true;
//                    }
//                }
//            } catch(Exception e) { /* Do nothing */ } finally { downloadCount++; }
//        }
//        return false;
//    }
//}
//
//// represent a file chunk being downloaded
//class ChunkToDownload {
//    long offset = 0;
//    byte[] bytes;
//    BigHash hash;
//    ChunkToDownload(long offset, byte[] bytes, BigHash hash) {
//        this.offset = offset;
//        this.bytes = bytes;
//        this.hash = hash;
//    }
}


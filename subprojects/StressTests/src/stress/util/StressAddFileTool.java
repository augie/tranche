/*
 * StressAddFileTool.java
 *
 * Created on October 6, 2007, 7:28 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package stress.util;

/**
 * Modify AFT to use 1 socket per thread.
 * @author Bryan E. Smith
 */
public class StressAddFileTool {
//    /**
//     * FLAGS
//     */
//    // flag the boolean
//    private boolean useEncryption = false;
//    // flag for if this is a single file upload
//    private boolean singleFileUpload = false;
//    // flag to signal if archives should be exploded before upload
//    private boolean explodeBeforeUpload = false;
//    // the flag if logging is enabled
//    private boolean loggingEnabled = false;
//    // Use remote replication
////    public static boolean useRemoteReplication = true;
//    public static boolean useRemoteReplication = false;
//    // Flag to print upload summary to stdout after complete.
//    private boolean showUploadSummary = true;
//    // flag for gzip
//    private boolean useCompression = true;
//    // flag for skipping files based on hash -- this might skip missing chunks and adds a network IO call to the file encoding thread (i.e. can slow it down alot)
//    private boolean skipPreviouslyUploaded = false;
//    // flag to skip chunks previously uploaded to selected servers
//    private boolean skipExistingChunk = true;
//    
//    /**
//     * SECURITY
//     */
//    private UserZipFile user = null;
//    private String userPassword = null;
//    // the private key to use
//    private PrivateKey privateKey;
//    // the public key to use
//    private X509Certificate publicCertificate;
//    // the security passphrase
//    private String passphrase = null;
//    // the padding to apply
//    private byte[] padding = new byte[0];
//    
//    /**
//     * UPLOAD INFORMATION/META DATA
//     */
//    private String description = null;
//    private String title = null;
//    // the project's hash
//    BigHash projectFileHash = null;
//    
//    /**
//     * UPLOAD MONITORING
//     */
//    Set<AddFileToolListener> listeners = new HashSet<AddFileToolListener>();
//    // track bytes to upload
//    private long dataToUpload = 0;
//    // track bytes uploaded
//    private long dataUploaded = 0;
//    
//    /**
//     * EXCEPTION MESSAGES & HANDLING
//     */
//    // Used to append any possible exception messages to end of file upload if fails
////    private String msgAboutUploadFailure = null;
//    final static String CANT_FIND_SERVERS_MSG = "Can't find any servers to upload to!",
//            OUT_OF_SPACE_SERVER_MSG = "One of the servers is out of disk space.",
//            OUT_OF_SPACE_DIR_MSG = "Cannot find a directory with available disk space",
//            SECURITY_EXCEPTION_MSG = "There was a security exception. Perhaps your user is not known by a server.";
//    Exception reportedException = null;
//    
//    /**
//     * CONSTANTS/NUMERIC PARAMETERS
//     */
//    // the cutoff size to use for buffering files in memory -- size
//    private long bufferEncodingsInMemoryCutoff = 1024*1024;
//    // how many times to try and upload things -- 3 times for luck
//    private int maxRetries = 3;
//    // number of servers to use
//    private int serversToUploadTo = 2;
//    
//    // the servers to use
//    private boolean triedSettingServersToUse = false;
//    private ArrayList<String> serversToUse = new ArrayList();
//    
//    /**
//     * ENCODING & UPLOADING INTERNALS
//     */
//    // a pool to keep track of encoded files that are supposed to be uploaded
//    ArrayBlockingQueue<FileToEncode> unencodedFileQueue = new ArrayBlockingQueue(10000);
////    // a pool to keep track of encoded files that are supposed to be uploaded
////    ArrayBlockingQueue<EncodedFileToUpload> encodedFileQueue = new ArrayBlockingQueue(10);
//    
//    /**
//     * PROJECT FILES
//     */
//    // make the project file
//    Set<ProjectFilePart> projectFileParts = Collections.synchronizedSet(new DiskBackedProjectFilePartSet());
//    // The root directory
//    private File rootDir;
//    
//    /**
//     * FOR PAUSING
//     */
//    private boolean isPaused = false;
//    public void setPaused(boolean isPaused) {
//        this.isPaused = isPaused;
//    }
//    public boolean isPaused() {
//        return isPaused;
//    }
//    
//    /**
//     * FOR STOPPING
//     */
//    private boolean stop = false;
//    public void stop() {
//        stop = true;
//    }
//    
//    /**
//     * Internal helper method for cooperating threads to bubble exceptions back out of the main AddFileTool's thread.
//     */
//    private void waitForExceptionToBeReported(Thread otherThread, Exception e) throws Exception {
//        System.out.println("*** Critical Error. Waiting on program to catch and throw. This will result in an error. ***");
//        e.printStackTrace(System.out);
//        
//        // Parse exception for useful mesages
////        parseAndSetMessageFromStackTrace(e);
//        
//        // if the other thread
//        if (!Thread.currentThread().equals(otherThread)) throw e;
//        
//        // set the reported exception
//        reportedException = e;
//        try {
//            // indefinitely wait for the exception to be read
//            synchronized(this) {
//                wait();
//            }
//        } catch (InterruptedException ex) {
//            throw new RuntimeException("Coulnd't report exception!", e);
//        }
//    }
//    
//    private void bubbleAnyExceptions() throws Exception {
//        // throw any buffered Exceptions
//        if  (reportedException != null) {
//            Exception e = reportedException;
//            // notify any locks
//            synchronized(this) {
//                this.notifyAll();
//            }
//            // throw the exception
//            throw e;
//        }
//    }
//    
//    /**
//     * Adds a server URL that should be used for the upload. Specifying a server will cause this tool to ignore the normally used 'core' Tranche servers.
//     * @param url The URL of the server to use, e.g. "tranche://dev.proteomecommons.org:443
//     */
//    public void addServerURL(String url) {
//        triedSettingServersToUse = true;
//        if (url != null && ServerUtil.isServerOnline(url)) {
//            serversToUse.add(url);
//        }
//    }
//    
////    /**
////     * Entry point to the program.
////     * @param args The set of arguments to be used when parsing the program.
////     */
////    public static void main(String[] args) {
////        // register the bouncy castle code
////        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
////        
////        // check arg
////        if (args.length < 1) {
////            System.out.println("Usage: java -jar AddFileTool.jar <filename or directory>");
////            System.out.println("  e.g: java -jar AddFileTool.jar data.pkl");
////            System.out.println("");
////            System.out.println("\nThis tool adds files to the network. If a file has previously been added, it will automatically be ignored.");
////            return;
////        }
////        
////        try {
////            // Determine whether a user file was provided first
////            String userFile = null;
////            String userPassword = null;
////            
////            for (int i=0;i<args.length-2;i+=2){
////                
////                if (args[i].equals("--user")){
////                    System.out.println("Using user file: "+args[i+1]);
////                    userFile = args[i+1];
////                    continue;
////                }
////                
////                if (args[i].equals("--userpassword")){
////                    System.out.println("Using user password: "+args[i+1]);
////                    userPassword = args[i+1];
////                    continue;
////                }
////            }
////            
////            // make a new file util
////            AddFileTool afu = null;
////            
////            if (userFile != null) {
////                
////                UserZipFile user = new UserZipFile(new File(userFile));
////                
////                try {
////                    if (userPassword != null) {
////                        user.setPassphrase(userPassword);
////                    }
////                    
////                    afu = new AddFileTool(user.getCertificate(), user.getPrivateKey());
////                }
////                
////                catch (Exception e) {
////                    System.err.println("ERROR: Could not read the user file. Perhaps your passphrase is incorrect or you forgot to include your passphrase.");
////                    return;
////                }
////            }
////            
////            else {
////                System.out.println("Can't run AddFileTool without specifying a user key.");
////                return;
////            }
////            
////            // enable logging
////            afu.setLoggingEnabled(true);
////            
////            // make a file from the first arg
////            String fileName = args[args.length-1];
////            
////            // load all the parameters
////            for (int i=0;i<args.length-2;i+=2){
////                
////                // Already handled user and password, so skip
////                if (args[i].equals("--user") || args[i].equals("--userpassword"))
////                    continue;
////                
////                // handle passwords
////                if (args[i].equals("--passphrase")){
////                    System.out.println("Using passphrase: "+args[i+1]);
////                    afu.setPassphrase(args[i+1]);
////                    continue;
////                }
////                if (args[i].equals("--useEncryption")){
////                    afu.setUseEncryption(true);
////                    System.out.println("Using encryption.");
////                    System.out.println("  - Auto-generated passphrase: "+afu.getPassphrase());
////                    continue;
////                }
////                // check for explode flag
////                if (args[i].equals("--explodeBeforeUpload")){
////                    afu.setExplodeBeforeUpload(Boolean.parseBoolean(args[i+1]));
////                    System.out.println("Flagging Auto-File Decompression: "+Boolean.parseBoolean(args[i+1])+" or "+afu.isExplodeBeforeUpload());
////                    continue;
////                }
////                // check for title/description
////                if (args[i].equals("--title")){
////                    afu.setTitle(args[i+1]);
////                    continue;
////                }
////                if (args[i].equals("--titleFile")){
////                    try {
////                        FileInputStream fis = new FileInputStream(args[i+1]);
////                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
////                        IOUtil.getBytes(fis, baos);
////                        afu.setTitle(new String(baos.toByteArray()));
////                        fis.close();
////                    } catch (Exception e) {
////                        System.out.println("Can't read title from file "+args[i+1]+", exiting!");
////                        return;
////                    }
////                    continue;
////                }
////                if (args[i].equals("--description")){
////                    afu.setDescription(args[i+1]);
////                    continue;
////                }
////                if (args[i].equals("--descriptionFile")){
////                    try {
////                        FileInputStream fis = new FileInputStream(args[i+1]);
////                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
////                        IOUtil.getBytes(fis, baos);
////                        afu.setDescription(new String(baos.toByteArray()));
////                        fis.close();
////                    } catch (Exception e) {
////                        System.out.println("Can't read description from file "+args[i+1]+", exiting!");
////                        return;
////                    }
////                    continue;
////                }
////                if (args[i].equals("--tempDir")){
////                    TempFileUtil.setTemporaryDirectory(args[i+1]);
////                    continue;
////                }
////                
////                if (args[i].equals("--dontSkipPreviouslyUploaded")) {
////                    // flag on that previously uploaded shouldn't be skipped
////                    afu.setSkipPreviouslyUploaded(false);
////                    continue;
////                }
////                if (args[i].equals("--dontSkipExistingChunk")) {
////                    // flag on that previously uploaded shouldn't be skipped
////                    afu.setSkipExistingChunk(false);
////                    continue;
////                }
////                if (args[i].equals("--dontRemoteReplicate")) {
////                    // flag off remote replication
////                    afu.setUseRemoteReplication(false);
////                    continue;
////                }
////                if (args[i].equals("--url")){
////                    afu.addServerURL(args[i+1]);
////                    continue;
////                }
////                System.out.println("Can't handle parameter \""+args[i]+"\". Exiting.");
////                return;
////            }
////            
////            // if no servers are specified, add the default
////            if (afu.serversToUse.size() > 0) {
////                System.out.println("Using the following servers:");
////                for (String s : afu.serversToUse) {
////                    System.out.println("  "+s);
////                }
////                // check the servers to upload count
////                if (afu.serversToUse.size() < afu.getServersToUploadTo()) {
////                    afu.setServersToUploadTo(afu.serversToUse.size());
////                    System.out.println(" Automatically reducing to uploading to "+afu.getServersToUploadTo());
////                }
////            } else {
////                // startup the core servers
////                ServerUtil.waitForStartup();
////            }
////            
////            System.out.println("Trying to upload file(s): "+fileName);
////            // add a command-line listener
////            CommandLineAddFileToolListener claftl = new CommandLineAddFileToolListener(System.out);
////            afu.listeners.add(claftl);
////            
////            // make the file
////            File file = new File(fileName);
////            
////            // try adding the file
////            try {
////                afu.addFile(file);
////            } catch (FileNotFoundException fnfe) {
////                LogUtil.error(fnfe);
////                System.out.println("Can't add file. Please check that the file exists.");
////                System.exit(1);
////            }
////        }
////        // handle can't connect to url exceptions
////        catch (ConnectException ce) {
////            System.out.println("ProteomeCommons.org url is either busy or off-line. Please try again later.");
////        }
////        // handle can't connect to url exceptions
////        catch (FileNotFoundException ce) {
////            System.out.println("Can't find the file. Please check that it exists.");
////        } catch (Exception e) {
////            System.out.println("Program error. Please run again with the '-debug' flag and send the error to jfalkner@umich.edu");
////            LogUtil.error(e);
////        }
////        
////        // Return so tests can pass
////        return;
////    }
//    
//    /**
//     * A method for registering AddFileToolListener instances to this code.
//     * @param aftl The AddFileToolListener implementation to register as an event listener.
//     */
//    public void addListener(AddFileToolListener aftl) {
//        this.listeners.add(aftl);
//    }
//    
//    public Thread caller = null;
//    
//    /**
//     * Constructor for when you have a specific certificate and key pair.
//     * @param cert The X.509 certificate that represents the public key of the user uploading files.
//     * @param key The X.509 private key for the user uploading files.
//     */
//    public StressAddFileTool(Thread caller, X509Certificate cert, PrivateKey key) {
//        this.caller = caller;
//        setPublicCertificate(cert);
//        setPrivateKey(key);
//    }
//    
//    private void fireStartedUpload() {
////        for (AddFileToolListener l : listeners) {
////            // catch exceptions, don't let a listener's exception bubble out
////            try{
////                l.startedUpload(this);
////            } catch (Exception e) {
////                // noop
////            }
////        }
//    }
//    
//    private void fireFinishedUpload() {
////        for (AddFileToolListener l : listeners) {
////            // catch exceptions, don't let a listener's exception bubble out
////            try{
////                l.finishedUpload(this);
////            } catch (Exception e) {
////                // noop
////            } catch (Error e) {
////                // ignore errors!? haha!
////            }
////        }
//    }
//    
//    private void fireChunkUpload(StressAddFileToolChunkEvent aftce) {
////        // change uploaded bytes if this isn't meta-data
////        if (!aftce.isMetaData) {
////            dataUploaded += aftce.hash.getLength();
////        }
////        
////        // fire all of the listeners
////        for (AddFileToolListener l : listeners) {
////            try{ l.chunkUpload(aftce); } catch (Exception e) {}
////        }
//    }
//    
//    private void fireAddedFile(String relativeName, BigHash hash) {
////        // fire all of the listeners
////        for (AddFileToolListener l : listeners) {
////            try{ l.addedFile(relativeName, hash, this); } catch (Exception e){}
////        }
//    }
//    
//    private void fireSkippedFile(String relativeName, BigHash hash) {
////        // change uploaded bytes
////        dataUploaded += hash.getLength();
////        // fire all of the listeners
////        for (AddFileToolListener l : listeners) {
////            try{ l.skippedFile(relativeName, hash, this);  } catch (Exception e){}
////        }
//    }
//    
//    private void fireStartedFile(String relativeName, BigHash hash) {
////        // fire all of the listeners
////        for (AddFileToolListener l : listeners) {
////            try{ l.startedFile(relativeName, hash, this); } catch (Exception e){}
////        }
//    }
//    
//    private void fireFailedFile(String relativeName, BigHash hash) {
////        for (AddFileToolListener l : listeners) {
////            try{ l.failedFile(relativeName, hash, this); } catch (Exception e){}
////        }
//    }
//    
//    /**
//     * Add the file(s) after setting the upload parameters.
//     * @param file The File object that represents either the file or directory to upload.
//     * @return Returns the hash to upload.
//     * @throws java.lang.Exception All errors are thrown.
//     */
//    public BigHash addFile(final File file) throws Exception {
//        final long start = System.currentTimeMillis();
//        
//        // Set the global var root directory (used for extracting relative names)
//        rootDir = file;
//        
//        // add a memory listener
//        if (this.isLoggingEnabled()) {
//            addListener(new LoggingAddFileToolListener());
//        }
//        
//        // fire the start event
//        fireStartedUpload();
//        
//        // loop through all the files quickly to calc total upload size
//        Thread t = new Thread("AddFileTool file size calculating thread") {
//            public void run() {
//                int fileCount = 0;
//                LinkedList<File> stack = new LinkedList();
//                stack.add(file);
//                while (stack.size() > 0) {
//                    final File f = stack.removeFirst();
//                    if (f.isDirectory()) {
//                        // add all the files
//                        File[] files = f.listFiles();
//                        for (File ff : files) {
//                            // add to the front to make this depth-first/memory efficient
//                            stack.addFirst(ff);
//                        }
//                    } else {
//                        // update bytes to download
//                        dataToUpload += f.length() + getPadding().length;
//                        fileCount++;
//                    }
//                }
//            }
//        };
//        t.start();
//        
//        // yeild a sec
//        Thread.yield();
//        
//        // start up the add a few file encoding threads
//        final FileEncodingThread[] fet = new FileEncodingThread[30];
//        for (int i=0;i<fet.length;i++) {
//            fet[i] = new FileEncodingThread();
//            fet[i].start();
//        }
//        
//        // the flag for if the file was exploaded or not
//        boolean exploded = false;
//        
//        // loop through all the files
//        LinkedList<File> stack = new LinkedList();
//        stack.add(file);
//        while (stack.size() > 0) {
//            final File f = stack.removeFirst();
//            if (f.isDirectory()) {
//                // add all the files
//                File[] files = f.listFiles();
//                for (File ff : files) {
//                    // add to the front to make this depth-first/memory efficient
//                    stack.addFirst(ff);
//                }
//            } else {
//                // get the file name to work with
//                String fileName = f.getName().toLowerCase();
//                // if exploding, check for ZIP compressed files
//                if (isExplodeBeforeUpload() && fileName.endsWith(".zip")) {
//                    exploded = true;
//                    explodeAndUploadZipFile(file, f);
//                }
//                // if exploding, check for TAR+GZIP and TAR+bzip2 compressed files
//                else if (isExplodeBeforeUpload() && (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")|| fileName.endsWith(".tar.gzip") || fileName.endsWith(".tar.bz2") || fileName.endsWith(".tar.bzip2") || fileName.endsWith(".tbz2") || fileName.endsWith(".tbz"))) {
//                    exploded = true;
//                    explodeAndUploadTarballFile(file, f);
//                }
//                // if exploding, check for GZIP compression
//                else if (isExplodeBeforeUpload() && (fileName.endsWith(".gzip") || fileName.endsWith(".gz"))) {
//                    exploded = true;
//                    explodeAndUploadGZIP(file, f);
//                }
//                // if exploding, check for bzip2 compression
//                else if (isExplodeBeforeUpload() && (fileName.endsWith(".bzip") || fileName.endsWith(".bzip2") || fileName.endsWith(".bz") || fileName.endsWith(".bz2"))) {
//                    exploded = true;
//                    explodeAndUploadBzip2(file, f);
//                }
//                // if exploding, check for LZMA compression
//                else if (isExplodeBeforeUpload() && (fileName.endsWith(".lzma") || fileName.endsWith(".lz"))) {
//                    exploded = true;
//                    explodeAndUploadLZMA(file, f);
//                }
//                // if exploding, check for LZMA compression
//                else if (isExplodeBeforeUpload() && (fileName.endsWith(".tar") || fileName.endsWith(".tar"))) {
//                    exploded = true;
//                    explodeAndUploadTAR(file, f);
//                }
//                // else submit the file on its own
//                else {
//                    // get the relative name
//                    final String relativeName = relativeName(file, f);
//                    // add the file
//                    uploadFile(f, relativeName, false);
//                }
//            }
//        }
//        
//        // nicely notify the threads to finish and wait on them
//        for (int i=0;i<fet.length;i++) {
//            fet[i].finished = true;
//            while (fet[i].isAlive()) {
//                // join a little bit
//                fet[i].join(50);
//                // check for exceptions
//                bubbleAnyExceptions();
//            }
//        }
//        
//        // finish the upload process
//        try {
//            // update file info
//            if(!file.isDirectory() && !exploded) {
//                // set the project file hash
//                projectFileHash = new BigHash(file, padding);
//                
//                return projectFileHash;
//            } else {
//                // upload the project
//                ProjectFile pf = new ProjectFile(getTitle(), getDescription(), projectFileParts);
//                
//                // make a temporary file
//                File tempProjectFile = TempFileUtil.createTemporaryFile(".pf");
//                FileOutputStream fos = null;
//                BufferedOutputStream bos = null;
//                
//                try {
//                    try {
//                        // write the data out to a file
//                        fos = new FileOutputStream(tempProjectFile);
//                        bos = new BufferedOutputStream(fos);
//                        ProjectFileUtil.write(pf, bos);
//                        bos.flush();
//                        fos.flush();
//                        bos.close();
//                        fos.close();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    
//                    // upload the project file
//                    for (int projectFileUploadAttempt = 0;projectFileUploadAttempt<maxRetries;projectFileUploadAttempt++) {
//                        // stop
//                        if (stop) {
//                            Thread.currentThread().interrupt();
//                            return null;
//                        }
//                        
//                        try {
//                            BigHash pfHash = new BigHash(tempProjectFile, padding);
//                            
//                            // upload the file
//                            uploadFile(tempProjectFile, ProjectFile.DEFAULT_PROJECT_FILE_NAME, false, true);
//                            
//                            // manually encode the project file be sure to bubble any exceptions
//                            FileEncodingThread fet2 = new FileEncodingThread();
//                            fet2.start();
//                            // nicely notify the threads to finish and wait on them
//                            fet2.finished = true;
//                            while (fet2.isAlive()) {
//                                // join a little bit
//                                fet2.join(50);
//                                // check for exceptions
//                                bubbleAnyExceptions();
//                            }
//                            
//                            // set the reference to the project file
//                            projectFileHash = pfHash;
//                            // return the hash -- this must be set up the call to uploadFile()
//                            return projectFileHash;
//                        } catch (Exception e){
//                            System.err.print("Failed project file upload attempt "+projectFileUploadAttempt+".");
//                            e.printStackTrace();
//                        }
//                    }
//                } finally {
//                    IOUtil.safeClose(fos);
//                    IOUtil.safeDelete(tempProjectFile);
//                }
//            }
//        } finally {
//            // fire the finished upload
//            fireFinishedUpload();
//            
//            // Clean up the disk
//            if (projectFileParts instanceof DiskBackedProjectFilePartSet) {
//                ((DiskBackedProjectFilePartSet)projectFileParts).destroy();
//            }
//            
//            if (showUploadSummary){
//                System.out.println("Total upload time: " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis()-start));
//            }
//        }
//        
//        // should never make it here
//        throw new RuntimeException("The servers are no longer responding.");
//    }
//    
//    private void explodeAndUploadLZMA(final File file, final File f) throws Exception {
//        // subtract the file's size from bytes to upload
//        dataToUpload -= f.length();
//        // normalize the file name
//        String fileName = f.getName().toLowerCase();
//        // make a temp file
//        File temp = CompressionUtil.lzmaDecompress(f);
//        // inc bytes to upload appropriately
//        dataToUpload += temp.length();
//        // make up an appropriate relative name
//        String relativeName = relativeName(file, f);
//        relativeName = relativeName.split("\\.lz")[0];
//        // upload the file
//        uploadFile(temp, relativeName, true);
//    }
//    
//    private void explodeAndUploadBzip2(final File file, final File f) throws Exception {
//        // subtract the file's size from bytes to upload
//        dataToUpload -= f.length();
//        // normalize the file name
//        String fileName = f.getName().toLowerCase();
//        // make a temp file
//        File temp = CompressionUtil.bzip2Decompress(f);
//        // inc bytes to upload appropriately
//        dataToUpload += temp.length();
//        // make up an appropriate relative name
//        String relativeName = relativeName(file, f);
//        relativeName = relativeName.split("\\.bz")[0];
//        // upload the file
//        uploadFile(temp, relativeName, true);
//    }
//    
//    private void explodeAndUploadGZIP(final File file, final File f) throws Exception {
//        // subtract the file's size from bytes to upload
//        dataToUpload -= f.length();
//        // normalize the file name
//        String fileName = f.getName().toLowerCase();
//        // make a temp file
//        File temp = CompressionUtil.gzipDecompress(f);
//        // inc bytes to upload appropriately
//        dataToUpload += temp.length();
//        // make up an appropriate relative name
//        String relativeName = relativeName(file, f);
//        relativeName = relativeName.split("\\.gz")[0];
//        // upload the file
//        uploadFile(temp, relativeName, true);
//    }
//    
//    private void explodeAndUploadTarballFile(final File file, final File f) throws Exception {
//        // subtract the file's size from bytes to upload
//        dataToUpload -= f.length();
//        // read the ZIP
//        FileInputStream fis = null;
//        BufferedInputStream bis = null;
//        // stream for compression -- either gzip or bzip2
//        InputStream cis = null;
//        TarInputStream tis = null;
//        // normalize the file name
//        String fileName = f.getName().toLowerCase();
//        try {
//            // make the streams
//            fis = new FileInputStream(f);
//            bis = new BufferedInputStream(fis);
//            // dynamically set either GZIP or bzip2 as the decompression scheme
//            if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz") || fileName.endsWith(".tar.gzip")) {
//                cis = new GZIPInputStream(bis);
//            } else if (fileName.endsWith(".tar.bz2") || fileName.endsWith(".tar.bzip2") || fileName.endsWith(".tbz2") || fileName.endsWith(".tbz")) {
//                cis = new CBZip2InputStream(bis);
//            } else {
//                throw new RuntimeException("Can't determine TAR file's compression algorithm.");
//            }
//            // finally pipe through a TAR reader
//            tis = new TarInputStream(cis);
//            // read the entries
//            for (TarEntry te = tis.getNextEntry();te!=null;te=tis.getNextEntry()) {
//                // only upload if it is a file
//                if (te.isDirectory()) continue;
//                // make a temp file
//                File temp = TempFileUtil.createTemporaryFile();
//                FileOutputStream fos = null;
//                BufferedOutputStream bos = null;
//                try {
//                    fos = new FileOutputStream(temp);
//                    bos = new BufferedOutputStream(fos);
//                    IOUtil.getBytes(tis, bos);
//                } finally {
//                    IOUtil.safeClose(bos);
//                    IOUtil.safeClose(fos);
//                }
//                // inc bytes to upload
//                dataToUpload += temp.length();
//                // make up an appropriate relative name
//                String relativeName = relativeName(file, f);
//                relativeName = relativeName.split("\\.t")[0]+"/"+te.getName().replace("./", "");
//                // upload the file
//                uploadFile(temp, relativeName, true);
//            }
//        } finally {
//            IOUtil.safeClose(tis);
//            IOUtil.safeClose(cis);
//            IOUtil.safeClose(bis);
//            IOUtil.safeClose(fis);
//        }
//    }
//    
//    private void explodeAndUploadTAR(final File file, final File f) throws Exception {
//        // subtract the file's size from bytes to upload
//        dataToUpload -= f.length();
//        // read the ZIP
//        FileInputStream fis = null;
//        BufferedInputStream bis = null;
//        TarInputStream tis = null;
//        // normalize the file name
//        String fileName = f.getName().toLowerCase();
//        try {
//            // make the streams
//            fis = new FileInputStream(f);
//            bis = new BufferedInputStream(fis);
//            // finally pipe through a TAR reader
//            tis = new TarInputStream(bis);
//            tis.setDebug(true);
//            // read the entries
//            for (TarEntry te = tis.getNextEntry();te!=null;te=tis.getNextEntry()) {
//                // only upload if it is a file
//                if (te.isDirectory()) {
//                    TarEntry[] entries = te.getDirectoryEntries();
//                    continue;
//                }
//                // make a temp file
//                File temp = TempFileUtil.createTemporaryFile();
//                FileOutputStream fos = null;
//                BufferedOutputStream bos = null;
//                try {
//                    fos = new FileOutputStream(temp);
//                    bos = new BufferedOutputStream(fos);
//                    IOUtil.getBytes(tis, bos);
//                } finally {
//                    IOUtil.safeClose(bos);
//                    IOUtil.safeClose(fos);
//                }
//                // inc bytes to upload
//                dataToUpload += temp.length();
//                // make up an appropriate relative name
//                String relativeName = relativeName(file, f);
//                relativeName = relativeName.split("\\.t")[0]+"/"+te.getName().replace("./", "");
//                System.out.println("Trying to upload: "+relativeName+", te.getName(): "+te.getName()+", size: "+te.getSize()+", "+te.getFile());
//                // upload the file
//                uploadFile(temp, relativeName, true);
//            }
//        } finally {
//            IOUtil.safeClose(tis);
//            IOUtil.safeClose(bis);
//            IOUtil.safeClose(fis);
//        }
//    }
//    
//    private void explodeAndUploadZipFile(final File file, final File f) throws Exception {
//        // subtract the file's size from bytes to upload
//        dataToUpload -= f.length();
//        // read the ZIP
//        FileInputStream fis = null;
//        BufferedInputStream bis = null;
//        ZipInputStream zis = null;
//        try {
//            // make the streams
//            fis = new FileInputStream(f);
//            bis = new BufferedInputStream(fis);
//            zis = new ZipInputStream(bis);
//            // read the entries
//            for (ZipEntry ze = zis.getNextEntry();ze!=null;ze=zis.getNextEntry()) {
//                // only upload if it is a file
//                if (ze.isDirectory()) continue;
//                // make a temp file
//                File temp = TempFileUtil.createTemporaryFile();
//                FileOutputStream fos = null;
//                BufferedOutputStream bos = null;
//                try {
//                    fos = new FileOutputStream(temp);
//                    bos = new BufferedOutputStream(fos);
//                    IOUtil.getBytes(zis, bos);
//                } finally {
//                    IOUtil.safeClose(bos);
//                    IOUtil.safeClose(fos);
//                }
//                // inc bytes to upload
//                dataToUpload += temp.length();
//                // make up an appropriate relative name
//                String relativeName = relativeName(file, f);
//                relativeName = relativeName.substring(0, relativeName.length()-4)+"/"+ze.getName();
//                // upload the file
//                uploadFile(temp, relativeName, true);
//            }
//        } finally {
//            IOUtil.safeClose(zis);
//            IOUtil.safeClose(bis);
//            IOUtil.safeClose(fis);
//        }
//    }
//    
//    /**
//     *<p>Adds the file using the default authority information.</p>
//     */
//    private void uploadFile(File dataFile, String relativeName, boolean deleteFileAfterUpload) throws Exception {
//        uploadFile(dataFile, relativeName, true, deleteFileAfterUpload);
//    }
//    private void uploadFile(File dataFile, String relativeName, boolean isProjectFile, boolean deleteFileAfterUpload) throws Exception {
//        // add a new item to the queue
//        FileToEncode fte = new FileToEncode(relativeName, dataFile, padding, isProjectFile, deleteFileAfterUpload);
//        try {
//            // add to the queue -- it'll auto-block if too many files are waiting to go on-line
//            while (!this.unencodedFileQueue.offer(fte, 50, TimeUnit.MILLISECONDS)) {
//                // check for any exceptions that might be thrown
//                bubbleAnyExceptions();
//            }
//        } catch (InterruptedException ex) {
//            throw new RuntimeException("Can't add file to the upload queue. Upload queue prematurely terminated.");
//        }
//    }
//    
//    private void uploadChunk(final byte[] buffer, final BigHash hash, final String fileRelativeName, final int bytesRead, final boolean isMetaData) throws Exception {
//        
//        // wrap the whole thing in a try/finally to ensure logging
//        final StressAddFileToolChunkEvent event = new StressAddFileToolChunkEvent(this, hash, fileRelativeName, isMetaData);
//        
//        // Only one server will actually get data chunk. It will be used to remotely replicate to other servers.
//        String workhorseServerURL = null;
//        
//        // get all of the servers to try
//        event.serverList = getServersToTry(hash);
////        System.out.println("Upload Chunk Selected Server(s): "+hash.getLength());
////        for (String server : event.serverList) {
////            System.out.println("  "+server);
////        }
//        
//        // calc the number of servers to really use -- lower optimal count if there aren't enough servers
//        event.serversToUploadTo = getServersToUploadTo();
//        if (event.serverList.size() < getServersToUploadTo()) {
//            event.serversToUploadTo = event.serverList.size();
//        }
//        
//        // if out of servers, throw an exception
//        if (event.serversToUploadTo == 0) {
//            // note that nothing is available!
//            throw new Exception("No servers to upload to!");
//        }
//        
//        // keep track of the number of bytes read
//        final int b = bytesRead;
//        // add using the helper methods
//        event.bytesToUpload = new byte[b];
//        System.arraycopy(buffer, 0, event.bytesToUpload, 0, b);
//        
//        // Hold on to exception message
//        String exceptionMsg = null;
//        
//        // Keep track of out of space servers
//        int serversOutOfSpace = 0;
//        
//        // start all of the pings
//        BooleanCallback[] skipCallbacks = new BooleanCallback[event.serverList.size()];
//        for (int i=0;i<event.serverList.size() && isSkipExistingChunk();i++) {
//            String url = event.serverList.get(i);
//            TrancheServer ts = StressIOUtil.connect(url,caller);
//            try {
//                if (ts instanceof RemoteTrancheServer) {
//                    RemoteTrancheServer rts = (RemoteTrancheServer)ts;
//                    // get the callback/request started
//                    if (isMetaData) {
//                        skipCallbacks[i] = rts.hasMetaDataInternal(hash);
//                    } else {
//                        skipCallbacks[i] = rts.hasDataInternal(hash);
//                    }
//                }
//            } finally {
//                IOUtil.safeClose(ts);
//            }
//        }
//        
//        // Add the file chunk to every server.
//        SERVER_LOOP:
//            for (int i=0;i<event.serverList.size();i++) {
//                String server = event.serverList.get(i);
//                // break out if enough are on-line
//                if (event.uploadedTo.size() + event.skipped.size() >= event.serversToUploadTo) {
//                    break;
//                }
//                
//                // flag for if it was successful
//                boolean chunkUploadSuccessful = false;
//                // attempt to transfer the chunk with retries
//                for (int retryCount = 0;!chunkUploadSuccessful && retryCount < maxRetries; retryCount++){
//                    
//                    // pause
//                    while (isPaused) {
//                        Thread.sleep(500);
//                    }
//                    
//                    // stop
//                    if (stop) {
//                        Thread.currentThread().interrupt();
//                        return;
//                    }
//                    
//                    // if the server is off-line or past failure rate, skip
//                    ServerInfo sic = ServerUtil.getServerInfo(server);
//                    if (sic != null) {
//                        // check for exceeding failure rate
//                        long failureCutoff = ServerUtil.getUploadFailureRateCutoff();
//                        if (sic.getFails() > failureCutoff) {
//                            //System.out.println("Failure Rate Too High. Skipping: "+server);
//                            break;
//                        }
//                        // check for off-line
//                        if (!sic.isOnline()) {
//                            //System.out.println("Off-line. Skipping: "+server);
//                            break;
//                        }
//                    }
//                    // Connect to server
//                    TrancheServer dfs = null;
//                    try {
//                        // connect to the server
//                        dfs = StressIOUtil.connect(server,caller);
//                        
//                        // conditionally skip the chunk if it is already uploaded
//                        if (isMetaData) {
//                            if (isSkipExistingChunk() && skipCallbacks[i].getResponse()) {
//                                // add to the used list
//                                event.skipped.add(server);
//                                continue SERVER_LOOP;
//                            }
//                        } else {
//                            if (isSkipExistingChunk() && skipCallbacks[i].getResponse()) {
//                                // add to the used list
//                                event.skipped.add(server);
//                                continue SERVER_LOOP;
//                            }
//                        }
//                        
//                        // upload meta-data either directly or via a proxy server (i.e. 'workhorse')
//                        if (isMetaData) {
//                            // If not uploaded once, do so
//                            if (workhorseServerURL == null || !isUseRemoteReplication()) {
//                                IOUtil.setMetaData(dfs, getPublicCertificate(), getPrivateKey(), hash, event.bytesToUpload);
//                                
//                                // Only if using remote
//                                if (isUseRemoteReplication())
//                                    workhorseServerURL = IOUtil.createURL(dfs);
//                            }
//                            
//                            // Use remote server to replicate to other servers
//                            else {
//                                // Note we are getting the nonce for the remote target server, not workhorse
//                                byte[] nonce = dfs.getNonce();
//                                
//                                // make the bytes
//                                byte[] bytes = new byte[nonce.length+BigHash.HASH_LENGTH+event.bytesToUpload.length];
//                                System.arraycopy(nonce, 0, bytes, 0, nonce.length);
//                                byte[] hashBytes = hash.toByteArray();
//                                System.arraycopy(hashBytes, 0, bytes, nonce.length, hashBytes.length);
//                                System.arraycopy(event.bytesToUpload, 0, bytes, nonce.length+hashBytes.length, event.bytesToUpload.length);
//                                
//                                
//                                String alg = SecurityUtil.getSignatureAlgorithm(getPrivateKey());
//                                
//                                byte[] sig_bytes = SecurityUtil.sign(new ByteArrayInputStream(bytes), getPrivateKey(), alg);
//                                
//                                Signature sig = new Signature(sig_bytes, alg, getPublicCertificate());
//                                
//                                // connect to the workhorse server
//                                TrancheServer workhorseServer = StressIOUtil.connect(workhorseServerURL,caller);
//                                try {
//                                    // invoke the remote replication on that server
//                                    IOUtil.setRemoteMetaData(workhorseServer, server, sig, nonce, hash);
//                                }
//                                // fall back on normal replication
//                                catch (Exception e){
//                                    // ugly, but this will cover any faults in remote replication
//                                    System.out.println("--- Can't remote rep meta-data. falling back on normal ---");
//                                    e.printStackTrace(System.out);
//                                    IOUtil.setMetaData(dfs, getPublicCertificate(), getPrivateKey(), hash, event.bytesToUpload);
//                                } finally {
//                                    IOUtil.safeClose(workhorseServer);
//                                }
//                            }
//                            
//                            event.uploadedTo.add(server);
//                        } else {
//                            // If not uploaded once, do so
//                            if (workhorseServerURL == null || !isUseRemoteReplication()) {
//                                IOUtil.setData(dfs, getPublicCertificate(), getPrivateKey(), hash, event.bytesToUpload);
//                                
//                                // Only if using remote
//                                if (this.isUseRemoteReplication())
//                                    workhorseServerURL = IOUtil.createURL(dfs);
//                            }
//                            
//                            // Use remote server to replicate to other servers
//                            else {
//                                // Note we are getting the nonce for the remote target server, not workhorse
//                                byte[] nonce = dfs.getNonce();
//                                
//                                // make the bytes
//                                byte[] bytes = new byte[nonce.length+BigHash.HASH_LENGTH+event.bytesToUpload.length];
//                                System.arraycopy(nonce, 0, bytes, 0, nonce.length);
//                                byte[] hashBytes = hash.toByteArray();
//                                System.arraycopy(hashBytes, 0, bytes, nonce.length, hashBytes.length);
//                                System.arraycopy(event.bytesToUpload, 0, bytes, nonce.length+hashBytes.length, event.bytesToUpload.length);
//                                
//                                String alg = SecurityUtil.getSignatureAlgorithm(getPrivateKey());
//                                
//                                byte[] sig_bytes = SecurityUtil.sign(new ByteArrayInputStream(bytes), getPrivateKey());
//                                
//                                Signature sig = new Signature(sig_bytes, alg, getPublicCertificate());
//                                
//                                // connect to the workhorse server
//                                TrancheServer workhorseServer = StressIOUtil.connect(workhorseServerURL,caller);
//                                try {
//                                    // do the remote replication
//                                    IOUtil.setRemoteData(workhorseServer, server, sig, nonce, hash);
//                                }
//                                // fall back on normal replication
//                                catch (Exception e){
//                                    // failed remote upload! Falling back on regular
//                                    System.out.println("--- Failed RemoteData to "+workhorseServerURL+" to "+server+". Falling back on regular ---");
//                                    e.printStackTrace(System.out);
//                                    // ugly, but this will cover any faults in remote replication
//                                    IOUtil.setData(dfs, getPublicCertificate(), getPrivateKey(), hash, event.bytesToUpload);
//                                } finally {
//                                    IOUtil.safeClose(workhorseServer);
//                                }
//                            }
//                            // flag this server as having a copy of the data
//                            event.uploadedTo.add(server);
//                        }
//                        // flag as successful
//                        chunkUploadSuccessful = true;
//                    }
//                    
//                    // catch any socket timeouts
//                    catch (Exception ste) {
//                        System.out.println("[ AFT Caught Error ] Retrying "+(maxRetries - retryCount)+" more times; "+server);
//                        ste.printStackTrace(System.out);
//                        exceptionMsg = ste.getMessage();
//                        // tally the exception
//                        ServerInfo si = ServerUtil.getServerInfo(server);
//                        if (si != null) {
//                            si.incrementFails();
//                            System.out.println("Reporting Failure: "+si.getFails()+" for "+server+"; current cutoff: "+ServerUtil.getUploadFailureRateCutoff());
//                        }
//                    } finally {
//                        IOUtil.safeClose(dfs);
//                    }
//                } // Trying upload to server n times (n = max retries)
//                
//                // if it wasn't a success, flag as off-line
//                if (!chunkUploadSuccessful) {
//                    // log the server that errored
//                    event.errors.add(server);
//                    
//                    if (isOutOfSpaceMsg(exceptionMsg)) {
//                        serversOutOfSpace++;
//                    }
//                    
////                    // add to the skip list
////                    ServerUtil.setServerOnlineStatus(server, false);
////                    System.out.println("Flagging Off-line: "+server);
//                }
//            }
//            
//            // flag for if the uploaded succeeded
//            event.uploadSucceeded = !(event.uploadedTo.size() + event.skipped.size() < event.serversToUploadTo);
//            
//            // If flag is false, try to determine whether exception due to out of
//            // space exception AND at least one server has chunk
//            if (!event.uploadSucceeded) {
//                if (serversOutOfSpace > 0 && event.uploadedTo.size() + event.skipped.size() >= 1) {
//                    event.uploadSucceeded = true;
//                }
//            }
//            // if not enough files uploaded, fire the event
//            if (!event.uploadSucceeded) {
//                
//                // Build message
//                String msg = "Only uploaded "+(event.uploadedTo.size()+event.skipped.size())+" out of "+getServersToUploadTo();
//                
//                // Is there addition information available?
//                if (exceptionMsg != null) {
//                    msg += System.getProperty("line.separator") + exceptionMsg;
//                }
//                
////                // Other exceptions may occur, they need reported, too
////                resetMsgAboutUploadFailure();
//                
//                // add to the list of failures
//                throw new Exception(msg);
//            }
//            
//            // pause
//            while (isPaused) {
//                Thread.sleep(500);
//            }
//            
//            // stop
//            if (stop) {
//                Thread.currentThread().interrupt();
//                return;
//            }
//            
//            // notify the listeners of the chunk upload
//            fireChunkUpload(event);
//    }
//    
//    /**
//     * Returns true only if currently registered exception is about a server out of space.
//     */
//    private boolean isOutOfSpaceMsg(String msg) {
//        if (msg == null)
//            return false;
//        
//        boolean isOutOfSpaceMsg = msg.contains(this.OUT_OF_SPACE_SERVER_MSG) || msg.contains(this.OUT_OF_SPACE_DIR_MSG);
//        return isOutOfSpaceMsg;
//    }
//    
//    /**
//     * Returns a list of online servers to use sorted by speed. If user set certain server, considers only those.
//     */
//    public List<String> getServersToTry(final BigHash partHash) {
//        // if the user didn't specify any, fall back on all
//        if (!triedSettingServersToUse) {
//            // start by adding the known server
//            List<String> dfsUrlsToTry = new ArrayList();
//            // add all servers with spans
//            dfsUrlsToTry.addAll(ServerUtil.getServersWithHash(partHash));
//            // populate a hash set to check for repeats
//            HashSet usedServers = new HashSet();
//            usedServers.addAll(dfsUrlsToTry);
//            // fall back on all servers
//            List<ServerInfo> allServers = ServerUtil.getServers();
//            // sort pseudo-randomly according to known seed (i.e. predictable sort in case of future re-uploads)
//            Collections.shuffle(allServers, new Random(partHash.getLength()));
//            // get the failure cutoff
//            long limitFailures = ServerUtil.getUploadFailureRateCutoff();
//            // add the servers in the order they are to be tried
//            for (ServerInfo si : allServers) {
//                // skip off-line servers and remove from the servers to use
//                if (!si.isOnline()) continue;
//                // skip server URLs that were already included via the hash span
//                if (usedServers.contains(si.getUrl())) continue;
//                // check if the server shoulnd't be uploaded to
//                if (si.getFails() > limitFailures) {
//                    System.out.println("Won't try "+si.getUrl()+" has "+si.getFails()+" and cutoff is "+limitFailures);
//                    continue;
//                }
//                // otherwise, add the server as a fallback
//                dfsUrlsToTry.add(si.getUrl());
//            }
//            return dfsUrlsToTry;
//        }
//        // if the user specified some servers, try to use just them -- we have to still abide by hash span rules
//        else {
//            // split the servers in to two groups: hashspan and non
//            List<String> inHashSpan = new ArrayList(10);
//            List<String> outOfHashSpan = new ArrayList(10);
//            // get the limit of acceptable upload failures
//            long limitFailures = ServerUtil.getUploadFailureRateCutoff();
//            // check each server for validity and hash span
//            for (String s : serversToUse) {
//                // register the servers
//                ServerInfo si = ServerUtil.getServerInfo(s);
//                // Make sure stress server flagged online
//                ServerUtil.setServerOnlineStatus(s,true);
//                // skip the server if it is off-line
//                if (si != null && !si.isOnline()) continue;
//                // skip the server if it is past the failure rate
//                if (si != null && si.getFails() > limitFailures) {
//                    System.out.println("Won't try "+si.getUrl()+" has "+si.getFails()+" and cutoff is "+limitFailures);
//                    continue;
//                }
//                
//                // check if the server is in the hash
//                synchronized (si) {
//                    
//                    if (si.getConfiguration() == null) {
//                        outOfHashSpan.add(si.getUrl());
//                        continue;
//                    }
//                    
//                    Set<HashSpan> spans = si.getConfiguration().getHashSpans();
//                    boolean inSpan = false;
//                    for (HashSpan span : spans) {
//                        if (span.contains(partHash)) {
//                            inSpan = true;
//                            break;
//                        }
//                    }
//                    // add according to the span
//                    if (inSpan) {
//                        inHashSpan.add(si.getUrl());
//                    } else {
//                        outOfHashSpan.add(si.getUrl());
//                    }
//                }
//            }
//            // sort pseudo-randomly according to known seed (i.e. predictable sort in case of future re-uploads)
//            Collections.shuffle(outOfHashSpan, new Random(partHash.getLength()));
//            // add to the primary list
//            inHashSpan.addAll(outOfHashSpan);
//            return inHashSpan;
////            return ServerUtil.sortServersBySpeed(servers);
//        }
//    }
//    
//    /**
//     * Returns relative name of file in root dir. Used by ProjectFile to store location in project, which is used by GetFileTool when downloading a project.
//     */
//    private String relativeName(File rootDir, File dataFile) throws IOException {
//        // create the relative name
//        String rootString = rootDir.getCanonicalPath();
//        String file = dataFile.getCanonicalPath();
//        
//        // if equal, return the name -- why are these equal??
//        if (rootString.equals(file)) {
//            return dataFile.getName();
//        }
//        
//        String toReturn = file.substring(rootString.length()+1);
//        toReturn = rootDir.getName()+"/"+toReturn.replaceAll("\\\\", "/");
//        
//        return toReturn;
//    }
//    
//    public PrivateKey getPrivateKey() {
//        return privateKey;
//    }
//    
//    public void setPrivateKey(PrivateKey privateKey) {
//        this.privateKey = privateKey;
//    }
//    
//    public X509Certificate getPublicCertificate() {
//        return publicCertificate;
//    }
//    
//    public void setPublicCertificate(X509Certificate publicCertificate) {
//        this.publicCertificate = publicCertificate;
//    }
//    
//    public String getPassphrase() {
//        return passphrase;
//    }
//    
//    public void setPassphrase(String passphrase) {
//        this.passphrase = passphrase;
//        
//        // toggle on encryption
//        setUseEncryption(passphrase != null);
//        
//        // set the default padding to be the BigHash of the password
//        if (passphrase != null) {
//            padding = new BigHash(passphrase.getBytes()).toByteArray();
//        }
//    }
//    
//    public boolean isUseEncryption() {
//        return useEncryption;
//    }
//    
//    public void setUseEncryption(boolean useEncryption) {
//        this.useEncryption = useEncryption;
//        
//        // toggle off if false
//        if (!useEncryption){
//            passphrase = null;
//            return;
//        }
//        
//        // check for a password
//        if (getPassphrase()==null){
//            passphrase = SecurityUtil.generateBase64Password();
//        }
//    }
//    
//    public void setDescription(String description) {
//        this.description = description;
//    }
//    public void setTitle(String title) {
//        this.title = title;
//    }
//    public String getDescription() {
//        return description;
//    }
//    public String getTitle() {
//        return title;
//    }
//    
//    public boolean useCompression() {
//        return useCompression;
//    }
//    
//    public void setUseGZIP(boolean useGZIP) {
//        this.useCompression = useGZIP;
//    }
//    
//    /**
//     * Number of bytes uploaded through this AddFileTool thus far.
//     */
//    public long getDataUploaded() {
//        return dataUploaded;
//    }
//    
//    /**
//     * Total number of raw (unzipped, unencrypted) bytes to be uploaded
//     * by the AddFileTool.
//     */
//    public long getDataToUpload() {
//        return dataToUpload;
//    }
//    
//    public boolean isSkipPreviouslyUploaded() {
//        return skipPreviouslyUploaded;
//    }
//    
//    public void setSkipPreviouslyUploaded(boolean skipPreviouslyUploaded) {
//        this.skipPreviouslyUploaded = skipPreviouslyUploaded;
//    }
//    
//    public int getServersToUploadTo() {
//        return serversToUploadTo;
//    }
//    
//    public void setServersToUploadTo(int serversToUploadTo) {
//        this.serversToUploadTo = serversToUploadTo;
//    }
//    
//    public boolean isSingleFileUpload() {
//        return singleFileUpload;
//    }
//    
//    public void setSingleFileUpload(boolean singleFileUpload) {
//        this.singleFileUpload = singleFileUpload;
//    }
//    
//    public boolean isSkipExistingChunk() {
//        return skipExistingChunk;
//    }
//    
//    public void setSkipExistingChunk(boolean skipExistingChunk) {
//        this.skipExistingChunk = skipExistingChunk;
//    }
//    
//    public boolean isExplodeBeforeUpload() {
//        return explodeBeforeUpload;
//    }
//    
//    public void setExplodeBeforeUpload(boolean explodeBeforeUpload) {
//        this.explodeBeforeUpload = explodeBeforeUpload;
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
//    public boolean isLoggingEnabled() {
//        return loggingEnabled;
//    }
//    
//    public void setLoggingEnabled(boolean loggingEnabled) {
//        this.loggingEnabled = loggingEnabled;
//        // also turn on the exception logging
//        LogUtil.setEnabled(true);
//    }
//    
//    public long getBufferEncodingsInMemorySize() {
//        return bufferEncodingsInMemoryCutoff;
//    }
//    
//    public void setBufferEncodingsInMemorySize(long bufferEncodingsInMemorySize) {
//        this.bufferEncodingsInMemoryCutoff = bufferEncodingsInMemorySize;
//    }
//    
//    public UserZipFile getUser() {
//        return user;
//    }
//    
//    public void setUser(UserZipFile user) {
//        this.user = user;
//    }
//    
//    public void setUserPassword(String userPassword) {
//        this.userPassword = userPassword;
//    }
//    
//    public boolean userPasswordIsSet() {
//        return this.userPassword != null;
//    }
//    
//    public String getUserPassword() {
//        return userPassword;
//    }
//    
//    public int getMaxRetries() {
//        return maxRetries;
//    }
//    
//    public void setMaxRetries(int maxUploadAttempts) {
//        this.maxRetries = maxUploadAttempts;
//    }
//    
//    public boolean isUseRemoteReplication() {
//        return useRemoteReplication;
//    }
//    
//    public void setUseRemoteReplication(boolean use) {
//        useRemoteReplication = use;
//    }
//    
//    /**
//     * A helper class that takes unencoded files, encodes them, and queues up the encoded files for later upload.
//     * @author Jayson Falkner - jfalkner@umich.edu
//     */
//    class FileEncodingThread extends Thread {
//        // buffer to help IO speed
//        final byte[] buffer = new byte[1000];
//        
//        // finished flag
//        boolean finished = false;
//        
//        public FileEncodingThread() {
//            // don't let this thread pause JVM termination
//            setDaemon(true);
//            setPriority(Thread.MIN_PRIORITY);
//            // set the name
//            setName("AddFileTool File Uploading Thread");
//        }
//        
//        public void run() {
//            try {
//                doWork();
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }
//        
//        private void doWork() throws Exception {
//            // go until finished
//            while (!finished || !unencodedFileQueue.isEmpty()) {
//                try {
//                    // stop
//                    if (stop) {
//                        Thread.currentThread().interrupt();
//                        return;
//                    }
//                    
//                    // get from the queue a file that needs to be encoded
//                    FileToEncode fte = unencodedFileQueue.poll(100, TimeUnit.MILLISECONDS);
//                    // if nothing is had, loop
//                    if (fte == null) {
//                        Thread.yield();
//                        continue;
//                    }
//                    
//                    boolean succeded = false;
//                    BigHash noneHash = null;
//                    try {
//                        
//                        // file IO just once
//                        FileInputStream fis = new FileInputStream(fte.dataFile);
//                        BufferedInputStream bis = new BufferedInputStream(fis);
//                        
//                        // make the meta-data
//                        MetaData metaData = new MetaData();
//                        String[] parts = fte.relativeName.split("/");
//                        metaData.setName(parts[parts.length-1]);
//                        
//                        // the final stream
//                        PipedOutputStream pos = new PipedOutputStream(fte.relativeName, metaData);
//                        AESEncodingStream aes = null;
//                        GZIPEncodingStream gzip = null;
//                        
//                        // list of streams
//                        LinkedList hashStreams = new LinkedList();
//                        
//                        // the currrent stream
//                        OutputStream currentStream = pos;
//                        
//                        // layer encodings backwards -- think of how the write() calls will be invoked
//                        if(isUseEncryption()) {
//                            // use a compressing stream
//                            aes = new AESEncodingStream(getPassphrase(), currentStream);
//                            currentStream = aes;
//                            hashStreams.addFirst(aes);
//                        }
//                        
//                        // force compression and it must result in a file with a different hash
//                        if (useCompression()) {
//                            // use a compressing stream
//                            gzip = new GZIPEncodingStream(currentStream);
//                            currentStream = gzip;
//                            hashStreams.addFirst(gzip);
//                        }
//                        
//                        // finally calc the unencoded hash info
//                        BigHashMaker noneBigHashMaker = new BigHashMaker();
//                        WrappedOutputStream noneOutputStream = new WrappedOutputStream(currentStream, noneBigHashMaker);
//                        hashStreams.addFirst(noneOutputStream);
//                        
//                        // write all of the data through the stream chain
//                        for (int bytesRead = bis.read(buffer);bytesRead != -1; bytesRead = bis.read(buffer)) {
//                            noneOutputStream.write(buffer, 0, bytesRead);
//                        }
//                        // if padding is used, add it -- always added to the end of files
//                        if (padding.length != 0) {
//                            noneOutputStream.write(padding);
//                        }
//                        // flush
//                        noneOutputStream.flush();
//                        noneOutputStream.close();
//                        
//                        // figure out how many bytes where sent vs. in original file
//                        noneHash = BigHash.createFromBytes(noneBigHashMaker.finish());
//                        long sizeDiff = noneHash.getLength();
//                        if (aes != null) {
//                            sizeDiff -= aes.getHash().getLength();
//                        } else if (gzip != null) {
//                            sizeDiff -= gzip.getHash().getLength();
//                        }
//                        
//                        // make the default encoding
//                        FileEncoding none = new FileEncoding(FileEncoding.NONE, noneHash);
//                        metaData.getEncodings().add(none);
//                        // check/add gzip
//                        if (gzip != null) {
//                            FileEncoding gzipEncoding = new FileEncoding(FileEncoding.GZIP, gzip.getHash());
//                            metaData.getEncodings().add(gzipEncoding);
//                        }
//                        // check/add aes
//                        if (aes != null) {
//                            FileEncoding aesEncoding = new FileEncoding(FileEncoding.AES, aes.getHash());
//                            metaData.getEncodings().add(aesEncoding);
//                        }
//                        
//                        // close the streams
//                        try {
//                            if (fis != null) fis.close();
//                            if (bis != null) bis.close();
//                            if (pos != null) pos.close();
//                            if (aes != null) aes.close();
//                            if (gzip != null) gzip.close();
//                        } catch (Exception e) {
//                            // do nothing
//                        }
//                        
////                        System.out.println("Uploaded: "+noneHash);
////                        System.out.println("  "+metaData.getEncodings().get(0));
////                        System.out.println("  "+metaData.getEncodings().get(metaData.getEncodings().size()-1));
//                        
//                        // conditionally add theproject file part
//                        if (fte.isProjectFilePart) {
//                            // the part's is: name, normal hash, encoded hash
//                            ProjectFilePart pfp = new ProjectFilePart(fte.relativeName, noneHash, fte.padding);
//                            projectFileParts.add(pfp);
//                        }
//                        
//                        // change the bytes to upload according to the encoded file's size
//                        StressAddFileTool.this.dataToUpload -= sizeDiff;
//                        
//                        
//                        // upload the meta-data
//                        // add a signature for the data to the meta-data
//                        byte[] sigBytes = SecurityUtil.sign(fte.dataFile, getPrivateKey());
//                        String algorithm = SecurityUtil.getSignatureAlgorithm(getPrivateKey());
//                        Signature sig = new Signature(sigBytes, algorithm, getPublicCertificate());
//                        metaData.getSignatures().add(sig);
//                        
//                        // if it is a project file, flag the bit
//                        if (!fte.isProjectFilePart) {
//                            metaData.setFlags(metaData.getFlags() | MetaData.PROJECT_FILE_BIT);
//                        }
//                        
//                        // upload the meta-data by serializing it in memory
//                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                        MetaDataUtil.write(metaData, baos);
//                        // check size, if it is bigger than a block we'd have to make yet another SplitFile....throw exceptions for now
//                        if (baos.size() > DataBlockUtil.ONE_MB) {
//                            throw new RuntimeException("meta-data file is too large for this code version! Please request that this feature be added and provide a description of the files sizes being uploaded.");
//                        }
//                        // to a byte array
//                        byte[] splitFileBytes = baos.toByteArray();
//                        
//                        // track any errors
//                        LinkedList<Exception> chunkUploadErrors = new LinkedList();
//                        
//                        // try to upload the chunk repeatedly
//                        for (int uploadAttemptCount=0;uploadAttemptCount<getMaxRetries();uploadAttemptCount++) {
//                            // try to do the upload
//                            try {
//                                // upload this file
//                                uploadChunk(splitFileBytes, noneHash, fte.relativeName, splitFileBytes.length, true);
//                                
//                                // break out if successful
//                                break;
//                            }
//                            
//                            catch (Exception e) {
//                                // failed one upload, try another
//                                chunkUploadErrors.add(e);
//                            }
//                        }
//                        
//                        // check that all attemps didn't error
//                        if (chunkUploadErrors.size() >= getMaxRetries()) {
//                            // pitch an exception
//                            throw new RuntimeException("Upload error.", chunkUploadErrors.getFirst());
//                        }
//                        
//                        // flag as succeeded
//                        succeded = true;
//                        
//                        // notify that a file has been uploaded
//                        fireAddedFile(fte.relativeName, noneHash);
//                        
//                        // if requested, delete the original file
//                        if (fte.deleteFileAfterUpload && fte.dataFile.exists() && !fte.dataFile.delete()) {
//                            throw new RuntimeException("Can't delete uploaded file with delete-after flag "+fte.dataFile);
//                        }
//                    } finally {
//                        // notify that a file has been uploaded
//                        if (!succeded) {
//                            fireFailedFile(fte.relativeName, noneHash);
//                        }
//                    }
//                }
//                // catch all other exceptions and report them
//                catch (InterruptedException e) {
//                    // bubble the exception out
//                    System.out.println("FET Thread Interrupted!");
//                }
//                // catch all other exceptions and report them
//                catch (Exception e) {
//                    e.printStackTrace(System.out);
//                    // bubble the exception out
//                    StressAddFileTool.this.waitForExceptionToBeReported(this, e);
//                }
//            }
//        }
//    }
//    
//    public void setShowUploadSummary(boolean showUploadSummary) {
//        this.showUploadSummary = showUploadSummary;
//    }
//    
//    class PipedOutputStream extends OutputStream {
//        // buffer one chunk and upload
//        byte[] buffer = new byte[DataBlockUtil.ONE_MB];
//        int bufferOffset = 0;
//        // the relative name
//        String relativeName;
//        // flag for closed
//        boolean closed = false;
//        // the meta-data to update
//        MetaData metaData;
//        
//        
//        public PipedOutputStream(String relativeName, MetaData metaData) {
//            this.relativeName = relativeName;
//            this.metaData = metaData;
//        }
//        
//        public void write(byte[] b, int off, int len) throws IOException {
//            try {
//                // if closed, throw exception
//                if (closed) throw new IOException("Stream is closed!");
//                
//                // update the buffer
//                for (int i=0;i < len;i++) {
//                    buffer[bufferOffset] = b[i];
//                    bufferOffset++;
//                    // if full, upload the chunk
//                    if (bufferOffset >= buffer.length) {
//                        // calc the appropriate hash
//                        BigHash hash = new BigHash(buffer);
//                        // upload teh chunk
//                        uploadChunk(buffer, hash, relativeName, buffer.length, false);
//                        // reset the offset
//                        bufferOffset = 0;
//                        // add the meta-data chunk
//                        metaData.getParts().add(hash);
//                    }
//                }
//            } catch (Exception e) {
//                throw new RuntimeException("Caught exception!", e);
//            }
//        }
//        
//        public void write(byte[] b) throws IOException {
//            write(b, 0, b.length);
//        }
//        
//        byte[] buf = new byte[1];
//        public void write(int b) throws IOException {
//            buf[0] = (byte)b;
//            write(buf);
//        }
//        
//        public void flush() throws IOException {
//            // noop
//        }
//        
//        public void close() throws IOException {
//            try {
//                // upload the last chunk!
//                if (closed) {
//                    return;
//                }
//                // upload the last chunk
//                closed = true;
//                // calc the appropriate hash
//                BigHash hash = new BigHash(buffer, bufferOffset);
//                // upload teh chunk
//                uploadChunk(buffer, hash, relativeName, bufferOffset, false);
//                // add the meta-data chunk
//                metaData.getParts().add(hash);
//            } catch (Exception e) {
//                throw new RuntimeException("Caught exception!", e);
//            }
//        }
//    }
//}
//
///**
// * State-keeping class that uses very little memory and passes required parameters to the file encoding thread.
// */
//class FileToEncode {
//    final String relativeName;
//    final File dataFile;
//    final byte[] padding;
//    final boolean isProjectFilePart;
//    final boolean deleteFileAfterUpload;
//    
//    FileToEncode(String relativeName, File dataFile, byte[] padding, boolean isProjectFilePart, boolean deleteFileAfterUpload) {
//        this.relativeName = relativeName;
//        this.dataFile = dataFile;
//        this.padding = padding;
//        this.isProjectFilePart = isProjectFilePart;
//        this.deleteFileAfterUpload = deleteFileAfterUpload;
//    }
}

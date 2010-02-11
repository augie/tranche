****************************************
           INCLUDED TOOLS
****************************************

Certs: Makes a set of user zip files to be used as the base users of a repository.

Get: Downloads.

Add: Uploads.

Server: Starts a data server.

GUI: Launches the Java Swing GUI.



****************************************
        GENERAL INSTRUCTIONS
****************************************

PREREQUISITE: Install Java 5 or later version

1. Check that the ports that are used by your repository are open.

2. Unzip the downloaded zip file.

3. Navigate to the top-most directory in the unzipped file.

4. Run the following command to print usage, replacing <JAR> with the jar file name: java -jar ./<JAR>



****************************************
      TOOL-SPECIFIC INSTRUCTIONS
****************************************

Add, Get, Server, GUI: The first argument must be the location of the configuration file for the Tranche repository. This can be in the JVM classpath, on the local file system, or on the Internet.

Server: If you are starting your own Tranche repository, you must have/create a configuration file and certificates.



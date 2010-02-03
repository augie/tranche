***********************************************************
        INSTRUCTIONS ON RUNNING A TRANCHE SERVER
***********************************************************

PREREQUISITES: 
	* Install Java 5 or later version
	* If you are starting your own Tranche repository, you must have/create a configuration file and certificates.

1. Check that your desired port is open. If it is not, your Tranche server will not communicate with the outside world.

2. Unzip the downloaded Tranche server zip file.

3. Navigate to the top-most directory in the unzipped file.

4. Run the following command and read the output to determine what to put in palce of {OPTIONS} in the next step: java -jar ./tranche.jar

5. Run the following command to start your Tranche server, replacing {CONFIGURATION FILE LOCATION} with the classpath location, file system location , or URL of the configuration file for your repository: java -Xmx512m -jar ./tranche.jar {CONFIGURATION FILE LOCATION} {OPTIONS}

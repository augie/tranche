=====================================================
README
=====================================================
This allows user(s) to run a stress test using separate connections for a lot of simulated clients.

There are two modes:
* SINGLE: One client computer. This allows the server to clean up files after itself.
* MULTI: Allows multiple client computers. Server cannot clean up after itself, and must be manually cleaned. When in doubt, use this mode.

For more information, see RUNNING THE CLIENT

=====================================================
HOW THIS WORKS
=====================================================
The server has a socket listening for stress client connections. It understands a very simple protocol. (See class: stress.server.Protocol)

Here's a simple transaction:

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

# Start a test, meaning start a Tranche server on a different port. See class: stress.server.Configuration for port
Client sends message => START
Server receives message, responds => OK

# Now the client pounds the Tranche server, which is on a different port than the stress server. When done...

Client sends message => STOP
Server receives message, responds => OK

# Perhaps a few more tests. Tests are automated, so get some sleep!

# Client is done with all tests...

Client sends message => DISCONNECT
Server receives message, responds => OK

# Now both server and client are offline!

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

Each client has n client threads, where n is a number specified in the configuration file. (See: RUNNING THE CLIENT) Each thread uploads its own project and subsequently downloads it, verifying it's contents.

=====================================================
RUNNING THE SERVER
=====================================================

Configuring the server is simple. Open up stress.server.Configuration:

* TRANCHE_SERVER_PORT <Integer>: Must be different from STRESS_SERVER_PORT. If change, the client will need to change corresponding value in stress.client.Configuration.
* STRESS_SERVER_PORT <Integer>: Must be different from TRANCHE_SERVER_PORT. If change, the client will need to change corresponding value in stress.client.Configuration. Not a lot of activity will happen across the socket binding to this port, but it is important for scripting activity.
* DATA_DIRECTORY_PATH <String>: Choose a directory where a _lot_ of data can be stored.

After compiling (Netbeans preferred method), simply run StressServer.main.

=====================================================
RUNNING THE CLIENT
=====================================================

Configuring the client is more complex, but not so much so. Open up stress.client.Configuration:

* SERVER_IP <String>: The IPv4 address of the server.
* TRANCHE_SERVER_PORT <Integer>: Must match the running server's config.
* STRESS_SERVER_PORT <Integer>: Must match the running server's config.
* TEMP_DIRECTORY <String>: Choose a directory with a lot of space. Will be auto-deleted, so think twice!!!
* MULTI_MODE <Boolean>: False if only you are going to run, true if intending on running more than one client at a time.
* OUTPUT_FILE <String>: Path for the output file, the data from running the test. Think twice: shouldn't be in temporary directory or will be deleted! (Desktop is a good choice. Make sure not to clobber output from previous run.)

(NOPE: Moved to client.conf, in this directory. Self-explanatory.)

After configuring, you must create the test. The test is scriptable using a very simple Domain Specific Language (DSL) so you can start the test and do something else.

In the Netbeans project, there is a subdirectory called "files". (For convenience, you can add to the Project view in Netbeans by right-clicking on Project, select "Properties" > "Sources". In "Source Package Folders", click "Add Project", navigate to the Netbeans project directory and add the subdir called "files".)

In this directory, the two relevant files are:
* stress_test.conf: Create your test suite here.
* examples.conf: Show an example file with comments. If you follow its design, your test should work. Note there are multiple tests, but that is fine: this stress test is designed to run multiple tests in succession!

Each test has only three required options:

test {
  client_count=3
  files=10
  max_file_size=32768
}

This means that there will be 3 simulatenous clients (each with separate socket connection to Tranche server) each uploading a test project of random data with 10 files, each have random size of max 32768 bytes (32KB).

There's a fourth option for deleting files. This will make the test run longer (by default, this is turned off):

test {
  client_count=3
  files=10
  max_file_size=32768
  delete_chunks_randomly=true
}

What if you want to see the effect of doubling the client size. The following test suite would do that for you:

test {
  client_count=3
  files=10
  max_file_size=32768
}

test {
  client_count=6
  files=10
  max_file_size=32768
}

The entire file is validated before starting the tests so that it will catch any errors early. Standard out will tell you whether they were successfully loaded. Once you get a message about configuration verified, your work is done.

After compiling (Netbeans preferred method), simply run StressClient.main. When finished, test results will be in the output file!
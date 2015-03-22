Each server on a Tranche network has a healing thread that runs as long as a server is running. This healing thread serves three primary functions:
  * **Downloading**: looks for any new chunks on the network (other servers) and downloads to itself if the chunk falls in the server's hash span.
  * **Deleting**: looks at the server's own chunks, and deletes any if not in the server's hash span and there are sufficient replications on the network.
  * **Repairing**: looks at the server's own chunks, and repairs if the chunk is corrupted and the server finds another copy on the network.
  * **Special case**: If a server does not have a hash span, as a few smaller or older servers do not, they will only hold on to data until it is replicated sufficiently. These servers will delete (everything, if sufficient resources) and repair, but not download.

The three compete for the healing thread's time. These were designed so that administrators can adjust the focus. (Eventually, we will design automatons to do this work intelligently.) This is accomplished by assigning how much of work each will do in succession. Each of the three handles a batch of chunks at the same time (default of 50 for each). However, this value is changed if a user sets a different value for the following three configuration attributes:
  * hashSpanFix: BatchSizeForDownloadChunks (default value of 50)
  * hashSpanFix: BatchSizeForDeleteChunks (default value of 50)
  * hashSpanFix: BatchSizeForReplicateChunks (default value of 50)

So using the default values, every time fifty chunks are checked for new data to download to a server, 50 more chunks are checked to be deleted from a server if there are sufficient replications and the chunk doesn't belong, and fifty chunks are checked to see whether they were corrupted.

So why would we want to change these values while the server is running?
  * A server without a hash span is low on space. In this case, we'd want to focus on deleting on this server, and perhaps focus on downloading on servers with full hash spans so that the no-hash span server can remain useful during uploads.
  * A known disk error causes bad chunks to replicate across the network. Setting repair high and download low will combat the replication more effectively than using the defaults. Furthermore, turning off delete could help prevent the loss of good copies of a chunk.
  * A lot of data just entered the network. Setting the downloads high can help the network balance the data according to the hash spans more quickly.
It is not possible to predetermine every scenario, but runtime adjustments allows the network administrators to respond to non-ideal circumstances and protect data.
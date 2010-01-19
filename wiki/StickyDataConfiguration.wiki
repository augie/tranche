A user can optionally upload a full copy of any data set to any server. This is called a sticky data set, since a full copy is kept on a server. In the event that that particular server is the only server available, the user will still be able to download the sticky data set.

However, just because a full copy of a data set is uploaded to a server doesn't mean it will stick. Servers actively try to delete data that does not belong to the server's hash span.

There are two options that will ensure that a sticky data set will stick (only one is required):

  * A server has a full hash span
  * Deletion is disabled for a server using the configuration name-value pair, `hashSpanFix: AllowDelete » false`

Either of these will ensure that chunks are not deleted. If one of these is not selected, then the server will still receive a full copy of the data set; it is only a matter of time, however, before the server will start removing chunks that do not belong to its hash span.

Having a full copy at a specific server might be reassuring, but this might be a problem, particularly for a small server.

Small servers can run out of disk space fast, which is why setting an appropriate hash span (the smaller the server, the smaller the hash span) and allowing the server to delete chunks outside its hash span is important.

Ultimately, this choice belongs to the administrator. If an administrator or user has a question about sticky data sets or configuring servers, they should contact us.
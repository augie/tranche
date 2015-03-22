## Options ##

  * **Annotations**: Provide more information about your upload. This information is stored in the meta data for the data set.
  * **Compress**: Whether to GZIP compress the data sets as they are stored on the repository. Your upload will be downloaded as it is uploaded, but it will take up less space on the repository.
  * **Explode**: Whether to unarchive and decrompess data sets as they are uploaded to the repository. Supports ZIP, TAR, GZIP, LZMA, and BZIP2.
  * **Data Only**: When selected, no meta data will be uploaded. This is typically used to replace data chunks that may be missing from the repository.
  * **Pool Size**: The number of uploads that will occur at any one time. Larger pool size can make the most of network latency but requires more memory and processor resources. Setting to 0 makes the pool size unlimited.
  * **Send Email On Failure**: Whether to be sent an email upon failure of an upload. Successful uploads always result in an email.
  * **Servers**: Specify the servers that are to be used for this upload. Typically, you should not change this, but the option is available for advanced users.
  * **Sticky Servers**: Specify the servers to which this data set should be "stuck".
  * **Use Unspecified Servers**: Whether any servers that are available on the network should be used for the upload or only the servers that you have specified.


## Tips ##

Don't worry about uploading too large of a file or too many small files. Tranche is very good at handling both of these cases.

Feel free to rearrange and upload the same data as many times as you want in order to make convenient downloads. Tranche is smart enough to reuse uploaded data. For example, you might upload an entire data set, then reupload just the peak lists, then reupload just the raw spectra. This would make 3 different hash strings that you could provide to let users download everything, just the peak lists, or just the raw data.


## Sticky Data ##

A user can optionally upload a full copy of any data set to any server. This is called a sticky data set, since a full copy is kept on a server, or stuck to it. In the event that that particular server is the only server available, the user will still be able to download the entire sticky data set.

**However**, just because a full copy of a data set is uploaded to a server doesn't mean it will stick. Servers actively try to delete data that does not belong to the server's target hash span. This is a defect of the current version of Tranche and there are plans to make this work in the near future.

Uploading a sticky data set to a server can use a lot of a server's space, so you should have a good reason to do so.

Using the GUI, setting a sticky data set is very simple:
  1. Open Sticky Data Set Servers popup: In the upload tool, go to Options > Sticky Data Set Servers
  1. Select server(s) to receive sticky data set: Just check the checkbox next to server(s). You can close this popup when you are done selecting servers.
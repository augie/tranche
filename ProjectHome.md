Tranche is a free and [open source](https://trancheproject.org/license.jsp) ([Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0)) file storage and dissemination software. Designed and built with scientists and researchers in mind, Tranche can handle very large data sets, is secure, is scalable, and all data sets are citable in scientific journals. [Learn more about Tranche ...](https://trancheproject.org/about.jsp)


### Features ###
  * **Decentralized Architecture**: lack of a single point of failure is a big step towards ensuring optimal availability. Also improves speeds through parallelization.
  * **Large Files**: theoretically supports single files up to 1.2 TB (currently, 89 MB of hashes), and directories of unlimited size.
  * **Persistence/Preservation**: files are replicated and distributed to protect against loss because of the loss of a single hard-drive, server, or data center. In addition, when the data is hosted amongst a community, the data is protected against disappearance due to failure of a single institution/organization.
  * **Immutability/Integrity**: when a file is uploaded, it will always stay exactly the same. It is impossible to change an uploaded file. Uploading a new version makes it easy to revise the data, but the original upload will remain unless deleted.
  * **Provenance**: keeps a reference of who uploaded the file with each file on the repository. Tranche always knows who originally uploaded the file, so the proper person can be blamed for any inappropriate or illegal content that is uploaded.
  * **Encryption**: uploads can optionally by encrypted using AES-256. Encrypted uploads can be made publicly available without a password at a later time without re-uploading.
  * **Licensing**: uploads can include a license to indicate what can and cannot be done with the files.
  * **Versioning**: new versions of data sets can be uploaded, thought the previous versions are not replaced.
  * **Citability**: uploads are referenced by a hash that represents the content of the upload (there is no namespace), so the hash is not susceptible to link rot as long as the files are not deleted.
  * **Open Source**


### Requirements ###

  * Java 1.5+


### Development Status ###

There is one public repository with several terabytes of files on it that the Tranche Project team supports for proteomics research. [Go to ProteomeCommons.org to try out a working Tranche repository!](https://proteomecommons.org/tranche/)


### Participate ###

If you have an interest in participating in the development of Tranche, please introduce yourself in the [Developer's Group](http://groups.google.com/group/tranche-developers) and say why you would like to participate. We'll then give you permissions to commit changes.


### Origin ###

Tranche started as part of Jayson Falkner's PhD work at the University of Michigan in 2005. The work was done as part of the National Resource for Proteomics and Pathways (Grant #P41 RR018627), directed by Philip C. Andrews. Since Jayson graduated in 2008, the software has been developed by a small full-time team at the University of Michigan.
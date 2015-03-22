# Why Should I Publish The Tranche Hash? #

Most published manuscripts include a set of supplemental material, which commonly includes software, data, and/or the details of analysis. In most every case, this information is digital, meaning it is really nothing more than a collection of files. Tranche is designed to provide an ideal mechanism for publishing and referencing such supplemental data. Here is why.

A single, simple unique string identifies your supplemental data, e.g. `x4iy3M6tTKzKRGz/JPqZWjKAvXtQ0KcoQamU8aj9olFFvaLvwaIFUHsJrnqJSl8fmK8K0DO2DAeJFYFnhtuZnPSwkqoAAAAAAAAM3A==`. The ID is a proper digital hash, which anyone who has the supplemental data can recalculate, ensuring the data is exactly what was published.
Downloading a file or collection of associated files is easy.

Unlike weblinks, a Tranche hash cannot break, or suddenly point to an incorrect page.
Once the data is uploaded the hash can be used to verify the complete and uncorrupted availability of the data on the Tranche network.



# Publication Examples #

A brief example clearly illustrates how this functionality works. Take for example a manuscript that is citing availability of code and supplementary data. Here is a template sentence that would work well for citing the data on ProteomeCommons.org Tranche repository:

```
The data associated with this manuscript may be downloaded from the ProteomeCommons.org Tranche repository using the following hash:

[insert your hash here]

The hash may be used to prove exactly what files were published as part of this manuscript's dataset, and the hash may also be used to check that the data has not changed since publication.
```

Note that Base 16 encoding is used because Base 64 encoding (the one Tranche more commonly uses) is not compatible with hyperlinks unless you exchange the plus sign characters "+" with "%2B". Tranche can handle hashes encoded in either Base 64 or Base 16.

Just for clarity, here is a filled out example with a hash:

```
The data associated with this manuscript may be downloaded from the ProteomeCommons.org Tranche repository using the following hash:

x4iy3M6tTKzKRGz/JPqZWjKAvXtQ0KcoQamU8aj9olFFvaLvwaIFUHsJrnqJSl8fmK8K0DO2DAeJFYFnhtuZnPSwkqoAAAAAAAAM3A==

The hash may be used to prove exactly what files were published as part of this manuscript's dataset, and the hash may also be used to check that the data has not changed since publication.
```

The idea is that the above lines should appear somewhere in your availability section.

It is very important that you double-check that your hash is correctly typed for publishing. We advise that you also get a proof from the publisher before printing to verify that it is correct. Our download tool can compensate for some mistypes in hashes and also uses predictive algorithms, but there are instances that cannot be compensated for.

Included in the above availability section is a normal looking reference to a website, which is often how supplemental data is distributed, but notice the last line, "The hash for downloading this project from the ProteomeCommons.org Tranche repository is ..." This line is all that is required to properly reference the exact data set used by the publication. Anyone with access to the data network can enter this hash and retrieve the data.



# Common Errors #
A complete hash should always appear as an unbroken string. For example: `x4iy3M6tTKzKRGz/JPqZWjKAvXtQ0KcoQamU8aj9olFFvaLvwaIFUHsJrnqJSl8fmK8K0DO2DAeJFYFnhtuZnPSwkqoAAAAAAAAM3A==` is a complete hash string. When a Tranche hash is published however, it sometimes happens that the hash itself becomes corrupted by the editing process. Hashes should always be copied and pasted rather than manually typed to avoid errors. Use type fonts that differentiate similar looking characters well (Lower case "L" and the number "1" are a common error). Below you will find three examples of hashes that have been corrupted through various means. These are the most common errors that have been encountered.

When publishing a Tranche hash in a manuscript it has sometimes occurred that the hash itself has been corrupted through the editing process, i.e. dashes have been added and the hash broken up over multiple lines in the manuscript. This generally occurs because the column width is not great enough to accommodate the hash in its entirety. An example of this can be seen below.

```
x4iy3M6tTKzKRGz/JPqZWjKAvXtQ-
0KcoQamU8aj9olFFvaLvwaIFUHsJ-
rnqJSl8fmK8K0DO2DAeJFYFnhtuZ-
nPSwkqoAAAAAAAAM3A==
```

When editors of journals add in these extra characters, instead of creating continuity between the line breaks, superfluous characters are added to the hash that can change the data set to which it points. Thus, a user is unable to simply "copy and paste" the hash from the manuscript into the URL format shown above. However, if the user is running the download Java GUI tool this "copy and paste" procedure will still work. This is due to the fact that Tranche is intelligently designed to filter out all non base 64 characters, and as dashes are not in that language they are not recognized, and thrown out.




# Embedding Hashes In Emails #

When Tranche hashes are sent in email they normally stay relatively intact; however, when multiple forwards or replies are sent out the hash can sometimes become fragmented, either through line breaks or the ">" symbols that show the different levels of an email. The first situation is demosntrated by the text below:

```
> x4iy3M6tTKzKRGz/JPqZWjKAvXtQ
> 0KcoQamU8aj9olFFvaLvwaIFUHsJ
> rnqJSl8fmK8K0DO2DAeJFYFnhtuZ
> nPSwkqoAAAAAAAAM3A==
```

In this instance there are line breaks and white space interrupting the hash. This is usually caused by the email programs attempt to show the different levels of communication, however, whatever the cause there are a few simple solutions.

The user can simply edit the email thereby removing all of the line breaks and white space in between the Tranche hash characters. The complete hash can then be used to access the data.

If the user is running the Java GUI download tool then the entire "broken" hash can be copied and pasted into the download tool. The Tranche program has been intelligently designed to filter out an white space and non 64 base characters, thus completing the editing for you and supplying you with a complete and valid hash.
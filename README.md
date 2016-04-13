# Software for the SIGIR 2016 article "New Collection Announcement: Focused Retrieval Over the Web"

This software was used to extract, clean, annotate, and evaluate the corpus described in our SIGIR 2016 article. 

Please use the following citation:

```
@InProceedings{smith:20xx:CONFERENCE_TITLE,
  author    = {Smith, John},
  title     = {My Paper Title},
  booktitle = {Proceedings of the 20XX Conference on XXXX},
  month     = {Month Name},
  year      = {20xx},
  address   = {Gotham City, USA},
  publisher = {Association for XXX},
  pages     = {XXXX--XXXX},
  url       = {http://xxxx.xxx}
}
```

> **Abstract:** TODO finish 


Contact person: Ivan Habernal, habernal@ukp.informatik.tu-darmstadt.de

http://www.ukp.tu-darmstadt.de/

http://www.tu-darmstadt.de/


Don't hesitate to send us an e-mail or report an issue, if something is broken (and it shouldn't be) or if you have further questions.

> This repository contains experimental software and is published for the sole purpose of giving additional background details on the respective publication. 

## Project structure

* `de.tudarmstadt.ukp.experiments.dip.hadoop` - for extracting ClueWeb12 files stored on a Hadoop system
* `de.tudarmstadt.ukp.experiments.dip.wp1` - preprocessing pipeline for document annotation

## Requirements

* Java 1.8 and higher
* Maven 3

## Running the experiments

* All classes in `de.tudarmstadt.ukp.experiments.dip.wp1` are self-documented, using `StepX` as a prefix.
    * By default, we do not provide either the templates required for generating data for Amazon Mechanical Turk HITs or
the actual CSV files obtained as a direct output from AMT annotations, thus Step6 and Step7 cannot work.
    * If you are interested in these files, send us an e-mail


## DIP2016Corpus Data format

The data are split into 49 files, one file per query.

The files are in a XML utf-8 format:

```
<?xml version="1.0" encoding="utf-8"?>
<singleQueryResults queryID="1002">
  <query>cellphone for 12 years old kid</query>
  <documents>
  ...
  </documents>
</singleQueryResults>
```

and each document contains the ClueWeb ID and a list of annotated sentences:

```
<documents>
    <document clueWebID="clueweb12-1401wb-91-21649">
      <sentences>
        <s relevant="false">
          <content>You should also know that The Sacramento Bee does not screen comments before they are posted.</content>
        </s>
        <s relevant="false">
          <content>You are more likely to see inappropriate comments before our staff does, so we ask that you click the &quot;Report Abuse&quot; link to submit those comments for moderator review.</content>
        </s>
        <s relevant="false">
          <content>You also may notify us via email at feedback@sacbee.com .</content>
        </s>
        ...
        <s relevant="true">
          <content>Brandon Gonzales, 12, has been using a cellphone since he was 10.</content>
        </s>
        <s relevant="true">
          <content>Almost all his Sutter Middle School friends have cellphones, too.</content>
        </s>
        <s relevant="true">
          <content>His mom, Elizabeth Gonzales, likes knowing that he can call home at any time.</content>
        </s>
        ...
```

## Modifying the final output corpus

You can modify the resulting corpus by changing the implementation of `Step12GoldDataExporter`.

* You need to specify the input data ``Step10AggregatedCleanGoldData``
    * You can access the original HTML (although the annotated sentences are not annotated directly
    on top of HTML but rather after boilerplate removal and cleaning)
    * The sentences boundaries annotations are also embedded in the data
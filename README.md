# Software for the SIGIR 2016 article "New Collection Announcement: Focused Retrieval Over the Web"

This software was used to extract, clean, annotate, and evaluate the corpus described in our SIGIR 2016 article. 

Please use the following citation:

```
@InProceedings{Habernal.et.al.SIGIR.2016,
  author    = {Habernal, Ivan and Sukhareva, Maria and Raiber, Fiana and
               Shtok, Anna and Kurland, Oren and Ronen, Hadar and
               Bar-Ilan, Judit and Gurevych, Iryna},
  title     = {{New Collection Announcement: Focused Retrieval Over the Web}},
  booktitle = {Proceedings of the 39th International ACM SIGIR Conference on Research and
               Development in Information Retrieval},
  month     = {July},
  year      = {2016},
  publisher = {ACM},
  address   = {New York, NY, USA},
  pages     = {701--704},
  series    = {SIGIR '16},
  location  = {Pisa, Italy},
  doi       = {10.1145/2911451.2914682},
  url       = {http://dl.acm.org/citation.cfm?doid=2911451.2914682}
}
```

> **Abstract:** Focused retrieval (a.k.a., passage retrieval) is important at its own right and as an intermediate step in question answering systems. We present a new Web-based collection for focused retrieval. The document corpus is the Category A of the ClueWeb12 collection. Forty-nine queries from the educational domain were created. The 100 documents most highly ranked for each query by a highly effective learning-to-rank method were judged for relevance using crowdsourcing. All sentences in the relevant documents were judged for relevance.


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

* The corpus is available for download at http://ie.technion.ac.il/~kurland/dip2016corpus/
    * Mirrored at https://public.ukp.informatik.tu-darmstadt.de/dip2016corpus/

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

## Extracted list of queries

This list of queries is extracted from the full XML files (using `cat *.xml | grep -o -P "(?<=<query>).*(?=</query>)" | sed "s/&apos;/'/g`)

```
alternative ADHD treatments
cellphone for 12 years old kid
dealing with kid that skip classes
depression in children
discipline for 8 year old daughter
discipline issues in elementary school
getting rid of childhood phobia
handling conflicts between children
homeschooling legal issues
homeschooling of two children
homeschooling versus public school
pros and cons of montessori school
quite smoking teenagers
reasons for homeschooling
sleep problems in preschool children
student loans
students loans without credit history
studying techniques and methods
teenagers behavior problems and solutions
school dealing with bullying
signs of bullied kids
parents involvement dealing with bullying
parents dealing with their kids being cyber-bullied
signs of cyber-bullied kids
explaining kids the online dangers
guide for parents to protect their kids online
parents' involvement in children alcohol behavior
parents of kids doing drugs 
school punishment policy
home discipline methods for grade-schoolers
parents of kids suffering from test anxiety
parents involvement in relieving study load at school
parents of kids hooked on computer games
kids with depression
parents of shy kids at school
the right age to enter kindergarten/school
signs to know if child is ready to start school
parents involvement in didactic assessments of their child's learning capabilities
school dealing with diagnosed learning disability
parents involvement in educational content at school
parents concerns about religious classes at school
parents of early reading kids
parents deal with children's obesity
parents concerns about early age to use cellphone
ways parents can get involved in school
special education needs of gifted children
toddler and preschooler - Potty training at home
family fit to Waldorf (anthroposophy) or Montessori school 
parents involvement in schools' attitude to gifted children
```

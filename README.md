# TextAnalytics

Environment Setup
-----------------
We set up our system on a virtual machine running CentOS but likely any Linux distribution would work fine.

If using CentOS:
We recommend downloading a version of CentOS with a desktop environment set up rather than the CentOS minimal install. It can be a little bit tricky connecting CentOS minimal to the internet for the first time, but the following site describes how it can be done:

http://lintut.com/how-to-setup-network-after-rhelcentos-7-minimal-installation/

Once you have an OS set up you need to download and install:
Hadoop, Mahout and their dependencies.

We looked at the following tutorials to get Hadoop set up:

http://www.tutorialspoint.com/hadoop/hadoop_enviornment_setup.htm

http://doctuts.readthedocs.org/en/latest/hadoop.html

You will also need to checkout the word2vec code:

https://code.google.com/archive/p/word2vec/

Collecting and Processing Data
-------------------------------
You can get the latest Wikipedia dump from:
https://dumps.wikimedia.org/enwiki/latest/

The file: enwiki-latest-pages-articles.xml.bz2 is the full dump, and the one we used.

word2vec
--------
To run word2vec on Wikipedia we modified the word2phrase script to download and process the Wikiedia dump. Our script may need adjusted a bit more and may not work the first try. Additionally, you may consider using Matt Mahoney's script for cleaning up the wikipedia articles rather than the sed command: http://mattmahoney.net/dc/textdata.html. This script is mentioned on the word2vec page and may do a little better job removing irrelevant information than sed.

	make
	if [ ! -e enwiki-latest-pages-articles.xml ]; then
	  wget https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2
	  time bzip2 -dk enwiki-latest-pages-articles.xml.bz2
	fi
	sed -e "s/’/'/g" -e "s/′/'/g" -e "s/''/ /g" < enwiki-latest-pages-articles.xml | tr -c "A-Za-z'_ \n" " " > enwiki-latest-pages-articles.xml-norm0
	time $WORD2VEC_HOME/word2phrase -train enwiki-latest-pages-articles.xml-norm0 -output enwiki-latest-pages-articles.xml-norm0-phrase0 -threshold 200 -debug 2 | tee phrase1.out
	time $WORD2VEC_HOME/word2phrase -train enwiki-latest-pages-articles.xml-phrase0 -output enwiki-latest-pages-articles.xml-phrase1 -threshold 100 -debug 2 | tee phrase2.out
	tr A-Z a-z < enwiki-latest-pages-articles.xml-norm0-phrase1 > enwiki-latest-pages-articles.xml-norm1-phrase1
	time $WORD2VEC_HOME/word2vec -train enwiki-latest-pages-articles.xml-norm1-phrase1 -output vectors-phrase-wikipedia.bin -cbow 1 -size 200 -window 10 -negative 25 -hs 0 -sample 1e-5 -threads 20 -binary 1 -iter 15 | tee word.out
	./vectors-phrase-wikipedia.bin

Word2vec took around 5 cpu days to process the Wikipedia dump so you can go do something else for a while after you start this up.

The end result of running word2phrase is a file containing word vectors.

parsing and tfidf
-----------------
Command line arguments to ParseWikipedia:

	-i /path/to/data/enwiki-latest-pages-articles.xml -o /path/to/parse_output

Running the Pipeline
--------------------
This program takes in a search string then returns a list of relevant documents based on word2vec and tfidf information.

Note: In order to run on the Wikipedia dataset you need at least 12GB RAM on your system allocated to this process.

Command line arguments to Pipeline:

	/path/to/parse_output/dictionary.file /path/to/parse_output/tfidf/tfidf-vectors/part-r-00000

Running the Vector Addition
---------------------------
This is a program experimenting in the area of adding vectors to create document vectors. It creates document vectors by adding the word vectors for the top ten words in a document based on their tfidf values.

Note: In order to run on the Wikipedia dataset you need at least 12GB RAM on your system allocated to this process.

Command line arguments to VectorAddition:

	/path/to/parse_output/dictionary.file /path/to/parse_output/tfidf/tfidf-vectors/part-r-00000

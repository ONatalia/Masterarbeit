#!/bin/bash  

#from the start directory change to the project bin directory
cd inprotk/bin

#set libraries to start programs from terminal
export CLASSPATH=/Users/Natalia/inprotk/lib/sphinx4/jsapi.jar:/Users/Natalia/inprotk/lib/sphinx4/sphinx4.jar:/Users/Natalia/inprotk/bin:/Users/Natalia/inprotk/lib/common-for-convenience/log4j-1.2.15.jar:/Users/Natalia/inprotk/lib/weka.jar:/Users/Natalia/inprotk/lib/mary5/marytts-client-5.2-SNAPSHOT-jar-with-dependencies.jar:/Users/Natalia/inprotk/lib/Cocolab_DE_8gau_13dCep_16k_40mel_130Hz_6800Hz.jar:/Users/Natalia/inprotk/lib/jrtp_1.0a.jar:/Users/Natalia/inprotk/lib/ooa2/antir-oaa.jar:/Users/Natalia/inprotk/lib/remote/commons-logging-1.1.jar:/Users/Natalia/inprotk/lib/remote/apache-commons-lang.jar:/Users/Natalia/inprotk/lib/json-20140107.jar:/Users/Natalia/inprotk/lib/common-for-convenience/jflac-1.3.jar:/Users/Natalia/inprotk/lib/javaFlacEncoder-0.3.1.jar


#starts SimpleReco  (Google) Once
java inpro.apps.SimpleReco -F file:../res/g002acn1_103_AAJ.wav  -O -C -L -Lp ../res/google/google -G AIzaSyCXHs3mzb1IyfGx2tYxDC1ClgYUv0x8Kw8 -Gout /tmp/Googledump.json


#starts SimpleReco  (Sphinx) Once
java inpro.apps.SimpleReco -F file:../res/g002acn1_103_AAJ.wav -O -In -C -L -Lp ../res/sphinx/sphinx -v -T


#starts Google with TestData from excerpt folder
for file in /Users/Natalia/inprotk/src/vm-excerpt/*.wav; do java inpro.apps.SimpleReco -F file:"$file" -O -C -L -G AIzaSyCXHs3mzb1IyfGx2tYxDC1ClgYUv0x8Kw8 -Gout /tmp/Googledump.json -Lp ${file%%.wav}.google_inc_reco;done 

#starts SimpleReco  (Google) Once with file from excerpt folder

java inpro.apps.SimpleReco -F file:/Users/Natalia/inprotk/src/vm-excerpt/g001acn1_058_AAJ.wav  -O -C -L -Lp /Users/Natalia/inprotk/src/vm-excerpt/g001acn1_058_AAJ.wav%%.wav.google_inc_reco -G AIzaSyCXHs3mzb1IyfGx2tYxDC1ClgYUv0x8Kw8 -Gout /tmp/Googledump.json


#starts GoogleSphinx with TestData from excerpt folder
for file in /Users/Natalia/inprotk/src/vm-excerpt/*.wav; do java inpro.apps.SimpleReco -F file:"$file" -O -C -L -G AIzaSyCXHs3mzb1IyfGx2tYxDC1ClgYUv0x8Kw8 -Gout /tmp/Googledump.json -Lp ${file%%.wav}.google_inc_reco;done 


 



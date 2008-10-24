#!/bin/bash

java -cp ../../bin:$CLASSPATH gov.nist.sphere.jaudio.TestSphereFileReader sample_vm.nis /tmp/sample_vm.java.wav
java -cp ../../bin:$CLASSPATH gov.nist.sphere.jaudio.TestSphereFileReader sample_swbd.sph /tmp/sample_swbd.java.wav

sox sample_vm.nis /tmp/sample_vm.sox.wav
sox sample_swbd.sph /tmp/sample_swbd.sox.wav

cp sample_* /tmp

ls -l /tmp/sample_* 

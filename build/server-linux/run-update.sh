#!/bin/sh

# assume a local JVM would be in the 'jre' directory
export PATH=./jre/bin:$PATH

# make a variable for storing temp data on the disk
export TEMPDIR=./temp
mkdir -p $TEMPDIR

# run the code forever
while true
do
  # build up the classpath
  LIB="."
  # add all the JARs
  for FILE in $( ls ./lib/*  )
  do
    LIB=$LIB:$FILE
  done

  # make the run command
  RUN_UPDATE="java -Xmx512m -server -Djava.io.tmpdir=$TEMPDIR -classpath $LIB org.tranche.AutoUpdater"

  # remove any lock files
  rm -f ./*.lock
  rm -f ./data/*.lock
  rm -f ./metaData/*.lock

  # try to update the code
  $RUN_UPDATE


  # build up the classpath again, just in case JARs are added or removed
  LIB="."
  # add all the JARs
  for FILE in $( ls ./lib/*  )
  do
    LIB=$LIB:$FILE
  done

  # make the run command
  RUN="java -Xmx512m -server -Djava.io.tmpdir=$TEMPDIR -classpath $LIB org.tranche.LocalDataServer"

  # run the code
  $RUN

  # wait 10 seconds for luck
  sleep 10
done


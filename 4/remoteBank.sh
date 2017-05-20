#!/usr/bin/env bash
#ssh al1076@agate.cs.unh.edu 'bash -s' < killBank.sh
#sbt 'runMain cs735_835.CompareClient 132.177.4.36:51076 500000 10 rem_500000_10.csv'
#ssh al1076@agate.cs.unh.edu 'bash -s' < killBank.sh
#sbt 'runMain cs735_835.CompareClient 132.177.4.36:51076 500000 100 rem_500000_100.csv'
#ssh al1076@agate.cs.unh.edu 'bash -s' < killBank.sh
#sbt 'runMain cs735_835.CompareClient 132.177.4.36:51076 500000 1000 rem_500000_1000.csv'
#ssh al1076@agate.cs.unh.edu 'bash -s' < killBank.sh
sbt 'runMain cs735_835.CompareClient 132.177.4.36:51076 500000 10000 rem_500000_10000.csv'
#ssh al1076@agate.cs.unh.edu 'bash -s' < killBank.sh
sbt 'runMain cs735_835.CompareClient 132.177.4.36:51076 500000 100000 rem_500000_100000.csv'
#ssh al1076@agate.cs.unh.edu 'bash -s' < killBank.sh

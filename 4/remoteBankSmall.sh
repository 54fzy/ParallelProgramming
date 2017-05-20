#!/usr/bin/env bash
sbt 'runMain cs735_835.CompareClient 132.177.4.36:51076 100000 10 rem_100000_10.csv'
sbt 'runMain cs735_835.CompareClient 132.177.4.36:51076 100000 100 rem_100000_100.csv'
sbt 'runMain cs735_835.CompareClient 132.177.4.36:51076 100000 1000 rem_100000_1000.csv'
sbt 'runMain cs735_835.CompareClient 132.177.4.36:51076 100000 10000 rem_100000_10000.csv'
sbt 'runMain cs735_835.CompareClient 132.177.4.36:51076 100000 100000 rem_100000_100000.csv'
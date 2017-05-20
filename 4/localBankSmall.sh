#!/usr/bin/env bash
sbt 'runMain cs735_835.CompareClient localhost:51076 100000 10 loc_100000_10.csv'
sbt 'runMain cs735_835.CompareClient localhost:51076 100000 100 loc_100000_100.csv'
sbt 'runMain cs735_835.CompareClient localhost:51076 100000 1000 loc_100000_1000.csv'
sbt 'runMain cs735_835.CompareClient localhost:51076 100000 10000 loc_100000_10000.csv'
sbt 'runMain cs735_835.CompareClient localhost:51076 100000 100000 loc_100000_100000.csv'
#!/usr/bin/env bash
sbt 'runMain cs735_835.CompareClient localhost:51076 500000 10 loc_500000_10.csv'
sbt 'runMain cs735_835.CompareClient localhost:51076 500000 100 loc_500000_100.csv'
sbt 'runMain cs735_835.CompareClient localhost:51076 500000 1000 loc_500000_1000.csv'
sbt 'runMain cs735_835.CompareClient localhost:51076 500000 10000 loc_500000_10000.csv'
sbt 'runMain cs735_835.CompareClient localhost:51076 500000 100000 loc_500000_100000.csv'
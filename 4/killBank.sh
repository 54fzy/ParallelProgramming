#!/usr/bin/env bash
pkill -f 'java.*BankServer' &
sleep 10 &
cd 4/ &
sbt 'runMain cs735_835.remoteBank.BankServer' &

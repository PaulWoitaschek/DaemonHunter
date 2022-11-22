# Daemon Hunter

This repository contains a simple script that lets you kill gradle and kotlin daemons. After updating gradle or kotlin or checking out older commits, often time you end up with multiple versions of gradle and the kotlin daemon running on your machine. These daemons consume a lot of RAM and slow down your system unnecessarily if you don't use them.

## Usage
Select the versions to kill. By default the prefill contains the outdated daemons. In the example, Kotlin `1.3.72` is older than Kotlin `1.4.10` and so is Gradle `6.6.1`
Therefore these daemons will be killed if you specify no input.
```console
➜  AndroidDaemonKiller git:(main) ✗ ./kill-daemons.main.kts
[1]     Kotlin  1.4.10
[2]     Kotlin  1.3.72
[3]     Gradle  6.7
[4]     Gradle  6.6.1
Enter the numbers, separated by commas you want to kill [2,4]: 
killed Kotlin   1.3.72
killed Gradle   6.6.1
```

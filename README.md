# AndroidDaemonKiller

This repository contains a simple script that lets you kill gradle and kotlin daemons. After updating gradle or kotlin or checking out older commits, often time you end up with multiple versions of gradle and the kotlin daemon running on your machine. These daemons consume a lot of RAM and slow down your system unnecessarily if you don't use them.

## Usage
### Manually select the versions to kill.
```console
./kill-daemons.main.kts
➜  AndroidDaemonKiller git:(main) ./kill-daemons.main.kts      
[1]     Gradle  6.7
[2]     Gradle  6.6.1
[3]     Kotlin 1.4.0
[4]     Kotlin 1.3.71
Enter the numbers, separated by commas you want to kill: 2,4
```
### Only keep the latest versions
```console
./kill-daemons.main.kts -- --auto-kill # or just -k
killed    Gradle  6.6.1
killed    Kotlin 1.3.71
```

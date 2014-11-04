AndroidCPUThrottling
====================

An app to show Android CPU frequency thermal throttling status

The app shows CPU temperature and CPU frequency with the various task load.
To monitor stats, it's reading files below:
/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq
/sys/devices/system/cpu/cpu0/cpufreq/cpu_utilization
/sys/devices/virtual/thermal/thermal_zone0/temp

External component:
GraphView: http://android-graphview.org/



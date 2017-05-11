mavproxy.py --master=/dev/ttyUSB0 --out=127.0.0.1:14550 --out=127.0.0.1:14551
dronekit-sitl copter
python script.py --connect /dev/ttyUSB0
https://github.com/wiseman/mavelous
mavproxy.py --module mavelous --master=/dev/ttyUSB0 --baud=57600
mavproxy.py --master /dev/ttyUSB0 --out=udpout:127.0.0.1:4550 --out=127.0.0.1:4551


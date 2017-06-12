# Import DroneKit-Python
import dronekit
import socket
import exceptions
print("Start")
# Connect to the Vehicle.
#vehicle = connect("/dev/ttyUSB0", wait_ready=True)

try:
    dronekit.connect('/dev/ttyUSB0', heartbeat_timeout=500, wait_ready=True)
except exceptions.OSError as e:
    print 'No serial exists!'
except dronekit.APIException:
    print 'Timeout!'
except:
    print 'Some other error!'

# Get some vehicle attributes (state)
#print "Get some vehicle attribute values:"
#print " GPS: %s" % vehicle.gps_0
#print " Battery: %s" % vehicle.battery
#print " Last Heartbeat: %s" % vehicle.last_heartbeat
#print " Is Armable?: %s" % vehicle.is_armable
#print " System status: %s" % vehicle.system_status.state
#print " Mode: %s" % vehicle.mode.name    # settable

# Close vehicle object before exiting script
#vehicle.close()

# Shut down simulator
print("Completed")

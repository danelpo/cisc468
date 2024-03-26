import socket
import zeroconf
import time
from scanner import publish_devices

#broadcast TCP connection
def broadcast_connection():
        ip_address = socket.gethostbyname(socket.gethostname())

        broadcast_engine = zeroconf.Zeroconf()
        service_info = zeroconf.ServiceInfo(
            "_pbroadcaster._tcp.local.",
            "{0}._pbroadcaster._tcp.local.".format(ip_address),
            addresses=[ip_address],
            port=12345,
            properties={},
            server=f"{ip_address}"
        )
        broadcast_engine.register_service(service_info)

        print(f"Python-Broadcaster is active on {ip_address}, port 12345")
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            pass
        finally:
            print("Shutting down service")
            broadcast_engine.unregister_service(service_info)
            broadcast_engine.close()

#discover local connections
def print_mDNS():
    publish_devices()

#establish outgoing connection

#establish incoming connection

#key exchange

#key verification

#key migration

from zeroconf import Zeroconf, ServiceBrowser
import time

class scanHandler:
    def __init__(self):
        self.services = {}

    def remove_service(self, zeroconf, type, name):
        if name in self.services:
            del self.services[name]

    def add_service(self, zeroconf, type, name):
        info = zeroconf.get_service_info(type, name)
        if info is not None:
            self.services[name] = info
    
    def update_service(self):
        pass

def discover_devices():
    zeroconf = Zeroconf()
    handler = scanHandler()

    ServiceBrowser(zeroconf, "_pbroadcaster._tcp.local.", handler)
    time.sleep(2)
    
    zeroconf.close()
    return handler.services

def publish_devices():
    devices = discover_devices()
    print("Devices:")
    for name, info in devices.items():
        print(f"Service Name: {name}")
        print(f"IP address: {info.server}")
        print(f"Port: {info.port}")
        print(f"Binary IP address: {info.addresses[0]}")

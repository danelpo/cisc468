import socket
import zeroconf
import time
from scanner import publish_devices

#broadcast TCP connection
#returns socket
def broadcast_connection():
        ip_address = socket.gethostbyname(socket.gethostname())

        broadcast_engine = zeroconf.Zeroconf()
        service_info = zeroconf.ServiceInfo(
            "_broadcaster._tcp.local.",
            "{0}._broadcaster._tcp.local.".format(ip_address),
            addresses=[socket.inet_pton(socket.AF_INET, ip_address)],
            port=12345,
            properties={},
            server=f"{ip_address}"
        )
        broadcast_engine.register_service(service_info)

        print(f"Python-Broadcaster is active on {ip_address}, port 12345")
        java_socket = None
        try:
            while java_socket == None:
                java_socket = wait_for_incoming()
            print("successfully received connection")
        except KeyboardInterrupt:
            print("failed to received connection")
        finally:
            print("Shutting down broadcasting")
            broadcast_engine.unregister_service(service_info)
            broadcast_engine.close()
        return java_socket

#discover local connections
#RETURNS ip and port of available devices
def discover_mDNS():
    d_name, d_ip_bytes, d_port = publish_devices()
    if d_ip_bytes:
        d_ip = socket.inet_ntoa(d_ip_bytes)
        print(f"device found: {d_name}")
        should_connect = input("Do you want to connect to this device? ('Y'=yes)\n")
        if should_connect == "Y":
            return d_ip, d_port
    else:
        return None, None

#establish outgoing connection
#RETURNS outgoing port
def establish_connection(ip, port):
    print(f"establishing connection to {ip}:{port}")
    java_outgoing_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        java_outgoing_socket.connect((ip, port))
        print(f"Connected to {ip}:{port}")
        return java_outgoing_socket
    except ConnectionRefusedError as e:
        print(f"Connection to {ip}:{port} refused.")
        print(e)
        return None
    except Exception as e:
        print("Error occurred:", e)
        return None

#establish incoming connection
#RETURNS socket to Java
def wait_for_incoming():
    ip = '0.0.0.0'
    port = 12345

    java_incoming_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    java_incoming_socket.bind((ip, port))

    # Wait for connection request
    java_incoming_socket.listen(1)
    print("Server is listening")

    # Approve connection request
    java_outgoing_socket, java_client_address = java_incoming_socket.accept()
    print("Connected to:", java_client_address)

    java_incoming_socket.close()

    return java_outgoing_socket

#key exchange

#key verification

#key migration

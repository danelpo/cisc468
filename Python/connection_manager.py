import socket
import zeroconf
from scanner import publish_devices
from message_manager import send_msg, receive_msg

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
def key_exchange_init(to_socket):
    p_public = None
    a_public = None
    a_private = None
    A_var = None
    B_var = None

    #public keys
    while p_public == None:
        p_public = input("Please suggest a public key (p parameter), type 'r' to generate one randomly")
        if p_public == 'r':
            p_public = "random prime p-public key"
        elif valid_key(p_public) != True:
            p_public = None
            print("key invalid")
    print(f"    p_public: {p_public}")
    while a_public == None:
        a_public = input("Please suggest a public key (alpha parameter), type 'r' to generate one randomly")
        if a_public == 'r':
            a_public = "random a-public key"
        elif valid_key(a_public, p_public) != True: #up to p-2
            a_public = None
            print("key invalid")
    print(f"    a_public: {a_public}")
    send_msg(p_public, to_socket)
    send_msg(a_public, to_socket)
    print("Public keys sent to client")

    #private key
    while a_private == None:
        a_private = input("Please suggest a private key, type 'r' to generate one randomly")
        if a_private == 'r':
            a_private = "random a-public key"
        elif valid_key(a_private, p_public) != True: #up to p-2
            a_private = None
            print("key invalid")
    print(f"    a_private: {a_private}")

    #A variable
    A_var = pow(a_public, a_private)%p_public
    send_msg(A_var, to_socket)
    print("send A to client")

    #receive B
    B_var = receive_msg(to_socket)
    print("Received B from client")

    #calculate key
    key = pow(B_var, a_private)

    return key

def valid_key(key, p=None):
    return True

#key verification

#key migration

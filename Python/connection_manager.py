import socket
import zeroconf
from scanner import publish_devices
from message_manager import send_msg, receive_msg
from cryptography.hazmat.primitives.asymmetric import dh
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.backends import default_backend

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
#RETURNS symmetric key
def key_exchange_init(to_socket):
    p_public = None
    a_public = None
    a_private = None
    A_var = None
    B_var = None
    
    print("Generating keys")
    key_gen_params = dh.generate_parameters(generator=2, key_size=512, backend=default_backend())
    a_raw = key_gen_params.generate_private_key()
    p_public = key_gen_params.parameter_numbers().p
    a_public = key_gen_params.parameter_numbers().g
    a_private = a_raw.private_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption()
    )

    print(f"p_public: {p_public}")
    print(f"a_public: {a_public}")
    send_msg(p_public, to_socket)
    p_verification = receive_msg(to_socket)
    print(f"client repled with {p_verification}")

    send_msg(a_public, to_socket)
    a_verification = receive_msg(to_socket)
    print(f"client repled with {a_verification}")

    print("Public keys sent to client")
    print(f"a_private: {a_private}")

    #A variable
    A_var = a_raw.public_key().public_numbers().y
    send_msg(A_var, to_socket)
    print(f"A: {A_var}")
    print("send A to client")

    #receive B
    B_var = None
    while B_var == None:
        B_var = receive_msg(to_socket)
    print(f"B: {B_var}")
    print("Received B from client")

    #get B object
    B = dh.DHPublicNumbers(int(B_var), dh.DHParameterNumbers(int(p_public), int(a_public))).public_key(default_backend())

    #calculate key
    key = a_raw.exchange(B)
    #key = pow(int(B_var), int(a_private))
    print("Calculated key")
    print(f"key: {key}")

    return key

def key_exchange_rcv(from_socket):
    #public keys
    p_public = receive_msg(from_socket)
    print(f"p_public: {p_public}")
    send_msg(p_public, from_socket)
    a_public = receive_msg(from_socket)
    print(f"a_public: {a_public}")
    send_msg(a_public, from_socket)
    A_var = receive_msg(from_socket)
    print(f"A_var: {A_var}")

    #private keys
    key_gen_params = dh.DHParameterNumbers(int(p_public), int(a_public)).parameters(default_backend())
    B_raw = key_gen_params.generate_private_key()
    a_private = B_raw.private_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption()
    )
    print(f"a_private: {a_private}")
    B_var = B_raw.public_key().public_numbers().y
    send_msg(B_var, from_socket)
    print(f"A: {B_var}")
    A = dh.DHPublicNumbers(int(A_var), dh.DHParameterNumbers(int(p_public), int(a_public))).public_key(default_backend())
    key = B_raw.exchange(A)
    #key = pow(int(B_var), int(a_private))
    print("Calculated key")
    print(f"key: {key}")

#key verification

#key migration

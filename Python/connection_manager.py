import socket
import zeroconf
from scanner import publish_devices
from message_manager import send_msg, receive_msg
from cryptography.hazmat.primitives.asymmetric import dh
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.primitives import hashes

#This file managers all connectivity-related functions.

#broadcast TCP connection so other devices will find this one
#returns the socket
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

        #while broadcasting, it will also listen for any incoming connections.
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

#discover local devices that are currently broadcasting. Return the ip and port
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

#establish outgoing connection to broadcasting device once we have the port and ip
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

#establish incoming connection from scanning device
def wait_for_incoming():
    #looks for any ip
    ip = '0.0.0.0'
    port = 12345

    java_incoming_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    java_incoming_socket.bind((ip, port))

    #wait for a single connection request
    java_incoming_socket.listen(1)
    print("Server is listening")

    #approve the connection request
    java_outgoing_socket, java_client_address = java_incoming_socket.accept()
    print("Connected to:", java_client_address)

    java_incoming_socket.close()

    return java_outgoing_socket

#key exchange, this is the initiating function
#RETURNS symmetric key
def key_exchange_init(to_socket):
    p_public = None
    a_public = None
    a_private = None
    A_var = None
    B_var = None
    
    #generates random parameters for the key exchange
    key_gen_params = dh.generate_parameters(generator=2, key_size=1024, backend=default_backend())
    a_raw = key_gen_params.generate_private_key()
    p_public = key_gen_params.parameter_numbers().p
    a_public = key_gen_params.parameter_numbers().g

    #first it send the public key pair starting with p
    send_msg(p_public, to_socket)
    #after sending, wait for confirmation, confirmation is the same p value
    p_verification = receive_msg(to_socket)
    if int(p_public) != int(p_verification):
        raise ValueError("Key returned from client is faulty")

    #sends the second part of the public key pair
    send_msg(a_public, to_socket)
    a_verification = receive_msg(to_socket)
    if int(a_public) != int(a_verification):
        raise ValueError("Key returned from client is faulty")

    #calculates and send the A variable
    A_var = a_raw.public_key().public_numbers().y
    send_msg(A_var, to_socket)

    #after the other device calculates and sends B, here it is received
    B_var = None
    while B_var == None:
        B_var = receive_msg(to_socket)

    #get B object so we can work with it to generate the key
    B = dh.DHPublicNumbers(int(B_var), dh.DHParameterNumbers(int(p_public), int(a_public))).public_key(default_backend())

    #calculate key
    full_key = a_raw.exchange(B)
    #key parameters (such as a size of 256 bits)
    key = HKDF(
            algorithm=hashes.SHA256(),
            length=16,
            salt=None,
            info=b'info',
            backend=default_backend()
        ).derive(full_key)
    print(f"key: {key}")

    #what this function returns is the symmetric key
    return key

#dhke key exchange, this is the function that works with the one above on the other device
def key_exchange_rcv(from_socket):
    #receives and confirms public keys
    p_public = receive_msg(from_socket)
    send_msg(p_public, from_socket)
    a_public = receive_msg(from_socket)
    send_msg(a_public, from_socket)

    #receives the A variable from the other device
    A_var = receive_msg(from_socket)

    #calculates B based of the public key
    key_gen_params = dh.DHParameterNumbers(int(p_public), int(a_public)).parameters(default_backend())
    B_raw = key_gen_params.generate_private_key()
    B_var = B_raw.public_key().public_numbers().y
    send_msg(B_var, from_socket)

    #uses A to calculate the symmetric key
    A = dh.DHPublicNumbers(int(A_var), dh.DHParameterNumbers(int(p_public), int(a_public))).public_key(default_backend())
    full_key = B_raw.exchange(A)
    #sets key size to 16 bytes -> 256 bits
    key = HKDF(
            algorithm=hashes.SHA256(),
            length=16,
            salt=None,
            info=b'info',
            backend=default_backend()
        ).derive(full_key)
    print(f"key: {key}")
    return key
    
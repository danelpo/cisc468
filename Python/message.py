from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding
import os
import socket

#***build message with accordance to protocol
def make_message(message, key):
    iv = os.urandom(16)
    ciphertext = msg_enc(message, iv, key)
    plaintext = msg_dec(ciphertext, iv, key)
    print(message == plaintext)


#send message
def send_msg(message, ip, port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        try:
            s.connect((ip, port))
            s.sendall(message.encode())
            
        except Exception as e:
            print(f"Error sending message: {e}")

#message encryption
def msg_enc(plaintxt, iv, key):
    #pad message
    padding_engine = padding.PKCS7(128).padder()
    padded_plaintxt = padding_engine.update(plaintxt) + padding_engine.finalize()

    #encrypt
    encryption_engine = Cipher(algorithms.AES(key), modes.CBC(iv)).encryptor()
    enc_plaintxt = encryption_engine.update(padded_plaintxt) + encryption_engine.finalize()

    return enc_plaintxt

#***receive message

#message decryption
def msg_dec(ciphertxt, iv, key):
    #decrypt
    decryption_engine = Cipher(algorithms.AES(key), modes.CBC(iv)).decryptor()
    dec_ciphertxt = decryption_engine.update(ciphertxt) + decryption_engine.finalize()
    
    #remove the padding
    padding_engine = padding.PKCS7(128).unpadder()
    unpadded_ciphertxt = padding_engine.update(dec_ciphertxt) + padding_engine.finalize()
    
    return unpadded_ciphertxt

#***message verification

#***signature signing
    
make_message(b"hello", b"12312345123123451231234512312345")
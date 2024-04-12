from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding
import os
import time
from message_manager import hash_msg

#build message with accordance to agreed upon protocol
def package_message(message, key):
    #first 16 bytes are the IV
    iv = os.urandom(16)

    #next we add the ciphertext
    ciphertext = msg_enc(message.encode(), iv, key)

    #used to divide some parts
    divisor = "!!!!AAAABBBB!!!!"

    #used to protect against replay attaks
    timestamp = str(time.time() * 1000)

    #used for verification
    signature = hash_msg(iv + ciphertext + timestamp)

    #put everything together
    return iv + ciphertext + divisor + timestamp + divisor + signature

#message encryption
def msg_enc(plaintxt, iv, key):
    #pad message
    padding_engine = padding.PKCS7(128).padder()
    padded_plaintxt = padding_engine.update(plaintxt) + padding_engine.finalize()

    #encrypt using the key and iv
    encryption_engine = Cipher(algorithms.AES(key), modes.CBC(iv)).encryptor()
    enc_plaintxt = encryption_engine.update(padded_plaintxt) + encryption_engine.finalize()

    return enc_plaintxt

#similar to the pack function above, but in the opposite order
def unpack_message(message, key):
    #will divide into message, timestamp, and verification
    sections = message.split("!!!!AAAABBBB!!!!")

    #seperate iv from ciphertext
    iv = sections[0][:16]
    ciphertext = sections[0][16:]

    #get the timestamp and signature
    m_time = sections[1]
    signature = sections[2]

    #generate hash for verification below
    verification = hash_msg(iv + ciphertext + m_time)

    #verify time
    if int(m_time) < (int(time.time() * 1000) - 5000):
        raise RuntimeError("message failed verification due to time")
    #verify signature
    if signature != verification:
        raise RuntimeError("message failed verification due to signature")
    
    return msg_dec(ciphertext, iv, key)

#message decryption
def msg_dec(ciphertxt, iv, key):
    #decrypt using key and iv
    decryption_engine = Cipher(algorithms.AES(key), modes.CBC(iv)).decryptor()
    dec_ciphertxt = decryption_engine.update(ciphertxt) + decryption_engine.finalize()
    
    #remove the padding
    padding_engine = padding.PKCS7(128).unpadder()
    unpadded_ciphertxt = padding_engine.update(dec_ciphertxt) + padding_engine.finalize()
    
    return unpadded_ciphertxt
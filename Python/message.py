from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding
import os

#***build message with accordance to protocol
def package_message(message, key):
    iv = os.urandom(16)
    ciphertext = msg_enc(message.encode(), iv, key)
    return iv + ciphertext

#message encryption
def msg_enc(plaintxt, iv, key):
    #pad message
    padding_engine = padding.PKCS7(128).padder()
    padded_plaintxt = padding_engine.update(plaintxt) + padding_engine.finalize()

    #encrypt
    encryption_engine = Cipher(algorithms.AES(key), modes.CBC(iv)).encryptor()
    enc_plaintxt = encryption_engine.update(padded_plaintxt) + encryption_engine.finalize()

    return enc_plaintxt

def unpack_message(message, key):
    iv = message[:16]
    ciphertext = message[16:]
    return msg_dec(ciphertext, iv, key)

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
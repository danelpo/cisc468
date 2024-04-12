import socket
import hashlib
import message

#***send message
def send_msg(m, s, key=None, password=None):
    try:
        if password:
            save_message(m, password)
        if key:
            m_enc = message.package_message(m, key)
            return s.send(m_enc)
        else:
            return s.send(str(m).encode())
    except Exception as e:
        print("Can't send message:", e)

#receive message
def receive_msg(s, key=None, password=None, path=None):
    try:
        while True:
            data = s.recv(1024)
            if not data:
                break
            if key == None:
                return data.decode()
            else:
                if path == None:
                    plaintext = message.unpack_message(data, key).decode()
                    if password:
                        save_message(plaintext, password)
                    return plaintext
                else:
                    plaintext = message.unpack_message(data, key).decode()
                    with open(path, 'wb') as file:
                        file.write(plaintext)
                    return path
    except KeyboardInterrupt:
        print("Shutting down receiving")
        exit()
    return None

#save messages to device
def save_message(m, password):
    hash = hash_msg(password)
    iv = hash[:16]
    key = hash[-16:]
    file = "data.txt"
    ciphertext = message.msg_enc(m, iv, key)
    try:
        with open(file, 'a') as f:
            f.write('\n' + ciphertext)
    except FileNotFoundError:
         with open(file, 'w') as f:
            f.write(ciphertext)
    except Exception as e:
        print("Can't save messages to file:", e)

#retrieve messages from device
def read_messages(password):
    hash = hash_msg(password)
    iv = hash[:16]
    key = hash[-16:]
    file = "data.txt"
    all_ciphertext = []
    all_messages = []
    try:
        with open(file, 'r') as f:
            all_ciphertext.append(f.read().strip().split('\n'))
    except Exception as e:
        print("Can't get data:", e)
    else:
        for ciphertext in all_ciphertext:
            all_messages.append(message.msg_dec(ciphertext, iv, key))
        return all_messages

#hash messages, 64-bytes
def hash_msg(message):
    hash_object = hashlib.sha256()
    hash_object.update(message)
    return hash_object.hexdigest()
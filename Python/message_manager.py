import socket
import hashlib
import message

#***send message
def send_msg(m, s, key=None):
    try:
        if key:
            m_enc = message.package_message(m, key)
            return s.send(m_enc)
        else:
            return s.send(str(m).encode())
    except Exception as e:
        print("Can't send message:", e)

#receive message
def receive_msg(s, key=None):
    try:
        while True:
            data = s.recv(1024)
            if not data:
                break
            if key == None:
                return data.decode()
            else:
                return message.unpack_message(data, key).decode()
    except KeyboardInterrupt:
        print("Shutting down receiving")
        exit()
    return None

#save messages to device
def save_message(message):
    hashed = hash_msg(message)
    file = "data.txt"
    try:
        with open(file, 'a') as f:
            f.write('\n' + hashed)
    except FileNotFoundError:
         with open(file, 'w') as f:
            f.write(hashed)
    except Exception as e:
        print("Can't save messages to file:", e)

#retrieve messages from device
def read_messages():
    file = "data.txt"
    all_messages = []
    try:
        with open(file, 'r') as f:
            all_messages = f.read().strip().split('\n')
    except Exception as e:
        print("Can't get data:", e)
    else:
        return all_messages

#hash messages
def hash_msg(message):
    hash_object = hashlib.sha256()
    hash_object.update(message)
    return hash_object.hexdigest()
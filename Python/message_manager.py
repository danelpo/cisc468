import socket
import hashlib

#***send message
def send_msg(message, s):
    try:
        return s.send(message.encode())
    except Exception as e:
        print("Can't send message:", e)

#receive message
def receive_msg(s):
    try:
        while True:
            data = s.recv(1024)
            if not data:
                break
            print("Received:", data.decode())
            return data.decode()
    finally:
        s.close()
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
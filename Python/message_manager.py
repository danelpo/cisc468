import socket
import hashlib
import message

#send message
def send_msg(m, s, key=None, password=None):
    try:
        #password is needed to encrypt the messages locally to be saved on file 
        if password:
            #saves message locally
            save_message(m, password)
        #if we have key, it means we need to encrypt it
        if key:
            #gets ciphertext
            m_enc = message.package_message(m, key)
            #sends it off
            return s.send(m_enc)
        else:
            #if we don't encrypt it we send as it
            #this  is used for things like public keys before we have our symmetric key
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
            #similar formet to above with key and password
            #if its not encrypted, simple return it
            if key == None:
                return data.decode()
            else:
                #path is used for files. If path doesn't exist, its a simple text message
                if path == None:
                    #decrypt using key
                    plaintext = message.unpack_message(data, key).decode()
                    if password:
                        #if needed save to file
                        save_message(plaintext, password)
                    return plaintext
                else:
                    #decrypt file
                    plaintext = message.unpack_message(data, key).decode()
                    #save file to given path
                    with open(path, 'wb') as file:
                        file.write(plaintext)
                    return path
    except KeyboardInterrupt:
        print("Shutting down receiving")
        exit()
    return None

#save messages to device
def save_message(m, password):
    #generate hash from password
    hash = hash_msg(password)
    
    #we will use the first 16 bytes as the IV and the last 16 as the key
    iv = hash[:16]
    key = hash[-16:]

    #here is the file where we will save our messages
    file = "data.encrypted"

    #encrypt given message using the generated iv and key
    ciphertext = message.msg_enc(m, iv, key)
    try:
        #append to existing file
        with open(file, 'a') as f:
            f.write('\n' + ciphertext)
    except FileNotFoundError:
         #if no file exists, create one
         with open(file, 'w') as f:
            f.write(ciphertext)
    except Exception as e:
        print("Can't save messages to file:", e)

#retrieve messages from device
def read_messages(password):
    #generates hash from password and get iv and key
    hash = hash_msg(password)
    iv = hash[:16]
    key = hash[-16:]

    file = "data.encrypted"

    all_ciphertext = []
    all_messages = []
    try:
        #add all encrypted messages to array
        with open(file, 'r') as f:
            all_ciphertext.append(f.read().strip().split('\n'))
    except Exception as e:
        print("Can't get data:", e)
    else:
        #decrypt all messages in array and return
        for ciphertext in all_ciphertext:
            all_messages.append(message.msg_dec(ciphertext, iv, key))
        return all_messages

#hash messages, 64-bytes
def hash_msg(message):
    hash_object = hashlib.sha256()
    hash_object.update(message)
    return hash_object.hexdigest()
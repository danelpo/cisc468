import connection_manager
from message_manager import send_msg, receive_msg
from message_manager import read_messages

def search_for_connection():
    ip, port = connection_manager.discover_mDNS()
    if ip is not None:
        outgoing_socket = connection_manager.establish_connection(ip, port)
        return outgoing_socket

if __name__ == "__main__":
    #get socket
    outgoing_socket = None
    ui = None
    while outgoing_socket == None:
        print("Would you rather broadcast port and wait a connection or scan for an available port to connect to?")
        ui = input("Please type 'b' to broadcast, or 's' to scan\n")
        if ui == 'b':
            outgoing_socket = connection_manager.broadcast_connection()
        elif ui == 's':
            outgoing_socket = search_for_connection()
        else:
            print('incorrect input. Please try again')
    if outgoing_socket is None:
        raise ValueError("Outgoing socket failed to be established")
    else:
        #initiate key exchange
        if ui == 's':
            key = connection_manager.key_exchange_init(outgoing_socket)
        elif ui == 'b':
            key = connection_manager.key_exchange_rcv(outgoing_socket)

        #start messaging
        ui = None
        password = None
        ui = print("Would you like to save all messages to file? ('y' = Yes)")
        if ui == 'y':
            while password == None:
                password1 = print("Please enter password:\n")
                password2 = print("Please confirm password:\n")
                if password1 == password2:
                    password = password1
                else:
                    print("passwords do not match. Please try again.")
        print("If you want to read all messages saved to file, type 'read' at any point")
        while True:
            print("Would you rather send a message first or wait to receive one?")
            ui = input("Please type 's' to send, or 'r' to receive\n")
            if ui == 's':
                ui = input("Please type 't' to send text, or 'f' for files\n")
                if ui == 't':
                    message = input("Please enter message to be sent:\n")
                    send_msg(message, outgoing_socket, key=key, password=password)
                elif ui == 'f':
                    path = input("Please enter path to file:\n")
                    with open(path, 'rb') as file:
                        filetext = file.read()
                    send_msg(filetext, outgoing_socket, key=key, password=password)
                    pass
                else:
                    print('incorrect input. Please try again')
                    ui = None
            elif ui == 'r':
                ui = print("do you expect to receive text or file? 't' for text, 'f' for file")
                if ui == 't':
                    msg = receive_msg(outgoing_socket, key=key, password=password)
                    print(msg)
                elif ui == 'f':
                    path = input("Please enter path to save file:\n")
                    msg = receive_msg(outgoing_socket, key=key, password=password, path=path)
                    print(f"file saved to {path}")
                else:
                    print('incorrect input. Please try again')
                    ui = None
            elif ui == 'read':
                ui = input("Please provide password\n")
                saved_messages = read_messages(ui)
                for m in saved_messages:
                    print(m)
            else:
                print('incorrect input. Please try again')
                ui = None
        
    outgoing_socket.close()
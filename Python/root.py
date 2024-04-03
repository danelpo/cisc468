from connection_manager import broadcast_connection, discover_mDNS, establish_connection, key_exchange_init
from message_manager import send_msg, receive_msg

def search_for_connection():
    ip, port = discover_mDNS()
    if ip is not None:
        outgoing_socket = establish_connection(ip, port)
        return outgoing_socket

if __name__ == "__main__":
    #get socket
    outgoing_socket = None
    ui = None
    while outgoing_socket == None:
        print("Would you rather broadcast port and wait a connection or scan for an available port to connect to?")
        ui = input("Please type 'b' to broadcast, or 's' to scan\n")
        if ui == 'b':
            outgoing_socket = broadcast_connection()
        elif ui == 's':
            outgoing_socket = search_for_connection()
        else:
            print('incorrect input. Please try again')
    if outgoing_socket is None:
        raise ValueError("Outgoing socket failed to be established")
    else:
        #initiate key exchange
        if ui == 's':
            key = key_exchange_init(outgoing_socket)
        """
        option = None
        while option == None:
            print("Would you rather send a message first or wait to receive one?")
            ui = input("Please type 's' to send, or 'r' to receive\n")
            if ui == 's':
                message = input("Please enter message to be sent:\n")
                send_msg(message, outgoing_socket)
            elif ui == 'r':
                receive_msg(outgoing_socket)
            else:
                print('incorrect input. Please try again')
                option = None
        """
        outgoing_socket.close()
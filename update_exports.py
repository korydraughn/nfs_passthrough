import socket
import json

payload = (
'/     10.1.1.1(rw,sec=sys,anonuid=1000,anongid=1000)\n'
'/home 10.1.1.2(rw,sec=sys,anonuid=2000,anongid=2001)\n'
'/home 10.1.1.2(rw,sec=sys,anonuid=2000,anongid=2001)\n'
'/home 10.1.1.2(rw,sec=sys,anonuid=2005,anongid=2005)\n'
'/home/kory/Downloads *(rw,sec=sys)\n'
)

req = json.dumps({
    'op_code': 1000,
    'payload': payload
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


#!/usr/bin/env python3
import argparse, json, socket, struct, time

MAGIC = 0x4348
VERSION = 1
HEADER_BYTES = 28

TYPES = {
    'temperature': 1,
    'door-open': 2,
    'door-close': 3,
    'light-on': 4,
    'light-off': 5,
    'heartbeat': 6,
    'generic': 100,
}

def frame(msg_type, sequence, device_id, space_id, payload):
    dev = device_id.encode('utf-8')
    space = space_id.encode('utf-8')
    body = json.dumps(payload, separators=(',', ':')).encode('utf-8')
    total = HEADER_BYTES + len(dev) + len(space) + len(body)
    return struct.pack('>HBBIqqHH', MAGIC, VERSION, msg_type, total, sequence,
                       int(time.time() * 1000), len(dev), len(space)) + dev + space + body

def recv_exact(sock, n):
    chunks = []
    left = n
    while left:
        part = sock.recv(left)
        if not part:
            raise ConnectionError('connection closed')
        chunks.append(part)
        left -= len(part)
    return b''.join(chunks)

def read_ack(sock):
    head = recv_exact(sock, 8)
    magic, version, msg_type, total = struct.unpack('>HBBI', head)
    if magic != MAGIC or msg_type != 127 or total != 28:
        raise ValueError(f'invalid ACK header magic={magic:x} type={msg_type} total={total}')
    rest = recv_exact(sock, total - 8)
    sequence, status, event_seq = struct.unpack('>qIq', rest)
    return sequence, status, event_seq

def main():
    ap = argparse.ArgumentParser(description='CHRONOS Device Protocol v1 simulator')
    ap.add_argument('--host', default='127.0.0.1')
    ap.add_argument('--port', type=int, default=9100)
    ap.add_argument('--device', default='TEMP-001')
    ap.add_argument('--space', default='LAB-001')
    ap.add_argument('--type', choices=TYPES.keys(), default='temperature')
    ap.add_argument('--count', type=int, default=1)
    ap.add_argument('--sequence', type=int, default=1)
    ap.add_argument('--temperature', type=float, default=24.3)
    ap.add_argument('--split', action='store_true', help='send each frame in fragments to test TCP reassembly')
    args = ap.parse_args()

    with socket.create_connection((args.host, args.port), timeout=5) as s:
        for i in range(args.count):
            seq = args.sequence + i
            if args.type == 'temperature':
                payload = {'temperature': args.temperature + i * 0.1, 'unit': 'C'}
            elif args.type == 'heartbeat':
                payload = {'status': 'OK'}
            else:
                payload = {'value': True}
            data = frame(TYPES[args.type], seq, args.device, args.space, payload)
            if args.split and len(data) > 10:
                s.sendall(data[:3]); time.sleep(0.02)
                s.sendall(data[3:10]); time.sleep(0.02)
                s.sendall(data[10:])
            else:
                s.sendall(data)
            ack = read_ack(s)
            print(f'ACK sequence={ack[0]} status={ack[1]} eventSeq={ack[2]}')

if __name__ == '__main__':
    main()

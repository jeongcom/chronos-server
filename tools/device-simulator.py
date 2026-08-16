#!/usr/bin/env python3
import argparse, json, socket, struct, time

MAGIC=0x4348; VERSION=1; HEADER_BYTES=28; AUTH=7; ACK=127; RETRANSMIT=126
TYPES={'temperature':1,'door-open':2,'door-close':3,'light-on':4,'light-off':5,'heartbeat':6,'generic':100}

def frame(msg_type,sequence,device_id,space_id,payload):
    dev=device_id.encode(); space=space_id.encode(); body=json.dumps(payload,separators=(',',':')).encode()
    total=HEADER_BYTES+len(dev)+len(space)+len(body)
    return struct.pack('>HBBIqqHH',MAGIC,VERSION,msg_type,total,sequence,int(time.time()*1000),len(dev),len(space))+dev+space+body

def recv_exact(sock,n):
    b=b''
    while len(b)<n:
        p=sock.recv(n-len(b))
        if not p: raise ConnectionError('connection closed')
        b+=p
    return b

def read_response(sock):
    magic,version,msg_type,total=struct.unpack('>HBBI',recv_exact(sock,8)); rest=recv_exact(sock,total-8)
    if magic!=MAGIC: raise ValueError('invalid response magic')
    if msg_type==ACK:
        sequence,status,event_seq=struct.unpack('>qIq',rest); return ('ack',sequence,status,event_seq)
    if msg_type==RETRANSMIT:
        from_seq,to_seq=struct.unpack('>qq',rest); return ('retransmit',from_seq,to_seq)
    raise ValueError(f'unsupported response type={msg_type}')

def send_frame(sock,data,split=False):
    if split and len(data)>10:
        sock.sendall(data[:3]); time.sleep(.02); sock.sendall(data[3:10]); time.sleep(.02); sock.sendall(data[10:])
    else: sock.sendall(data)

def main():
    ap=argparse.ArgumentParser(description='CHRONOS Device Protocol v1 simulator')
    ap.add_argument('--host',default='127.0.0.1'); ap.add_argument('--port',type=int,default=9100)
    ap.add_argument('--device',default='TEMP-001'); ap.add_argument('--space',default='LAB-001'); ap.add_argument('--secret',required=True)
    ap.add_argument('--type',choices=TYPES.keys(),default='temperature'); ap.add_argument('--count',type=int,default=1)
    ap.add_argument('--sequence',type=int,default=1); ap.add_argument('--temperature',type=float,default=24.3)
    ap.add_argument('--split',action='store_true'); ap.add_argument('--skip-sequence',type=int,default=0)
    ap.add_argument('--hold-seconds',type=float,default=0)
    args=ap.parse_args(); pending={}
    with socket.create_connection((args.host,args.port),timeout=5) as s:
        send_frame(s,frame(AUTH,0,args.device,args.space,{'secret':args.secret}),args.split)
        auth=read_response(s); print('AUTH',auth)
        if auth[0]!='ack' or auth[2] not in (0,1): raise SystemExit('authentication failed')
        for i in range(args.count):
            seq=args.sequence+i
            if args.skip_sequence and seq==args.skip_sequence: continue
            payload={'temperature':args.temperature+i*.1,'unit':'C'} if args.type=='temperature' else ({'status':'OK'} if args.type=='heartbeat' else {'value':True})
            pending[seq]=frame(TYPES[args.type],seq,args.device,args.space,payload); send_frame(s,pending[seq],args.split)
            while True:
                r=read_response(s); print('RESPONSE',r)
                if r[0]=='retransmit':
                    for missing in range(r[1],r[2]+1):
                        data=pending.get(missing)
                        if data is None:
                            payload={'temperature':args.temperature+(missing-args.sequence)*.1,'unit':'C'} if args.type=='temperature' else {'value':True}
                            data=frame(TYPES[args.type],missing,args.device,args.space,payload); pending[missing]=data
                        send_frame(s,data,args.split); missing_ack=read_response(s); print('RETRANSMIT_RESPONSE',missing_ack)
                        if missing_ack[0]!='ack' or missing_ack[2] not in (0,1): raise SystemExit(f'retransmit failed for {missing}')
                    send_frame(s,pending[seq],args.split); continue
                if r[0]=='ack' and r[1]==seq: break
        if args.hold_seconds>0: time.sleep(args.hold_seconds)

if __name__=='__main__': main()

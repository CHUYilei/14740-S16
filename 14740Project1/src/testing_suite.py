from __future__ import division
import httplib
import sys
import socket

'''
14740 Project 1 

Author: Yilei CHU
Andrew ID: ychu1
Created on 2016 Feb 18

Testing suite:
send requests and parse response
requests include:
    improper requests: 
        (1) empty request
        (2) requested file not exists 
        (3) http version error
        (4) requested file permission error (no read authority)
    incomplete requests: ?
    client disconnection: ? thread: send then exit
'''

def printText(txt):
    lines = txt.split('\n')
    for line in lines:
        print line.strip()

def printResponse(response):   
    # http version, status code and message 
    print 'HTTP/'+str(response.version/10.0),response.status,response.reason
    
    # other headers
    for header in response.getheaders():
        print header[0]+': '+header[1]
        
    # content (if any)
    printText (response.read())

def printHeader(heads):
    for header in heads:
        print header[0]+': '+header[1]

# This is where the program begins execution
if __name__ == '__main__':
    # Make sure all the arguments are correct
    if len(sys.argv) != 3:
        sys.stderr.write('Usage: %s <server address> <port>\n' % (sys.argv[0]))
        sys.exit(1)
        
    # read server IP address
    serverIP = sys.argv[1]
    # validate IP
    try:
        socket.inet_aton(serverIP)
    except socket.error:
        sys.stderr.write('Usage: %s <server address> <port>\n' % (sys.argv[0]))
        sys.stderr.write('Server IP should be xx.xx.xx.xx where xx belongs to 0 to 255 \n')
        sys.exit(1)
        
    # read server port    
    try:
        PORT = int(sys.argv[2])
    except ValueError:
        sys.stderr.write('Usage: %s <server address> <port>\n' % (sys.argv[0]))
        sys.stderr.write('Port number should be with 1024 and 65535\n')
        sys.exit(1)        

    if (PORT > 65535 or PORT < 1024):
        sys.stderr.write('Usage: %s <server address> <port>\n' % (sys.argv[0]))
        sys.stderr.write('Port number should be with 1024 and 65535\n')
        sys.exit(1)

    # start constructing requests
    httpServ = httplib.HTTPConnection(serverIP, PORT)
    httpServ.connect()
        
    # test1: normal GET request
    print "================= Test 1: normal GET request ================="
    # 1/(1) GET index.html
    httpServ.request('GET', "/")  
    response = httpServ.getresponse()
    if response.status == httplib.OK:
        print "Output from GET HTML request:\n"
        printResponse(response)
     
    # 1/(2) GET image
    httpServ.request('GET', "/images/server_attention_span.png")    
    response = httpServ.getresponse()
    if response.status == httplib.OK:
        print "Output from GET image request:\n"
        printResponse(response)
          
    # test2: normal HEAD request
    print "================= Test 2: normal HEAD request ================="
    # 2/(1) HEAD index.html
    httpServ.request('HEAD', "/")    
    response = httpServ.getresponse()
    print "Output from HEAD HTML request:\n"
    printResponse(response)
      
    # 2/(2) HEAD image
    httpServ.request('HEAD', "/images/server_attention_span.png")    
    response = httpServ.getresponse()
    print "Output from HEAD image request:\n"
    printResponse(response)                                                                                                     
      
    # test3: dynamic CGI request
    print "================= Test 3: dynamic CGI request: calculate sum ================="
    httpServ.request('GET', '/cgi-bin/CalculateSum.cgi?A=2.4&B=3.5')
    response = httpServ.getresponse()
    if response.status == httplib.OK:
        print "Output from CGI request:\n"
        printResponse(response)
      
    # test4: test improper requests
    print "================= Test 4: improper requests ================="
      
    # 4/(1) requested file not exists
    print "================= (1) requested file not exist ================="    
    httpServ.request('GET', 'fake.html') 
    response = httpServ.getresponse()
    print "Output from improper request:\n"
    printResponse(response)
  
    # 4/(2): http version error
    print "================= (2) http version not correct ================="   
    httplib.HTTPConnection._http_vsn = 9
    httplib.HTTPConnection._http_vsn_str = 'HTTP/0.9'
    httpServ.request('GET', '/') 
    response = httpServ.getresponse()
    print "Output from improper request:\n"
    printResponse(response)
      
    # 4/(3): requested file permission error (no read authority)
    print "================= (3) requested file no read permission ================="   
    httplib.HTTPConnection._http_vsn = 10
    httplib.HTTPConnection._http_vsn_str = 'HTTP/1.0'
    httpServ.request('GET', 'readNotPermitted.txt') 
    response = httpServ.getresponse()
    print "Output from improper request:\n"
    printResponse(response)
        
    httpServ.close()
    
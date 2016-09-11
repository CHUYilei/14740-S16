Author: 
Yilei CHU(ychu1)
Xiaocheng OU(xou) 
==================================
instructions for running:

cd src
make clean;make
java -cp . simulate.Simulation > output.txt 2>&1

Then the output could be viewed in output.txt

Note: due to the random number setting in the project, you may need to run several times to see different results.

===================================
Documentation of classes and their functions:
** model package:
(1) Bandwidth: simulate the bandwidth condition in each node, used for selecting faster and stronger nodes in peer selection
(2) Location: simulate the location condition of each node, used for selecting closer nodes in peer selection
(3) Message: carry both the opcode and the corresponding info for communication between peers. Opcode lets peers know whether it is handshake or download/upload request.
(4) Peer: a.k.a node in P2P. Each peer will hold some resources, and will request resources from other peers according to torrent file and tracker’s instructions.
(5) TargetFile: the real file that peers need to download and upload, containing meta data like size, which is useful for chunking.
(6) Torrent.java: the .torrent file generated randomly for each peer, may contain files that the peer already has. It provides the goal of downloading for each peer.

** simulate package:
(1) Simulation: the entry of this project, defines the parameters for simulation. The main function is inside.
(2) Tracker: the tracker server of P2P, contains the latest chunk allocation info.

** util package:
(1) CommunicationUtil: callback method which calls the receiver function of node.
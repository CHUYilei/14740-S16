Author: Xiaocheng OU(xou) and Yilei CHU(ychu1)

instructions for running:
cd src
make clean;make
java -cp . applications.FtpServer 10000 10 1 
java -cp . applications.FtpClient 20000 10000 Project2-Handout.pdf 10000 1000

--------------- we have done bonus part ------------------
instructions for running test:
modify the DatagramService.java file
uncomment the lines after test case in sendDatagram method,
comment the lines after Send packet
then make clean, remake and run the client and server again

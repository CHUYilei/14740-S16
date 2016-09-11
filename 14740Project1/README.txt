Instructions for 14740 Project1:

==================== Command line intstructions =====================
Firstly, create a directory cgi-bin under www folder. Put the CalculateSum.class under cgi-bin folder

(1) Run server:
cd src
make clean;make
java Server 2345 ../www

(2) Run testing scripts:
cd src
python testing_suite.py 127.0.0.1 2345

(3) Test dynamic CGI:
After running (2)'s testing suite, go to /www/cgi-bin/ you will find temp.txt
with content:

a: 2.4 b: 3.5
current time: 15:38:37 <your current time>

==================== bonus part =================================
The following bonus parts have been implemented:
(1) Add support of dynamic requests  CGI Implementation
The CGI program is CalculateSum.java, which calculates a sum and prints the result and current time to a file under /www/cgi-bin/ called temp.txt

(2) Benchmarking the server using httperf and write a report.
Screenshots are provided in report. 

(3) Testing suite for Simple
testing_suite.py
JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	GetMime.java \
	CgiHandler.java \
	ParseRequest.java \
	ServerExecutor.java \
	Server.java \
	Client.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class

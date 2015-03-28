# JFLAGS=-g
#JFLAGS=-sourcepath absyn:rtl:mips -d build 

all:
	cd absyn; cp *.java ../generated
	cd rtl; cp *.java ../generated
	cd mips; cp *.java ../generated
	javacc parser/parser.jj 
	javac -d generated generated/*.java
clean:
	rm generated/rtl/*.class
	mkdir -p .waste
	mv generated/*.java .waste


# To compile: make
# To run:  cd generated && java -ea rtl.AyeC test.c && cd ..
# test.c -> your test file
# To remove generated files: make clean
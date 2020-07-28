# Linux shell script that compiles the Java code
#
# this is a Linux shell script
#
# it compiles the Java program for the static HTML photo page creator
#
# This code IS DEPENDENT on the use of 'Jackson' for JSON processing
#



JAVAC=javac
echo "- - - - - - -"
echo "- - - - - - -"
echo "- - - - - - -"
echo "- - - - - - -"


# following for JSON
CP=$CP:jackson-core-2.9.7.jar
CP=$CP:jackson-databind-2.9.7.jar
CP=$CP:.

OBJ=MakeWeb



echo "$JAVAC" -classpath "$CP" $OBJ.java
"$JAVAC" -classpath "$CP" $OBJ.java


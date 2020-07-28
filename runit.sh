# Linux shell script that executes the Java program to make static HTML photo pages
#
# this is a Linux shell script
#
# It requires that the 'project' name be
# specified on the command line
#
# that name is concatenated with some fixed strings
# and becomes the filenames processed by the Java
# code
#
# NOTE this code is DEPENDENT on the 'Jackson' product
# for JSON processing. The JAR's must be present when
# compiling and executing this program
#


PROJ=${1:?Must specify project name (arg 1)} 

JAVA=java

##MEM=" -Xms100m -Xmx100m "
##MEM=" -Xms400m -Xmx400m "
##MEM=" -Xms200m -Xmx200m "
MEM=



# following for JSON
CP=$CP:jackson-core-2.9.7.jar
CP=$CP:jackson-databind-2.9.7.jar
CP=$CP:jackson-annotations-2.9.7.jar
CP=$CP:.

echo classpath= $CP

OBJ=MakeWeb

echo "$JAVA" $MEM -classpath "$CP" $OBJ $PROJ
"$JAVA" $MEM -classpath "$CP" $OBJ  $PROJ


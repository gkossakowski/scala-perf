set -e

LIB="lib"
YOURKIT_JAR="$LIB/yjp-controller-api-redist.jar"
[ -z "$SCALA_HOME" ] && echo "Need to set SCALA_HOME variable" >&2 && exit 1;
CLASSPATH="$SCALA_HOME/lib/scala-library.jar:$SCALA_HOME/lib/scala-compiler.jar:$SCALA_HOME/lib/scala-reflect.jar:$YOURKIT_JAR"

mkdir -p target/classes
javac -cp $CLASSPATH -d target/classes/ src/main/java/scala/perf/*.java

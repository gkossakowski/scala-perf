set -e

[ -z "$YOURKIT_AGENT" ] && echo "Need to set SCALA_AGENT variable" >&2 && exit 1;
YOURKIT_OPTION="-agentpath:$YOURKIT_AGENT=dir=$OUTPUT"
JAVA_CMD="java $YOURKIT_OPTION"
LIB="lib"
YOURKIT_JAR="$LIB/yjp-controller-api-redist.jar"
[ -z "$SCALA_HOME" ] && echo "Need to set SCALA_HOME variable" >&2 && exit 1;
APP_CLASSPATH="$SCALA_HOME/lib/scala-library.jar:$SCALA_HOME/lib/scala-compiler.jar:$SCALA_HOME/lib/scala-reflect.jar:target/classes/:$YOURKIT_JAR"
SCALAC_CLASSPATH="$SCALA_HOME/lib/scala-library.jar:$SCALA_HOME/lib/scala-reflect.jar:$SCALA_HOME/lib/scala-compiler.jar"

[ -z "$OUTPUT" ] && echo "Need to set OUTPUT variable" >&2 && exit 1;

CLASSES="$OUTPUT/classes"

rm -rf $OUTPUT/classes
mkdir -p $OUTPUT/classes

$JAVA_CMD -Xmx512m \
  -cp $APP_CLASSPATH \
  scala.perf.Main \
    -cp $SCALAC_CLASSPATH \
    -d $CLASSES \
    $* | tee $OUTPUT/run.log

set -e

SCALA_GIT_REPO="$PWD/scala_repo"

WORKDIR=`pwd`/workdir
SCALA_PACKS=`pwd`/scala_packs
FILE_LIST="$WORKDIR/file-list"
# rm -rf WORKDIR
mkdir -p $SCALA_PACKS
mkdir -p $WORKDIR

function singleRun {
  local INPUT="$1"
  echo "Performing single run with INPUT: $INPUT"
  if [ ! -e $INPUT ]; then
    echo "Cannot find input: $INPUT"
    exit 1
  fi
  if [ -d $INPUT ]; then
    (find $INPUT -name '*.scala'; find $INPUT -name '*.java') > $FILE_LIST
  else
    echo "$INPUT" > $FILE_LIST
  fi
  
  echo "SCALA_HOME is $SCALA_HOME"
  echo "Compiling profiling runner"
  (cd profiling-runner && ./compile.sh) || { return 1; }
  echo "Running profiling"
  echo "file list is"
  cat $FILE_LIST
  (cd profiling-runner && ./run.sh "$SCALAC_OPTS" @$FILE_LIST)
}

function resolveRev {
  (cd $SCALA_GIT_REPO && git rev-parse "$1")
}

function revInfo {
 (cd $SCALA_GIT_REPO && git --no-pager log -n 1 --pretty=format:"commit %h %ai %s" $REV && echo "")
}

function ensureScalac {
  set -e
  local REV="$1"
  local FILE_NAME="pack-$REV.tgz"
  local FILE_PATH="$SCALA_PACKS/$FILE_NAME"
  local DIR="$SCALA_PACKS/pack-$REV"
  local URL="http://scala-webapps.epfl.ch/artifacts/$REV/pack.tgz"
  local URL_ALT="http://scala-webapps.epfl.ch/artifacts-manual/$REV/pack.tgz"
  if [ ! -d "$DIR" ]; then
    rm -rf $FILE_PATH
    rm -rf $DIR
    wget -q "$URL" -O $FILE_PATH || wget -q "$URL_ALT" -O $FILE_PATH || { rm -f $FILE_PATH; return 1; }
    mkdir -p $DIR
    cd $DIR
    tar xzf $FILE_PATH
    mv pack/* ./
    rm -rf pack/
  fi
  echo "$DIR"
}

function ensureScalaGitRepo {
  set -e
  if [ ! -d "$SCALA_GIT_REPO" ]; then
    git clone --mirror git://github.com/scala/scala.git $SCALA_GIT_REPO
  fi
  (cd $SCALA_GIT_REPO && git fetch --all)
}

function ensureYourKit {
  if [[ -z "$YOURKIT_HOME" ]]; then
    if [[ `uname -s` == "Darwin" ]]; then
      echo YOURKIT_TODO
      exit 1
    elif [[ `uname -s` == "Linux" ]]; then
      if [[ ! -d "$PWD/yjp-11.0.8" ]]; then
        wget http://www.yourkit.com/download/yjp-11.0.8-linux.tar.bz2
        tar xfj yjp-11.0.8-linux.tar.bz2
        rm -rf yjp-11.0.8-linux.tar.bz2
      fi
      export YOURKIT_HOME="$PWD/yjp-11.0.8"
    else
      echo "Unknown operating system"
      exit 1
    fi
  fi
  case `uname -s` in
    'Darwin')
      export YOURKIT_AGENT="$YOURKIT_HOME/bin/mac/libyjpagent.jnilib";;
    'Linux')
      # we assume that we always run on 64-bit machine
      export YOURKIT_AGENT="$YOURKIT_HOME/bin/linux-x86-64/libyjpagent.so";;
  esac
}

# This is a function that takes input file/directory name and turns it into
# nice directory name by applying the following transformations:
#  * replace all spaces by '_'
#  * replace all dots by '_'
#  * strip down all slashes
#
# E.g.:
# "Test.scala" -> "Test_scala"
# "scala-scalap/" -> "scala-scalap"
function turnIntoDirName {
  local result="$1"
  result=${result// /_}
  result=${result//./_}
  result=${result///}
  echo $result
}

REVISIONS=(`cd scala_repo && git rev-list 2.10.x --since="3/1/2012"`)

INPUTS=(`find $PWD/inputs -maxdepth 1 -mindepth 1 -name '*.scala'; find $PWD/inputs -maxdepth 1 -mindepth 1 -type d`)

echo "INPUTS are ${INPUTS[@]}"

rm -f run.log
ensureScalaGitRepo
ensureYourKit

function singleInput {
  set -e
  local INPUT_DIRNAME=$1
  [ -z "$INPUT_DIRNAME" ] && echo "Need to set INPUT_DIRNAME variable" >&2 && exit 1;
  export OUTPUT="$REV_OUTPUT/$INPUT_DIRNAME"
  export PROFILING_ITERATIONS="200"
  rm -rf "$OUTPUT"
  mkdir $OUTPUT
  singleRun $INPUT | tee -a run.log
  test ${PIPESTATUS[0]} -eq 0 || { return 1; }
}

function singleRev {
  set -e
  REV=`resolveRev $1`
  revInfo $REV
  
  export SCALA_HOME
  SCALA_HOME=`ensureScalac $REV` || { echo "Failed to obtain Scala $REV. Skipping."; return 1; }
  local REV_OUTPUT="$WORKDIR/$REV"
  if [ -d $REV_OUTPUT ]; then
    echo "The $REV_OUTPUT already exists. Skipping running benchmarks for $REV."
    return 0
  fi
  mkdir -p $REV_OUTPUT
  touch -c $REV_OUTPUT
 
  for INPUT in "${INPUTS[@]}"; do
    local INPUT_DIRNAME
    INPUT_DIRNAME=`turnIntoDirName ${INPUT##$PWD/inputs}`
    singleInput $INPUT_DIRNAME || { rm -rf $REV_OUTPUT; return 1; }
  done
}

for i in "${REVISIONS[@]}"; do
  (set -e; singleRev $i || true)
  echo ""
  # break
done

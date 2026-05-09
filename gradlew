#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a symlink
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls -ld "$PRG"
    link=`expr "$PRG" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MSYS* | MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/bin/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/bin/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" -a "$darwin" = "false" -a "$nonstop" = "false" ] ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n $MAX_FD
        if [ $? -ne 0 ] ; then
            warn "Could not set maximum file descriptor limit: $MAX_FD"
        fi
    else
        warn "Could not query maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# For Cygwin or MSYS, switch paths to Windows format before running java
if [ "$cygwin" = "true" -o "$msys" = "true" ] ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`

    JAVACMD=`cygpath --unix "$JAVACMD"`

    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    for arg do
        if expr "$arg" : '\(.*\)=' > /dev/null ; then
            arg_name=`expr "$arg" : '\(.*\)='`
            arg_value=`expr "$arg" : '.*=\(.*\)$'`
            case $arg_value in
                *" "*) arg_value=`printf '%s\n' "$arg_value" | sed "s/'/'\\\\''/g"` ;;
            esac
            eval "$arg_name=\$$arg_value"
            export $arg_name
        else
            case $arg in
                *" "*) arg=`printf '%s\n' "$arg" | sed "s/'/'\\\\''/g"` ;;
            esac
        fi
        args="$args \"$arg\""
    done
    set -- $args
fi

# Collect all arguments for the java command, stacking in reverse order:
#   * args from the command line
#   * the main class name
#   * -classpath
#   * -D...=... system properties
#   * anything else to be passed through to the main class name representing the path of the Java Virtual Machine.
#   * -Xmx...
#   * -Xms...

while [ $# -gt 0 ]
do
    case "$1" in
      -D*=*)  args="$args \"$1\"" ;;
      -J*)    let "first_arg=${1#-J}" && args="$args \"-D$first_arg\"" ;;
      *)      case "$1" in
                 --module-path ) jigsaw_args="--module-path=$2" && shift ;;
                 * ) break ;;
               esac ;;
    esac
    shift
done

rest=
for arg do
    rest="$rest \"$arg\""
done

# Determine if there's a java_modules_release file we should be checking.
if [ -f "$APP_HOME/gradle/java_modules_release" ]; then
    . "$APP_HOME/gradle/java_modules_release"
    for module in $MODULES_THAT_EXPORTS_JAVA_BASE; do
        jigsaw_args="$jigsaw_args --add-modules $module"
    done
fi

# Link the gradle wrapper jar in the classpath.
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar:$CLASSPATH"

if [ ! -z "$jigsaw_args" ] ; then
    GRADLE_OPTS="$jigsaw_args $GRADLE_OPTS"
fi

# This is normally unused
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Escape application args
save () {
    for i do printf %s\\n "$i" | sed "s/'/'\\\\''/g;s/.*/&/"; done
    echo " "
}
APP_ARGS=$(save "$@")

# Collect all arguments for the java command, following the shell quoting and substitution rules
eval "set -- $DEFAULT_JVM_OPTS $GRADLE_OPTS $APP_ARGS"

# by default we should be in the correct project dir, but when run from Finder on Mac, the cwd is wrong
if [ "$(uname)" = "Darwin" ] && [ "$HOME" = "$PWD" ]; then
    cd "$(dirname "$0")"
fi

exec "$JAVACMD" "$@"

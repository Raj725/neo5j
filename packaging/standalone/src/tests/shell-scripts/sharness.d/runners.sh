run_console() {
  neo5j-home/bin/neo5j console
}

start_daemon() {
  export JAVA_SENTINEL=$(mktemp /tmp/java-sentinel.XXXXX)
  trap "rm -rf ${JAVA_SENTINEL}" EXIT
  neo5j-home/bin/neo5j start
}

run_daemon() {
  start_daemon && \
    FAKE_JAVA_DISABLE_RECORD_ARGS="t" neo5j-home/bin/neo5j stop
}

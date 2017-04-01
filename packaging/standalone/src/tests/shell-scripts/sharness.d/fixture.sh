fake_install() {
  path="neo5j-home"

  mkdir -p "${path}/bin"
  cp ../../../main/distribution/shell-scripts/bin/* "${path}/bin" 2>/dev/null
  chmod +x "${path}/bin/neo5j"
  mkdir -p "${path}/conf"
  mkdir -p "${path}/data/databases"
  mkdir -p "${path}/lib"
  mkdir -p "${path}/logs"
  mkdir -p "${path}/run"
}

clear_config() {
  rm -f neo5j-home/conf/*
}

set_config() {
  name=$1
  value=$2
  file=$3
  echo "${name}=${value}" >>"${SHARNESS_TRASH_DIRECTORY}/neo5j-home/conf/${file}"
}

set_main_class() {
  class=$1
  sed -i.bak -e "s/#{neo5j\.mainClass}/${class}/" neo5j-home/bin/neo5j
}

neo5j_home() {
  echo "${SHARNESS_TRASH_DIRECTORY}/neo5j-home"
}

export JAVA_CMD="$(pwd)/sharness.d/fake-java"

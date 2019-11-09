#!/bin/bash

usage() {
cat << EOF
  Usage:
  $0 <install_dir>

  This script installs Sokrates in a given folder.

EOF
    exit 0
}

install_dir=${1}
shift

if [ -z "${install_dir}" ]; then
  echo "The installation folder not set";
  usage;
fi
if [ -d "${install_dir}" ]; then
  echo "The installation directory already exists, please remove it and try again.";
  exit 1
fi

echo "Installing Sokrates in ${install_dir}"
mvn -DskipITs=true -Dskip.unit.tests=true clean install $*

mkdir -p ${install_dir}

cp codeexplorer/target/codeexplorer-1.0-jar-with-dependencies.jar ${install_dir}/sokrates.jar

find ${install_dir} -type d | xargs chmod 755
find ${install_dir} -type f | xargs chmod 644
chmod 755 ${install_dir}/*

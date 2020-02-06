#!/bin/bash

declare SCRIPT_FILE="$(realpath "$0")"
declare SCRIPT_DIR="${SCRIPT_FILE%/*}"

if [[ -z "$TARGET_DIR" ]]; then
  declare TARGET_DIR="$PWD"
fi

declare TARGET_DIR_REALPATH="$(realpath "${TARGET_DIR}")"

if [[ -z "${TARGET_DIR_REALPATH}" || ! -d "${TARGET_DIR_REALPATH}" ]]; then
  echo "error: invalid target directory. Try TARGET_DIR=/some/path $0"
  exit 1
fi

if [[ "$SCRIPT_DIR" == "${TARGET_DIR_REALPATH}" ]]; then
  echo "error: invalid target directory '${TARGET_DIR_REALPATH}'. Same as source '${SCRIPT_DIR}'"
  exit 1
fi

TARGET_DIR="${TARGET_DIR_REALPATH}"

echo ":: copying configuration files..."
cp -v "${SCRIPT_DIR}/.editorconfig" "${SCRIPT_DIR}/.gitattributes" "${SCRIPT_DIR}/.gitignore" "${TARGET_DIR}"

echo ":: copying maven wrapper files..."
cp -v "${SCRIPT_DIR}/mvnw" "${SCRIPT_DIR}/mvnw.cmd" "${TARGET_DIR}"
mkdir -pv "${TARGET_DIR}/.mvn/wrapper"
cp "${SCRIPT_DIR}/.mvn/wrapper/maven-wrapper.properties" "${TARGET_DIR}/.mvn/wrapper"

echo ":: fixing line ending"
dos2unix "${TARGET_DIR}/mvnw"

echo ":: fixing permissions"
if [[ ! -x "${TARGET_DIR}/mvnw" ]]; then
  chmod u+x "${TARGET_DIR}/mvnw"
  git add --chmod=+x "${TARGET_DIR}/mvnw"
fi

echo ":: done"

#!/bin/bash

usage() {
  echo "
NAME

  ${0##*/} - release version using flatten maven plugin and friendly ci

OPTIONS

  -r, --release VERSION

    Target release version (default to revision minus SNAPSHOT)

  -d, --development VERSION

    Target development version (default to releaseVersion + 1 + SNAPSHOT)

"
}

main() {
  if ! TEMP=$(getopt -o 'hr:d:' --long 'help,release:,development:' -n "${0##*/}" -- "$@") ; then
    echo 'Terminating...' >&2
    return 1
  fi

  eval set -- "$TEMP"
  unset TEMP


  local releaseVersion=''
  local developmentVersion=''
  while true; do
    case "$1" in
      -h|--help)        usage ; return 2 ;;
      -r|--release)     releaseVersion="$2"     ; shift 2 ; continue ;;
      -d|--development) developmentVersion="$2" ; shift 2 ; continue ;;
      --)                                         shift   ; break ;;
      *)  echo "internal error" >&2 ; return 1 ;;
    esac
  done
  for arg; do
    echo "$arg"
  done
}

main "$@"

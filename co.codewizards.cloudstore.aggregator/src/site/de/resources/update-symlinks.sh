#!/bin/bash

## This script DELETES ALL SYMLINKS in the current directory and all subdirectories and replaces
## them by (correct) symlinks. Real files (that are not symlinks) are NOT deleted!
## Note: This script completely ignores hidden files and directories (which protects .svn!!!).
## 2012-03-30. Marco :-)

## SUB_DIR references the original directory from which we symlink files into this directory.
SUB_DIR="../../resources"

COPY_BASEDIR=`dirname $0`
COPY_BASEDIR=`pwd`
COPY_BASEDIR="${COPY_BASEDIR}/"

ORIG_BASEDIR="${COPY_BASEDIR}${SUB_DIR}"
cd "${ORIG_BASEDIR}" || exit 1
ORIG_BASEDIR=`pwd`
ORIG_BASEDIR="${ORIG_BASEDIR}/"

echo "COPY_BASEDIR=${COPY_BASEDIR}"
echo "ORIG_BASEDIR=${ORIG_BASEDIR}"

if [ "${COPY_BASEDIR}" = "${ORIG_BASEDIR}" ]; then
	echo "ERROR: COPY_BASEDIR = ORIG_BASEDIR !!!"
	exit 1
fi

function createRelativeSymlink
{
	ORIG_DIR=$1
	ORIG_FILE=$2
	SUFFIX_DIR=${ORIG_DIR#${ORIG_BASEDIR}}
	if [ "${SUFFIX_DIR}" != "" ]; then
		SUFFIX_DIR="/$SUFFIX_DIR"
	fi
	echo "SUFFIX_DIR=${SUFFIX_DIR}"

	SUFFIX="${SUFFIX_DIR}/${ORIG_FILE}"
#	if [ "${SUFFIX:0:1}" != "/" ]; then
#		SUFFIX="/$SUFFIX"
#	fi
	echo "SUFFIX=${SUFFIX}"

	echo mkdir -p "${COPY_BASEDIR}${SUFFIX_DIR}"
	/bin/mkdir -p "${COPY_BASEDIR}${SUFFIX_DIR}"


	if [ -L "${COPY_BASEDIR}${SUFFIX}" ]; then
		echo rm -v "${COPY_BASEDIR}${SUFFIX}"
		/bin/rm -v "${COPY_BASEDIR}${SUFFIX}"
	fi


	if [ ! -f "${COPY_BASEDIR}${SUFFIX}" ]; then

		REPLACEMENT=""
		REST=${SUFFIX_DIR}
		echo "REST=$REST"
		while [ "$REST" != "" ]; do
			REST="${REST%\/*}"
			REPLACEMENT="${REPLACEMENT}../"
		done
		echo "REPLACEMENT=$REPLACEMENT"

		echo ln -s "${REPLACEMENT}${SUB_DIR}${SUFFIX}" "${COPY_BASEDIR}${SUFFIX}"
		/bin/ln -s "${REPLACEMENT}${SUB_DIR}${SUFFIX}" "${COPY_BASEDIR}${SUFFIX}"

	fi
}


## DELETE first

echo

cd "${COPY_BASEDIR}"
DIR=${COPY_BASEDIR}
DIR_RELATIVE="."

ls -R | while read FILE; do
	FILE_LAST_CHAR=${FILE:((${#FILE} - 1)):1}
#	echo "FILE_LAST_CHAR=${FILE_LAST_CHAR}"
	if [ "${FILE_LAST_CHAR}" = ":" ]; then
		FILE_CLEANED=${FILE%\:}
		FILE_CLEANED=${FILE_CLEANED#.}
		FILE_CLEANED=${FILE_CLEANED#/}
#		echo "FILE_CLEANED=$FILE_CLEANED"
		DIR_RELATIVE=$FILE_CLEANED
		DIR="${COPY_BASEDIR}${FILE_CLEANED}"
	else
#		if [ "$DIR_RELATIVE" != "." -a "$FILE" != "" ]; then
#		echo "DIR=$DIR FILE=$FILE"
		if [ "$FILE" != "" ]; then
			if [ -L "$DIR/$FILE" -a "$FILE" != "`basename $0`" ] ; then
				echo "Deleting file '$FILE' in directory '$DIR'."
				/bin/rm "$DIR/$FILE"
			fi
		fi
	fi
done


## Create symlinks (including parent directories)

echo

cd "${ORIG_BASEDIR}"
DIR=${ORIG_BASEDIR}
DIR_RELATIVE="."

ls -R | while read FILE; do 
	FILE_LAST_CHAR=${FILE:((${#FILE} - 1)):1}
#	echo "FILE_LAST_CHAR=${FILE_LAST_CHAR}"
	if [ "${FILE_LAST_CHAR}" = ":" ]; then
		FILE_CLEANED=${FILE%\:}
		FILE_CLEANED=${FILE_CLEANED#.}
		FILE_CLEANED=${FILE_CLEANED#/}
#		echo "FILE_CLEANED=$FILE_CLEANED"
		DIR_RELATIVE=$FILE_CLEANED
		DIR="${ORIG_BASEDIR}${FILE_CLEANED}"
	else
#		if [ "$DIR_RELATIVE" != "." -a "$FILE" != "" ]; then
		if [ "$FILE" != "" ]; then
			if [ -f "$DIR/$FILE" ] ; then
				echo "Processing file '$FILE' in directory '$DIR'."
				createRelativeSymlink "$DIR" "$FILE"
				echo
			fi
		fi
	fi
done

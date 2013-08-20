#!/bin/bash

# makeimages.sh -> builds a set of basic images and a manifest

# calls prepareimage.sh [releaseVersion(quantal)] [image file] [size in mb]

IMAGES_DIR=`dirname $0`/../anubuntu-images
if [ ! -d "$IMAGES_DIR" ]; then
	echo "$IMAGES_DIR" does not exist.
	exit 1
fi
echo IMAGES_DIR="$IMAGES_DIR"

MANIFEST_FILE=MANIFEST.csv

rm -i "$IMAGES_DIR"/*

# image base names
declare -a imageBaseNames=(\
		ubuntu_2048m \
	)

# sizes in mb
declare -a imageSizes=(\
		2048 \
	)
	
# distros to use
declare -a distros=(\
		raring \
	)

# descriptions of these distros
declare -a distroDesc=(\
		'Raring Ringtail 13.04'\
	)

for distroIndex in ${!distros[@]}; do
	distro=${distros[$distroIndex]}
	distroDesc=${distroDesc[$distroIndex]}
	for sizeIndex in ${!imageBaseNames[@]}; do
		size=${imageSizes[$sizeIndex]}
		baseName=${imageBaseNames[$sizeIndex]}
		imageFile="$baseName"_$distro.img
		desc="$distroDesc - ${size} mb Root Image"
		echo `dirname $0`/prepareimage.sh "$distro" "$IMAGES_DIR"/"$imageFile" "$size"
		`dirname $0`/prepareimage.sh "$distro" "$IMAGES_DIR"/"$imageFile" "$size" || exit 2
		echo "Generated $IMAGES_DIR/$imageFile"

		echo MD5 Sum Generate
		(cd "$IMAGES_DIR" && md5sum "$imageFile" > "$imageFile".md5)

		echo Gzipping ...
		(cd "$IMAGES_DIR" && gzip --best "$imageFile")

		echo Splitting $imageFile
		(cd "$IMAGES_DIR" && split -a 1 -d -b 10000000 "$imageFile".gz "$imageFile".gz. )

		numchunks=`ls -1 "$IMAGES_DIR"/*.gz.* |wc -l `
		echo "chunks: $numchunks"

		echo "$imageFile,$numchunks,$desc"
		echo "$imageFile,$numchunks,$desc" >> "$IMAGES_DIR"/"$MANIFEST_FILE"

		rm "$IMAGES_DIR"/"$imageFile".gz
	done
done

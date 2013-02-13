#!/bin/bash

# 
# Copyright 2013 - Neal Hardesty - All rights reserved
#

# prepareimage.sh
#
# Run this on the desktop side...
#
# Usage: prepareimage.sh [releaseVersion(quantal)] [image file] [size in mb]

# Release version ..  natty, oneiric, precise, quantal, raring, etc...
if [ ! -z "$1" ]; then
	RELEASE="$1"
else 
	RELEASE="raring"
fi
echo Using RELEASE="$RELEASE"

# Size in megabytes of the image
if [ ! -z "$3" ]; then
	SIZE="$3"
else
	SIZE=2048
fi
echo Using SIZE=$SIZE

# Image file to generate...
if [ ! -z "$2" ]; then
	IMG_FILE="$2"
else
	IMG_FILE="ubuntu.img"
fi
echo Using IMG_FILE="$IMG_FILE"

# Architecture, ubuntu < precise = armel (no fpu required), ubuntu >= precise = armhf (w/ fpu)
ARCH="armhf"
echo Using ARCH="$ARCH"

# Temporary directory to use for debootstrap
TMP_DIR="$IMG_FILE"_tmp
echo Using TMP_DIR="$TMP_DIR"

# Ensure debootstrap is installed
if [ ! -x /usr/sbin/debootstrap ]; then
	echo Installing debootstrap...
	sudo apt-get install debootstrap
fi

# Clean up any previous work
if [ -d "$TMP_DIR" -o -f "$IMG_FILE" ]; then
	echo Spinelessly refusing to overwrite "$TMP_DIR" or "$IMG_FILE.  Bye."
	exit 1
fi

# Create the empty image
realsize=$(($SIZE*1024*1024))
echo Creating image "($realsize bytes)"
dd if=/dev/zero of="$IMG_FILE" seek=$realsize bs=1 count=1

# Build a filesystem
echo Creating filesystem
mkfs.ext4 -F "$IMG_FILE"

# Make the temporary directory
mkdir "$TMP_DIR"

# Mount this image into the temp directory
sudo mount -o loop "$IMG_FILE" "$TMP_DIR/"

# Run debootstrap
#sudo debootstrap --verbose --arch "$ARCH" --foreign stable debian http://ftp.us.debian.org/debian
echo sudo debootstrap --arch "$ARCH" --foreign "$RELEASE" "$TMP_DIR" "$ARCHIVE"
sudo debootstrap --arch "$ARCH" --variant=minbase --foreign "$RELEASE" "$TMP_DIR" "$ARCHIVE"

# Some sane defaults:
echo "nameserver 8.8.8.8" | sudo tee "$TMP_DIR"/etc/resolv.conf
echo "nameserver 8.8.4.4" | sudo tee -a "$TMP_DIR"/etc/resolv.conf
echo "android" | sudo tee "$TMP_DIR"/etc/hostname
echo "deb http://ports.ubuntu.com/ubuntu-ports/ $RELEASE main restricted universe multiverse" >> "$TMP_DIR"/etc/apt/sources.list
echo "127.0.0.1 localhost android" | sudo tee "$TMP_DIR"/etc/hosts

# Unmount
sudo umount "$TMP_DIR"

# Remove temporary directory
rm -rf "$TMP_DIR"

# gzip a copy of it
#echo Gzip\'ing a copy to "$IMG_FILE".gz
#gzip --best "$IMG_FILE" -c > "$IMG_FILE".gz

# gzip it
echo Gzip\'ing "$IMG_FILE"
gzip --best "$IMG_FILE"

# Bye
echo "Generated $IMG_FILE"

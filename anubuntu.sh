#!/system/bin/sh
# anubuntu.sh -- main setup/run script for anubuntu

#
# Copyright 2013 - Neal Hardesty - All rights reserved
#

MYNAME=$(basename $0)

# Generic information message                           
msg() {                                                 
	if $VERBOSE ; then
		echo "$MYNAME ["$(date +%H:%M:%S)"][$?]:" $*                
	fi
}                                           

# Fatal error with message                                                                   
die() {                                                              
	VERBOSE=true
	msg $*                                                       
	exit 1
}

# Confirmation message
readYesOrDie() {
	if ! $FORCE ; then
		echo -n "*** $* [yes|no] "
		read answer
		case "$answer" in
			yes|y)
				msg continuing
				;;
			*)
				die aborting
				;;
		esac
	fi
}

if [ -x /system/xbin/busybox ]; then 
	export BBOX=/system/xbin/busybox
else
	msg can not find busybox in /system/xbin/busybox
	die download it here: http://busybox.net/downloads/binaries/latest/
fi

if [ -z "$VERBOSE" ]; then
	VERBOSE=true
fi

export PATH=/sbin:/vendor/bin:/system/sbin:/system/bin:/system/xbin:/system/xbin:/system/bin:/usr/sbin:/bin:/usr/bin:/usr/local/bin
export HOME=/root
export SHELL=/bin/bash
LOOP_DEVICE_MINOR=$(($RANDOM%254))
#LOOP_DEVICE_MINOR=234
LOOP_DEVICE="/dev/block/loop$LOOP_DEVICE_MINOR"
export ROOT_DEFAULT="/sdcard/anubuntu"
if [ -z "$PS1" ]; then export PS1='\$'; fi

# this controls where we will copy this script to on init
export TARGET_PATH=/system/xbin/anubuntu

if [ -z "$ROOT_IMAGE" ]; then
	export ROOT_IMAGE="$ROOT_DEFAULT"/ubuntu.img
fi
ROOT_IMAGE=$(realpath "$ROOT_IMAGE")
if [ ! -f "$ROOT_IMAGE" ]; then
	msg can not find root image "'$ROOT_IMAGE'"
	die you can specify an image by setting ROOT_IMAGE
fi

if [ -z "$ROOT_MOUNT" ]; then
	export ROOT_MOUNT="$ROOT_DEFAULT"/mnt
fi
ROOT_MOUNT=$(realpath "$ROOT_MOUNT")

# Extra packages (space separated)
# cf https://help.ubuntu.com/community/MetaPackages
if [ -z "$EXTRA_PACKAGES" ]; then
	#export EXTRA_PACKAGES="openssh-server ubuntu-standard ubuntu-desktop build-essential"
	export EXTRA_PACKAGES="ubuntu-standard build-essential openssh-server xrdp"
fi

# Check for root
checkRootOrDie() {
	if [[ ! `$BBOX id |grep 'id=0'` ]]; then die must be root; fi
}

# Guess if we have run setup already or not..
isSetup=false
mount |grep "$ROOT_MOUNT" >> /dev/null
if [ $? -eq 0 ]; then
        # Setup probably already run
	isSetup=true
	msg setup already run
else
	isSetup=false
	msg setup NOT already run
fi


makeSystemWritable() {
	msg make /system read write
	mount -o remount,rw /system
}

makeSystemReadOnly() {
	msg make /system read only
	mount -o remount,ro /system
}

doChroot() {
	if $VERBOSE; then
		msg $BBOX chroot "$ROOT_MOUNT" $*
	fi
	$BBOX chroot "$ROOT_MOUNT" $*
}

#
# initialize -> first time initialization
#
initialize() {
	checkRootOrDie

	msg begin init

	if $isSetup && ! $FORCE ; then
		die it looks like you have already run setup, no need to initialize.
	fi

	msg copy $MYNAME to $TARGET_PATH
	makeSystemWritable
	rm -f "$TARGET_PATH"
	cp "$0" "$TARGET_PATH"
	chmod 755 "$TARGET_PATH"
	makeSystemReadOnly
	msg you can now run anubuntu from $TARGET_PATH

	setup

	# Handle the first time setup as necessary:
	if [ -f "$ROOT_MOUNT"/debootstrap/debootstrap ]; then
		msg first setup
		echo "nameserver 8.8.8.8" > "$ROOT_MOUNT"/etc/resolv.conf
		echo "nameserver 8.8.4.4" >> "$ROOT_MOUNT"/etc/resolv.conf

		msg running /debootstrap/debootstrap --second-stage
		doChroot /debootstrap/debootstrap --second-stage

	else
		readYesOrDie 'It looks like you have already run init, are you sure you wish to continue?'
		msg continuing...
	fi

	msg fixing up upstart for dpkg
	msg /usr/sbin/dpkg-divert --local --rename --add /sbin/initctl
	doChroot /usr/sbin/dpkg-divert --local --rename --add /sbin/initctl
	msg /bin/ln -sf /bin/true /sbin/initctl
	doChroot /bin/ln -sf /bin/true /sbin/initctl

	msg running apt-get autoclean
	doChroot /usr/bin/apt-get autoclean
	msg running apt-get update
	doChroot /usr/bin/apt-get update

	msg creating group inet
	doChroot /usr/sbin/groupadd -g 3004 inet

	if [ ! -z "$SETUP_USER" ]; then
		# https://blog.tuinslak.org/socket-permission-denied
		msg creating $SETUP_USER
		doChroot /usr/sbin/useradd -s /bin/bash -G inet -m $SETUP_USER
	fi

	if [ ! -z "$EXTRA_PACKAGES" ]; then
		msg installing EXTRA_PACKAGES: $EXTRA_PACKAGES
		doChroot /usr/bin/apt-get -y install $EXTRA_PACKAGES
	fi

	msg first setup complete

	msg end init
}


#
# download -> download the base image
#
download() {
	if $isSetup && ! $FORCE ; then
		die looks like setup already run and FORCE '(-f)' not set
	fi

	if [ -f "$ROOT_IMAGE" ]; then
		readYesOrDie you already have $ROOT_IMAGE and FORCE '(-f)' not set.  are you sure you want to overwrite the current file?
	fi

	dfetch=$($BBOX dirname "$0")/dfetch.jar

	manifestUrl="https://github.com/nealhardesty/anubuntu-images/blob/master/MANIFEST.csv?raw=true"
	urlBase="https://github.com/nealhardesty/anubuntu-images/blob/master"

	msg downloading manifest
	manifest=`dalvikvm -cp $dfetch dfetch - "$manifestUrl" 2> /dev/null`

	if [ -z "$IMAGE_SELECT" ]; then
		echo You must select a base image to use by typing the corresponding number:
		count=0
		echo "$manifest" | while read -r line; do
			echo $count : $line
			count=$((count + 1))
		done
		read imageSelectIndex
	else
		imageSelectionIndex=$IMAGE_SELECT
	fi

	msg imageSelectIndex = $imageSelectIndex

	count=0;
	manifestLine=`echo "$manifest" | while read -r line; do
		if [ "$count" == "$imageSelectIndex" ]; then
			echo $line
			break
		fi
		count=$((count + 1))
	done`
	msg got oline: $manifestLine

	chunks=`echo $manifestLine |cut -d ',' -f 2`
	urlFile=`echo $manifestLine |cut -d ',' -f 1`
	description=`echo $manifestLine |cut -d ',' -f 3`
	msg chunks=$chunks
	msg urlFile=$urlFile
	msg description=$description

	localDir=$(dirname "$ROOT_IMAGE")

	md5url="${urlBase}/${urlFile}.md5?raw=true"
	md5file="${localDir}/${urlFile}.md5"

	msg downloading md5 checksum hash to ${md5file}
	dalvikvm -cp $dfetch dfetch "$md5file" "$md5url" || die can not download ${md5url}

	for i in $(seq 0 $(($chunks - 1))); do
		url="${urlBase}/${urlFile}.gz.${i}?raw=true"
		local="${localDir}/${urlFile}.${i}"

		msg downloading "$local"

		dalvikvm -cp $dfetch dfetch "$local" "$url"
	done

	msg concatenating and decompressing ...
	$BBOX cat ${localDir}/${urlFile}.? |gunzip > "${localDir}/$urlFile"

	$BBOX md5sum -c -s $md5file || die $md5file md5 checksum failed

	mv "$localDir/$urlFile" "$ROOT_IMAGE"

	msg remove temporary files
	rm ${localDir}/${urlFile}.?
	rm ${md5file}

	msg download complete
	msg "you should now run '$MYNAME init'"
}

#
# setup -> needs to be run before a shell is created
#
setup() {
	checkRootOrDie

	if $isSetup && ! $FORCE ; then
		die looks like setup already run and FORCE '(-f)' not set
	fi

	msg begin setup
	msg ROOT_IMAGE=$ROOT_IMAGE
	msg ROOT_MOUNT=$ROOT_MOUNT

	if [ ! -b "$LOOP_DEVICE" ]; then $BBOX mknod "$LOOP_DEVICE" b 7 $LOOP_DEVICE_MINOR || die can not make loop $LOOP_DEVICE; fi
	if [ ! -d "$ROOT_MOUNT" ]; then mkdir -p "$ROOT_MOUNT" || die can not mkdir "$ROOT_MOUNT"; fi


	msg "mount $ROOT_IMAGE on $ROOT_MOUNT using $LOOP_DEVICE"
	$BBOX losetup "$LOOP_DEVICE" "$ROOT_IMAGE" || die failed to losetup $ROOT_IMAGE on $ROOT_MOUNT using $LOOP_DEVICE
	$BBOX mount -t ext4 -o noatime,nodiratime,dev,exec "$LOOP_DEVICE" "$ROOT_MOUNT" || die failed to mount $LOOP_DEVICE on $ROOT_MOUNT
	msg mount $ROOT_MOUNT/proc
	$BBOX mount -t proc proc "$ROOT_MOUNT"/proc || msg failed to mount /proc
	msg mount $ROOT_MOUNT/sys
	$BBOX mount -t sysfs sysfs "$ROOT_MOUNT"/sys || msg failed to mount /sys
	mkdir -p $ROOT_MOUNT/dev/pts
	msg mount $ROOT_MOUNT/dev/pts
	$BBOX mount -t devpts devpts "$ROOT_MOUNT"/dev/pts || msg failed to mount /dev/pts

	if [ ! -d "$ROOT_MOUNT"/etc ]; then
		die could not mount "$ROOT_MOUNT"
	fi

	msg "setup networking"
	$BBOX sysctl -w net.ipv4.ip_forward=1 >> /dev/null 2>&1 || die could not setup forwarding

	msg "mount sdcard in chroot /sdcard"
	if [ ! -d "$ROOT_MOUNT"/sdcard ]; then mkdir "$ROOT_MOUNT"/sdcard; fi
	$BBOX mount --bind /sdcard/ "$ROOT_MOUNT"/sdcard || msg could not mount /sdcard

	msg "build $ROOT_MOUNT/etc/mtab"
	
	echo "rootfs / rootfs ro,relatime 0 0" |tee "$ROOT_MOUNT"/etc/mtab
	echo "/dev/fuse /sdcard fuse rw,noatime 0 0" |tee -a "$ROOT_MOUNT"/etc/mtab

	if [ -x "$ROOT_MOUNT"/etc/rc.local ]; then
		# we run /etc/rc.local ONLY
		msg running scripts from /etc/rc.local
		doChroot /bin/bash /etc/rc.local
	fi

	msg end setup
}

#
# teardown -> cleanup
#
teardown() {
	checkRootOrDie

	msg "teardown $ROOT_MOUNT ..."
	msg "sending SIGINT ..."
	$BBOX lsof |$BBOX grep -i "$ROOT_MOUNT" | $BBOX awk '{print $1}' |$BBOX uniq |$BBOX xargs kill
	$BBOX sleep 5
	msg "sending SIGKILL ..."
	$BBOX lsof |$BBOX grep -i "$ROOT_MOUNT" | $BBOX awk '{print $1}' |$BBOX uniq |$BBOX xargs kill -9
	sleep 1

	realloop=`mount |grep anubuntu/mnt |grep loop |awk '{print $1}'`

	msg umount $ROOT_MOUNT/sdcard
	$BBOX umount "$ROOT_MOUNT"/sdcard
	msg umount $ROOT_MOUNT/proc
	$BBOX umount "$ROOT_MOUNT"/proc
	msg umount $ROOT_MOUNT/sys
	$BBOX umount "$ROOT_MOUNT"/sys
	msg umount "$ROOT_MOUNT"/dev/pts
	$BBOX umount "$ROOT_MOUNT"/dev/pts
	msg umount $ROOT_MOUNT
	$BBOX umount "$ROOT_MOUNT"
	if [ ! -z "$realloop" ]; then
		msg unsetup $realloop
		$BBOX losetup -d "$realloop" >> /dev/null 2>&1
	fi


	msg end teardown
}

#
# help -> help message
#
help() {
echo 'Usage: $MYNAME <init|start|stop|run|issetup|download|help> [-f|commands*]'
echo ''
echo 'Standard commands:'
echo '	start *:	setup mounts and preconditions (accepts -f for force)'
echo '			[aliases: startup setup]'
echo ''
echo '	stop *:		teardown mounts and kill processes '
echo '			[aliases: teardown shutdown]'
echo ''
echo '	<empty>:	when no command specified, run the remaining command line '
echo '			in a chroot shell'
echo ''
echo 'Specialized commands:'
echo '	init *: 	first run setup (accepts -f for force)'
echo '			[aliases: initialize]'
echo ''
echo '	issetup:	return status 0 if setup, 1 if not'
echo '			[aliases: is_setup]'
echo '	'
echo '	help:		this message'
echo ''
echo '	cd:		changes to the root of the chroot'
echo ''
echo ''
echo 'Environment Variables:'
echo '	VERBOSE		[default: true]'
echo '	ROOT_IMAGE	[default: /sdcard/anubuntu/ubuntu.img]'
echo '	ROOT_MOUNT	[default: /sdcard/anubuntu/mnt]'
echo '	SETUP_USER	[default: ]'
echo '			used only in init'
echo '	EXTRA_PACKAGES	[default: ubuntu-standard build-essential openssh-server xrdp]'
echo '			used only in init'
echo '	IMAGE_SELECT	[default: ]'
echo '			used only in download'
echo ''
}

issetup() {
	if $isSetup ; then
		echo setup
		exit 0
	else
		echo not setup
		exit 1
	fi
}


#
# run -> main chroot run section
#
run() {
	if $isSetup ; then	
		msg Welcome to anubuntu
		msg ""
		doChroot $*
	else
		die please run "'$MYNAME setup'" first
	fi
}


# 
# Command parsing (Main)
#

case "$2" in 
	"-f"|"--force")
		FORCE=true
		;;
	*)
		FORCE=false
		;;
esac

case "$1" in
	initialize|init)
		initialize
		;;
	download)
		download
		;;
	setup|startup|start)
		setup
		;;
	issetup|is_setup)
		issetup
		;;
	teardown|shutdown|stop)
		teardown
		;;
	help|-h|--help|-?)
		help
		;;
	cd)
		cd "$ROOT_MOUNT"
		;;
	*)
		run $*
		;;
esac

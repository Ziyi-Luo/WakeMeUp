# coding: utf-8

import numpy as np
import cv2
import sys
from pylibfreenect2 import Freenect2, SyncMultiFrameListener
from pylibfreenect2 import FrameType, Registration, Frame
import time
import os

try:
    from pylibfreenect2 import OpenGLPacketPipeline
    pipeline = OpenGLPacketPipeline()
except:
    from pylibfreenect2 import CpuPacketPipeline
    pipeline = CpuPacketPipeline()

fn = Freenect2()
num_devices = fn.enumerateDevices()
if num_devices == 0:
    print("No device connected!")
    sys.exit(1)

serial = fn.getDeviceSerialNumber(0)
device = fn.openDevice(serial, pipeline=pipeline)

listener = SyncMultiFrameListener(FrameType.Ir)

device.setIrAndDepthFrameListener(listener)

directory = os.path.dirname(os.path.realpath('__file__'))

try:
	while 1:
		device.start()
		count=0
		for i in range(1,5):
			frames = listener.waitForNewFrame()
			ir = frames["ir"]
			tmpimg = ir.asarray() / 256.
			tmp = tmpimg.astype(int)
			path = directory +"/" + time.asctime(time.localtime()) + ".png"
			print path
			cv2.imwrite(path, tmp)
			listener.release(frames)
			if i < 4 :
				time.sleep(30-time.localtime().tm_sec%30)
		device.stop()
		time.sleep((89-time.localtime().tm_sec)%30)
except KeyboardInterrupt:
	device.close()
	exit

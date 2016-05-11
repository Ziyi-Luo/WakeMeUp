# coding: utf-8

import numpy as np
import cv2
import sys
from pylibfreenect2 import Freenect2, SyncMultiFrameListener
from pylibfreenect2 import FrameType, Registration, Frame
import time
import os

class KinectControl:
	listener = None
	device = None
	def __init__(self):
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
		    return

		serial = fn.getDeviceSerialNumber(0)
		device = fn.openDevice(serial, pipeline=pipeline)
		listener = SyncMultiFrameListener(FrameType.Ir)
		device.setIrAndDepthFrameListener(listener)

	def service_start(self):
		self.device.start()

	def service_stop(self):
		self.device.stop();

	def service_func(self):
		try:
			frames = self.listener.waitForNewFrame()
			ir = frames["ir"]
			tmpimg = ir.asarray() / 256.
			tmp = tmpimg.astype(int)
			self.listener.release(frames)
			return tmp
		except KeyboardInterrupt:
			self.device.close()
			return None

# if __name__ == '__main__':
# 	service_func()

from flask import Flask, request
import threading
import time

# for capture image
import numpy as np
import cv2
import sys
from pylibfreenect2 import Freenect2, SyncMultiFrameListener
from pylibfreenect2 import FrameType, Registration, Frame
import os

# for analyze image
from PIL import Image
import numpy
from PIL import ImageEnhance
from os import listdir
from os.path import isfile, join

app = Flask(__name__)
shouldntwake = True
# CHANGE HERE
coefficients = [0.01632861, 0.01365656, 0.01353211, 0.01518467, 0.01966368]
threshold = 0.457611117866

# method used in analyze_image
def mse(imageA, imageB):
	# the 'Mean Squared Error' between the two images is the
	# sum of the squared difference between the two images;
	# NOTE: the two images must have the same dimension
	err = numpy.sum((imageA.astype(float) - imageB.astype(float)) ** 2)
	err /= float(imageA.shape[0] * imageA.shape[1])
	
	# return the MSE, the lower the error, the more "similar"
	# the two images are
	return err

def analyze_image_thread(waketime, interval):

	global shouldntwake

	# Start analyzing images
	time.sleep(max(150, waketime - interval - time.time())) # wait for 5 images or wait for the earliest start time

	while shouldntwake and time.time() < waketime + interval:
		shouldntwake = analyze_image(coefficients, threshold)
		time.sleep(31) # wait for a new image to come in

	shouldntwake = False

def capture_image():
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

def analyze_image(coefficients, threshold):
	mypath = '/usr/local/Cellar/python/2.7.11/Frameworks/Python.framework/Versions/2.7/Resources/Python.app/Contents/Resources' # MY PATH COULD BE WRONG
	files = [f for f in listdir(mypath) if isfile(join(mypath, f))]

	previous = None
	current = None
	MSEs = []
	for img in files:
		tmp = Image.open(mypath+"/"+img)
		contraster = ImageEnhance.Contrast(tmp)
		imEnhance = contraster.enhance(4.0)
		current = numpy.asarray(tmp)
		if previous == None:
			previous = current
			continue
		cmse = mse(previous,current)
		previous = current
		MSEs.append(cmse)
		# print cmse

	wake_or_not = sum([a*b for a,b in zip(coefficients, MSE[len(MSE)-5:len(MSE)])])
	if wake_or_not >= threshold: # THIS IS A LIGHT SLEEP
		return False
	return True

@app.route("/trigger", methods =['POST'])
def handle_trigger():

	global shouldntwake
	# NOTE: this assumes trigger only happens 30 min prior to the ealiest wakeup time
	# test to receive message from http post
	# parameters = waketime +- interval
	data = request.data
	jsonobj = json.loads(data)
	print 'REQUEST_DATA is: ', jsonobj
	waketime = jsonobj['waketime']
	interval = jsonobj['interval']

	# Start collecting images
	ci = threading.Thread(name='capture_image', target=capture_image)
	ci.start()

	# Start preparing to analyze data
	ai = threading.Thread(name='analyze_image_thread', target=analyze_image_thread, args=[waketime, interval])
	ai.start()

	return jsonify({'data':'OK!!!!!'})

@app.route("/wakeornot", methods=["GET"])
def wake_or_not():
	return jsonify({"data":str(not shouldntwake)}) # COULD BE WRONG

if __name__ == '__main__':
	app.run(debug=True, host='0.0.0.0')


from flask import Flask, request, jsonify
import threading
import time
import json

# for capture image
import numpy as np
import cv2
import sys
from pylibfreenect2 import Freenect2, SyncMultiFrameListener
from pylibfreenect2 import FrameType, Registration, Frame
import os
from get2 import KinectControl

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

MSEs = []
day = None
waketime = None
interval = None

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

def analyze_image_thread():

	print 'waketime = ' + str(waketime)
	print 'interval = ' + str(interval)

	rightnow = time.localtime().tm_hour * 3600 + time.localtime().tm_min * 60 + time.localtime().tm_sec
	localtime = (day - time.localtime().tm_mday) * 24 * 3600 + waketime - interval - rightnow - 150 # 

	print localtime
	return localtime

def mustWakeup():
	global shouldntwake
	# Start analyzing images
	# Only when 
	# if localtime > 0:
	# time.sleep(max(150, localtime)) # wait for 5 images or wait for the earliest start time

	rightnow = time.localtime().tm_hour * 3600 + time.localtime().tm_min * 60 + time.localtime().tm_sec
	till_latest = (day - time.localtime().tm_mday) * 24 * 3600 + waketime + interval - rightnow # sec till latest wakeup time

	if till_latest > 0:
		return False
	return True
	
def capture_image(localtime):
	print "Capturing data......"
	previous = None
	current = None
	global MSEs
	if localtime > 0:
		time.sleep(localtime)
	kinect = KinectControl()
	while shouldntwake:
		kinect.service_start()
		for i in range(1,5):
			tmp = kinect.service_func()
			if(previous == None):
				previous = tmp
				current = previous
			else:
				previous = current
				current = tmp
				MSEs.append(mse(previous,current))
				analyze_image()
			if i < 4:
				time.sleep(30-time.localtime().tm_sec%30)
		kinect.service_stop()
		time.sleep((90-time.localtime().tm_sec)%30)



def analyze_image():
	global shouldntwake
	global MSEs

	if (not shouldntwake):
		return

	if(len(MSEs) < 5):
		shouldntwake = True
		return
	else:
		wake_or_not = sum([a*b for a,b in zip(coefficients, MSEs[len(MSEs)-5:len(MSEs)])])
		if wake_or_not >= threshold or mustWakeup(): # THIS IS A LIGHT SLEEP
			shouldntwake = False
			return
	shouldntwake = True


	# mypath = '/usr/local/Cellar/python/2.7.11/Frameworks/Python.framework/Versions/2.7/Resources/Python.app/Contents/Resources' # MY PATH COULD BE WRONG
	# files = [f for f in listdir(mypath) if isfile(join(mypath, f))]

	# previous = None
	# current = None
	# MSEs = []
	# for img in files:
	# 	tmp = Image.open(mypath+"/"+img)
	# 	contraster = ImageEnhance.Contrast(tmp)
	# 	imEnhance = contraster.enhance(4.0)
	# 	current = numpy.asarray(tmp)
	# 	if previous == None:
	# 		previous = current
	# 		continue
	# 	cmse = mse(previous,current)
	# 	previous = current
	# 	MSEs.append(cmse)
		# print cmse





@app.route("/trigger", methods =['POST'])
def handle_trigger():
	global shouldntwake
	global day
	global waketime
	global interval
	# NOTE: this assumes trigger only happens 30 min prior to the ealiest wakeup time
	# test to receive message from http post
	# parameters = waketime +- interval (BOTH IN MIN)
	data = request.data
	jsonobj = json.loads(data)
	print 'REQUEST_DATA is: ', jsonobj

	day = int(jsonobj['day'].encode("utf-8")) # which day
	waketime = int(jsonobj['waketime'].encode("utf-8")) * 60 # wake up time in min
	interval = int(jsonobj['interval'].encode("utf-8")) * 60 # wake up interval in min
	print day
	print waketime
	print interval

	# Start collecting images
	shouldntwake = True
	localtime = analyze_image_thread();
	ci = threading.Thread(name='capture_image', target=capture_image,args=[localtime])
	ci.start()

	return jsonify({'data':'ok'})

@app.route("/wakeornot", methods=["GET"])
def wake_or_not():
	print "At this time, not shouldntwake = " + str(not shouldntwake)
	return jsonify({"data":not shouldntwake}) # COULD BE WRONG
	# return jsonify({"data":True})

if __name__ == '__main__':
	app.run(debug=True, host='0.0.0.0')


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
previous = None
current = None

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

def get_pending_time(day, waketime, interval):
	rightnow = time.localtime().tm_hour * 3600 + time.localtime().tm_min * 60 + time.localtime().tm_sec
	localtime = (day - time.localtime().tm_mday) * 24 * 3600 + waketime - interval - rightnow - 150 # 
	if localtime<0:
		return 0
	print "pending time: ",localtime
	return localtime

def analyze_image_thread(day, waketime, interval,localtime):

	global shouldntwake

	print 'waketime = ' + str(waketime)
	print 'interval = ' + str(interval)

	# Start analyzing images
	# Only when 
	# if localtime > 0:
	time.sleep(localtime) # wait for 5 images or wait for the earliest start time

	print "START Analyzing data......"

	rightnow = time.localtime().tm_hour * 3600 + time.localtime().tm_min * 60 + time.localtime().tm_sec
	till_latest = (day - time.localtime().tm_mday) * 24 * 3600 + waketime + interval - rightnow # sec till latest wakeup time
	if time.localtime().tm_sec % 30 == 0:
		time.sleep(15)
	while (shouldntwake and till_latest > 0):
		print "???????? INSIDE OF WHILE LOOP ?????????"
		tmp = time.time()
		shouldntwake = analyze_image(coefficients, threshold)
		rightnow = time.localtime().tm_hour * 3600 + time.localtime().tm_min * 60 + time.localtime().tm_sec
		till_latest = (day - time.localtime().tm_mday) * 24 * 3600 + waketime + interval - rightnow
		delta = time.time() - tmp
		print "-------- analyzing once --------"
		time.sleep(30-delta)

	shouldntwake = False

def capture_image():

	print "Capturing data......"
	os.system("python get2.py")

def analyze_image(coefficients, threshold):
	global MSEs
	global previous
	global current

	mypath = '/usr/local/Cellar/python/2.7.11/Frameworks/Python.framework/Versions/2.7/Resources/Python.app/Contents/Resources' # MY PATH COULD BE WRONG
	tmp = Image.open(mypath+"/kinect.png")
	print "read kinect.png"
	contraster = ImageEnhance.Contrast(tmp)
	imEnhance = contraster.enhance(4.0)
	if previous == None:
		current = numpy.asarray(tmp)
		previous = current
		return True
	previous = current
	current = numpy.asarray(tmp)
	MSEs.append(mse(previous,current))
		# print cmse
	if(len(MSEs)<5):
		return True

	wake_or_not = sum([a*b for a,b in zip(coefficients, MSEs[len(MSEs)-5:len(MSEs)])])
	if wake_or_not >= threshold: # THIS IS A LIGHT SLEEP
		return False
	return True

@app.route("/trigger", methods =['POST'])
def handle_trigger():
	
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
	pending_time = get_pending_time(day, waketime, interval)

	ci = threading.Thread(name='capture_image', target=capture_image)
	ci.start()


	# Start preparing to analyze data
	ai = threading.Thread(name='analyze_image_thread', target=analyze_image_thread, args=[day, waketime, interval, pending_time])
	ai.start()

	return jsonify({'data':'ok'})

@app.route("/wakeornot", methods=["GET"])
def wake_or_not():
	print "At this time, not shouldntwake = " + str(not shouldntwake)
	return jsonify({"data":not shouldntwake}) # COULD BE WRONG
	# return jsonify({"data":True})

if __name__ == '__main__':
	app.run(debug=True, host='0.0.0.0')


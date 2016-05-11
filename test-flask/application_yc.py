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

def analyze_image_thread(day, waketime, interval):

	global shouldntwake

	print 'waketime = ' + str(waketime)
	print 'interval = ' + str(interval)

	rightnow = time.localtime().tm_hour * 3600 + time.localtime().tm_min * 60 + time.localtime().tm_sec
	localtime = (day - time.localtime().tm_mday) * 24 * 3600 + waketime - interval - rightnow - 150 # 

	print localtime

	# Start analyzing images
	# Only when 
	# if localtime > 0:
	time.sleep(max(150, localtime)) # wait for 5 images or wait for the earliest start time

	print "START Analyzing data......"

	rightnow = time.localtime().tm_hour * 3600 + time.localtime().tm_min * 60 + time.localtime().tm_sec
	till_latest = (day - time.localtime().tm_mday) * 24 * 3600 + waketime + interval - rightnow # sec till latest wakeup time

	while (shouldntwake and till_latest > 0):
		print "???????? INSIDE OF WHILE LOOP ?????????"
		shouldntwake = analyze_image(coefficients, threshold)
		print "-------- analyzing once --------"
		time.sleep(30) # wait for a new image to come in

		rightnow = time.localtime().tm_hour * 3600 + time.localtime().tm_min * 60 + time.localtime().tm_sec
		till_latest = (day - time.localtime().tm_mday) * 24 * 3600 + waketime + interval - rightnow


	# # simulate updating shouldntwake
	# for i in range(1,3):
	# 	time.sleep(5)

	# set the global shouldnt awake
	shouldntwake = False

def capture_image():

	print "Capturing data......"
	os.system("python get2.py")

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
	ci = threading.Thread(name='capture_image', target=capture_image)
	ci.start()


	# Start preparing to analyze data
	ai = threading.Thread(name='analyze_image_thread', target=analyze_image_thread, args=[day, waketime, interval])
	ai.start()

	return jsonify({'data':'ok'})

@app.route("/wakeornot", methods=["GET"])
def wake_or_not():
	print "At this time, not shouldntwake = " + str(not shouldntwake)
	return jsonify({"data":not shouldntwake}) # COULD BE WRONG
	# return jsonify({"data":True})

if __name__ == '__main__':
	app.run(debug=True, host='0.0.0.0')


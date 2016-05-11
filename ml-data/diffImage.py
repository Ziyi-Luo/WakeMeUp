import Image,numpy
from PIL import ImageEnhance

def mse(imageA, imageB):
	# the 'Mean Squared Error' between the two images is the
	# sum of the squared difference between the two images;
	# NOTE: the two images must have the same dimension
	err = numpy.sum((imageA.astype(float) - imageB.astype(float)) ** 2)
	err /= float(imageA.shape[0] * imageA.shape[1])
	
	# return the MSE, the lower the error, the more "similar"
	# the two images are
	return err

from os import listdir
from os.path import isfile, join
import sys
if len(sys.argv) < 2:
	print "Lack argument"
	exit(-1)
mypath = sys.argv[1]
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
	print cmse
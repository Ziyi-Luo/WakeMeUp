import os
import time

directory = os.path.dirname(os.path.realpath('__file__'))
print directory

try:
	while 1:
		for i in range(1,5):
			print i
			if i<4:
				time.sleep(1)
		time.sleep(2)
except KeyboardInterrupt:
	exit

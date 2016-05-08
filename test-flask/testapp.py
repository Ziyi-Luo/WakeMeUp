from flask import Flask, request
import threading
import time
import requests
import json
app = Flask(__name__)


@app.route("/trigger", methods =['GET', 'POST', 'PUT'])
def handle_trigger():
	print 'REQUEST FORM: ', request.form
	latest_time = str(request.data)
	print 'REQUEST_DATA is: ', latest_time
	return 'hello world'
	# data = json.loads(request.data)
 #        print 'JSON:',data

    #return json.dumps({'data':'Hello World'})

if __name__ == '__main__':
	app.run(debug=True, host='0.0.0.0')
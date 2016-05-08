from flask import Flask, request
import threading
import time
import requests
import json
from flask import render_template, jsonify, Flask
app = Flask(__name__)


@app.route("/trigger", methods =['GET', 'POST', 'PUT'])
def handle_trigger():
	data = request.data
	jsonobj = json.loads(data)
	print 'REQUEST_DATA is: ', jsonobj
	
	return jsonify({'data':data})


if __name__ == '__main__':
	app.run(debug=True, host='0.0.0.0')
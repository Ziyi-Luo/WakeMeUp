# WakeMeUp
Class project of Spring 2016 E6765 Internet of Things, Columbia University

__Group member__: Ziyi Luo, Yuechen Zhao, Yazhuo Nan

Description
-----------
Project website [link](http://iotcolumbia2016tue4.weebly.com/).

Course website [link](http://iotcolumbia.weebly.com/).

Running instruction
-----------
To run the program, basic hardwares & softwares are required:


__Hardware__: <br /> 
Kinect V2 (with adapter for PC connection) <br /> 
Android phone

__Software__: <br /> 
Python 2.7 with [OpenCV](http://opencv.org/); [OpenKinect2](https://github.com/OpenKinect/libfreenect2); [Python Imaging Library (PIL)](http://www.pythonware.com/products/pil/); [Flask framework](http://flask.pocoo.org/). <br /> 
Android Studio <br />

To build up the server, connect Kinect V2 to the server. You can check the connection by doing
```
cd get-data
python multiframe_listener.py
```
If no error shows and the all frames work well, the connection of Kinect is successful. <br /> 
To start the server, under the root directory do
```
cd test-flask
python application.py
```
To create the Android appication, build `WakeMeUp-android` using Android Studio. Note that the application requires a minimum Android OS version 4.4.
To run the application, make sure the server has a reachable IP address for the client. Set the IP and enjoy the comfortable sleep provided by WakeMeUp!


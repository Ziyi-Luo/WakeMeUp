from sklearn import linear_model
from sklearn.metrics import mean_squared_error
import csv
import statistics

# Load the data, split into training and test datasets
sleepdatafile = open('combine.csv', 'rb')
sleepdataReader = csv.reader(sleepdatafile, delimiter = ',')
clf = linear_model.Lasso(alpha=0)
output_csv = open('output_csv.csv', 'a')
output_writer = csv.writer(output_csv, delimiter = ',')

training_X = []
training_Y = []

for row in sleepdataReader:
	del row[0:2]
	row = map(float, row)
	training_Y.append(row[5])
	# del row[5:10]
	del row[5]
	training_X.append(row)

# 6200:6839
#10500:11200
test_X = training_X[9479:10479]
del training_X[9479:10479]
test_Y = training_Y[9479:10479]
del training_Y[9479:10479]

# Fit the data
clf.fit(training_X, training_Y)

# Predict the training Y; Calculate training error
predict_from_training = clf.predict(training_X)

# Set the threshold
threshold = statistics.median(predict_from_training)


#WRITE TO THE FILE!!!!!!!!!!!
for i in range(0, len(training_Y)):
	output_writer.writerows([[training_Y[i], predict_from_training[i]]])



for i in range(0, len(predict_from_training)):
	if (predict_from_training[i] >= threshold):
		predict_from_training[i] = 1
	else:
		predict_from_training[i] = 0

print 'Training error = ', mean_squared_error(training_Y, predict_from_training)

# Predict the test Y; Calculate test error
predict_from_test = clf.predict(test_X)


# WRITE TO THE FILE!!!!!!!!!!!
for i in range(0, len(test_Y)):
	output_writer.writerows([[test_Y[i], predict_from_test[i]]])



for i in range(0, len(predict_from_test)):
	if (predict_from_test[i] >= threshold):
		predict_from_test[i] = 1
	else:
		predict_from_test[i] = 0


print 'Test error = ', mean_squared_error(test_Y, predict_from_test)
print 'The threshold is = ' , threshold



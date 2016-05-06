import sys

if __name__ == "__main__":
	f = open(sys.argv[1], 'r')
	i=0
	for line in f:
		ele = line.split(',')
		opline = None
		if ele[2]=="deep":
			opline = str(i)+",0,"+ele[3]
		else:
			opline = str(i)+",1,"+ele[3]
		# opline = str(i)+","+ele[1]+","+ele[2]
		sys.stdout.write(opline)
		i=i+30

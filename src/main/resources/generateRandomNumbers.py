import random
import math

def dis(point1,point2):
	return math.sqrt((point1[0]-point2[0])**2+(point1[1]-point2[1])**2)

a = -100
b = 100
n = int(eval(input("Please enter # of points: ")))
print("Begin generating...")
points = []
for i in range(n):
	points.append((random.uniform(a,b),random.uniform(a,b)))
	# print(points[i])
res = []
for i in range(n):
	temp = []
	for j in range(n):
		temp.append(dis(points[i],points[j]))
	res.append(temp)
outfile = open("./matrix.txt","w")
for i in range(n):
	for j in range(n):
		if (j != n-1):
			outfile.write("%.3f " % res[i][j])
		else:
			outfile.write("%.3f\n" % res[i][j])
print("Finished!!!")

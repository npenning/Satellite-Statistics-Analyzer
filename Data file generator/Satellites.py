# -*- coding: utf-8 -*-
"""
Spyder Editor

This is a temporary script file.
"""

from skyfield.api import load
#from pathos.multiprocessing import ProcessPool
import numpy as np
from numba import cuda
import math

class Connection:
    stop = None
    duration = 0
    def __init__(self, sat1, sat2, start):
        self.sat1 = sat1
        self.sat2 = sat2
        self.start = start

    def toString(self):
        return(str(self.sat1) + "\t\t" + str(self.sat2) + "\t\t" + str(self.start) + "\t\t" + str(self.stop) + "\t\t" + str(self.duration))

stations_url = 'http://celestrak.com/NORAD/elements/active.txt'
satellites = load.tle(stations_url)
satellite = satellites['COSMOS 2545 [GLONASS-M]']
TRANSMISSION_RANGE = 150
keys = []
sats = []
connections = []

current = 0
total = len(satellites.values());
print(satellite.epoch.utc_jpl())
  
def coordCalc(sat, t):
    return sat.at(t).position.km

@cuda.jit()   
def getLocation(m, transRange, inRange):
    # Matrix index.
    x = cuda.threadIdx.x
    size = m.shape[0]
    # Skip threads outside the matrix.
    earthRad = 6371;
    maxRange = 5100
    
    if x >= size:
        return
    # Run the simulation.
    for i in range(x, 2627, 1024):
        for j in range(i+1, size):
            dx = m[j][0] - m[i][0]
            dy = m[j][1] - m[i][1]
            dz = m[j][2] - m[i][2]
            dMag = math.sqrt(dx**2 + dy**2 + dz**2)
            if(dMag > maxRange):
                inRange[i][j] = False
                continue
            dx = dx/dMag
            dy = dy/dMag
            dz = dz/dMag
            b = m[i][0] * dx + m[i][1] * dy + m[i][2] * dz
            c = (m[i][0]**2 + m[i][1]**2 + m[i][2]**2) - earthRad**2
            h = b**2 - c;
            if(h < 0):
                inRange[i][j] = True # no intersection
                continue
            h = math.sqrt(h)
            if(min(-b-h, -b+h) > dMag):
                inRange[i][j] = True
                continue
            inRange[i][j] = False
    return
        #inRange[i][j] = earthRad < (np.linalg.norm((np.cross(m[i]), np.negative(m[j]))))/np.linalg.norm(m[j]-m[i]))
        #inRange[i][j] = (math.sqrt((m[i][0]-m[j][0])**2 + (m[i][1]-m[j][1])**2 + (m[i][2]-m[j][2])**2) <= transRange)
        


print(cuda.gpus[0].name)



for satellite in satellites.values():
    if satellite.name in keys:
        continue
    keys.append(satellite.name)
    sats.append(satellite)
    
    
ts = load.timescale()
t = ts.utc(2020, 3, 17, 0, 0, 0)

for sat in sats:
    if abs(t - sat.epoch) > 2:
        print("flag", sat.name)
        sats.remove(sat)
     
inclinations = {}
for sat in sats:
    i = round(sat.model.inclo, 1)
    if i in inclinations:
        inclinations[i] = inclinations[i] + 1
    else:
        inclinations[i] = 1
for k in inclinations.keys():
    print(k, inclinations[k])
            
for inc in inclinations:
    print(inc)
print(len(sats))
m = np.array([sat.at(t).position.km for sat in sats])
inRange = np.zeros((len(m), len(m)))
lastRange = np.zeros((len(m), len(m)))
connectionStart = np.zeros((len(m), len(m)))
names = [sat.name for sat in sats]

# 16x16 threads per block.
bs = 1024
# Number of blocks in the grid.
bpg = int(np.ceil(len(sats) / bs))
# We prepare the GPU function.
getLoc = getLocation[(bpg), (bs)]

with open("key.txt", 'w') as file:
    for name in names:
        file.write(str(name))
        file.write('\n')

with open("output"+str(TRANSMISSION_RANGE)+".txt", 'w') as file:
    for hour in range(0, 24):
        for minute in range(0, 60):
            for second in range(0, 60, 20):
                t = ts.utc(2020, 3, 17, hour, minute, second)
                m = np.array([sat.at(t).position.km for sat in sats])
                getLoc(m, TRANSMISSION_RANGE, inRange)
                changed = np.where((np.logical_xor(inRange, lastRange)) == True)
                lastRange = inRange.copy()
                linkedSats = list(zip(changed[0], changed[1]))
                for pair in linkedSats:
                    for num in pair:
                        file.write(str(num) + " ")
                file.write('\n')
                print(hour, minute, second)



"""

                manager = Manager()
                mChunks = []
                for i in range(4):
                    mChunks.append(manager.list())
                processes = []
                processes.append(Process(target=coordCalc, args=(m[0:1499], mChunks[0], t)))
                processes.append(Process(target=coordCalc, args=(m[1500:2999], mChunks[1], t)))
                processes.append(Process(target=coordCalc, args=(m[3000:4499], mChunks[2], t)))
                processes.append(Process(target=coordCalc, args=(m[4500:], mChunks[3], t)))
                for p in processes:
                    p.start()
                for p in processes:
                    p.join()
                m = mChunks[0] + mChunks[1] + mChunks[2] + mChunks[3]
                """

"""
for hour in range(0, 24):
    for minute in range(0, 60):
        for second in range(0, 60):
            time = ts.utc(2020, 3, 18, hour, minute, second)
            print('processing timestamp ', hour, ":", minute, ":", second)
            for sat1 in sats:
                print(sats.index(sat1))
                if not(sat1.name in hasOpenConnection.keys()):
                    hasOpenConnection[sat1.name] = {}
                    connections[sat1.name] = {}
                for sat2 in sats[sats.index(sat1):]:
                    if not(sat2.name in hasOpenConnection[sat1.name].keys()):
                        hasOpenConnection[sat1.name][sat2.name] = False
                        connections[sat1.name][sat2.name] = []
                    pos1 = sat1.at(time)
                    pos2 = sat2.at(time)
                    distance = np.sqrt(np.sum((pos1.position.km-pos2.position.km)**2))
                    if distance < TRANSMISSION_RANGE:
                        if hasOpenConnection[sat1.name][sat2.name] != True:
                            hasOpenConnection[sat1.name][sat2.name] = True
                            connections[sat1.name][sat2.name].append(Connection(sat1.name, sat2.name, time))
                        else:
                            connections[sat1.name][sat2.name][-1:].duration += 1
                    else:
                        if hasOpenConnection[sat1.name][sat2.name] == True:
                            hasOpenConnection[sat1.name][sat2.name] = False
                            connections[sat1.name][sat2.name][-1:].stop = time
                            
with open("output.txt", 'w') as file:
    for s1 in connections:
        for s2 in s1:
            for c in s2:
                file.write(c.toString)
                file.write('\n')
                """
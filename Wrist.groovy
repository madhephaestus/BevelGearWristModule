CSGDatabase.clear()
LengthParameter printerOffset 			= new LengthParameter("printerOffset",0.5,[1.2,0])
LengthParameter boltlen 			= new LengthParameter("Bolt Length",0.675,[1.2,0])
def motorOptions = []
def shaftOptions = []
for(String vitaminsType: Vitamins.listVitaminTypes()){
	HashMap<String, Object> meta = Vitamins.getMeta(vitaminsType);
	if(meta !=null && meta.containsKey("actuator"))
		motorOptions.add(vitaminsType);
	if(meta !=null && meta.containsKey("shaft"))
		shaftOptions.add(vitaminsType);
}
StringParameter motors = new StringParameter("Motor Type","roundMotor",motorOptions)
StringParameter shafts = new StringParameter("Shaft Type","dShaft",shaftOptions)
StringParameter motorSize = new StringParameter("Motor Size","WPI-gb37y3530-50en",Vitamins.listVitaminSizes(motors.getStrValue()))
StringParameter shaftSize = new StringParameter("Shaft Size","WPI-gb37y3530-50en",Vitamins.listVitaminSizes(shafts.getStrValue()))

def motorBlank= Vitamins.get(motors.getStrValue(),motorSize.getStrValue())
def shaftBlank= Vitamins.get(shafts.getStrValue(),shaftSize.getStrValue())
double motorAngleOffset = 65
double knuckelThicknessAdd = 2
double pitch = 4
double pinRadius = ((3/16)*25.4+printerOffset.getMM())/2
double pinLength = (16)+ (printerOffset.getMM()*2)

double partsGapBetweenGearsAndBrackets =1
double actualBoltLength = 35+partsGapBetweenGearsAndBrackets*2
double boltPattern = 10
String size ="M5"
HashMap<String, Object>  boltData = Vitamins.getConfiguration( "capScrew",size)
HashMap<String, Object>  nutData = Vitamins.getConfiguration( "lockNut",size)
CSG nut =Vitamins.get("lockNut",size)

CSG nutKeepaway =nut.hull().makeKeepaway(printerOffset.getMM())
double nutHeight = nut.getMaxZ()
double washerRadius =  boltData.outerDiameter/2+printerOffset.getMM()*5

if(args == null){
	args=[
		Vitamins.get("ballBearing","695zz").hull().makeKeepaway(printerOffset.getMM()).toZMin(),
		new Cylinder(pinRadius,pinRadius,pinLength,(int)30).toCSG(), // steel reenforcmentPin
		new Cylinder(pinRadius,pinRadius,pinLength,(int)30).toCSG().movez(-pinLength/2) // steel reenforcmentPin
		
	]
}
double washerInset = motorBlank.getMaxZ()-args[0].getTotalZ()
double washerThickness = washerInset+partsGapBetweenGearsAndBrackets
def washer = new Cylinder(washerRadius,washerThickness).toCSG()
			.difference(new Cylinder(boltData.outerDiameter/2+printerOffset.getMM()/2,washerThickness).toCSG())
def washerKeepaway = 	new Cylinder(washerRadius+printerOffset.getMM(),washerThickness).toCSG()	

println "Bolt"+boltData
println "nut"+nutData
println "Pin len ="+pinLength

double linkMaterialThickness = pinLength/2

double washerOd = 17
double washerId = 12.75
double bearingThickness = args[0].getMaxZ()
double gearThickness =(pinLength -(bearingThickness*3 -washerThickness*2))/2

double encoderToEncoderDistance =args[0].getMaxX()*3+
					gearThickness*2 -
					washerThickness*4 +
					args[0].getMaxZ()*2+
					bearingThickness*2+
					nutHeight*2+
					partsGapBetweenGearsAndBrackets*2

int aTeeth =    Math.PI*(encoderToEncoderDistance+washerThickness*2)/pitch
int bTeeth =    Math.PI*(args[0].getMaxX()+washerThickness*2+gearThickness+nutHeight)*2/pitch 
// call a script from another library
List<Object> bevelGears = (List<Object>)ScriptingEngine
					 .gitScriptRun(
            "https://github.com/madhephaestus/GearGenerator.git", // git location of the library
            "bevelGear.groovy" , // file to load
            // Parameters passed to the funcetion
            [	  aTeeth,// Number of teeth gear a
	            bTeeth,// Number of teeth gear b
	            gearThickness,// thickness of gear A
	            pitch,// gear pitch in arch length mm
	           90
            ]
            )
List<Object> spurGears = (List<Object>)ScriptingEngine
					 .gitScriptRun(
            "https://github.com/madhephaestus/GearGenerator.git", // git location of the library
            "bevelGear.groovy" , // file to load
            // Parameters passed to the funcetion
            [	  bTeeth,// Number of teeth gear a
	            20,// Number of teeth gear b
	            gearThickness,// thickness of gear A
	            pitch,// gear pitch in arch length mm
	           0,
	           0
            ]
            )
//Print parameters returned by the script
println "Bevel gear radius A " + bevelGears.get(2)
println "Bevel gear radius B " + bevelGears.get(3)
println "Spur gear radius A " + spurGears.get(2)
println "Spur gear radius B " + spurGears.get(3)
double ratio1 = (spurGears[7])
double ratio2 = (bevelGears[7])
double ratio = ratio1*ratio2
println "Total Wrist Ratio is 1=" +ratio1+" 2="+ratio2+" total = "+ratio
encoderToEncoderDistance = bevelGears.get(2)*2
double outerBearingDistance = (encoderToEncoderDistance/2+gearThickness+washerThickness+args[0].getTotalZ())*2
double gearBThickness =bevelGears.get(6)
double distanceToShaft =bevelGears.get(3)
double distancetoGearFace = bevelGears.get(2)
double distanceToMotor = bevelGears.get(3)+spurGears[2]+spurGears[3]
double shaftToMotor = spurGears[2]+spurGears[3]
double knuckelY = args[0].getTotalY()+knuckelThicknessAdd*2
double knuckelZ = distanceToShaft+(knuckelY/2)-gearThickness+knuckelThicknessAdd+nut.getMaxX()-partsGapBetweenGearsAndBrackets
double knuckelX = encoderToEncoderDistance-(gearBThickness*2)-1-partsGapBetweenGearsAndBrackets*2
def centeredSpur = spurGears[1].movex((spurGears[2]+spurGears[3]))
				.toZMax()
//return centeredSpur

def spurs =[spurGears[1],spurGears[0]].collect{
		it
		.rotz(bTeeth%2!=0?0:spurGears[8]/2)
		.roty(-90)
		.movez(distanceToShaft)
		.movex(distancetoGearFace)
}

CSG drivenA = spurs[1]
def spursB = spurs.collect{
	it.rotz(180)
}

CSG drivenB = spursB[1]


CSG outputGear = bevelGears.get(0)
CSG adrive = bevelGears.get(1).rotz(180).union(drivenA)
CSG bdrive = bevelGears.get(1).union(drivenB)
// return the CSG parts

double bearingLocation =  adrive.getMinX()-washerThickness
CSG innerBearing = args[0]
			.toZMin()
			.roty(-90)
			.toXMax()
			.movex( bearingLocation)
			.movez(bevelGears.get(3))

CSG bearing =args[0]
			.toZMin()
			.roty(-90)
			.movex(  encoderToEncoderDistance/2+gearThickness+washerThickness)
			.movez(bevelGears.get(3))
def bearingHeight = bearingThickness+washerThickness+gearThickness
def boltlenvalue= washerThickness*2+bearingThickness*2+gearBThickness+gearThickness+nutHeight
println "Bolt length ="+boltlenvalue
boltlen.setMM(actualBoltLength+gearThickness*2)
CSG bolt = Vitamins.get("capScrew",size)
			.roty(180)
			.toZMax()
			.movez(nut.getMaxZ() +(actualBoltLength-boltlenvalue)+partsGapBetweenGearsAndBrackets)
bearing=CSG.unionAll([bearing,
		bearing.rotz(180),
		args[0].hull().toZMax().movez(bearingHeight),
		args[0].hull().toZMax().movez(knuckelZ+gearThickness+1+partsGapBetweenGearsAndBrackets),
		innerBearing,
		innerBearing.rotz(180)
		])

CSG motor = 	args[2]
			.roty(-90)
			.movez(	distanceToMotor)
double washerTobearing = args[0].getTotalZ()+printerOffset.getMM()*2
def nutLocations =[
new Transform()
	.translate(0,0,knuckelZ+gearThickness+1+partsGapBetweenGearsAndBrackets)// X , y, z	
 ,
 new Transform()
		.translate( -bearingLocation+ washerTobearing,0,distanceToShaft)// X , y, z
		.rot( 0, -90, 0) // x,y,z
  ,
  new Transform()
		.translate(bearingLocation-washerTobearing,0,distanceToShaft)// X , y, z		
		.rot( 0, 90, 0) // x,y,z
		
]
def outerPlateLocationL = new Transform().roty(-90).movez(distanceToShaft).movex(-(encoderToEncoderDistance/2+gearThickness+washerThickness))
def outerPlateLocationR= new Transform().roty(90).movez(distanceToShaft).movex(encoderToEncoderDistance/2+gearThickness+washerThickness)

def washerLocations =[
new Transform().movez(gearThickness),
new Transform().roty(-90).movez(distanceToShaft).movex(bearingLocation),
outerPlateLocationR,
new Transform().roty(90).movez(distanceToShaft).movex(-bearingLocation),
outerPlateLocationL
]

def allWashers = washerLocations.collect{
	def val = washer.transformed(it)
	val.setManufacturing({ toMfg ->
		return toMfg.transformed(it.inverse())
	})
	return val
}
double upperNutsZ = distanceToShaft+args[0].getMaxX()+3.5
double lowerNutsZ = distanceToShaft-args[0].getMaxX()-3.5
def mountBoltHeight = gearThickness-1
def mountLocations =[
new Transform().translate(boltPattern,boltPattern,mountBoltHeight),
new Transform().translate(-boltPattern,-boltPattern,mountBoltHeight),
new Transform().translate(-boltPattern,boltPattern,mountBoltHeight),
new Transform().translate(boltPattern,-boltPattern,mountBoltHeight),
new Transform()
	.rotx(90)
	.movez(upperNutsZ)
	.movey(args[0].getMaxX()+knuckelThicknessAdd+2)
	.movex(nutHeight+args[0].getTotalZ()+2),
new Transform()
	.rotx(90)
	.movez(upperNutsZ)
	.movey(args[0].getMaxX()+knuckelThicknessAdd+2)
	.movex(-(nutHeight+args[0].getTotalZ()+2)),
new Transform()
	.rotx(90)
	.movez(lowerNutsZ)
	.movey(args[0].getMaxX()+knuckelThicknessAdd+2)
	.movex(nutHeight+args[0].getTotalZ()+2),
new Transform()
	.rotx(90)
	.movez(lowerNutsZ)
	.movey(args[0].getMaxX()+knuckelThicknessAdd+2)
	.movex(-(nutHeight+args[0].getTotalZ()+2))
	
]
println "Making Bolt keepaway"
def boltKeepaway = bolt.toolOffset(printerOffset.getMM())
def NutKW =CSG.unionAll(Extrude.revolve(nut.hull().makeKeepaway(printerOffset.getMM()),
		(double)0, // rotation center radius, if 0 it is a circle, larger is a donut. Note it can be negative too
		(double)65,// degrees through wich it should sweep
		(int)5))
		
def nuts = nutLocations.collect{
	NutKW.movez(-0.75).transformed(it)
}
def bolts = nutLocations.collect{
	boltKeepaway.transformed(it)
}
def mountNuts = mountLocations.collect{
	nutKeepaway.movez(-0.5).transformed(it)
}
nuts.addAll(mountLocations.collect{
	nut.transformed(it)
})
def mountBolts = mountLocations.collect{
	boltKeepaway.transformed(it)
}


def sweep = new Cylinder(encoderToEncoderDistance/2, // Radius at the bottom
                      		encoderToEncoderDistance/2, // Radius at the top
                      		nut.getTotalZ()+2, // Height
                      		(int)30 //resolution
                      		).toCSG()//convert to CSG to display      
			.difference(new Cylinder(args[0].getMaxY()+2, // Radius at the bottom
                      		args[0].getMaxY()+3, // Radius at the top
                      		nut.getTotalZ()+2, // Height
                      		(int)30 //resolution
                      		).toCSG())
                .movez(gearThickness)
               
outputGear=outputGear
			.union(allWashers.get(0))
			.difference(mountNuts)
			.difference(mountBolts)
			.union()
adrive=adrive.union(allWashers.get(1))
bdrive=bdrive.union(allWashers.get(3))
println "Making Washer keepaway"
def washerKW = allWashers.collect{it.hull().toolOffset(1)}
println "Making Knuckel"
def knuckel = new Cube(knuckelX,knuckelY,knuckelZ).toCSG()
				.toZMin()
				.movez(gearThickness+0.5+partsGapBetweenGearsAndBrackets)
				.difference(sweep)
				.difference(nuts)
				.difference(bolts)
				.difference(washerKW)
				.difference(bearing)
				.difference(mountBolts)
				.difference(mountNuts)
println "Slicing Knuckel"				
def bbox = knuckel.getBoundingBox()
			.toYMin()
def knuckelLeft = knuckel.intersect(bbox)
def knuckelRigth = knuckel.difference(bbox)


double distToGearEdge = encoderToEncoderDistance/2+gearThickness

def MotorLoacations = [
new Transform()
	.roty(-90).movez(shaftToMotor).rotx(motorAngleOffset).movez(distanceToShaft).movex(distToGearEdge),
new Transform()
	.roty(90).movez(shaftToMotor).rotx(motorAngleOffset).movez(distanceToShaft).movex(-distToGearEdge)
]
double boltMountHeight =adrive.getMaxZ()
double upperPlateBoltPattern  = boltPattern+7
double motorBrackerTHick = washerInset+args[0].getTotalZ()
def mountLocationsOuterUpper =[
new Transform().roty(-90).movex(outerBearingDistance/2).movey(upperPlateBoltPattern),
new Transform().roty(-90).movex(outerBearingDistance/2).movey(-upperPlateBoltPattern),
new Transform().roty(90).movex(-outerBearingDistance/2).movey(upperPlateBoltPattern),
new Transform().roty(90).movex(-outerBearingDistance/2).movey(-upperPlateBoltPattern)
].collect{
	it.movez(boltMountHeight+nut.getMaxZ()*2)
}
println "Placing gears"
def driveGearsFinal = MotorLoacations.collect{
	centeredSpur.difference (shaftBlank
	.toZMax()).transformed(it)
}
println "Placing Motors"
def allMotors = MotorLoacations.collect{
	motorBlank
	.toZMax()
	.rotx(180)
	.transformed(it)
}
def allShafts = MotorLoacations.collect{
	shaftBlank
	.toZMax()
	.transformed(it)
}
double plateTHick = nut.getMaxZ()*2
def upperMountLocations = mountLocations.subList(0, 4).collect{
	it.movez(boltMountHeight)
}
def upperNuts = upperMountLocations.collect{
	nut.roty(180).transformed(it)
}
def uppermountNuts = upperMountLocations.collect{
	nutKeepaway.movez(-1).roty(180).transformed(it)
}
println "Making Upper Screw keepaway"
def upperBOlt = Vitamins.get("capScrew",size)
			.movez(nut.getMaxZ()*2)
			.toolOffset(printerOffset.getMM())
boltlen.setMM(75)
println "Making Upper Screw keepaway 2"
def sideUpperBolt = Vitamins.get("capScrew",size)
			.toolOffset(printerOffset.getMM())
def uppermountBolts = upperMountLocations.collect{
	upperBOlt.transformed(it)
}

def upperSidemountBolts = mountLocationsOuterUpper.collect{
	sideUpperBolt.transformed(it)
}
double mountBrackerY = upperSidemountBolts.get(0).getMaxY()*2
double actualMotorThickness = motorBrackerTHick
println "Making upper bracket"
def bracket = new Cube( outerBearingDistance-(bearingThickness+washerInset)*2,
					mountBrackerY,
					plateTHick).toCSG()
			.toZMin()
			.movez(boltMountHeight+nut.getMaxZ())
			.difference(upperSidemountBolts)
			.difference(uppermountBolts)
			.difference(uppermountNuts)
println "Making lower brackets "
def boltLug = new Cube( actualMotorThickness,
					mountBrackerY,
					plateTHick).toCSG()
			.toZMin()
			.movez(boltMountHeight+nut.getMaxZ())
def motorHold = new Cube(motorBlank.getTotalX()+5,
					motorBlank.getTotalY()+5,
					actualMotorThickness).toCSG()
				.toZMin()
				.toYMin()
				.movey(-motorBlank.getMaxY()-2.5)
def bearingLug = new Cube(actualMotorThickness,
					args[0].getTotalY()+2.5,
					args[0].getTotalY()+2.5).toCSG()
			.movez(distanceToShaft)	
boltLug=boltLug.union(	bearingLug)					
def motorHoldL = motorHold.transformed(	MotorLoacations.get(0)).movex(printerOffset.getMM()).toXMin().movex(bracket.getMaxX())			
def motorHoldR = motorHold.transformed(	MotorLoacations.get(1)).movex(-printerOffset.getMM()).toXMax().movex(bracket.getMinX())
println "Hull lower bracket left"
def boltLugL = boltLug.toXMin().movex(bracket.getMaxX()).union(motorHoldL)	.hull()
println "Hull lower bracket right"
def boltLugR = boltLug.toXMax().movex(bracket.getMinX()).union(motorHoldR).hull()
println "Making lower brackets cutouts "
def motorBracketSets = [boltLugL,boltLugR].collect{
	it.difference(upperSidemountBolts)
	.difference(allMotors)
	.difference(bolts)
	.difference(washerKW)
	.difference(bearing)
}
def  releifHole= new Cylinder(bolt.getMaxX()+1,plateTHick).toCSG()
			.movez(bracket.getMinZ())
bracket=bracket.difference(releifHole)
double upperDistLinkLen =  (boltLugL.getMaxZ()-distanceToShaft)
def nearestFive(def num){
	def mod = (num%5.0)
	def offset = 5.0-mod
	if(mod<1)
		return [num-mod,num]
	return [num+offset,num]
}
println "Bottom to shaft "+ distanceToShaft
println "Shaft to top  "+ upperDistLinkLen
println "2x Top bolt length  "+ nearestFive(bracket.getTotalX()+actualMotorThickness*2+nut.getTotalZ())
println "1x Center Bolt length " +nearestFive(knuckelRigth.getMaxZ()+nut.getTotalZ())
println "4x Knuckel Bolt length " +nearestFive(knuckelRigth.getTotalY()*2+nut.getTotalZ())
println "8x Mount Bolt length " +nearestFive(outputGear.getTotalZ()+bracket.getTotalZ()+nut.getTotalZ())
println "2x Axil Bolt length " +nearestFive(outerBearingDistance/2-3)

outputGear.setName("outputGear")
adrive.setName("adrive")
	.setManufacturing({ toMfg ->
	return toMfg
			.roty(-90)
			.toXMin()
			.toYMin()
			.toZMin()
})	
bdrive.setName("bdrive")
	.setManufacturing({ toMfg ->
	return toMfg
			.roty(90)
			.toXMin()
			.toYMin()
			.toZMin()
})
knuckelLeft.setName("knuckelLeft")
	.setManufacturing({ toMfg ->
	return toMfg
			.rotx(90)
			.toXMin()
			.toYMin()
			.toZMin()
})
knuckelRigth.setName("knuckelRigth")
	.setManufacturing({ toMfg ->
	return toMfg
			.rotx(-90)
			.toXMin()
			.toYMin()
			.toZMin()
})
bracket.setName("bracket")
	.setManufacturing({ toMfg ->
	return toMfg.roty(180)
			.toXMin()
			.toYMin()
			.toZMin()
})
allWashers.get(0)
	.setName("washer-"+0)
	.setManufacturing({ toMfg ->
	return toMfg
			.toXMin()
			.toYMin()
			.toZMin()
})
for(int i=1;i<allWashers.size();i++){
	allWashers.get(i)
	.setName("washer-"+i)
	.setManufacturing({ toMfg ->
	return toMfg
			.roty(90)
			.toXMin()
			.toYMin()
			.toZMin()
})
}

driveGearsFinal.get(0)
	.setName("driveGearsFinal-"+0)
	.setManufacturing({ toMfg ->
	return toMfg
			.roty(90)
			.toXMin()
			.toYMin()
			.toZMin()
})
driveGearsFinal.get(1)
	.setName("driveGearsFinal-"+1)
	.setManufacturing({ toMfg ->
	return toMfg
			.roty(-90)
			.toXMin()
			.toYMin()
			.toZMin()
})
motorBracketSets.get(0)
	.setName("motorBracketSets-"+0)
	.setManufacturing({ toMfg ->
	return toMfg
			.roty(90)
			.toXMin()
			.toYMin()
			.toZMin()
})
motorBracketSets.get(1)
	.setName("motorBracketSets-"+1)
	.setManufacturing({ toMfg ->
	return toMfg
			.roty(-90)
			.toXMin()
			.toYMin()
			.toZMin()
})
def parts =[outputGear,adrive,bdrive,
//bearing,
//nuts,bolts,upperSidemountBolts,
knuckelLeft,knuckelRigth,
//upperNuts,
bracket,
allWashers.get(2),
allWashers.get(4),
]
//parts.addAll(allWashers)
parts.addAll(driveGearsFinal)
parts.addAll(motorBracketSets)
return parts
LengthParameter printerOffset 			= new LengthParameter("printerOffset",0.5,[1.2,0])
LengthParameter boltlen 			= new LengthParameter("Bolt Length",0.5,[1.2,0])
double pitch = 3
double pinRadius = ((3/16)*25.4+printerOffset.getMM())/2

double pinLength = (16)+ (printerOffset.getMM()*2)
double actualBoltLength = 30
double boltPattern = 10
String size ="M5"
HashMap<String, Object>  boltData = Vitamins.getConfiguration( "capScrew",size)
HashMap<String, Object>  nutData = Vitamins.getConfiguration( "lockNut",size)
CSG nut =Vitamins.get("lockNut",size)

CSG nutKeepaway =nut.hull().makeKeepaway(printerOffset.getMM())
double nutHeight = nut.getMaxZ()
println "Bolt"+boltData
println "nut"+nutData
println "Pin len ="+pinLength
if(args == null){
	args=[
		Vitamins.get("ballBearing","695zz"),
		new Cylinder(pinRadius,pinRadius,pinLength,(int)30).toCSG(), // steel reenforcmentPin
		new Cylinder(pinRadius,pinRadius,pinLength,(int)30).toCSG().movez(-pinLength/2) // steel reenforcmentPin
		
	]
}
double linkMaterialThickness = pinLength/2
double washerThickness = 1.5
double washerOd = 17
double washerId = 12.75
double bearingThickness = args[0].getMaxZ()
double gearThickness =(pinLength -(bearingThickness*3 -washerThickness*2))/2

double encoderToEncoderDistance =args[0].getMaxX()*3+
					gearThickness*2 -
					washerThickness*4 +
					args[0].getMaxZ()*2+
					bearingThickness*2+
					nutHeight*2

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
            [	  bTeeth-2,// Number of teeth gear a
	            0.75*(bTeeth-2),// Number of teeth gear b
	            gearThickness,// thickness of gear A
	            pitch,// gear pitch in arch length mm
	           0,
	           20
            ]
            )
//Print parameters returned by the script
println "Bevel gear radius A " + bevelGears.get(2)
println "Bevel gear radius B " + bevelGears.get(3)
println "Spur gear radius A " + spurGears.get(2)
println "Spur gear radius B " + spurGears.get(3)
double ratio = spurGears[2]/spurGears[3]*bevelGears[2]/bevelGears[3]
println "Total Wrist Ratio is " +ratio
encoderToEncoderDistance = bevelGears.get(2)*2
double gearBThickness =bevelGears.get(6)
double distanceToShaft =bevelGears.get(3)
double distancetoGearFace = bevelGears.get(2)
double distanceToMotor = bevelGears.get(3)+spurGears[2]+spurGears[3]



def spurs =[spurGears[1],spurGears[0]].collect{
		it.roty(-90)
		.movez(distanceToShaft)
		.movex(distancetoGearFace)
}
CSG driveA = spurs[0]
CSG drivenA = spurs[1]
def spursB = spurs.collect{
	it.rotz(180)
}
CSG driveB = spursB[0]
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
def boltlenvalue= washerThickness*3+bearingThickness*2+gearBThickness+gearThickness+nutHeight
println "Bolt length ="+boltlenvalue
boltlen.setMM(actualBoltLength)
CSG bolt = Vitamins.get("capScrew",size)
			.roty(180)
			.toZMax()
			.movez(nut.getMaxZ() +(actualBoltLength-boltlenvalue))
bearing=CSG.unionAll([bearing,
		bearing.rotz(180),
		args[0].toZMax().movez(bearingHeight),
		innerBearing,
		innerBearing.rotz(180)
		])
CSG motor = 	args[2]
			.roty(-90)
			.movez(	distanceToMotor)
def nutLocations =[
new Transform()
	.translate(0,0,bearingHeight)// X , y, z	
 ,
 new Transform()
 		
		.translate( -bearingLocation+ bearingThickness,0,distanceToShaft)// X , y, z
		.rot( 0, -90, 0) // x,y,z
  ,
  new Transform()
		.translate(bearingLocation-bearingThickness,0,distanceToShaft)// X , y, z		
		.rot( 0, 90, 0) // x,y,z
		
]
def mountBoltHeight = gearThickness-1
def mountLocations =[
new Transform().translate(boltPattern,boltPattern,mountBoltHeight),
new Transform().translate(-boltPattern,-boltPattern,mountBoltHeight),
new Transform().translate(-boltPattern,boltPattern,mountBoltHeight),
new Transform().translate(boltPattern,-boltPattern,mountBoltHeight)
]
def boltKeepaway = bolt.toolOffset(printerOffset.getMM())
def nuts = nutLocations.collect{
	nut.transformed(it)
}
def bolts = nutLocations.collect{
	boltKeepaway.transformed(it)
}
def mountNuts = mountLocations.collect{
	nutKeepaway.transformed(it)
}
nuts.addAll(mountLocations.collect{
	nut.transformed(it)
})
def mountBolts = mountLocations.collect{
	boltKeepaway.transformed(it)
}
def sweep = Extrude.revolve(mountNuts[0],
		(double)0, // rotation center radius, if 0 it is a circle, larger is a donut. Note it can be negative too
		(double)360,// degrees through wich it should sweep
		(int)20)//number of sweep increments
	
outputGear=outputGear
			.difference(mountNuts)
			.difference(mountBolts)

return [outputGear,adrive,bdrive,bearing,driveA,driveB,nuts,bolts]

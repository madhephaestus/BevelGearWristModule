LengthParameter printerOffset 			= new LengthParameter("printerOffset",0.5,[1.2,0])

double pinRadius = ((3/16)*25.4+printerOffset.getMM())/2

double pinLength = (2.5*25.4)+ (printerOffset.getMM()*2)
double linkMaterialThickness = pinLength/2
double washerThickness = 1.5
double washerOd = 17
double washerId = 12.75
double gearThickness =3

if(args == null){
	args=[
		Vitamins.get("ballBearing","R8-60355K505"),
		new Cylinder(pinRadius,pinRadius,pinLength,(int)30).toCSG().movez(-pinLength/2) // steel reenforcmentPin
		
	]
}
double encoderToEncoderDistance = pinLength-args[0].getMaxZ()*2
double pitch = 5
int aTeeth =    Math.PI*(encoderToEncoderDistance -washerThickness*2 )/pitch
int bTeeth =    Math.PI*(args[0].getMaxX()+washerThickness+gearThickness)*2/pitch

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
//Print parameters returned by the script
println "Bevel gear radius A " + bevelGears.get(2)
println "Bevel gear radius B " + bevelGears.get(3)
println "Bevel angle " + bevelGears.get(4)
println "Bevel tooth face length " + bevelGears.get(5)
CSG outputGear = bevelGears.get(0)
CSG adrive = bevelGears.get(1)
CSG bdrive = bevelGears.get(1).rotz(180)
// return the CSG parts
gears= [outputGear,adrive,bdrive]

CSG bearing =args[0]
			.toZMin()
			.roty(-90)
			.movex(  encoderToEncoderDistance/2)
			.movez(bevelGears.get(3))
bearing=bearing.union(bearing.rotz(180))
		.union(args[0].movez(gearThickness+washerThickness))
CSG pin  = args[1]
			.roty(-90)
			.movez(bevelGears.get(3))
			

return [gears,pin,bearing]

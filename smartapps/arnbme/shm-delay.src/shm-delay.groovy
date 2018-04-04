 /*
 *  Smart Home Entry and Exit Delay and Open Contact Monitor, Parent 
 *  Functions: 
 *		Acts as a container/controller for Child module
 * 
 *  Copyright 2017 Arn Burkhoff
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 * 
 *  Apr 04, 2018 v2.0.1  Fix issue with burned pin, move all documentation to community forum release thread,
 *							change global keypad selection from capability to device
 *  Mar 20, 2018 v2.0.0  add reverse mode fix, user defined modes, set Alarm State when mode changes 
 *  Mar 18, 2018 v2.0.0  add Panic option
 *	Mar 14, 2018 v2.0.0  add logic that executes a Routine for a pin 
 *	Mar 13, 2018 v2.0.0  add logic for weekday, time and dates just added to SHM Delay User 
 *  Mar 02, 2018 v2.0.0  add support for users and total keypad control
 *							Use mode vs alarmstatus to set Keypad mode lights, requires modefix be live
 *	Dec 31, 2017 v1.6.0  Add bool to allow Multiple Motion sensors in delay profile,
 *							without forcing existing users to update their profile data.
 *	Sep 23, 2017 v1.4.0  Document True Entry Delay and optional followed motion sensor in Delay Profile 
 *	Sep 23, 2017 v1.3.0  Add Global setting for option True Entry Delay, default off/false 
 * 	Sep 06, 2017 v1.2.0b add custom app remove button and text
 * 	Sep 02, 2017 v1.2.0a fix sorry there was an unexpected error due to having app name as modefixx from testing on
 *					one of the app connections
 * 	Sep 02, 2017 v1.2.0  repackage Modefix logic back into child ModeFix module where it belongs
 * 	Aug 30, 2017 v1.1.1  add global for using the upgraded Keypad module.
 * 	Aug 27, 2017 v1.1.0  Add child module SHM Delay ModeFix for Mode fixup profiles and adjust menus to reflect status
 * 	Aug 25, 2017 v1.1.0  SmartHome send stay mode when going into night mode. Force keypad to show
 *					night mode and have no entry delay. Add globalTrueNight for this option and globalFixMode 
 *	Aug 23, 2017 v1.0.7  Add police 911 and telephone numbers as links in notification messages
 *	Aug 20, 2017 v1.0.6a Change default global options: non-unique to false, create intrusion messages to true
 *					update documentation
 *	Aug 19, 2017 v1.0.6  Add global options allowing non unique simulated sensors, and alarm trigger messages
 *	Aug 17, 2017 v1.0.5  Revise documentation prior to release
 *	Aug 14, 2017 v1.0.4  Revise documentation for exit delay, split about page into about and installation pages
 *	Aug 14, 2017 v1.0.3  Revise initial setup testing app.getInstallationState() for COMPLETE vs childApps.size
 *					done in v1.0.1
 *	Aug 13, 2017 v1.0.2  Add documentation pages (Thanks to Stephan Hackett Button Controller)
 *	Aug 12, 2017 v1.0.1  Add warning on initial setup to install first (Thanks to E Thayer Lock Manager code) 
 *	Aug 11, 2017 v1.0.0  Create from example in developer documentation 
 *
 */

definition(
    name: "SHM Delay",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "(V2.0.1)Smart Home Monitor Exit/Entry Delays with optional Keypad support",
    category: "My Apps",
    iconUrl: "https://www.arnb.org/IMAGES/hourglass.png",
    iconX2Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    iconX3Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    singleInstance: true)

preferences {
    page(name: "main")
    page(name: "globalsPage", nextPage: "main")	
}

def main()
	{
	dynamicPage(name: "main", install: true, uninstall: true)
		{
		if (app.getInstallationState() == 'COMPLETE')	//note documentation shows as lower case, but returns upper
			{  
			def modeFixChild="Create"
			def children = getChildApps()
			children.each
				{ child ->
				def childLabel = child.getLabel()
				def appid=app.getId()
//				log.debug "child label ${childLabel} ${appid}"
				if (childLabel.matches("(.*)(?i)ModeFix(.*)"))	
					{
					modeFixChild="Update"
					}
				}
			def modeActive=" Inactive"
			if (globalFixMode || globalKeypadControl)
				{modeActive=" Active"}
			def fixtitle = modeFixChild + modeActive + " Mode Fix Settings"
			section 
				{
				app(name: "EntryDelayProfile", appName: "SHM Delay Child", namespace: "arnbme", title: "Create A New Delay Profile", multiple: true)
				}
			if (globalKeypadControl)
				{
				section 
					{
					app(name: "UserProfile", appName: "SHM Delay User", namespace: "arnbme", title: "Create A New User Profile", multiple: true)
					}
				}	
			section
    			{
  				href(name: 'toglobalsPage', page: 'globalsPage', title: 'Globals Settings')
				}	
			section
				{
			if (globalFixMode && modeFixChild == "Create")
				{
				app(name: "ModeFixProfile", appName: "SHM Delay ModeFix", namespace: "arnbme", title: "${fixtitle}", multiple: false)
				}	
			else
				{
				app(name: "ModeFixProfile", appName: "SHM Delay ModeFix", namespace: "arnbme", title: "${fixtitle}", multiple: false)
				}	
				}
			}
		else
			{
			section 
				{
				paragraph "Please read the documentation, review and set global settings, then complete the install by clicking 'Save' above. After the install completes, you may set Delay, User and Modefix profiles"
				}
			section
    			{
  				href(name: 'toglobalsPage', page: 'globalsPage', title: 'Globals Settings')
				}	
			}	
		section
			{
			href (url: "https://community.smartthings.com/t/beta-shm-delay-version-2-0/121800",
			title: "Smartapp Documentation",
			style: "external")
			}
		section
			{
			paragraph "SHM Delay Version 2.0.1 Beta"
			}
		remove("Uninstall SHM Delay","Warning!!","This will remove the ENTIRE SmartApp, including all profiles and settings.")
		}
	}

def globalsPage()
	{	
	dynamicPage(name: "globalsPage", title: "Global Settings")
		{
		section 
			{
			input "globalDisable", "bool", required: true, defaultValue: false,
				title: "Disable All Functions. Default: Off/False"
			input "globalKeypadControl", "bool", required: true, defaultValue: false, submitOnChange: true,
				title: "Activate Total Keypad Control. Default: Off/False"
			input "globalIntrusionMsg", "bool", required: false, defaultValue: true,
				title: "Issue intrusion message with name of triggering real sensor? Default: On/True."
			input (name: "global911", type:"enum", required: false, options: ["911","999","112",""],
				title: "Add 3 digit emergency call number on intrusion message?")
			input "globalPolice", "phone", required: false, 
				title: "Include this phone number as a link on the intrusion message? Separate multiple phone numbers with a semicolon(;)"
			input "globalMultipleMotion", "bool", required: true, defaultValue: false,
				title: "Allow Multiple Motion Sensors in Delay Profile. Default: Off/False" 
			input "globalFixMode", "bool", required: true, defaultValue: false,
				title: "Mode Fix: When AlarmState changes, fix Mode when invalid. Default: Off/False"
//			input "globalKeypad", "bool", required: true, defaultValue: false,
//				title: "The upgraded Keypad module is installed Default: Off/False"
			input "globalTrueNight", "bool", required: true, defaultValue: false, 
				title: "True Night Flag. When arming in Stay from a non keypad device, or Partial from an Iris keypad, and monitored sensor triggers:\nOn: Instant intrusion\nOff: Entry Delay"
			if (globalKeypadControl)
				{
				def actions = location.helloHome?.getPhrases()*.label
				actions?.sort()
				input "globalKeypadDevices", "device.CentraliteKeypad", required: true, multiple: true,
					title: "One or more Keypads used to arm and disarm SHM"
				input "globalKeypadExitDelay", "number", required: true, range: "0..90", defaultValue: 30,
					title: "True exit delay in seconds when arming in Away mode from any keypad. range 0-90, default:30"
				input "globalOff", "enum", options: actions, required: true, defaultValue: "I'm Back!",
					title: "Keypad Disarmed/OFF executes Routine. Default: I'm Back!"
				input "globalStay", "enum", options: actions, required: true, defaultValue: "Good Night!",
					title: "Keypad Stay/Partial executes Routine. Default: Good Night!"
				input "globalNight", "enum", options: actions, required: true, defaultValue: "Good Night!",
					title: "Keypad Night executes Routine. Default: Good Night!"
				input "globalAway", "enum", options: actions, required: true, defaultValue: "Goodbye!",
					title: "Keypad Away/On executes Routine. Default: Goodbye!"
				input "globalPanic", "bool", required: true, defaultValue: true,
					title: "Panic Key is Monitored. No Panic key? Set this flag on, add a User Profile, Pin Usage: Panic. Default: On/True"
//				input "globalBadpins", "number", required: true, range: "0..5", defaultValue: 1,
//					title: "Sound invalid pin code tone on keypad after how many invalid pin code entries. 0 = disabled, range: 1-5, default: 1"
//				input "globalBadpinsIntrusion", "number", required: true, range: "0..10", defaultValue: 4,
//					title: "(Future enhancement) Create intrusion alert after how many invalid pin code entries. 0 = disabled, range: 1-10, default: 4"
				}	
			input "globalSimUnique", "bool", required: false, defaultValue:false,
				title: "Simulated sensors must be unique? Default: Off/False allows using a single simulated sensor."
			input "globalTrueEntryDelay", "bool", required: true, defaultValue: false,
				title: "True Entry Delay: This is a last resort when adding motion sensors to delay profile does not stop Intrusion Alert. AlarmState Away and Stay with an entry delay time, ignore triggers from all other sensors when Monitored Contact Sensor opens. Default: Off/False"
			}
		}
	}	

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize()
	{
	if (globalKeypadControl && !globalDisable)
		{
		subscribe(globalKeypadDevices, 'codeEntered',  keypadCodeHandler)
		subscribe (location, "mode", keypadModeHandler)
		if (globalPanic)
			{
		    subscribe (globalKeypadDevices, "contact.open", keypadPanicHandler)
		    }
		}
	}	

//  --------------------------Keypad support added Mar 02, 2018 V2-------------------------------
/*				Basic location modes are Home, Night, Away. This can be very confusing
Xfinity			Default mode
Centralite	Iris		Location	Default			Triggers	Xfinity		Iris
Icon		Button		Mode 		AlarmStatus		Routine		Icon lit	key lit
(off)		Off			Home		off				I'm Home	(none)		Off??
Stay		Partial		Night		stay			GoodNight	Stay		Partial
Night					Night		stay			GoodNight	Stay		Partial, but night key should not occur	
Away		On			Away		away			GoodBye!	Away		Away


Xfinity			When Location Stay mode is defined and SHM Stay routine defined for Xfinity only
Centralite				Location	Default			Triggers	Xfinity		
Keypad					Mode 		AlarmStatus		Routine		Icon lit	
(off)					Home		off				I'm Home	(none)		
Stay					Stay		stay			Stay		Stay		
Night					Night		stay			GoodNight	Night			
Away					Away		away			GoodBye!	Away		
*/
def keypadCodeHandler(evt)
	{
//	User entered a code on a keypad	
	if (!globalKeypadControl || globalDisable)
		{return false}			//just in case
	def keypad = evt.getDevice();
//	log.debug "keypadCodeHandler called: $evt by device : ${keypad.displayName}"
	def codeEntered = evt.value as String				//the entered pin
	def modeEntered = evt.data as Integer				//the selected mode off(0), stay(1), night(2), away(3)
	if (modeEntered < 0 || modeEntered> 3)				//catch an unthinkable bad mode, this is catastrophic 
		{
		log.error "${app.label}: Unexpected arm mode ${modeEntered} sent by keypad!"
		keypad.sendInvalidKeycodeResponse()
		return false
		}
//	def currentarmMode = keypad.currentValue('armMode')
//	log.debug("Delayv2 codeentryhandler searching user apps for keypad ${keypad.displayName} ${evt.data} ${evt.value}")
	def userName=false;
	def badPin=true;
	def error_message=""

//	Try to find a matching pin in the pin child apps	
	def userApps = getChildApps()		//gets all completed child apps
	userApps.find 	
		{
		if (it.getName()=="SHM Delay User" && it.theuserpin == codeEntered)	
			{
//			log.debug ("found the pin ${it.getName()} ${it.theuserpin} ${it.theusername} ")
//			verify burn cycles
			if (it.themaxcycles > 0)						//check if pin is burned
				{
				def atomicUseId=it.getId()+'uses'			//build unique atomic id for uses
				if (atomicState."${atomicUseId}" < 0)		//initialize if never set
					{atomicState."${atomicUseId}" = 1}
				else	
		    		{atomicState."${atomicUseId}" = atomicState."${atomicUseId}" + 1}
		    	if (atomicState."${atomicUseId}" > it.themaxcycles)
		    		{
					log.debug "pin $codeEntered burned"
					error_message = keypad.displayName + " Burned pin entered for " + it.theusername
	    			}
	    		}	
			badPin=false
//			log.debug "matched pin ${it.theuserpin} $it.pinScheduled"

//			When pin is scheduled verify Dates, Weekday and Time Range	
			if (error_message=="" && it.pinScheduled)
				{
//				keep this code in sync with similar code in SHM Delay Users				
    			def df = new java.text.SimpleDateFormat("EEEE")	//formatter for current time    			
    			df.setTimeZone(location.timeZone)
    			def day = df.format(new Date())
    			def df2 = new java.text.SimpleDateFormat("yyyyMMdd")    			
    			df2.setTimeZone(location.timeZone)
				def nowymd = df2.format(new Date());		//	the yyyymmdd format for comparing and processing
				def dtbetween=true
				def num_dtstart
				def num_dtend
				if (it.pinStartDt > "")
					num_dtstart=it.dtEdit(it.pinStartDt)
				if (it.pinEndDt > "")
					num_dtend=it.dtEdit(it.pinEndDt)
//				log.debug "pin found with schedule $nowymd $num_dtstart $num_dtend"
//				verify the dates
				if (it.pinStartDt>"" && it.pinEndDt>"")
					{
					if (num_dtstart > nowymd || num_dtend < nowymd)
						error_message = keypad.displayName + " dates out of range with pin for " + it.theusername
					}
				else
				if (it.pinStartDt>"")
					{
					if (num_dtstart > nowymd)
						error_message = keypad.displayName + " start date error with pin for " + it.theusername
					}
				else
				if (it.pinEndDt>"")
					{
					if (num_dtend < nowymd)
						error_message = keypad.displayName + " end date expired with pin for " + it.theusername
					}

//				verify the weekdays
				if (error_message=="" && it.pinDays)
					{
					if (!it.pinDays.contains(day))
						error_message = keypad.displayName + " not valid on $day with pin for " + it.theusername
					}
					
//				verify the hours stored by system as 2018-03-13T11:30:00.000-0400
				if (error_message=="" && it.pinStartTime>"" && it.pinEndTime>"")
					{
   					def between = timeOfDayIsBetween(it.pinStartTime.substring(11,16), it.pinEndTime.substring(11,16), new Date(), location.timeZone)
					if (!between)
						error_message = keypad.displayName + " time out of range with pin for " + it.theusername
					}
				}
//			Verify pin usage
			if (error_message=="")
				{
				switch (it.thepinusage)
					{
					case 'User':
						userName=it.theusername	
						break
					case 'Disabled':
						error_message = keypad.displayName + " disabled pin entered for " + it.theusername
						break
					case 'Ignore':
						error_message = keypad.displayName + " ignored pin entered for " + it.theusername
						break
					case 'Routine':
						error_message = keypad.displayName + " executed routine " + it.thepinroutine + " with pin for " + it.theusername
						location.helloHome?.execute(it.thepinroutine)
						break
					case 'Panic':
						if (globalPanic)
							{
							error_message = keypad.displayName + " Panic entered with pin for " + it.theusername
							keypadPanicHandler(evt)					
//							panicContactOpen()	//unable to get this working use klunky method above
							}
						else	
							{
							error_message = keypad.displayName + " Panic entered but globalPanic flag disabled with pin for " + it.theusername
							}
						break
					case 'Piston':
						try {
							include 'asynchttp_v1'
							def params = [uri: it.thepinpiston]
//							def params = [uri: "https://www.google.com"]		//use to test
							asynchttp_v1.get('getResponseHandler', params)
							error_message = keypad.displayName + " Piston executed with pin for " + it.theusername
							}
						catch (e)
							{
							error_message = keypad.displayName + " Piston Failed with pin for " + it.theusername + " " + e
							}    					
						break
					default:
						userName=it.theusername	
						break
					}		
				}
			return true				//this ends the ***find*** loop, not the function
			}
		else
			{return false}			//this continues the ***find*** loop, does not end function
		}

//	Now done with find loop and editing the pin entered on the keypad  		

	if (error_message!="")									// put out any messages to notification log
		{
//		log.debug error_message
		sendNotificationEvent(error_message)
		}
		
//	Was pin not found
/*  in theory acknowledgeArmRequest(4) and sendInvalidKeycodeResponse() send same command
	but not working that way. Look at this when I get some time
*/	

	if (badPin)
		{
		keypad.acknowledgeArmRequest(4)				//always issue badpin very long beep
/*
**		Deprecated this logic on Mar 18, 2018 for better overall operation
		if (globalBadPins==1)
			{
			keypad.acknowledgeArmRequest(4)			//sounds a very long beep
			}
		else
			{
			if (atomicState.badpins < 0)			//initialize if never set
				{atomicState.badpins=0}
	    	atomicState.badpins = atomicState.badpins + 1
	    	if (atomicState.badpins >= globalBadpins)
	    		{
				keypad.acknowledgeArmRequest(4)		//sounds a very long beep
				atomicState.badpins = 0
    			}
			else
				keypad.sendInvalidKeycodeResponse()	//sounds a medium duration beep
    		}	
*/		return;
 		}

		
//	was this pin associated with a person
	if (!userName)									//if not a user pin, no further processing
		{
		keypad.sendInvalidKeycodeResponse()			//sounds a medium duration beep
		return
		}
		
	keypad.acknowledgeArmRequest(modeEntered) 		//keypad demands a followup light setting or all lights blink
	unschedule(execRoutine)		//Attempt to handle rearming/disarming during exit delay by unscheduling any pending away tasks 
//	atomicState.badpins=0		//reset badpin count
	def armModes=['Home','Stay','Night','Away']
	def message = keypad.displayName + " set mode to " + armModes[modeEntered] + " with pin for " + userName
	def aMap = [data: [codeEntered: codeEntered, armMode: armModes[modeEntered]]]
	if (modeEntered==3 && globalKeypadExitDelay > 0)	//in away mode check if exit delay is coded for keypad
			{
			keypad.setExitDelay(globalKeypadExitDelay)
			runIn(globalKeypadExitDelay, execRoutine, aMap)
			}
		else
			{execRoutine(aMap.data)}	
	sendNotificationEvent(message)
	}


def execRoutine(aMap) 
//	Execute default SmartHome Monitor routine, setting ST AlarmStatus and SHM Mode
	{
	def armMode = aMap.armMode
	def kMap = [mode: armMode, dtim: now()]	//save mode dtim any keypad armed/disarmed the system for use with
//											  not ideal prefer alarmtime but its before new alarm time is set
	def kMode=false							//new keypad light setting, waiting for mode to change is a bit slow
	def kbMap = [value: armMode, source: "keypad"]		
	if (armMode == 'Home')					
		{
		keypadLightHandler(kbMap)
		location.helloHome?.execute(globalOff)
		}
	else
	if (armMode == 'Stay')
		{
		keypadLightHandler(kbMap)
		location.helloHome?.execute(globalStay)
		}
	else
	if (armMode == 'Night')
		{
		keypadLightHandler(kbMap)
		location.helloHome?.execute(globalNight)
		}
	else
	if (armMode == 'Away')
		{
		keypadLightHandler(kbMap)
		location.helloHome?.execute(globalAway)
		} 
	atomicState.kMap=kMap					//SHM Delay Child DoorOpens and MotionSensor active functions
	}

def keypadModeHandler(evt)		//react to all SHM Mode changes
	{
	if (globalDisable)
		{return false}			//just in case
	def	theMode=evt.value		//Used to set the keypad button/icon lights		
	def theStatus=false			//used to set SHM Alarm State
	if (globalFixMode)			//if global fix mode use data allowing for user defined modes
		{
		def it=findChildAppByName('SHM Delay ModeFix')
		if (it?.getInstallationState()!='COMPLETE')
			log.debug "keypadModeHandler: Modefix is not fully installed, please adjust data then save"
		else
		if (it.offDefault == theMode)
			{
			theStatus='off'
			theMode='Home'
			}
		else
		if (it.awayDefault == theMode)
			{
			theStatus='away'
			theMode='Away'
			}
		else
		if (it.stayDefault == theMode)
			{
			theStatus='stay'
			theMode='Night'
			}
		else 
		if (it.offModes.contains(theMode))
			{
			theStatus='off'
			theMode='Home'
			}
		else 
		if (it.awayModes.contains(theMode))
			{
			theStatus='away'
			theMode='Away'
			}
		else 
		if (it.stayModes.contains(theMode))
			{
			theStatus='stay'
			theMode='Night'
			}
		log.debug "keypadModeHandler input: ${evt.value} lightMode: $theMode STalarm: $theStatus"
		}

	if (globalKeypadControl)			//when we are controlling keypads, set lights
		{
		if (theMode=='Home' || theMode=='Away' || theMode=='Night' || theMode=='Stay')
			{
			def kMap=atomicState.kMap
			def kDtim=now()
			def kMode
			log.debug "keypadModeHandler entered ${evt} ${evt.value} $themode ${kMap}"
			def setKeypadLights=true
			if (kMap)
				{
				kDtim=kMap.dtim
				kMode=kMap.mode
//				log.debug "keypadModeHandler ${evt} ${theMode} ${kMode}"
				if (theMode==kMode)
					{
					log.debug "Keypad lights are OK, no messages sent"
					setKeypadLights=false
					}
				}

//			Reset the keypad lights and mode, keep time when atomicState previously set, time is last time real keypad set mode		
			if (setKeypadLights)
				{
				kMap = [mode: theMode, dtim: kDtim]			//save mode dtim any keypad armed/disarmed the system for use with
				atomicState.kMap=kMap						//SHM Delay Child DoorOpens and MotionSensor active functions
				log.debug "keypadModeHandler issuing keypadlightHandler ${evt} ${evt.value}"
				keypadLightHandler(evt)
				}
			}
		else
			{
			log.debug "keypadModeHandler mode $theMode cannot be used to set the keypad lights"
			}
		}	
		
//	When SHM alarm state does not match the requested SHM alarm state, change it
	if (theStatus)
		{
		def alarm = location.currentState("alarmSystemStatus")	//get ST alarm status
		def alarmstatus = alarm.value
		if (alarmstatus != theStatus)
			setSHM(theStatus)
		}	
	}

def keypadLightHandler(evt)						//set the Keypad lights
	{
	def	theMode=evt.value						//This should be a valid SHM Mode
	log.debug "keypadLightHandler entered ${evt} ${theMode} source: ${evt.source}"
	globalKeypadDevices.each
		{ keypad ->
		if (theMode == 'Home')					//Alarm is off
			{keypad.setDisarmed()}
		else
		if (theMode == 'Stay')
			{
			if (evt.source !="keypad" && globalTrueNight && keypad?.getModelName()=="3400" && keypad?.getManufacturerName()=="CentraLite")
				{keypad.setArmedNight()}
			else	
				{keypad.setArmedStay()}				//lights Partial light on Iris
			}
		else
		if (theMode == 'Night')					//Iris has no Night light set Partial on	
			{
			if (keypad?.getModelName()=="3400" && keypad?.getManufacturerName()=="CentraLite")
				{keypad.setArmedNight()}
			else
				{keypad.setArmedStay()}
			}	
		else
		if (theMode == 'Away')					//lights ON light on Iris
			{keypad.setArmedAway()}
		}
	}	

def keypadPanicHandler(evt)
	{
	if (!globalKeypadControl || globalDisable || !globalPanic)
		{return false}			//just in case
	def alarm = location.currentState("alarmSystemStatus")	//get ST alarm status
	def alarmstatus = alarm.value
	def keypad=evt.getDevice()		//set the keypad name
	def panic_map=[data:[cycles:5, keypad: keypad.name]]
	log.debug "the initial panic map ${panic_map} ${keypad.name}" 
	if (alarmstatus == "off")
		{
//		location.helloHome?.execute(globalAway)	//set alarm on deprecated Mar 20, 2018
		setSHM('away')							//set alarm on in the fastest possible way I know
		runIn(1, keypadPanicExecute,panic_map)
		}
	else
		{
		keypadPanicExecute(panic_map.data)		//Panic routine only uses the device name, should be ok
		}
	}	

def keypadPanicExecute(panic_map)						//Panic mode requested
/*	When system is armed: Open simulated sensor
**	When system is not armed: Wait for it to arm, open simulated sensor
**	Limit time to 5 cycles around 9 seconds of waiting maximum
*/		
	{
	def alarm = location.currentState("alarmSystemStatus")	//get ST alarm status
	def alarmstatus = alarm.value
	if (alarmstatus == "off")
		{
		log.debug "keypadPanicExecute entered $panic_map"
		if (panic_map.cycles > 1)
			{
			def cycles=panic_map.cycles-1
			def keypad=panic_map.keypad
			def newpanic_map=[data:[cycles: cycles, keypad: keypad]]
			runIn(2, keypadPanicExecute,newpanic_map)
			return false
			}
		}

//	prepare panic message, issued later		
	def message = "PANIC issued by $panic_map.keypad "
	if (global911 > ""  || globalPolice)
		{
		def msg_emergency
		if (global911 > "")
			{
			msg_emergency= ", call Police at ${global911}"
			}
		if (globalPolice)
			{
			if (msg_emergency==null)
				{
				msg_emergency= ", call Police at ${globalPolice}"
				}
			else
				{
				msg_emergency+= " or ${globalPolice}"
				}
			}
		message+=msg_emergency
		}
	else
		{
		message+=" by (SHM Delay App)"
		}
		
//	find a delay profile for use with panic
	log.debug "keypadPanicExecute searching for Delay profile"

	def childApps = getChildApps()		//gets all completed child apps
	def delayApp  = false	
	childApps.find 						//change from each to find to speed up the search
		{
		if (!delayApp && it.getName()=="SHM Delay Child")	
			{
			log.debug "keypadPanicExecute found Delay profile"
			delayApp=true		
			if (alarmstatus == "off")
				{
				message+=" System did not arm in 10 seconds, unable to issue Panic intrusion"
				it.doNotifications(message)		//issue messages as per child profile
				}
			else	
				{
				it.doNotifications(message)		//issue messages as per child profile
				it.thesimcontact.close()		//trigger an intrusion		
				it.thesimcontact.open()
				it.thesimcontact.close([delay: 4000])
				}
			return true							//this ends the **find** loop does not return to system
			}
		else	
			{return false}						//this continues the **find** loop does not return
		}
	if (!delayApp)
		{
		message +=' Unable to create instrusion, no delay profile found'
		sendNotificationEvent(message)				//log to notification we are toast
		}
	}

//	Directly set the SHM alarm status input must be off, away or stay	
def setSHM(state)
	{
	if (state=='off'|state=='away'||state=='stay')
		{
		def event = [name:"alarmSystemStatus", value: state, 
    		displayed: true, description: "System Status is ${state}"]
    	sendLocationEvent(event)
    	}
    }


/*
atempted to use this to trigger panic but it does not fire the subscribed event
and may create chaos when multiple keypad devices are defined
def panicContactOpen() {
	log.debug "Enter panicContactOpen $globalKeypadDevices"
    sendEvent(name: "contact", value: "open", displayed: true, isStateChange: true, Device: globalKeypadDevices)
    runIn(3, "panicContactClose")
}

def panicContactClose()
	{
	log.debug "Enter panicContactClose"
	sendEvent(name: "contact", value: "closed", displayed: true, isStateChange: true, Device: globalKeypadDevices)
	}
*/

//	Process response from async execution of WebCore Piston
def getResponseHandler(response, data)
	{
    if(response.getStatus() != 200)
    	sendNotificationEvent("SHM Delay Piston HTTP Error = ${response.getStatus()}")
	}	
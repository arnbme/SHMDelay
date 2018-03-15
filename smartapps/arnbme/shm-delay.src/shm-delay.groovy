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
    description: "(V2.0.0)Smart Home Monitor Exit/Entry Delays with optional Keypad support",
    category: "My Apps",
    iconUrl: "https://www.arnb.org/IMAGES/hourglass.png",
    iconX2Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    iconX3Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    singleInstance: true)

preferences {
    page(name: "main")
    page(name: "aboutPage", nextPage: "installPage")
    page(name: "installPage", nextPage: "delayPage")
    page(name: "delayPage", nextPage: "createSimPage")
    page(name: "createSimPage", nextPage: "main")
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
				paragraph "Please read the documentation, review and set global settings, then complete the install by clicking 'Done' above before creating child profiles"
				}
			section
    			{
  				href(name: 'toglobalsPage', page: 'globalsPage', title: 'Globals Settings')
				}	
			}	
		section
			{
			href(name: "href",
			title: "Introduction",
			required: false,
			page: "aboutPage")
			}
		section
			{
			href(name: "href",
			title: "Installation and Prerequisites",
			required: false,
			page: "installPage")
			}
		section
			{
			href(name: "href",
			title: "How to create a Delay Profile",
			required: false,
			page: "delayPage")
			}
		section
			{
			href(name: "href",
			title: "How to create a simulated contact sensor",
			required: false,
			page: "createSimPage")
			}
		section
			{
			paragraph "SHM Delay Version 2.0.0 Beta"
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
			if (!globalKeypadControl)
				{
				input "globalTrueNight", "bool", required: true, defaultValue: false,
					title: "True Night Mode: In AlarmState Armed (Stay/Night) trigger immediate intrusion, no entry delay. Default: Off/False"
				}
			input "globalFixMode", "bool", required: true, defaultValue: false,
				title: "Mode Fix: When AlarmState changes, fix Mode when invalid. Default: Off/False"
			if (!globalKeypadControl)
				{	
				input "globalKeypad", "bool", required: true, defaultValue: false,
					title: "The upgraded Keypad module is installed Default: Off/False"
				}
			if (globalKeypadControl)
				{
				def actions = location.helloHome?.getPhrases()*.label
				actions?.sort()
				input "globalKeypadDevices", "capability.button", required: true, multiple: true,
					title: "One or more Keypads used to arm and disarm SHM"
				input "globalKeypadExitDelay", "number", required: true, range: "0..90", defaultValue: 30,
					title: "True exit delay in seconds when arming in Away mode from any keypad. range 0-90, default:30"
				input "globalOff", "enum", options: actions, required: true, defaultValue: "I'm Back!",
					title: "Keypad Disarmed/OFF executes Routine. Default: I'm Back!"
				input "globalStay", "enum", options: actions, required: true, defaultValue: "Good Night!",
					title: "Keypad Stay/Partial executes Routine. Default: Good Night!"
				input "globalNight", "enum", options: actions, required: true, defaultValue: "Good Night!",
					title: "Keypad Night (Xfinity/Centralite only) executes Routine. Default: Good Night!"
				input "globalAway", "enum", options: actions, required: true, defaultValue: "Goodbye!",
					title: "Keypad Away/On executes Routine. Default: Goodbye!"
				input "globalBadpins", "number", required: true, range: "0..5", defaultValue: 1,
					title: "Sound invalid pin code tone on keypad after how many invalid pin code entries. 0 = disabled, range: 1-5, default: 1"
				input "globalBadpinsIntrusion", "number", required: true, range: "0..10", defaultValue: 4,
					title: "(Future enhancement) Create intrusion alert after how many invalid pin code entries. 0 = disabled, range: 1-10, default: 4"
				}	
			input "globalSimUnique", "bool", required: false, defaultValue:false,
				title: "Simulated sensors must be unique? Default: Off/False allows using a single simulated sensor."
			input "globalTrueEntryDelay", "bool", required: true, defaultValue: false,
				title: "True Entry Delay: This is a last resort when adding motion sensors to delay profile does not stop Intrusion Alert. AlarmState Away and Stay with an entry delay time, ignore triggers from all other sensors when Monitored Contact Sensor opens. Default: Off/False"
			}
		}
	}	
def aboutPage()
	{
	dynamicPage(name: "aboutPage", title: "Introduction")
		{
		section 
			{
			paragraph "This smartapp simulates the Entry and Exit Delay parameters currently missing in SmartHome, "+
			"giving you some time on entry to disarm the system using any method you choose prior to triggering an intrusion alert, "+
			"and does not trigger an intrusion when a monitored contact sensor opens during the exit delay time. ***"+
			"The delay operates only on monitored sensors, not the entire SmartHome application.***\n\n"+
			"It also replaces the 'Open contact when system is armed' message, lost when removing the contact sensor from SmartHome.\n\n"+
 			"***Please Note***: SmartHome is fully armed during operation of this SmartApp. Tripping a 'non-delayed sensor'"+ 
 			" immediately triggers a SmartHome intrusion alert. This means a motion sensor that 'sees' a door"+
 			" with a delayed sensor, or 'sees' a disarming device such as a keypad, will trigger an intrusion alert while you are disarming the system. The motion sensor, or disarming device must be relocated to successfully use this app." 
			
			}
		}
	}

def installPage()
	{
	dynamicPage(name: "installPage", title: "Installation and Prerequisites")
		{
		section
			{
			paragraph "Installation:\n"+
			"Please complete the install by clicking 'Done' on the main page before creating your first delay profile.\n\n"+
			"The name of the simulated sensor appears in SmartHome intrusion messages, but many users prefer not"+ 
			" to clutter their 'My Home' device list with simulated sensors. The default is non-unique (one) simulated sensor, the choice is yours. "+
			"Should you prefer unique simulated sensors, expand the Global "+
			"Application Settings, then set:\n"+
			" 1. Simulated sensors must be unique? Default Off/False\n\n"+
			" 2. Send intrusion notification with name of triggering real sensor?  Default On/True\n\n"+
			" 3. Set True EntryDelay: A last resort when intusions cannot be stopped by setting an optional followed motion sensor in the Delay Profile. Default: Off/False\n\n"+
			" 4. Set TrueNight flag on to trigger instant intrusion in Stay alarm state. Default: Off/False\n\n"+
			" 5. Set mode flag if you want Mode to be synced with AlarmState Default: Off/False\n"+
			" When set on: install app then create the ModeFix profile\n\n"+
			" 6. When upgraded Keypad module is installed this must be set on. Default: Off/False\n\n"+
			" 7. Select a 3 digit emergency number for inclusion on intrusion message (Optional)\n\n"+			
			" 8. Add emergency phone number for inclusion on intrusion message (Optional)\n"+
			"  Enter telephone number. Separate multiple numbers with a semicolon(;)\n\n"+
			"Prerequisites:\n"+
			" 1. When using non-unique simulated sensors: create one simulated contact sensor in the IDE.\n"+
			"    When using unique simulated contact sensors: Create a simulated contact sensor for each monitored real sensor in the IDE.\n"+
			"The name of the simulated sensor device is used by SmartHome on intrusion alert messages.\n\n"+
			" 2. Remove the real contact sensor from SmartHome security monitoring. "+
			"This app monitors this device and includes a replacement for the SmartHome 'system armed with open contact' monitor.\n\n"+
			" 3. Add each simulated contact sensor to SmartHome's security monitoring. "+
			"This app 'opens' the paired simulated sensor device, creating a SmartHome intrusion alert."
			}
		}
	}

def delayPage()
	{
	dynamicPage(name: "delayPage", title: "Creating a Delay Profile")
		{
		section
			{
			paragraph "Define a profile for each real contact sensor removed from SmartHome's monitoring, "+
			"pairing it with a simulated contact sensor.\n"+ 
			"Start by tapping 'Create A New Delay Profile'\n"+
			"  1. select a real contact sensor\n"+
			"  2. select a simulated contact device\n"+
			"  3. (Optional) when a motion sensor triggers an intrusion during an entry delay: select motion sensor device, "+
			"then remove it from SmartHome Armed (Away) Motion sensors\n"+
			"  4. Profile name is internally set to Profile: (real contact sensor name) It may be modified\n"+
			"  5. Tap 'Next' on top of page\n\n"+
			"The 'Entry and Exit Data' page displays\n"+
			"  1. set the entry delay time in seconds from 0 to 60. Default:30\n"+
			" (zero is allowed enabling this app to be used only as an exit delay)\n"+
			"  2. For away mode set the exit delay time in seconds from 0 to 60. Default:30\n"+
			" (When using the Keypad exit delay in E Thayer's lock-manager, or any other exit delay method, set exit delay to 0)\n"+
			" (This app's exit delay is not active on Stay or Night mode)\n"+
			"  3. (Optional) set keypads where to sound the entrydelay tones\n"+
			"  4. (Optional) set sirens where beep should be issued\n"+
			"  When a siren does not support the beep command, the app issues the On command, followed in 250 millseconds by an Off command.\n"+ 
			"  5. Tap 'Next' on top of page\n\n"+
			"The 'Open Door Monitor and Notifications' page displays\n"+
			"  Open Door Message Settings\n"+ 
			"  1. set the maximum number of open door warning messages. Default: 2, Minimum: 1\n"+
			"  2. set the number of minutes between open door messages from 1 to 15. Default: 1\n\n"+
			"  Notifcation Settings for Open Door and optional Intrusion Alert messages\n"+ 	
			"  3. should the message show in the notifications log. Default: true\n"+
			"  4. should the message be issued as an application push notification. Default: true\n"+
			"  5. should the message be sent by text (SMS) message. Default: false\n"+
			"  Enter telephone number. Separate multiple numbers with a semicolon(;)\n"+
			"  6. Tap 'Done' on top of page\n\n"
			}
		}
	}

def createSimPage()
	{
	dynamicPage(name: "createSimPage", title: "How to Create A Simulated Contact Sensor")
		{
		section 
			{
			href title: "1. Go to graph.api.smartthings.com", style: "external", 
				url: "https://graph.api.smartthings.com"
			paragraph " 2. Login\n"+
			" 3. Click 'My Locations'\n"+
			" 4. Under 'Name' click on the named location\n"+
			" (The URL changes to the server name servicing this location. Smarthings calls this a shard)\n"+
			" 5. On top menu: Click 'My Devices'\n"+ 
			" 6. Click the 'New Device' button\n"+
			" 7. Name: this can be whatever you want. Suggest adding 'SIM ' to the beginning of the real device's name. For example: SIM Front Door Sensor.\n"+ 
			" 8. Label: this is optional and can be whatever you want.\n"+ 
			" 9. Device Network Id: This can be anything you want, but it cannot duplicate other device ID's. For example SIM01.\n"+
			"10. Zigbee Id: leave blank\n"+ 
			"11. Type: from the dropdown select 'Simulated Contact Sensor'\n"+
			"12. Version: select 'Published'\n"+
			"13. Location: from the dropdown select your location name\n"+ 
			"14. Hub: from the dropdown select your hub name.\n"+ 
			"15. Group: you won't be able to select when creating, but these are Groups you may have created in the Things page in the SmartThings app.\n"+ 
			"16. Click Create" 
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
//		log.debug ("subscribing to keypad code entry events for ${globalKeypadDevices}" )
		subscribe(globalKeypadDevices, 'codeEntered',  keypadCodeHandler)
//		log.debug "sucscribing to location mode handler"
		subscribe (location, "mode", keypadModeHandler)
//		subscribe(location, "alarmSystemStatus", alarmStatusHandler)
		}
/*	def armwk='away'		//this is all stuff used for testing
	def kMap = [mode: armwk, dtim: now()]
	atomicState.kMap=kMap					//save time any keypad armed/disarmed the system
	def kMapin=atomicState.kMap
	def kSecs = Math.round(kMapin.dtim / 1000)
	log.debug "Keypad last mode test ${kMap} ${kMapin} ${kSecs} ${kMapin.mode} $kMapin.dtim}"
	def wdlc="off"
	log.debug "${wdlc.capitalize()}"
	def theChild = findChildAppByName("SHM Delay ModeFix")
	log.debug "offdefault is ${theChild.offDefault}"
	log.debug "${location}"
	log.debug "${location.helloHome}"
	def childApps = getChildApps()		//gets all completed child apps
	log.debug "there are ${childApps.size()} child smartapps"
	childApps.each {
	  	log.debug "child app: ${it.getLabel()} ${it.getName()} ${it.getId()}"

		if (it.getName()=="SHM Delay User")	
			{
	  		log.debug "User: ${it.theuserpin} ${it.theusername} **${it.themaxcycles}**"
			if (it.themaxcycles > 0)					//check if pin is burned
				{
				def atomicUseId=it.getId()+'uses'
				log.debug "${atomicUseId}"
				if (atomicState."${atomicUseId}" < 0)		//initialize if never set
					atomicState."${atomicUseId}" = 3
				def uses=atomicState."${atomicUseId}"
				log.debug "uses ${uses}"
				if (atomicState."${atomicUseId}" == 0)
					log.debug "it is zero"
				}	
			}
		}
*/	}	

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
					keypad.sendInvalidKeycodeResponse()
					return false
	    			}
	    		}	
			keypad.acknowledgeArmRequest(modeEntered)
			badPin=false
//			log.debug "matched pin ${it.theuserpin} $it.pinScheduled"

//			When pin is scheduled verify Dates, Weekday and Time Range	
			if (it.pinScheduled)
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
						badPin=true
						error_message = keypad.displayName + " disabled pin entered for " + it.theusername
						break
					case 'Ignore':
						break
					case 'Routine':
						error_message = keypad.displayName + " executed routine " + it.thepinroutine + " with pin for " + it.theusername
						location.helloHome?.execute(it.thepinroutine)
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

//	Now done with editing the pin entered on the keypad		

//	Was pin not found
	if (badPin)
		{
		if (globalBadPins==1)
			{
			keypad.sendInvalidKeycodeResponse()
			}
		else
			{
			if (atomicState.badpins < 0)		//initialize if never set
				{atomicState.badpins=0}
	    	atomicState.badpins = atomicState.badpins + 1
	    	if (atomicState.badpins >= globalBadpins)
	    		{
				keypad.sendInvalidKeycodeResponse()
				atomicState.badpins = 0
    			}
    		}	
		return;
  		}

//	pin found but it has errors or created a message  		
	if (error_message!="")
		{
		sendNotificationEvent(error_message)
		return
		}
		
//	was this pin associated with a person
	if (!userName)				//if not a user pin, no further processing
		return

	unschedule(execRoutine)		//Attempt to handle rearming/disarming during exit delay by unscheduling any pending away tasks 
	atomicState.badpins=0		//reset badpin count
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
	def kbMap = [value: armMode]		
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

def keypadModeHandler(evt)						//react to all SHM Mode changes, attempt to avoid keypad trafic
	{
	if (!globalKeypadControl || globalDisable)
		{return false}			//just in case
	def	theMode=evt.value		
	def kMap=atomicState.kMap
	def kDtim=now()
	def kMode
	if (kMap)
		{
		kDtim=kMap.dtim
		kMode=kMap.mode
//		log.debug "keypadModeHandler ${evt} ${theMode} ${kMode}"
		if (theMode==kMode)
			{
//			log.debug "Keypad lights are OK, no messages sent"
			return false
			}
		}
//	Reset the keyboard mode, keep time when atomicState previously set, time is last time real keyboard set mode		
	kMap = [mode: theMode, dtim: kDtim]			//save mode dtim any keypad armed/disarmed the system for use with
	atomicState.kMap=kMap						//SHM Delay Child DoorOpens and MotionSensor active functions
	keypadLightHandler(evt)
	}
	
	

def keypadLightHandler(evt)						//set the Keypad lights
	{
	def	theMode=evt.value						//This should be a valid SHM Mode
//	log.debug "keypadLightHandler ${evt} ${evt.value}"
	globalKeypadDevices.each
		{ keypad ->
		if (theMode == 'Home')					//Alarm is off
			{keypad.setDisarmed()}
		else
		if (theMode == 'Stay')
			{keypad.setArmedStay()}				//lights Partial light on Iris
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

//	this routine is not used
def alarmStatusHandler(event) 					//here just incase need to reuse currently disabled, no subscription								
	{
	return false;	
	if (globalKeypadControl || globalDisable)
		{return false}			//just in case
//	Alarm status was changed by something, make all keypads follow
//	add some code test keypad status: if already properly set before issuing the command skip setting	
	globalKeypadDevices.each
		{ keypad ->
		if (event.value == 'off')
			{keypad.setDisarmed()}
		else
		if (event.value == 'away')
			{keypad.setArmedAway()}
		else
		if (event.value == 'stay')
			{
			def theMode=location.currentMode;
			if (theMode=="Night" && keypad?.getModelName()=="3400" && keypad?.getManufacturerName()=="CentraLite")
				{keypad.setArmedNight()}
			else
				{keypad.setArmedStay()}
			}
		}
	}

//	Process response from async execution of WebCore Piston
def getResponseHandler(response, data)
	{
    if(response.getStatus() != 200)
    	sendNotificationEvent("SHM Delay Piston HTTP Error = ${response.getStatus()}")
	}	
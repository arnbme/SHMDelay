/**
 *  Smart Home Delay and Open Contact Monitor Child
 *	Functions: 
 *		Simulate contact entry delay missing from SmartHome.					
 *		Since contact is no longer monitored by SmartHome, monitor it for "0pen" status when system is armed
 *
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
 * 	Aug 16, 2017 v1.0.6  add logic check if sensors for unique usage. Stop on real sensor, Warn on simulated
 *	Aug 16, 2017 v1.0.5  add verification editing on sensors and illogical conditions
 *	Aug 15, 2017 v1.0.4  fill Label with real sensor name
 *	Aug 14, 2017 v1.0.3  add exit delay time and logic: 
 *					When away mode do not react to contact opens less than exit delay time
 *	Aug 12, 2017 v1.0.2  add log to notifications, fix push and sms not to log, add multiple SMS logic
 *	Aug 12, 2017 v1.0.1  Allow profile to be named by user with Label parameter on pageOne
 *	Aug 12, 2017 v1.0.0  Combine Smart Delay and Door Monitor into this single child SmartApp
 *
 */
definition(
    name: "SHM Delay Child",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "Simulate missing SmartHome entry and exit delay parameters, Child module",
    category: "My Apps",
    parent: "arnbme:SHM Delay",
    iconUrl: "https://www.arnb.org/IMAGES/hourglass.png",
    iconX2Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    iconX3Url: "https://www.arnb.org/IMAGES/hourglass@2x.png")

preferences {
    page(name: "pageOne", nextPage: "pageOneVerify")
    page(name: "pageOneVerify")
    page(name: "pageTwo", nextPage: "pageTwoVerify")
    page(name: "pageTwoVerify")
    page(name: "pageThree", nextPage: "pageThreeVerify")
}


def pageOne(error_data)
	{
	dynamicPage(name: "pageOne", title: "The Contact Sensors", uninstall: true)
		{
		section
			{
			if (error_data instanceof String )
				{
				paragraph error_data
				}
			input "thecontact", "capability.contactSensor", required: true, 
				title: "Real Contact Sensor (Remove from SmartHome Monitoring)", submitOnChange: true
			}
		section
			{	
			input "thesimcontact", "capability.contactSensor", required: true,
				title: "Simulated Contact Sensor (Must Monitor in SmartHome)"
			}
		if (thecontact)
			{
			section([mobileOnly:true]) 
				{
				label title: "Profile name", defaultValue: "Profile: ${thecontact.displayName}", required: false
				}

			}	
		else	
			{
			section([mobileOnly:true]) 
				{
				label title: "Profile name", required: false
				}
			}	
		}
	}


/*	Cant find a way to get the actual device type, so test manufacturer and model for null */
/*    When a method is found test for word simulated	*/
//				def str=error_data.toString()
//				paragraph str
def pageOneVerify() 				//edit page one info, go to pageTwo when valid
	{
	def error_data
	def pageTwoWarning
	if (thecontact)
		{
		if (thecontact.getManufacturerName() == null && thecontact.getModelName()==null)
			{
			error_data="The 'Real Contact Sensor' is simulated. Please select a differant real contact sensor or tap 'Remove'"
/*			error_data="'${thecontact.displayName}' is simulated. Please select a differant real contact sensor or tap 'Remove'"
				for some reason the prior line is not seen as a string
*/			}
		else
		if (!iscontactUnique())			
			{
			error_data="The 'Real Contact Sensor' is already in use. Please select a differant real contact sensor or tap 'Remove'"
			}
		}	

	if (thesimcontact)
		{
		if (thesimcontact.getManufacturerName() == null && thesimcontact.getModelName()==null)
			{
			if (!issimcontactUnique())
				{
				if (error_data!=null)
					{
					error_data+="\n\nWarning: 'Simulated Contact Sensor' already in use"
					}
				else
					{
					pageTwoWarning="Warning: 'Simulated Contact Sensor' already in use. Tap 'Back' to change device"
					}
				}	
			}	
		else
			{
			def msg="The 'Simulated Contact Sensor' is real. Please select a differant simulated contact sensor or tap 'Remove'"
			if (error_data!=null)
				{
				error_data+="\n\n"+msg
				}
			else
				{
				error_data=msg
				}
			}
		}	
	if (error_data!=null)
		{
		pageOne(error_data)
		}
	else
//	if (pageTwoWarning == null)	
//		{
//		pageTwo()
//		}
//	else
		{
		pageTwo(pageTwoWarning)
		}
	}	

def iscontactUnique()
	{
	def unique = true
	def children = parent?.getChildApps()
//  	log.debug "there are ${children.size()} apps"
//	log.debug "this contact id: ${thecontact.getId()}"
//	log.debug "app install: ${app.getInstallationState()}"
//	log.debug "app id: ${app?.getId()}"
//	def myState = app.currentState()
//	log.debug "current app id: ${myState}"	
//	log.debug current app Id "${myState.getId()}"
	children.each
		{ child ->
//		log.debug "child app id: ${child.getId()}"	
//		log.debug "child contact Id: ${child.thecontact.getId()}"	
		if (child.thecontact.getId() == thecontact.getId() &&
		    child.getId() != app.getId())
			{
			unique=false
			}
		}
	return unique
	}

def issimcontactUnique()
	{
	def unique = true
	def children = parent?.getChildApps()
	children.each
		{ child ->
		if (child.thesimcontact.getId() == thesimcontact.getId() &&
		    child.getId() != app.getId())
			{
			unique=false
			}
		}
	return unique
	}

/*  cant make this work in java
def isUnique(contact)
	{
	def unique = true
	def children = parent?.getChildApps()
	children.each
		{ child ->
		if (child.${contact}.getId() == ${contact}.getId() &&
		    child.getId() != app.getId())
			{
			unique=false
			}
		}
	return unique
	}
*/	

def pageTwo(error_data)
	{
	dynamicPage(name: "pageTwo", title: "Entry and Exit Data", uninstall: true)
		{
		section("") 
			{
			if (error_data instanceof String )
				{
				paragraph "${error_data}"
				}
			input "theentrydelay", "number", required: true, range: "0..60", defaultValue: 30,
				title: "Alarm entry delay time in seconds from 0 to 60"
			input "theexitdelay", "number", required: true, range: "0..60", defaultValue: 30,
				title: "When arming in away mode set an exit delay time in seconds from 0 to 60. When using lock-manager app's exit delay, set to 0"
			input "thekeypad", "capability.button", required: false, multiple: true,
				title: "Zero or more Optional Keypads: sounds entry delay tone "
			input "thesiren", "capability.alarm", required: false, multiple: true,
				title: "Zero or more Optional Sirens to Beep on entry delay"
			}
		}
	}	

def pageTwoVerify() 				//edit page one info, go to pageTwo when valid
	{
	def error_data
	if (theentrydelay < 1 && theexitdelay < 1)
		{
		if (error_data!=null)
			{
			error_data+="\n\nIllogical condition: entry and exit delays are both zero"
			}
		else
			{
			error_data="Illogical condition: entry and exit delays are both zero"
			}
		}	
	if (error_data!=null)
		{
		pageTwo(error_data)
		}
	else 
		{
		pageThree()
		}
	}


def pageThree(error_data)
	{
	dynamicPage(name: "pageThree", title: "Monitor: SmartHome changed to armed and this contact is open", install: true, uninstall: true)
		{
		section("")
			{
			if (error_data instanceof String )
				{
				paragraph "${error_data}"
				}
			input "maxcycles", "number", required: false, range: "1..99", defaultValue: 2,
				title: "Maximum number of warning messages"
			input "themonitordelay", "number", required: false, range: "1..15", defaultValue: 1,
				title: "Number of minutes between messages from 1 to 15"  	
			input "theLog", "bool", required: false, defaultValue:true,
				title: "Log to Notifications?"
			input "thesendPush", "bool", required: false, defaultValue:true,
				title: "Send Push Notification?"
			input "phone", "phone", required: false, 
				title: "Send a text message to this number. For multiple SMS recipients, separate phone numbers with a semicolon(;)"
			}
		}
	}	

def pageThreeVerify() 				//edit page three info
	{
	def error_data
	if (theLog || thesendPush || phone) 
		{}
	else
		{
		error_data="Please change settings to log the error message"
		}
	if (error_data!=null)
		{
		pageThree(error_data)
		}
//	else 
//		{
//		pageOne()
//		}
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
	subscribe(location, "alarmSystemStatus", alarmStatusHandler)
	subscribe(thecontact, "contact.open", doorOpensHandler)
	subscribe(thecontact, "contact.closed", contactClosedHandler)	//open door monitor
	}	

/******** Common Routine monitors the alarm state for changes ********/

def alarmStatusHandler(evt)
	{
	log.debug("alarmStatusHandler caught alarm status change: ${evt.value}")
	if (evt.value=="off")
		{
		unschedule(soundalarm)		//kill any lingering future tasks for delay or monitor
		killit()				//kill any lingering future tasks for delay or monitor
		}
	else
		{
		if (countopenContacts()==0)
			{
			killit()
			}
		else
			{
			new_monitor()
			}
		}
	}
	

/******** SmartHome Entry Delay Logic ********/

def doorOpensHandler(evt)
	{
	def alarm = location.currentState("alarmSystemStatus")
	def alarmstatus = alarm?.value
	def lastupdt = alarm?.date.time
	log.debug "doorOpensHandler called: $evt.value $alarmstatus $lastupdt"

//	get current time and alarm time in seconds
	def currT = now()
	def currSecs = Math.round(currT / 1000)	//round back to seconds
//	log.debug "${currSecs}"
	def alarmSecs = Math.round( lastupdt / 1000)
//	log.debug "${alarmSecs}"

//	alarmstaus values: off, stay, away
//	check first if this is an exit delay in away mode, if yes monitor the door, else its an alarm
	if (alarmstatus == "away" && currSecs - alarmSecs < theexitdelay)
		{
		new_monitor()
		}
	else	
	if (alarmstatus == "stay" || alarmstatus == "away")
		{
//		When keypad is defined: Issue an entrydelay for the delay on keypad. Keypad beeps
		if (settings.thekeypad)
			{
			thekeypad.setEntryDelay(theentrydelay)
			}

//		when siren is defined: wait 2 seconds allowing people to get through door, then blast a siren warning beep
		if (settings.thesiren)
			{
			thesiren.beep([delay: 2000])
			}

//		Trigger Alarm in theentrydelay seconds by opening the virtual sensor.
//		Do not delay alarm when additional triggers occur by using overwrite: false
		def now = new Date()
		def runTime = new Date(now.getTime() + (theentrydelay * 1000))
		runOnce(runTime, soundalarm, [data: [lastupdt: lastupdt], overwrite: false]) 
		}
	}

//	Sound the Alarm. When SmartHome sees simulated sensor change to open, alarm will sound
def soundalarm(data)
	{
	def alarm2 = location.currentState("alarmSystemStatus")
	def alarmstatus2 = alarm2.value
	def lastupdt = alarm2.date.time
	log.debug "soundalarm called: $alarmstatus2 $data.lastupdt $lastupdt"
	if (alarmstatus2=="off")		//This compare is optional, but just incase 
		{}
	else
	if (data.lastupdt==lastupdt)		//if this does not match, the system was set off then rearmed in delay period
		{
		log.debug "alarm triggered"
		thesimcontact.close()		//must use a live simulated sensor or this fails in Simulator
		thesimcontact.open()
		thesimcontact.close([delay: 4000])
		}
	unschedule(soundalarm)					//kill any lingering tasks caused by using overwrite false on runIn
	}

/******** Monitor for Open Doors when SmarthHome is initially Armed *********/	
def new_monitor()
	{
	log.debug "new_monitor called: cycles: $maxcycles"
	unschedule(checkStatus)
	state.cycles = maxcycles
	def now = new Date()
	def runTime = new Date(now.getTime() + (themonitordelay * 60000))
	runOnce (runTime, checkStatus)
	}

def killit()
	{
	log.debug "killit called"
	state.remove('cycles')
	unschedule(checkStatus)	//kill any pending cycles
	}

def countopenContacts() {
	log.debug "countopenContacts entered"
	def curr_contacts = thecontact.currentContact	//status of each contact in a list(array)
//	count open contacts	
	def open_contacts = curr_contacts.findAll 
		{
		contactVal -> contactVal == "open" ? true : false
		}
	log.debug "countopenContacts exit with count: ${open_contacts.size()}"
	return (open_contacts.size())
	}

def contactClosedHandler(evt) 
	{
	log.debug "contactClosedHandler called: $evt.value"
	if (countopenContacts()==0)
		killit()
	}

def checkStatus()
	{
	// get the current state for alarm system
	def alarmstate = location.currentState("alarmSystemStatus")
	def alarmvalue = alarmstate.value
	def door_count=countopenContacts()		//get open contact count
	log.debug "In checkStatus: Alarm: $alarmvalue Doors Open: ${door_count} MessageCycles remaining: $state.cycles"


//	Check if armed and one or more contacts are open
	if ((alarmvalue == "stay" || alarmvalue == "away") && door_count>0)
		{
		state.cycles = state.cycles - 1	//decrement cycle count
//		state.cycles--  note to self this does not work

//		calc standard next runOnce time
		def now = new Date()
		def runTime = new Date(now.getTime() + (themonitordelay * 60000))

//		get names of open contacts for message
		def curr_contacts= thecontact.currentContact	//status of each switch in a list(array)
		def name_contacts= thecontact.displayName		//name of each switch in a list(array)
		def door_names="";
		def door_sep="";
		def ikey=0
		curr_contacts.each
			{ value -> 
			if (value=="open")
				{
				door_names+=door_sep+name_contacts[ikey]
				door_sep=", "
				}
			ikey++;
			}
		if (door_names>"")
			{
			if (door_count > 1)
				door_names+=" are open"
			else	
				door_names+=" is open"
			}	
		def message = "System is armed, but ${door_names}"
		if (state.cycles<1)
			message+=" (Final Warning)"
//		log, send notification, SMS message	
		if (theLog)
			{
			sendNotificationEvent(message)
			}
		if (thesendPush)
			{
			sendPushMessage(message)
			}
		if (phone)
			{
			def phones = phone.split(";")
			for (def i = 0; i < phones.size(); i++)
				{
				sendSmsMessage(phones[i], message)
				}
			}
		if (themonitordelay>0 && state.cycles>0)
			{
			log.debug ("issued next checkStatus cycle $themonitordelay ${60*themonitordelay} seconds")
			runOnce(runTime,checkStatus)
			}
		}
	else
		{
		killit()
		}

	}	
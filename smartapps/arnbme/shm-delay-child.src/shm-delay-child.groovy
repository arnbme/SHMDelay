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
 *	Aug 15, 2017 v1.0.4  fill Label with sendor name 
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
    page(name: "pageOne", nextPage: "pageTwo")
    page(name: "pageTwo", nextPage: "pageThree")
    page(name: "pageThree")
}

def pageOne()
	{
	dynamicPage(name: "pageOne", title: "Contact sensors, must remove from SmartHome monitoring", uninstall: true)
		{
		section("")
			{
			input "thecontact", "capability.contactSensor", required: true, multiple:true,
				title: "One or more contact sensors", submitOnChange: true
			}
/*		if (thecontact)		//causes some wierd page not found error aaaagh frustrating
						also fails in log debug install WTF
			{
			def devLabel = thecontact.getLabel
			if (devLabel)
				log.debug "the label ${devLabel}"
			log.debug "the contact ${thecontact.displayName}"
			}
		else
			{
			log.debug "no contact sensor available"
			}
*/		if (thecontact)
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

def pageTwo()
	{
	dynamicPage(name: "pageTwo", title: "Contact Delay Controls", uninstall: true)
		{
		section("") 
			{
			input "thesimcontact", "capability.contactSensor", required: true,
				title: "Simulated Contact Sensor, monitored by SmartHome"
			input "theentrydelay", "number", required: true, range: "0..60", defaultValue: 30,
				title: "Alarm entry delay time in seconds from 0 to 60"
			input "theexitdelay", "number", required: true, range: "0..60", defaultValue: 30,
				title: "When arming in away mode set an exit delay time in seconds from 0 to 60. When using lock-manager app's exit delay, set to 0"
			input "thekeypad", "capability.button", required: false, multiple: true,
				title: "Zero or more Optional Keypads: sounds entry delay tone "
			input "thesiren", "capability.alarm", required: false, multiple: true,
				title: "Zero or more Optional Sirens to Beep"
			}
		}
	}	

def pageThree()
	{
	dynamicPage(name: "pageThree", title: "SmartHome arming with open contacts", install: true, uninstall: true)
		{
		section("")
			{
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
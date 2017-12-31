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
    description: "(V1.2.0)SmartApp simulating missing entry and exit delay option in SmartHome",
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
}

def main()
	{
	dynamicPage(name: "main", title: "Delay Simulator for SmartHome", install: true, uninstall: true)
		{
		if (app.getInstallationState() == 'COMPLETE')	//note documentation shows as lower case, but returns upper
			{  
			def modeFixChild="Create"
			def children = getChildApps()
			children.each
				{ child ->
				def childLabel = child.getLabel()
				if (childLabel.matches("(.*)(?i)ModeFix(.*)"))	
					{
					modeFixChild="Update"
					}
				}
			def modeActive=" Inactive"
			if (globalFixMode)
				{modeActive=" Active"}
			def fixtitle = modeFixChild + modeActive + " Mode Fix Settings"
			section 
				{
				app(name: "EntryDelayProfile", appName: "SHM Delay Child", namespace: "arnbme", title: "Create A New Delay Profile", multiple: true)
				}
			section (hideable: true, hidden: true, "Global Application Settings")
				{
				input "globalSimUnique", "bool", required: false, defaultValue:false,
					title: "Simulated sensors must be unique? Default: Off/False allows using a single simulated sensor."
				input "globalIntrusionMsg", "bool", required: false, defaultValue: true,
					title: "Issue intrusion message with name of triggering real sensor? When simulated sensors are not unique, this should this be set On/True."
				input "globalTrueEntryDelay", "bool", required: true, defaultValue: false,
					title: "True Entry Delay: In AlarmState Away and Stay with an entry delay time, ignore triggers from all other sensors when Monitored Contact Sensor opens. Default: Off/False"
				input "globalTrueNight", "bool", required: true, defaultValue: true,
					title: "True Night Mode: In AlarmState Armed (Stay/Night) trigger immediate intrusion, no entry delay. Default: On/True"
				input "globalFixMode", "bool", required: false, defaultValue: false,
					title: "Mode Fix: When AlarmState changes, fix Mode when invalid. Default: Off/False"
				input "globalKeypad", "bool", required: false, defaultValue: false,
					title: "The upgraded Keypad module is installed Default: Off/False"
				input (name: "global911", type:"enum", required: false, options: ["911","999","112",""],
					title: "Add 3 digit emergency call number on intrusion message?")
				input "globalPolice", "phone", required: false, 
					title: "Include this phone number as a link on the intrusion message? Separate multiple phone numbers with a semicolon(;)"
				input "globalMultipleMotion", "bool", required: true, defaultValue: false,
					title: "Allow Multiple Motion Sensors in Delay Profile. Default: Off/False"
				}	
			if (globalFixMode && modeFixChild == "Create")
				{
				section (hideable: true, hidden: false, "Mode Fix Settings are required"){
				app(name: "ModeFixProfile", appName: "SHM Delay ModeFix", namespace: "arnbme", title: "${fixtitle}", multiple: false)
					}
				}	
			else
				{
				section (hideable: true, hidden: true, "Mode Fix Settings Are$modeActive"){
				app(name: "ModeFixProfile", appName: "SHM Delay ModeFix", namespace: "arnbme", title: "${fixtitle}", multiple: false)
					}
				}	
			}
		else	{
			section 
				{
				paragraph "Please read the documentation, set optional global settings, then complete the install by clicking 'Done' above before creating your first profile"
				}
			section (hideable: true, hidden: false, "Global Application Settings")
				{
				input "globalSimUnique", "bool", required: true, defaultValue:false,
					title: "Simulated sensors must be unique? Default: Off/False allows using a single simulated sensor."
				input "globalIntrusionMsg", "bool", required: true, defaultValue: true,
					title: "Issue intrusion message with name of triggering real sensor? When simulated sensors are not unique, this should this be set On/True."
				input "globalTrueEntryDelay", "bool", required: true, defaultValue: false,
					title: "True Entry Delay: In AlarmState Away and Stay with an entry delay time, ignore triggers from all other sensors when Monitored Contact Sensor opens. Default: Off/False"
				input "globalTrueNight", "bool", required: true, defaultValue: false,
					title: "True Night Mode: In AlarmState Armed (Stay/Night) trigger immediate intrusion, no entry delay. Default: Off/False"
				input "globalFixMode", "bool", required: true, defaultValue: false,
					title: "Mode Fix: When AlarmState changes, fix Mode when invalid. Default: Off/False"
				input "globalKeypad", "bool", required: true, defaultValue: false,
					title: "The upgraded Keypad module is installed Default: Off/False"
				input (name: "global911", type:"enum", required: false, options: ["911","999","112",""],
					title: "Add emergency call link on intrusion message?")
				input "globalPolice", "phone", required: false, 
					title: "Include this phone number as a link on the intrusion message? Separate multiple phone numbers with a semicolon(;)"
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
		remove("Uninstall SHM Delay","Warning!!","This will remove the ENTIRE SmartApp, including all profiles and settings.")
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
			"Please complete the install by clicking 'Done' on the main page before creating your first profile.\n\n"+
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

def initialize() {
//	List the childapps just incase it may be needed
	log.debug "there are ${childApps.size()} child smartapps"
	childApps.each {child ->
	  log.debug "child app: ${child.label}"
	}
}	
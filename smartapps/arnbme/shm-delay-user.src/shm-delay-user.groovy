/**
 *  Smart Home Delay User Maintain
 *	Functions: 
 *		Maintain User keypad pins for SHM Delay V2					
 * 
 *  Copyright 2018 Arn Burkhoff
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
 *	Mar 18, 2018 v0.0.0  Add Panic pin usage type 
 *	Mar 01, 2018 v0.0.0  Create 
 *
 */
definition(
    name: "SHM Delay User",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "Maintain Users for SHM Delay. Child module",
    category: "My Apps",
    parent: "arnbme:SHM Delay",
    iconUrl: "https://www.arnb.org/IMAGES/hourglass.png",
    iconX2Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    iconX3Url: "https://www.arnb.org/IMAGES/hourglass@2x.png")

//import java.text.ParseException;
import java.text.SimpleDateFormat;

preferences {
	page(name: "pageZeroVerify")
	page(name: "pageZero", nextPage: "pageZeroVerify")
	page(name: "pageOne", nextPage: "pageOneVerify")
	page(name: "pageOneVerify")
	page(name: "pageTwo", nextPage: "pageTwoVerify")		//schedule page
	page(name: "pageTwoVerify")
	page(name: "pageThree")		//recap page when everything is valid. No changes allowed.
	}

def pageZeroVerify()
//	Verify this is installed as a child
	{
	if (parent && parent.getInstallationState()=='COMPLETE')
		{
		pageOne()
		}
	else
		{
		pageZero()
		}
	}	

def pageZero()
	{
	dynamicPage(name: "pageZero", title: "This App cannot be installed", uninstall: true, install:false)
		{
		section
			{
			paragraph "This SmartApp, SHM Delay User, must be used as a child app of SHM Delay."
			}
		}
	}	


def pageOne()
	{
	dynamicPage(name: "pageOne", title: "Pin code and Settings", uninstall: true)
		{
		section
			{
			if (state.error_data)
				{
				paragraph "${state.error_data}"
				state.remove("error_data")
				}
			input "pinScheduled", "bool", required: false, defaultValue:false,   
				title: "Date, Day or Time Scheduled?"
			input "theuserpin", "text", required: true, 
				title: "Four digit numeric code"
			input "theusername", "text", required: true, submitOnChange: true,
				title: "User Name"
			input "thepinusage", "enum", options:["User", "Ignore", "Disabled", "Routine", "Piston", "Panic"], 
				required: true, title: "Pin Usage", submitOnChange: true
			if (thepinusage == "Routine")
				{
				def actions = location.helloHome?.getPhrases()*.label
				actions?.sort()
				input "thepinroutine", "enum", options: actions, required: true, 
					title: "Pin executes this Smart Home Monitor Routine"
				}	
			else
			if (thepinusage == "Piston")
				{
				input "thepinpiston", "text", required: true, 
					title: "Pin executes this WebCore Piston", description: "Copy/Paste External URL"
				}	
			input "themaxcycles", "number", required: false, defaultValue: 0, submitOnChange: true,
				title: "Maximum times pin may be used, unlimited when zero"
			if (themaxcycles > 0)
				{
				def atomicUseId=app.getId()+'uses'			//build unique atomic id for uses
				if (parent.atomicState."${atomicUseId}" > 0) 
					{
					input "resetburn", "bool", required: false, defaultValue:false,
						title: "Reset use count to zero?"
					}
				if (parent.atomicState."${atomicUseId}")
					{
					def burnmsg=""
					if (parent.atomicState."${atomicUseId}" >= themaxcycles)
						burnmsg= " and pin is burned"
					paragraph "Pin use count is "+parent.atomicState."${atomicUseId}"+burnmsg 
                    }
				}
			}	
		if (theusername)
			{
			section([mobileOnly:true]) 
				{
				label title: "Profile name", defaultValue: "Profile: User: ${theusername}", required: false
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

def pageOneVerify() 				//edit page one info, go to pageTwo when valid
	{
	def error_data = ""
	def pageTwoWarning
	def unique_result=""
	if (theuserpin)
		{
		if (!theuserpin.matches("([0-9]{4})"))
			{
			error_data = "Pin must be four digits, please reenter\n\n"
			}
		}
	if (error_data=="")	
		{
		unique_result=isunique()
		if (unique_result != "")
			error_data+=unique_result
		}
	
	if (thepinusage == "Piston")
		{
		if (!thepinpiston.matches("https://graph-[^.]+[.]api.smartthings.com/api/token/[^/]+/smartapps/installations/[^/]+/execute/[:][^:]+[:]"))
			{
			error_data += "Please enter a valid Piston URL\n\n"
			}
		}	
		
	if (error_data!="")
		{
		state.error_data=error_data.trim()
		pageOne()
		}
	else
		{
		if (resetburn)
			{
			def atomicUseId=app.getId()+'uses'			//build unique atomic id for uses
			parent.atomicState."${atomicUseId}" = 0		//reset the burn count to zero 
//			resetburn=false;
			}
		if (pageTwoWarning!=null)			
			{state.error_data=error_data.trim()}
		if (pinScheduled)
			pageTwo()
		else
			pageThree()
		}
	}	

def isunique()
	{
	def unique = ""
	def children = parent?.getChildApps()
	children.each
		{ child ->
		if (child.getName()=="SHM Delay User")		//process only pin profiles	
			{
//			verify unique name
			if (child.theusername == theusername &&
			    child.getId() != app.getId())
				{
				unique+='Duplicate User Name, not allowed\n\n'
				}
//			verify unique pin
			if (child.theuserpin == theuserpin &&
			    child.getId() != app.getId())
				{
				unique+='Pin in use by user '+child.theusername+'\n\n'
				}
//			verify unique label
			if (child.getLabel() == app.getLabel() &&
			    child.getId() != app.getId())
				{
				unique+='Duplicate User Label used by user '+child.theusername+'\n\n'
				}
			}
		}	
	return unique
	}

def pageTwo() 
	{
	dynamicPage(name: 'pageTwo', title: 'Scheduling Rules, all fields are optional') 
		{
		def numdt=""
		section
			{
			if (state.error_data)
				{
				paragraph "${state.error_data}"
				state.remove("error_data")
				}
			input 'pinDays', 'enum', description: 'Valid all days',  
				options: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'],
				required: false, multiple: true, title: 'Select days pin may be used'
			input(name: 'pinStartTime', type: 'time', title: 'Start Time', required: false)
			input(name: 'pinEndTime', type: 'time', title: 'End Time', required: false)
			if (pinStartDt>"")
				{
				numdt=dtEdit(pinStartDt)
				if (state.dtedit)
					{
					numdt = "please correct"
//					numdt = state.dtedit
					state.remove("dtedit")
					}
				}
			input name: "pinStartDt", type: "text", title: "Start Date $numdt", required: false, submitOnChange: true,
				description: "Mth dd, yyyy" 
			numdt=""
			if (pinEndDt>"")
				{
				numdt=dtEdit(pinEndDt)
				if (state.dtedit)
					{
					numdt = "please correct"
//					numdt = state.dtedit
					state.remove("dtedit")
					}
				}
			input name: "pinEndDt", type: "text", title: "End Date $numdt", required: false, submitOnChange: true,
				description: "Mth dd, yyyy" 
			}
		}
	}
	
def pageTwoVerify() 					//edit schedule data, go to pageThree when valid
	{
	def error_data = ""
	def num_dtstart 
	def num_dtend
	if (pinStartDt > "")
		{
		num_dtstart=dtEdit(pinStartDt)
		if (state.dtedit)
			{
			error_data += state.dtedit
			state.remove("dtedit")
			}
//		log.debug "Start edit ${pinStartDt} ${num_dtstart} ${error_data}"
		}
	if (pinEndDt > "")
		{
		num_dtend=dtEdit(pinEndDt)
		if (state.dtedit)
			{
			error_data += state.dtedit
			state.remove("dtedit")
			}
//		log.debug "End edit ${pinEndDt} ${num_dtend} ${error_data}"
		}

	if (pinStartDt > "" && pinEndDt >"" && error_data == "")
		{
		if (num_dtend <= num_dtstart)
			error_data += "End date: ${num_dtend} must be greater than Start date: ${num_dtstart}\n\n"
		}

//	verify optional time data stored by system as 2018-03-13T11:30:00.000-0400
//	use only the time portion HH:MM when comparing and testing for when pin is entered in SHM Delay
	if (pinStartTime > "" && pinEndTime >"")
		{
//		log.debug "times ${pinStartTime} ${pinEndTime}"
		if (pinEndTime.substring(11,16) <= pinStartTime.substring(11,16))
			error_data += "End Time must be greater than Start Time\n\n"
		}
	else
	if (pinStartTime > "")
		error_data += "Please enter an End Time\n\n"
	else
	if (pinEndTime > "")
		error_data += "Please enter a Start Time\n\n"
		

	if (error_data!="")
		{
		state.error_data=error_data.trim()
		pageTwo()
		}
	else	
		pageThree()
	}

//	verify and format start and end date standard format is Jan 1, 2018
//	but logic allows for January 1 18 or Jan. 1 18 that create (seems a bit fickle) 
def dtEdit(dt)				
	{
	def input_mask = "MMM dd yy"		//also allows a 4 digit year with SimpleDateFormat, usually fixes 2 digit year
	def numdt_mask = "yyyyMMdd"			//result date
	def date
	def numdt
	try {
		def sdf= new SimpleDateFormat(input_mask)
		sdf.setLenient(false);						//reject stuff like Mar 32, 2018
		date = sdf.parse(dt.replaceAll("[.,]"," ")) //get rid of junk, verify and format date
		sdf.applyPattern(numdt_mask);				//convert date to
		numdt = sdf.format(date);					//	the yyyymmdd format for comparing and processing
//		log.debug "Start ${date} is valid ${numdt}"
		return numdt;								//return valid date in useable format
		} 
	catch (pe)
		{
		
		state.dtedit = "$date  ${pe}\n\n"		//date is invalid
		return false;
		}
	}	

//	This page summarizes the data prior to save	
def pageThree(error_data)
	{
	dynamicPage(name: "pageTwo", title: "Verify settings then tap Save, or tap < (back) to change settings", install: true, uninstall: true)
		{
		section
			{
			paragraph "Pin Code is ${theuserpin}"
			paragraph "User Name is ${theusername}"
			switch (thepinusage)
				{
				case "User":
					paragraph "The pin is assigned to a Person"
					break
				case "Ignore":
					paragraph "The pin is Ignored"
					break
				case "Disabled":
					paragraph "The pin is Disabled, processed as bad pin"
					break
				case "Routine":
					paragraph "The pin executes Routine: $thepinroutine"
					break
				case "Piston":
					paragraph "The pin executes WebCore Piston: $thepinpiston"
					break
				case "Panic":
					paragraph "Panic pin triggers the SmartThings intrusion alarm"
					break
				default:
					paragraph "Pin usage not set, Person assumed"
				}
			if (themaxcycles > 0)
				{
				paragraph "Max Cycles is ${themaxcycles}"
				def atomicUseId=app.getId()+'uses'			//build unique atomic id for uses
				if (parent.atomicState."${atomicUseId}" && parent.atomicState."${atomicUseId}" > 0)
					{
					def burnmsg=""
					if (parent.atomicState."${atomicUseId}" >= themaxcycles)
						burnmsg= " and pin is burned"
					paragraph "Pin use count is "+parent.atomicState."${atomicUseId}"+burnmsg 
                    }
				else
                	paragraph 'Pin use count is zero'
				}         
			else
				paragraph "Max Cycles is unlimited"
			if (pinScheduled)
				{
    			def df = new java.text.SimpleDateFormat("EEEE")	//from ST groovy api documentation    			
    			df.setTimeZone(location.timeZone)
    			def day = df.format(new Date())
    			def df2 = new java.text.SimpleDateFormat("yyyyMMdd")    			
    			df2.setTimeZone(location.timeZone)
				def nowymd = df2.format(new Date());		//	the yyyymmdd format for comparing and processing
				def dtbetween=true
				def num_dtstart
				def num_dtend
				if (pinStartDt > "")
					num_dtstart=dtEdit(pinStartDt)
				if (pinEndDt > "")
					num_dtend=dtEdit(pinEndDt)
				if (pinDays)
					paragraph "Valid Days: ${pinDays}. Currently: ${pinDays.contains(day)}"
				if (pinStartTime>"" && pinEndTime>"")
					{
   					def between = timeOfDayIsBetween(pinStartTime.substring(11,16), pinEndTime.substring(11,16), new Date(), location.timeZone)
   					paragraph "Valid Hours: ${pinStartTime.substring(11,16)} to ${pinEndTime.substring(11,16)}. Currently: $between"
					}
				if (pinStartDt>"" && pinEndDt>"")
					{
					if (num_dtstart > nowymd || num_dtend < nowymd)
						dtbetween=false
					paragraph "Valid Dates: $pinStartDt to $pinEndDt. Currently: $dtbetween" 
					}
				else
				if (pinStartDt>"")
					{
					if (num_dtstart > nowymd)
						dtbetween=false
					paragraph "Valid From: $pinStartDt. Currently: $dtbetween" 
					}
				else
				if (pinEndDt>"")
					{
					if (num_dtend < nowymd)
						dtbetween=false
					paragraph "Valid Until: $pinEndDt. Currently: $dtbetween" 
					}
				}		
			paragraph "The Profile Name is ${app.getLabel()}"	
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
	{}
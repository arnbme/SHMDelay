/**
 *  Smart Home Entry and Exit Delay, Internet Keypad SmartApp 
 *  Functions: 
 *		Acts as a container/controller for Internet Keypad simulation device: arnb.org/keypad.html
 * 
 *  Copyright 2018 Arn Burkhoff
 * 
 * 	Changes to Apache License
 *	4. Redistribution. Add paragraph 4e.
 *	4e. This software is free for Private Use. All derivatives and copies of this software must be free of any charges,
 *	 	and cannot be used for commercial purposes.
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
 *	Created from Ecobee Suite Service Manager,  Original Author: scott Date: 2013
 *	Updates by Barry A. Burke (storageanarchy@gmail.com) 2016, 2017, & 2018
 *	All of the unused coded was removed, not much left. It was going to be a service manager, but ended up as a Smartapp
 *  
 *	May 23, 2018 v1.0.0 Strip out all unused code, set version to 1.0.0
 *						prepare for initial Beta release
 *  May 08, 2018 v0.0.0 Create 
 */  
import groovy.json.JsonOutput

def version()
	{
	return "1.0.0";
	}
def VersionTitle() 
	{
	return "(${version()}) Connect Internet Keypad to SmartThings"
	}

definition(
	name: "SHM Delay Internet Keypad",
    namespace: "arnbme",
	author: "Arn Burkhoff",
	description: "${VersionTitle()}",
	category: "My Apps",
    iconUrl: "https://www.arnb.org/IMAGES/hourglass.png",
    iconX2Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    iconX3Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    singleInstance: true)

{appSetting "clientId"}

preferences 
	{
	page(name: "mainPage")
    page(name: "refreshTokenPage")    
    page(name: "removePage")
	}

mappings
	{
//	path("/oauth/initialize") {action: [GET: "oauthInitUrl"]}
//	path("/oauth/callback") {action: [GET: "callback"]}
	path("/keypost/:command") {action: [GET: "api_pinpost"]}
    }

def api_pinpost() 
	{
    def String cmd = params.command
	log.debug "api-keypost command: $cmd"
	if (cmd.matches("([0-3][0-9]{4})"))		
		{
		def keycode = cmd.substring(1)
		def armMode = cmd.substring(0,1)
		log.debug "valid command: ${armMode} and pin: ${keycode}"
//		simkeypad.deviceNotification(keycode, armMode)		//create an event in simulated keypad DTH	
		simkeypad.deviceNotification(keycode, armMode)		//create an event in simulated keypad DTH	
//		log.debug "returned from simkeypad"
		}
	else	
	if (cmd=='1')
		{
		log.debug "kypd svcmgr panic received"
//		simkeypad.devicePanic()								//for reasons unknown, this fails with an error	
		simkeypad.deviceNotification('panic',1)				//create a Panic Event in simulated keypad DTH	
		}
	else		
		httpError(400, "$cmd is not a valid data")
	}

def mainPage() 
	{	
	dynamicPage(name: "mainPage", title: "${VersionTitle()}", install: true, uninstall: false) 
		{
		if(!atomicState.accessToken) 
			{
			try 
				{
				atomicState.accessToken = createAccessToken()
				}
			catch(Exception e)
				{
				if (atomicState.accessToken)
					{
					revokeAccessToken()
					atomicState.accessToken=null
					}
				section()
					{
					paragraph ("Error initializing SHM Delay Keypad Svcmgr Authentication: could not get the OAuth access token.\n\nPlease verify that OAuth has been enabled in " +
								"the SmartThings IDE for the 'SHM Delay Keypad Svcmgr' SmartApp, and then try again.\n\nIf this error persists, view Live Logging in the IDE for " +
								"additional error information.")
					paragraph ("Detailed Error: ${e}")			
					}
				}
			if (atomicState.accessToken)
				{
				def redirectUrl = buildRedirectUrl //"${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}"
//				log.debug ("${redirectUrl}")
				def b64= redirectUrl.encodeAsBase64()
//				log.debug ("${b64.size()} ${redirectUrl.encodeAsBase64()}")
				try {
					def url='https://www.arnb.org/shmdelay/oauthinit_st.php'
					url+='?i='+b64   			//stop this data from interacting 
					include 'asynchttp_v1'
					asynchttp_v1.get('getResponseHandler', [uri: url])
					}
				catch (e)
					{
					revokeAccessToken()
					atomicState.accessToken=null
					section()
						{
						paragraph ("Error initializing SHM Delay Keypad Svcmgr Authentication: unable to connect to the database.\n\nIf this error persists, view Live Logging in the IDE for " +
									"additional error information.")
						paragraph ("Detailed Error: ${e}")			
						}
					}
				}
			if (atomicState.accessToken)
				{
//				log.debug "mainPage created access token ${atomicState.accessToken}"
//				def redirectUrl = buildRedirectUrl //"${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}"
				section("Save this page, then enter Authorization\n${atomicState.accessToken.substring(0,8)}\ninto Internet Keypad authorization id") 
//				section("URL ${redirectUrl}") 
				}
			}	
		else
			section("Current Authorization is\n${atomicState.accessToken.substring(0,8)}") 
				
		section
			{
			input "simkeypad", "device.InternetKeypad", multiple: false, required:true, title: "Virtual Keypad Device"
			}
		if(atomicState?.accessToken)
			{
			section()
				{
				href ("removePage", description: "Tap to revoke token", title: "Revoke authorization")
				} 
			}	
		
		section ("Name this instance of Keypad Manager") 
			{
			label name: "name", title: "Assign a name", required: false, defaultValue: app.name, description: app.name, submitOnChange: true
			}
     
		}
	}

def removePage()
	{
	dynamicPage(name: "removePage", title: "Remove Keypad Authorization", install: false, uninstall: true) 
		{
		if (atomicState.accessToken)
			{
			def b64= atomicState.accessToken.encodeAsBase64()
			revokeAccessToken()
			atomicState.accessToken=null
			try {
				def url='https://www.arnb.org/shmdelay/oauthkill_st.php'
				url+='?k='+b64   			
				include 'asynchttp_v1'
				asynchttp_v1.get('getResponseHandler', [uri: url])
				}
			catch (e)
				{
				section()
					{
					paragraph ("Error initializing SHM Delay Keypad Kill: unable to connect to the database.\n\nIf this error persists, view Live Logging in the IDE for " +
								"additional error information.")
					paragraph ("Detailed Error: ${e}")			
					}
				}
			}
		section ("Token was deactivacted. Tap Remove delete the app")
		}
	}	


//	Process response from async execution 
def getResponseHandler(response, data)
	{
    if(response.getStatus() == 200)
    	{
		def results = response.getJson()
		log.debug "SHM Delay response ${results.msg}"
		if (results.msg != 'OK')
    		sendNotificationEvent("${results.msg}")
        }
    else
    	sendNotificationEvent("SHM Delay, HTTP Error = ${response.getStatus()}")
    }	

def installed() 
	{
    log.debug "Installed with settings: ${settings}"
	initialize()
	}

def updated()	
	{
    log.debug "Updated with settings: ${settings}"
	unsubscribe()
    initialize()
	}

def initialize()
	{	
    log.debug "Initialize"
	}
	
private def getServerUrl()          { return "https://graph.api.smartthings.com" }
private def getShardUrl()           { return getApiServerUrl() }
private def getCallbackUrl()        { return "${serverUrl}/oauth/callback" }
private def getBuildRedirectUrl() 	{ return "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}&apiServerUrl=${shardUrl}"}
	
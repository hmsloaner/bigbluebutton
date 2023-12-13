/**
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
* 
* Copyright (c) 2012 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License as published by the Free Software
* Foundation; either version 3.0 of the License, or (at your option) any later
* version.
* 
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
*
*/
package org.bigbluebutton.freeswitch.voice.freeswitch.response;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author leif
 */
public class XMLResponseConferenceListParser extends DefaultHandler {
    private List<ConferenceMember> myConfrenceMembers;
    private String tempVal;
    private ConferenceMember tempMember;
    private ConferenceMemberFlags tempFlags;
    private String room;
    private boolean inFlags = false;
    
    public XMLResponseConferenceListParser() {
        myConfrenceMembers = new ArrayList<ConferenceMember>();
    }

    public String getConferenceRoom() {
        return room;
    }

    public void printConferneceMemebers() {
        Iterator<ConferenceMember> it = myConfrenceMembers.iterator();
        while(it.hasNext()) {
            
        }
    }

    public List<ConferenceMember> getConferenceList() {
        return myConfrenceMembers;
    }

            /*
<?xml version="1.0"?>
<conferences>
  <conference name="3001-192.168.1.10" member-count="1" rate="8000" running="true" answered="true" enforce_min="true" dynamic="true">
    <members>
      <member>
        <id>6</id>
        <flags>
          <can_hear>true</can_hear>
          <can_speak>true</can_speak>
          <talking>false</talking>
          <has_video>false</has_video>
          <has_floor>true</has_floor>
          <is_moderator>false</is_moderator>
          <end_conference>false</end_conference>
        </flags>
        <uuid>3a16f061-0df6-45d5-b401-d8e977e08a5c</uuid>
        <caller_id_name>1001</caller_id_name>
        <caller_id_number>1001</caller_id_number>
        <join_time>65</join_time>
        <last_talking>4</last_talking>
      </member>
    </members>
  </conference>
</conferences>

             */


    //SAX Event Handlers
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //reset
        // Do not reset to false as the flags won't be processed. (ralam OCt 21, 2019)
        //inFlags = false;
        tempVal = "";

        if("member".equalsIgnoreCase(qName)) {
            String memberType = attributes.getValue("type");
            //create a new instance of ConferenceMember
            tempMember = new ConferenceMember();
            tempMember.setMemberType(memberType);
        }

        if("flags".equalsIgnoreCase(qName)) {
            //create a new instance of ConferenceMember
            tempFlags = new ConferenceMemberFlags();
            inFlags = true;
        }

        if("conference".equalsIgnoreCase(qName)) {
            room = attributes.getValue("name");
        }
    }


    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal = new String(ch,start,length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        if("member".equalsIgnoreCase(qName)) {
            //add it to the list
            myConfrenceMembers.add(tempMember);
        }else if("flags".equalsIgnoreCase(qName)) {
            tempMember.setFlags(tempFlags);
            inFlags = false;
        }else if(inFlags) {
            if ("can_speak".equalsIgnoreCase(qName)) {
                tempFlags.setCanSpeak(tempVal);
            }else if ("talking".equalsIgnoreCase(qName)) {
                tempFlags.setTalking(tempVal);
            } else if ("hold".equalsIgnoreCase(qName)) {
                tempFlags.setHold(tempVal);
            }
        }else if ("id".equalsIgnoreCase(qName)) {
            try {
                tempMember.setId(Integer.parseInt(tempVal));
            } catch(NumberFormatException nfe) {
                
            }
        }else if ("uuid".equalsIgnoreCase(qName)) {
            tempMember.setUUID(tempVal);
        }else if ("caller_id_name".equalsIgnoreCase(qName)) {
            tempMember.setCallerIdName(tempVal);
        }else if ("caller_id_number".equalsIgnoreCase(qName)) {
            tempMember.setCallerId(tempVal);
        }else if ("join_time".equalsIgnoreCase(qName)) {
            if (tempMember.getMemberType().equalsIgnoreCase("caller")) {
                try {
                    tempMember.setJoinTime(Integer.parseInt(tempVal));
                } catch(NumberFormatException nfe) {

                }
            } else if (tempMember.getMemberType().equalsIgnoreCase("recording_node")) {
                try {
                    tempMember.setRecordStartTime(Long.parseLong(tempVal));
                } catch(NumberFormatException nfe) {

                }
            }

        }else if ("last_talking".equalsIgnoreCase(qName)) {
            try {
                tempMember.setLastTalking(Integer.parseInt(tempVal));
            } catch(NumberFormatException nfe) {
                
            }
        } else if ("record_path".equalsIgnoreCase(qName)) {
            tempMember.setRecordPath(tempVal);
        }
    }
}

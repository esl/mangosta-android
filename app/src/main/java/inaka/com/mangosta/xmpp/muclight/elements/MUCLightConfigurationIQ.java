/**
 *
 * Copyright 2016 Fernando Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inaka.com.mangosta.xmpp.muclight.elements;

import org.jivesoftware.smack.packet.IQ;

import inaka.com.mangosta.xmpp.muclight.MUCLightRoomConfiguration;
import inaka.com.mangosta.xmpp.muclight.MultiUserChatLight;

/**
 * MUC Light configuration response IQ class.
 * 
 * @author Fernando Ramirez
 *
 */
public class MUCLightConfigurationIQ extends IQ {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = MultiUserChatLight.NAMESPACE + MultiUserChatLight.CONFIGURATION;

    private final String version;
    private final MUCLightRoomConfiguration configuration;

    /**
     * MUC Light configuration response IQ constructor.
     * 
     * @param version
     * @param configuration
     */
    public MUCLightConfigurationIQ(String version, MUCLightRoomConfiguration configuration) {
        super(ELEMENT, NAMESPACE);
        this.version = version;
        this.configuration = configuration;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.optElement("version", version);
        xml.element(new MUCLightElements.ConfigurationElement(configuration));
        return xml;
    }

    /**
     * Returns the version.
     * 
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the room configuration.
     * 
     * @return the configuration of the room
     */
    public MUCLightRoomConfiguration getConfiguration() {
        return configuration;
    }

}

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
package inaka.com.mangosta.xmpp.bob.elements;

import org.jivesoftware.smack.packet.IQ;

import java.io.UnsupportedEncodingException;

import inaka.com.mangosta.xmpp.bob.BoBData;
import inaka.com.mangosta.xmpp.bob.BoBHash;
import inaka.com.mangosta.xmpp.bob.BoBManager;

/**
 * Bits of Binary IQ class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0231.html">XEP-0231: Bits of
 *      Binary</a>
 */
public class BoBIQ extends IQ {

    /**
     * data element.
     */
    public static final String ELEMENT = "data";

    /**
     * the IQ NAMESPACE.
     */
    public static final String NAMESPACE = BoBManager.NAMESPACE;

    private BoBHash bobHash;
    private BoBData bobData;

    /**
     * Bits of Binary IQ constructor.
     * 
     * @param bobHash
     * @param bobData
     */
    public BoBIQ(BoBHash bobHash, BoBData bobData) {
        super(ELEMENT, NAMESPACE);
        this.bobHash = bobHash;
        this.bobData = bobData;
    }

    /**
     * Bits of Binary IQ constructor.
     * 
     * @param bobHash
     */
    public BoBIQ(BoBHash bobHash) {
        this(bobHash, null);
    }

    /**
     * Get the BoB hash.
     * 
     * @return the BoB hash
     */
    public BoBHash getBoBHash() {
        return bobHash;
    }

    /**
     * Get the BoB data.
     * 
     * @return the BoB data
     */
    public BoBData getBoBData() {
        return bobData;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.attribute("cid", bobHash.toCid());

        if (bobData != null) {
            xml.attribute("max-age", String.valueOf(bobData.getMaxAge()));
            xml.attribute("type", bobData.getType());
            xml.rightAngleBracket();

            try {
                xml.escape(new String(bobData.getContent(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        } else {
            xml.setEmptyElement();
        }

        return xml;
    }

}

/**
 * Copyright 2016 Fernando Ramirez
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inaka.com.mangosta.xmpp.bob.providers;

import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import inaka.com.mangosta.xmpp.bob.BoBData;
import inaka.com.mangosta.xmpp.bob.BoBHash;
import inaka.com.mangosta.xmpp.bob.elements.BoBIQ;

/**
 * Bits of Binary IQ provider class.
 *
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0231.html">XEP-0231: Bits of
 * Binary</a>
 */
public class BoBIQProvider extends IQProvider<BoBIQ> {

    @Override
    public BoBIQ parse(XmlPullParser parser, int initialDepth) throws Exception {
        BoBHash bobHash = null;
        BoBData bobData = null;

        parser.next();

        if (parser.getName().equals(BoBIQ.ELEMENT)) {
            String cid = parser.getAttributeValue("", "cid");
            bobHash = BoBHash.fromCid(cid);

            String dataType = parser.getAttributeValue("", "type");
            String maxAgeString = parser.getAttributeValue("", "max-age");

            long maxAge = 0;
            if (maxAgeString != null) {
                maxAge = Long.parseLong(maxAgeString);
            }

            String data = null;
            try {
                data = parser.nextText();
            } catch (Exception e) {
            }

            if (data != null && dataType != null) {
                bobData = new BoBData(maxAge, dataType, data.getBytes());
            }

        }

        return new BoBIQ(bobHash, bobData);
    }

}

package inaka.com.mangosta.xmpp.microblogging.providers;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.util.XmppDateTime;
import org.xmlpull.v1.XmlPullParser;

import java.util.Date;

import inaka.com.mangosta.xmpp.microblogging.elements.PostEntryExtension;

public class PostEntryProvider extends ExtensionElementProvider<PostEntryExtension> {

    @Override
    public PostEntryExtension parse(XmlPullParser parser, int initialDepth) throws Exception {
        String title = null;
        String id = null;
        Date published = null;
        Date updated = null;
        String authorName = null;
        Jid authorJid = null;

        outerloop:
        while (true) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {

                if (parser.getName().equals("title")) {
                    title = parser.nextText();
                }

                if (parser.getName().equals("id")) {
                    id = parser.nextText().split("posts-")[1];
                }

                if (parser.getName().equals("author")) {
                    int authorDepth = parser.getDepth();

                    authorOuterloop:
                    while (true) {
                        int authorEventType = parser.next();

                        if (authorEventType == XmlPullParser.START_TAG) {
                            if (parser.getName().equals("name")) {
                                authorName = parser.nextText();
                            }

                            if (parser.getName().equals("uri")) {
                                authorJid = JidCreate.from(parser.nextText().split(":")[1]);
                            }
                        } else if (authorEventType == XmlPullParser.END_TAG) {
                            if (parser.getDepth() == authorDepth) {
                                break authorOuterloop;
                            }
                        }
                    }

                }

                if (parser.getName().equals("published")) {
                    published = XmppDateTime.parseXEP0082Date(parser.nextText());
                }

                if (parser.getName().equals("updated")) {
                    updated = XmppDateTime.parseXEP0082Date(parser.nextText());
                }

            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }

        }

        return new PostEntryExtension(title, id, published, updated, authorName, authorJid);
    }

}

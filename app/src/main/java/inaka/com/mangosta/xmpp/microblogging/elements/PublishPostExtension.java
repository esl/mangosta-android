package inaka.com.mangosta.xmpp.microblogging.elements;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jxmpp.jid.Jid;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;


public class PublishPostExtension implements ExtensionElement {

    public static final String ELEMENT = "publish";

    public static final String NODE = "urn:xmpp:microblog:0";

    private String id;
    private Jid jid;
    private String title;

    public PublishPostExtension(Jid jid, String title) {
        this.id = UUID.randomUUID().toString();
        this.jid = jid;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Jid getJid() {
        return jid;
    }

    public void setJid(Jid jid) {
        this.jid = jid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("node", NODE);
        xml.rightAngleBracket();

        xml.halfOpenElement("item");
        xml.attribute("id", id);
        xml.rightAngleBracket();

        xml.halfOpenElement("entry");
        xml.xmlnsAttribute("http://www.w3.org/2005/Atom");
        xml.rightAngleBracket();

        xml.halfOpenElement("title");
        xml.attribute("type", "text");
        xml.rightAngleBracket();
        xml.escape(title);
        xml.closeElement("title");

        Date today = Calendar.getInstance().getTime();
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today);
        String idTag = "tag:" + jid.getDomain() + "," + timeStamp + ":posts-" + id;
        xml.element("id", idTag);

        xml.element("published", today);

        xml.element("updated", today);

        xml.closeElement("entry");
        xml.closeElement("item");
        xml.closeElement(this);
        return xml;
    }

}

package inaka.com.mangosta.xmpp.microblogging.elements;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import inaka.com.mangosta.xmpp.XMPPSession;

public class PostEntryExtension implements ExtensionElement {

    public static final String ELEMENT = "entry";
    public static final String NAMESPACE = "http://www.w3.org/2005/Atom";

    private String id;
    private String title;
    private Date published;
    private Date updated;

    public PostEntryExtension(String title, String id, Date published, Date updated) {
        this.id = id;
        this.title = title;
        this.published = published;
        this.updated = updated;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Date getPublished() {
        return published;
    }

    public Date getUpdated() {
        return updated;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();

        xml.halfOpenElement("title");
        xml.attribute("type", "text");
        xml.rightAngleBracket();
        xml.escape(title);
        xml.closeElement("title");

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(published);
        String idTag = "tag:" + XMPPSession.getInstance().getXMPPConnection().getUser().getDomain() + "," + timeStamp + ":posts-" + id;
        xml.element("id", idTag);

        xml.element("published", published);

        xml.element("updated", updated);

        xml.closeElement(this);
        return xml;
    }

}

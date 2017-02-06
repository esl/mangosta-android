package inaka.com.mangosta.xmpp.microblogging.elements;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jxmpp.jid.Jid;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import inaka.com.mangosta.xmpp.XMPPSession;

public class PostEntryExtension implements ExtensionElement {

    public static final String ELEMENT = "entry";
    public static final String NAMESPACE = "http://www.w3.org/2005/Atom";

    public static final String BLOG_POSTS_NODE = "urn:xmpp:microblog:0";
    public static final String COMMENTS_NODE = "urn:xmpp:microblog:0:comments";

    private String id;
    private String title;
    private Date published;
    private Date updated;
    private String authorName;
    private Jid authorJid;

    public PostEntryExtension(String title, String id, Date published, Date updated, String authorName, Jid authorJid) {
        this.id = id;
        this.title = title;
        this.published = published;
        this.updated = updated;
        this.authorName = authorName;
        this.authorJid = authorJid;
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

    public String getAuthorName() {
        return authorName;
    }

    public Jid getAuthorJid() {
        return authorJid;
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

        if (authorName != null && authorJid != null) {
            xml.openElement("author");
            xml.element("name", authorName);
            xml.element("uri", "xmpp:" + authorJid);
            xml.closeElement("author");
        }

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(published);
        String idTag = "tag:" + XMPPSession.getInstance().getXMPPConnection().getUser().getDomain() + "," + timeStamp + ":posts-" + id;
        xml.optElement("id", idTag);

        xml.element("published", published);

        xml.optElement("updated", updated);

        xml.closeElement(this);
        return xml;
    }

}

package inaka.com.mangosta.xmpp.microblogging.elements;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jxmpp.jid.Jid;

import java.util.Date;
import java.util.UUID;

public class PublishCommentExtension implements ExtensionElement {

    public static final String ELEMENT = "publish";

    public static final String NODE = "urn:xmpp:microblog:0:comments";

    private String blogPostId;
    private String id;
    private String authorName;
    private Jid authorJid;
    private String content;
    private Date published;

    public PublishCommentExtension(String blogPostId, String authorName, Jid authorJid, String content, Date published) {
        this.blogPostId = blogPostId;
        this.id = UUID.randomUUID().toString();
        this.authorJid = authorJid;
        this.authorName = authorName;
        this.content = content;
        this.published = published;
    }

    public String getBlogPostId() {
        return blogPostId;
    }

    public void setBlogPostId(String blogPostId) {
        this.blogPostId = blogPostId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Jid getAuthorJid() {
        return authorJid;
    }

    public void setAuthorJid(Jid authorJid) {
        this.authorJid = authorJid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getPublished() {
        return published;
    }

    public void setPublished(Date published) {
        this.published = published;
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
        xml.attribute("node", NODE + "/" + blogPostId);
        xml.rightAngleBracket();

        xml.halfOpenElement("item");
        xml.attribute("id", id);
        xml.rightAngleBracket();

        xml.halfOpenElement("entry");
        xml.xmlnsAttribute("http://www.w3.org/2005/Atom");
        xml.rightAngleBracket();

        xml.openElement("author");
        xml.element("name", authorName);
        xml.element("uri", "xmpp:" + authorJid);
        xml.closeElement("author");

        xml.halfOpenElement("title");
        xml.attribute("type", "text");
        xml.rightAngleBracket();
        xml.escape(content);
        xml.closeElement("title");

        xml.element("published", published);

        xml.closeElement("entry");
        xml.closeElement("item");
        xml.closeElement(this);
        return xml;
    }

}

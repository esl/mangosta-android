package inaka.com.mangosta.utils;

import org.junit.Assert;
import org.junit.Test;

import inaka.com.mangosta.xmpp.XMPPUtils;

public class XMPPUtilsTest {

    @Test
    public void checkJidToUserName() throws Exception {
        // @erlang-solutions.com JIDs
        Assert.assertEquals("ramabit", XMPPUtils.fromJIDToUserName("ramabit@erlang-solutions.com"));
        Assert.assertEquals("ramabit", XMPPUtils.fromJIDToUserName("ramabit@erlang-solutions.com/b4749661b09ddee8"));
        Assert.assertEquals("gardano", XMPPUtils.fromJIDToUserName("gardano@erlang-solutions.com"));
        Assert.assertEquals("gardano", XMPPUtils.fromJIDToUserName("gardano@erlang-solutions.com/b4749661b09ddee8"));

        // other JIDs
        Assert.assertEquals("flo@geekplace.eu", XMPPUtils.fromJIDToUserName("flo@geekplace.eu"));
        Assert.assertEquals("hello@inaka.com", XMPPUtils.fromJIDToUserName("hello@inaka.com"));
    }

    @Test
    public void checkUserNameToJid() throws Exception {
        // @erlang-solutions.com JIDs
        Assert.assertEquals("ramabit@erlang-solutions.com", XMPPUtils.fromUserNameToJID("ramabit"));
        Assert.assertEquals("gardano@erlang-solutions.com", XMPPUtils.fromUserNameToJID("gardano"));

        // other JIDs
        Assert.assertEquals("flo@geekplace.eu", XMPPUtils.fromUserNameToJID("flo@geekplace.eu"));
        Assert.assertEquals("hello@inaka.com", XMPPUtils.fromUserNameToJID("hello@inaka.com"));
    }

}

package inaka.com.mangosta.xmpp;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.List;

import inaka.com.mangosta.models.User;

public class RosterManager {

    private static RosterManager mInstance;

    public static RosterManager getInstance() {
        if (mInstance == null) {
            mInstance = new RosterManager();
        }
        return mInstance;
    }

    public static void setSpecialInstanceForTesting(RosterManager rosterManager) {
        mInstance = rosterManager;
    }

    public void removeAllFriends()
            throws SmackException.NotLoggedInException, InterruptedException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            SmackException.NoResponseException {
        Roster roster = Roster.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
        if (!roster.isLoaded()) {
            roster.reloadAndWait();
        }
        for (RosterEntry entry : roster.getEntries()) {
            roster.removeEntry(entry);
        }
    }

    public List<Jid> getBuddies()
            throws SmackException.NotLoggedInException, InterruptedException,
            SmackException.NotConnectedException {
        Roster roster = Roster.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
        if (!roster.isLoaded()) {
            roster.reloadAndWait();
        }

        String groupName = "Buddies";

        RosterGroup group = roster.getGroup(groupName);

        if (group == null) {
            roster.createGroup(groupName);
            group = roster.getGroup(groupName);
        }

        List<Jid> buddies = new ArrayList<>();

        List<RosterEntry> entries = group.getEntries();
        for (RosterEntry entry : entries) {
            buddies.add(entry.getJid());
        }

        return buddies;
    }

    public void removeFromBuddies(User user)
            throws SmackException.NotLoggedInException, InterruptedException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            SmackException.NoResponseException, XmppStringprepException {
        Roster roster = Roster.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
        if (!roster.isLoaded()) {
            roster.reloadAndWait();
        }
        BareJid jid = JidCreate.bareFrom(XMPPUtils.fromUserNameToJID(user.getLogin()));
        roster.removeEntry(roster.getEntry(jid));
    }

    public void addToBuddies(User user)
            throws SmackException.NotLoggedInException, InterruptedException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            SmackException.NoResponseException, XmppStringprepException {
        Roster roster = Roster.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
        if (!roster.isLoaded()) {
            roster.reloadAndWait();
        }
        BareJid jid = JidCreate.bareFrom(XMPPUtils.fromUserNameToJID(user.getLogin()));
        String name = user.getLogin();
        String[] groups = new String[]{"Buddies"};
        roster.createEntry(jid, name, groups);
    }

}

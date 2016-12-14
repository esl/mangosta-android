package inaka.com.mangosta.xmpp;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public void removeAllContacts()
            throws SmackException.NotLoggedInException, InterruptedException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            SmackException.NoResponseException {
        Roster roster = Roster.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
        if (!roster.isLoaded()) {
            roster.reloadAndWait();
        }
        for (RosterEntry entry : roster.getEntries()) {
            roster.removeEntry(entry);
            Presence presence = new Presence(Presence.Type.unsubscribe);
            presence.setTo(entry.getJid());
            XMPPSession.getInstance().sendStanza(presence);
        }
    }

    public HashMap<Jid, Presence.Type> getBuddies()
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

        HashMap<Jid, Presence.Type> buddies = new HashMap<>();

        List<RosterEntry> entries = group.getEntries();
        for (RosterEntry entry : entries) {
            BareJid jid = entry.getJid();
            Presence.Type status = roster.getPresence(jid).getType();
            buddies.put(jid, status);
        }

        return buddies;
    }

    public void removeFromBuddies(String jidString)
            throws SmackException.NotLoggedInException, InterruptedException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            SmackException.NoResponseException, XmppStringprepException {
        Roster roster = Roster.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
        if (!roster.isLoaded()) {
            roster.reloadAndWait();
        }

        BareJid jid = JidCreate.bareFrom(jidString);
        roster.removeEntry(roster.getEntry(jid));

        Presence presence = new Presence(Presence.Type.unsubscribe);
        presence.setTo(JidCreate.from(jidString));
        XMPPSession.getInstance().sendStanza(presence);
    }

    public void removeFromBuddies(User user)
            throws SmackException.NotLoggedInException, InterruptedException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            SmackException.NoResponseException, XmppStringprepException {
        String jidString = XMPPUtils.fromUserNameToJID(user.getLogin());
        removeFromBuddies(jidString);
    }

    public void addToBuddies(String jidString)
            throws SmackException.NotLoggedInException, InterruptedException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            SmackException.NoResponseException, XmppStringprepException {
        Roster roster = Roster.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
        if (!roster.isLoaded()) {
            roster.reloadAndWait();
        }

        BareJid jid = JidCreate.bareFrom(jidString);
        String name = XMPPUtils.fromJIDToUserName(jidString);
        String[] groups = new String[]{"Buddies"};

        roster.createEntry(jid, name, groups);
        roster.sendSubscriptionRequest(jid);
    }

    public void addToBuddies(User user)
            throws SmackException.NotLoggedInException, InterruptedException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            SmackException.NoResponseException, XmppStringprepException {
        addToBuddies(XMPPUtils.fromUserNameToJID(user.getLogin()));
    }

    public Presence.Type getStatusFromContact(User user) {
        return getStatusFromContact(user.getLogin());
    }

    public Presence.Type getStatusFromContact(String name) {
        try {
            HashMap<Jid, Presence.Type> buddies = getBuddies();

            for (Map.Entry<Jid, Presence.Type> pair : buddies.entrySet()) {
                if (XMPPUtils.fromJIDToUserName(pair.getKey().toString()).equals(name)) {
                    return pair.getValue();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Presence.Type.unavailable;
    }

    public boolean isContact(Jid jid)
            throws SmackException.NotLoggedInException, InterruptedException,
            SmackException.NotConnectedException {
        HashMap<Jid, Presence.Type> buddies = getBuddies();
        return buddies.containsKey(jid);
    }

}

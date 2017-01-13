package inaka.com.mangosta.xmpp;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.roster.RosterListener;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.models.User;

public class RosterManager {

    private static RosterManager mInstance;

    public static RosterManager getInstance() {
        if (mInstance == null) {
            mInstance = new RosterManager();

            Roster roster = Roster.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
            roster.addRosterListener(new RosterListener() {
                @Override
                public void entriesAdded(Collection<Jid> collection) {
                    EventBus.getDefault().post(new Event(Event.Type.ROSTER_CHANGED));
                }

                @Override
                public void entriesUpdated(Collection<Jid> collection) {
                    EventBus.getDefault().post(new Event(Event.Type.ROSTER_CHANGED));
                }

                @Override
                public void entriesDeleted(Collection<Jid> collection) {
                    EventBus.getDefault().post(new Event(Event.Type.ROSTER_CHANGED));
                }

                @Override
                public void presenceChanged(Presence presence) {
                }
            });
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

    public HashMap<Jid, Presence.Type> getContacts()
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

    public HashMap<Jid, Presence.Type> getContactsWithSubscriptionPending()
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

        HashMap<Jid, Presence.Type> buddiesPending = new HashMap<>();

        List<RosterEntry> entries = group.getEntries();
        for (RosterEntry entry : entries) {
            if (entry.isSubscriptionPending()) {
                BareJid jid = entry.getJid();
                Presence.Type status = roster.getPresence(jid).getType();
                buddiesPending.put(jid, status);
            }
        }

        return buddiesPending;
    }

    public void removeContact(String jidString)
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

    public void removeContact(User user)
            throws SmackException.NotLoggedInException, InterruptedException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            SmackException.NoResponseException, XmppStringprepException {
        String jidString = XMPPUtils.fromUserNameToJID(user.getLogin());
        removeContact(jidString);
    }

    public void addContact(String jidString)
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

    public void addContact(User user)
            throws SmackException.NotLoggedInException, InterruptedException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            SmackException.NoResponseException, XmppStringprepException {
        addContact(XMPPUtils.fromUserNameToJID(user.getLogin()));
    }

    public Presence.Type getStatusFromContact(User user) {
        return getStatusFromContact(user.getLogin());
    }

    public Presence.Type getStatusFromContact(String name) {
        try {
            HashMap<Jid, Presence.Type> buddies = getContacts();

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
        HashMap<Jid, Presence.Type> buddies = getContacts();
        return buddies.containsKey(jid);
    }

    public boolean hasContactSubscriptionPending(Jid jid)
            throws SmackException.NotLoggedInException, InterruptedException,
            SmackException.NotConnectedException {
        HashMap<Jid, Presence.Type> buddies = getContactsWithSubscriptionPending();
        return buddies.containsKey(jid);

    }

}

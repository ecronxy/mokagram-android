package org.telegram.messenger.mokagram;

import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_stories;

public final class GhostRequests {
    private GhostRequests() {
    }

    public static boolean suppress(TLObject request) {
        if (request instanceof TL_account.updateStatus) {
            if (MokagramSettings.hides(GhostPolicy.Event.ONLINE)) {
                ((TL_account.updateStatus) request).offline = true;
            }
            return false;
        }
        if (request instanceof TLRPC.TL_messages_setTyping) {
            TLRPC.SendMessageAction action = ((TLRPC.TL_messages_setTyping) request).action;
            return !(action instanceof TLRPC.TL_speakingInGroupCallAction)
                    && !(action instanceof TLRPC.TL_sendMessageCancelAction)
                    && MokagramSettings.hides(GhostPolicy.Event.TYPING);
        }
        if (request instanceof TLRPC.TL_messages_setEncryptedTyping) {
            return ((TLRPC.TL_messages_setEncryptedTyping) request).typing
                    && MokagramSettings.hides(GhostPolicy.Event.TYPING);
        }
        if (request instanceof TL_stories.TL_stories_readStories
                || request instanceof TL_stories.TL_stories_incrementStoryViews) {
            return MokagramSettings.hides(GhostPolicy.Event.STORY);
        }
        if (request instanceof TLRPC.TL_messages_readHistory
                || request instanceof TLRPC.TL_channels_readHistory
                || request instanceof TLRPC.TL_messages_readDiscussion
                || request instanceof TLRPC.TL_messages_readEncryptedHistory
                || request instanceof TLRPC.TL_messages_readMessageContents
                || request instanceof TLRPC.TL_channels_readMessageContents
                || request instanceof TLRPC.TL_messages_readMentions
                || request instanceof TLRPC.TL_messages_readReactions) {
            return MokagramSettings.hides(GhostPolicy.Event.READ);
        }
        return false;
    }
}

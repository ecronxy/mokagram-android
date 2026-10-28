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
            long dialogId = dialogIdOf(request);
            return dialogId == 0 ? MokagramSettings.hides(GhostPolicy.Event.READ) : MokagramSettings.hidesRead(dialogId);
        }
        return false;
    }

    private static long dialogIdOf(TLObject request) {
        TLRPC.InputPeer peer = null;
        if (request instanceof TLRPC.TL_messages_readHistory) {
            peer = ((TLRPC.TL_messages_readHistory) request).peer;
        } else if (request instanceof TLRPC.TL_messages_readDiscussion) {
            peer = ((TLRPC.TL_messages_readDiscussion) request).peer;
        } else if (request instanceof TLRPC.TL_messages_readMentions) {
            peer = ((TLRPC.TL_messages_readMentions) request).peer;
        } else if (request instanceof TLRPC.TL_messages_readReactions) {
            peer = ((TLRPC.TL_messages_readReactions) request).peer;
        } else if (request instanceof TLRPC.TL_channels_readHistory) {
            TLRPC.InputChannel channel = ((TLRPC.TL_channels_readHistory) request).channel;
            return channel == null ? 0 : -channel.channel_id;
        } else if (request instanceof TLRPC.TL_channels_readMessageContents) {
            TLRPC.InputChannel channel = ((TLRPC.TL_channels_readMessageContents) request).channel;
            return channel == null ? 0 : -channel.channel_id;
        }
        if (peer == null) {
            return 0;
        }
        if (peer.user_id != 0) {
            return peer.user_id;
        }
        if (peer.chat_id != 0) {
            return -peer.chat_id;
        }
        return peer.channel_id != 0 ? -peer.channel_id : 0;
    }
}

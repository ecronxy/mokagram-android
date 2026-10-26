# Mokagram Android

This is an independent Android port of Mokagram, based on DrKLO/Telegram.

- Keep application integration in `Mokagram/`, feature logic in `TMessagesProj/src/main/java/org/telegram/messenger/mokagram/`, and custom screens in `org/telegram/ui/mokagram/`.
- Prefer small upstream integration calls; preserve upstream history and license notices.
- Read `docs/PORTING.md` before claiming desktop feature parity. Update it when implementing or validating a feature.
- Use Debug by default and Release when explicitly requested. Use `scripts/build-apk.sh`; never publish API credentials, account data, signing keys, or local configuration.
- Run `scripts/test-logic.sh` and compile affected Android code. A Java compile does not validate native linking or APK runtime.
- Preserve `MOKAGRAM_NOTICE.md` and visible author attribution in README and the About screen.
- Keep UTF-8 text without BOM and LF line endings. Follow neighboring Android Java style.

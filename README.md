# Mokagram for Android

Отдельный Android-клиент на основе [Telegram for Android](https://github.com/DrKLO/Telegram), с переносом логики [Mokagram Desktop](https://github.com/kapustaltd/mokagram).

**Основано на mokaGram, автор [@ecronx](https://t.me/ecronx). https://github.com/ecronxy/mokagram**

Адаптация для Android: kapustaltd. Это изменённая версия исходников, не официальный клиент Telegram. Уведомление об авторстве: [MOKAGRAM_NOTICE.md](MOKAGRAM_NOTICE.md).

## Состояние проекта

Первый этап переноса, не полная копия desktop-версии. Реализованы отдельный пакет, экран настроек Mokagram и начальная логика «Призрака»: скрытие прочтений, статуса онлайн, набора текста и просмотров историй, расписание через полночь. Настройки сохраняются локально, действуют на все аккаунты и по умолчанию выключены главным переключателем.

Поведение на устройстве требует проверки; до неё не следует полагаться на «Призрак» для конфиденциальности. Известные отличия и полный перечень модулей: [docs/PORTING.md](docs/PORTING.md).

## APK

Собственный модуль приложения — `Mokagram`, пакет Debug — `ltd.kapusta.mokagram.debug`. Он устанавливается рядом с Telegram. Первая конфигурация рассчитана на **arm64-v8a**, Android 6.0 и новее.

Инструкции: [docs/BUILD.md](docs/BUILD.md). По запросу владельца используются публичные API-параметры из официальных Android-исходников, одинаковые для Debug и Release. Переопределить их можно в игнорируемом `mokagram.properties` или через переменные окружения. Эти параметры не заменяют собственную регистрацию приложения и могут ограничиваться сервером Telegram.

```bash
./scripts/build-apk.sh debug
# Release: настройте постоянный ключ подписи по docs/BUILD.md.
./scripts/build-apk.sh release
```

Готовый Release APK после успешной сборки: `Mokagram/build/outputs/apk/release/Mokagram-release.apk`. Release-пакет — `ltd.kapusta.mokagram`, отладочный — `ltd.kapusta.mokagram.debug`.

GitHub Actions проверяет код при push/PR. Ручной запуск workflow **Android** собирает выбранный вариант APK. Для Release нужны Secrets подписи; API-параметры можно переопределить через Secrets `TELEGRAM_API_ID` и `TELEGRAM_API_HASH`. Debug-сборки CI подписываются временным ключом runner: для обновления поверх предыдущей CI-сборки понадобится постоянный ключ; до его настройки может потребоваться переустановка с потерей локальных данных. Не используйте такие сборки для постоянного аккаунта без резервирования локальных данных.

## Исходники и лицензии

- Исходная Android-основа: `DrKLO/Telegram`, commit `3de37c8cfb039452be7398909461da6288babf15`.
- Desktop-логика: `kapustaltd/mokagram`, commit `bb203b658d`.
- Android upstream лицензирован GPL v2 или новее: [LICENSE](LICENSE).
- Переносимые доработки mokaGram — GPLv3 с сохранением авторства: [LICENSE-MOKAGRAM](LICENSE-MOKAGRAM), [MOKAGRAM_NOTICE.md](MOKAGRAM_NOTICE.md).
- Исходная инструкция Telegram сохранена в [docs/UPSTREAM_README.md](docs/UPSTREAM_README.md).

## Изменения ecronx (форк)

В этом форке: дизайн в стиле mokaGram Desktop (палитра, скругления, переключатели md3, меню, пузыри без хвостов), замена «Telegram» на «mokaGram» в интерфейсе (кроме Telegram Premium и звёзд), галочки mokaGram через `api.moka.wtf` (список и события SSE, запасной путь через `@mokagent`), строка ID с датой регистрации в профиле, исключения и «Прочитать сейчас» для «Призрака», шрифт Nunito, иконка и имя mokaGram.

Шрифт Nunito: SIL Open Font License 1.1 (https://openfontlicense.org), © The Nunito Project Authors.
Свои `api_id` и `api_hash` положите в игнорируемый `mokagram.properties` (см. `mokagram.properties.example`).

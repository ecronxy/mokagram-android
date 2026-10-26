# Сборка Mokagram APK

## Инструменты

- JDK 17.
- Android SDK Platform 36 и Build Tools 36.0.0.
- NDK 27.2.12479018 и CMake 3.22.1.
- Git с поддержкой submodules; Gradle 8.13 загружает wrapper из репозитория.

```bash
git clone --recursive https://github.com/kapustaltd/mokagram-android.git
cd mokagram-android
# В существующем клоне:
git submodule update --init --recursive --depth 1
export ANDROID_HOME=/path/to/android-sdk
sdkmanager 'platforms;android-36' 'build-tools;36.0.0' 'ndk;27.2.12479018' 'cmake;3.22.1'
```

Исходные native-библиотеки и медиа требуют submodules. Предсобранные статические библиотеки upstream уже находятся в `TMessagesProj/jni/prebuild`.

## Telegram API

Получите собственные параметры на [my.telegram.org](https://my.telegram.org/apps), согласно [официальной инструкции](https://core.telegram.org/api/obtaining_api_id).

По запросу владельца в `gradle/mokagram-api.properties` сохранена публичная пара из официальных Android-исходников (`api_id=4`). Она общая для Debug и Release. Это не гарантирует отсутствие ограничений сервера; Telegram требует собственный API ID для распространения стороннего клиента.

Чтобы переопределить параметры, скопируйте `mokagram.properties.example` в `mokagram.properties` и заполните оба поля, убрав начальные `#`. Файл исключён из Git. Альтернатива — переменные окружения `TELEGRAM_API_ID` и `TELEGRAM_API_HASH`; они имеют приоритет над файлом. Не отправляйте значения в чат и не коммитьте их.

Сборка APK с некорректными параметрами останавливается. Компиляция Java и ресурсов без упаковки допускается без параметров для CI; такой APK не создаётся.

## Debug APK

```bash
./scripts/test-logic.sh
./scripts/build-apk.sh debug --max-workers=4
```

Результат: `Mokagram/build/outputs/apk/debug/Mokagram-debug.apk`. Используется обычная локальная Android debug-подпись, а не чужой upstream keystore. На первом этапе поддерживается arm64-v8a.

В Android Studio откройте корень проекта и выберите модуль `Mokagram`, вариант `debug`. Исходные приложения Telegram исключены из стандартной конфигурации; для их просмотра существует параметр `-PtelegramUpstreamApps=true`, но сборка этих приложений не относится к Mokagram.

## Release APK

Создайте собственный постоянный ключ, например `keytool -genkeypair -keystore /private/path/mokagram-release.jks -alias mokagram -keyalg RSA -keysize 3072 -validity 10000`. Пароли вводите интерактивно.

Скопируйте `mokagram-signing.properties.example` в `mokagram-signing.properties`, укажите абсолютный путь к ключу и пароли. Оба файла с реальными данными должны оставаться вне Git. Сохраните ключ и пароли: без них нельзя обновить уже установленную Release-сборку.

```bash
./scripts/build-apk.sh release --max-workers=2
```

Результат: `Mokagram/build/outputs/apk/release/Mokagram-release.apk`, пакет `ltd.kapusta.mokagram`. Release отключает отладку, использует R8 и сжатие ресурсов. Локальный первоначальный ключ хранится вне репозитория в `~/.local/share/mokagram-android/signing/`, параметры — в игнорируемом `mokagram-signing.properties` с правами `600`.

На машине первоначальной настройки JDK установлен через mise: `mise exec java@temurin-17 -- ./scripts/build-apk.sh release --no-daemon --max-workers=2 -Dorg.gradle.jvmargs=-Xmx5g`. SDK задан в игнорируемом `local.properties`.

## GitHub Actions

В Settings → Secrets and variables → Actions репозитория для Release нужны `MOKAGRAM_KEYSTORE_BASE64` (base64 ключа JKS) и `MOKAGRAM_KEYSTORE_PASSWORD` (пароль хранилища и ключа, alias `mokagram`). Опциональные `TELEGRAM_API_ID` и `TELEGRAM_API_HASH` переопределяют публичные API-параметры.

Затем Actions → Android → Run workflow → выберите `release` или `debug`. После успешной сборки скачайте artifact `mokagram-arm64-release` или `mokagram-arm64-debug`.

Автоматический job `verify` выполняет тесты логики и компиляцию Java/ресурсов без API-параметров. Ручной job `apk` собирает native-код и APK. Успешный `verify` сам по себе не подтверждает сборку APK и работу приложения на устройстве.

Firebase push, Google Maps и Play Integrity не настроены на новый пакет. Upstream JSON и ключи не являются конфигурацией Mokagram. Для полноценной поддержки этих сервисов нужна собственная регистрация; Google Services plugin в стандартной сборке отключён.

# SlayLauncher — независимая неофициальная сборка

Полностью переименованный форк open-source лаунчера Minecraft (GPL-3.0 + доп. условия upstream).
Название, package, JNI-символы, тема и иконки заменены — интерфейс не повторяет оригинал.

## Что изменено

| Область | Изменение |
|---|---|
| Имя | `SlayLauncher`, короткое имя `SL`, `launcher_name` / `app_name` в `gradle.properties` |
| Package | `com.slaylauncher` везде: Kotlin/Java-исходники, `namespace`, `applicationId`, JNI-функции (`Java_com_slaylauncher_...`), пути `com/slaylauncher/...` |
| Подпись | Свой ключ `SlayLauncher/slaylauncher.jks`, алиас `slaylauncher`; debug-ключ `slaylauncher_debug.jks` |
| Тема | Принудительная тёмная палитра в стиле современных лаунчеров (`SlayPalette`): фон #161718, поверхности #1E1F20/#26272A, зелёный акцент #5EC64D. Панели — скошенные «пластины» с фасками, жёстким верхним бликом, тёмным низом и пиксельными точками (`SlayCutShape` / `slayPixelSurface`) |
| Логотип | Векторные «скрещённые клинки» вместо исходной маско́ты: `slay_logo.xml`, `ic_launcher_foreground.xml`, monochrome, splash |
| Главный экран | Нижняя навигация из 5 вкладок (Играть / Сборки / Каталог / Серверы / Ещё) с «поднимающейся» активной вкладкой. Основной экран перестроен как лобби: слева рельс со списком всех установленных версий и аккаунтом, справа — большой блок активной сборки с кнопкой PLAY и селектором версии, полка «Ваши сборки» с карточками версий и плитки быстрого перехода. Все панели — скошенные пластины с фасками, без неона |
| Настройки | Боковое меню в виде скошенных пластин с зелёным выделением активного раздела; карточки настроек используют `SlayPlateShape` с фасками по углам вместо скруглений Material |
| Вдохновение | Визуальный язык вдохновлён открытыми проектами Millida Launcher (GPL-3.0) и Horizon/Starling (MIT): тёмные скошенные панели, зелёный акцент, рельс версий, лобби с крупной кнопкой запуска, нижний док. Код, иконки и бренды не использованы — всё реализовано заново под именем SlayLauncher |
| Предупреждение | Карточка «Unofficial modified build» на главном экране + строки в `strings.xml` |
| NDK | Условная версия: `27.1.12297006` на `aarch64`, `25.2.9519653` на остальных хостах |

## Сборка

В этой среде используется ARM64 SDK/NDK (`android-sdk-arm64`), так как хост `aarch64`.

```bash
bash /tmp/opencode/build.sh
```

или вручную:

```bash
export ANDROID_HOME=/path/to/android-sdk-arm64
export ANDROID_SDK_ROOT=$ANDROID_HOME
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/27.1.12297006
export PATH="/root/.cargo/bin:$ANDROID_HOME/platform-tools:$PATH"
./gradlew :SlayLauncher:assembleDebug   # или assembleRelease
```

APK: `SlayLauncher/build/outputs/apk/debug/*.apk`.

### Требования

- Android SDK Platform 37.2, Build Tools, NDK (см. выше), Rust + `cargo-ndk`
- Цели Rust: `rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android`

## Подпись release-сборки

Ключ: `SlayLauncher/slaylauncher.jks`, алиас `slaylauncher`.
Пароли лежат в gitignored-файлах `SlayLauncher/.store_password.txt` и `.key_password.txt`.

Перед публикацией:

- **не публикуйте** `slaylauncher.jks` и файлы с паролями;
- замените `url_home` в `SlayLauncher/gradle.properties` на ссылку на свой сайт/репозиторий;
- замените `default_store_password` / `default_key_password` в `gradle.properties` на свои значения;
- опубликуйте исходники модификации под GPL-3.0 (обязательное условие лицензии).

## Лицензия

Исходный код остаётся под GPL-3.0. Copyright-уведомления авторов сохранены во всех файлах.
Это независимая модификация: она не разработана и не поддерживается оригинальными авторами.

## Публикация на GitHub

```bash
git init
git remote add origin https://github.com/<вас>/<репозиторий>.git
git add .
git commit -m "SlayLauncher: independent unofficial build"
git push -u origin main
```

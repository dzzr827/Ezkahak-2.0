# Получение JAR через GitHub Actions

В текущей среде нет Gradle и сетевого доступа для загрузки Fabric/Minecraft-зависимостей. Поэтому добавлен workflow: GitHub Actions установит JDK 21 и Gradle 8.14.3, выполнит сборку и приложит JAR как artifact.

1. Создай пустой репозиторий GitHub.
2. Распакуй `EzkaHak_2_0_Fabric_Project_GitHub_Build.zip` и загрузи **содержимое** папки `EzkaHak_2_0` в корень репозитория, включая скрытую папку `.github`.
3. Открой вкладку **Actions** и разреши запуск GitHub Actions, если GitHub попросит.
4. Выбери **Build EzkaHak 2.0 Fabric mod** → **Run workflow**.
5. Если сборка завершится успешно, открой завершённый запуск и скачай artifact `ezkahak-2.0-fabric-1.21.11`.
6. Внутри архива будет JAR из `build/libs`. Для установки нужны Minecraft 1.21.11, Fabric Loader и Fabric API.

Если workflow завершится ошибкой, открой лог шага **Build Fabric mod**: там будет точная ошибка компиляции или зависимостей. Успешная сборка ещё не означает, что мод проверен в игре — запуск нужно протестировать отдельно.

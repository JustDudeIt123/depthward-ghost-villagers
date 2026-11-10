# Исходники релиза 1.1.3

Minecraft 1.20.1, Forge, Java 17. Опубликовано 10 ноября 2026.
Снимок исходников и ресурсов модуля `depthward-ghost-villagers` из пакета
`DepthWard-CurseForge-2026-11-10.zip`.

## Сборка

Используйте Java 17 и `gradlew.bat build` либо `sh gradlew build`.
Gradle wrapper загружает Gradle через сеть. Точные версии зависимостей указаны
в build.gradle. Для модулей с flatDir зависимости ожидаются в соседнем каталоге
`../mods/`: загрузите сторонние моды с CurseForge или Modrinth, собственные
зависимости соберите отдельно и поместите туда под ожидаемыми именами.
Чтобы сохранить исходное расположение проектов, клонируйте репозитории рядом.
Boss Scaling поддерживает Maven-зависимости либо `-PlocalMods=<путь>`.

Собственные зависимости:
- Stamina: https://github.com/JustDudeIt123/depthward-stamina
- Lanterns: https://github.com/JustDudeIt123/depthward-lanterns
- Compat: https://github.com/JustDudeIt123/depthward-mod-compat

## Проверки и происхождение

Ресурсы src/main/resources побайтно совпадают с подготовленным релизным JAR.
Boss Scaling компилировался локальным инструментом. Остальные четыре JAR
подготовлены с сохранением прежнего установленного байткода и refmap;
полная Gradle-пересборка и совпадение байткода с этими исходниками не подтверждены.
Игровые и GameTest прогоны новых релизов не выполнялись.
Исторические результаты в README не являются проверкой этого релиза.

Лицензии и авторские уведомления сохранены. Для Boss Scaling исходный автор —
sixik, изменения DepthWard — justthekingg, лицензия LGPL v3.
Для остальных модулей действуют собственные LICENSE (All Rights Reserved).

Общий снимок с инструментами локальной подготовки и исторической документацией:
https://github.com/JustDudeIt123/DepthWard/tree/f98d06252834e364e125473ac76f9444a46c1a0b
Старые ссылки README на общую структуру сборки относятся к этому снимку.

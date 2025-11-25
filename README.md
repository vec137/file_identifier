<h1>Программа для восстановления расширения файла по сигнатуре (магическим числам)</h1>

Программа тестировалась на:
- NixOS unstable с openjdk "21.0.9" 2025-10-21
- Windows 11 24H2 с Oracle JDK "21.0.9" 2025-10-21 LTS

Использованные библиотеки:
- log4j-api:2.24.1
- log4j-core:2.24.1
- junit-jupiter-api:5.11.3
- junit-jupiter-engine:5.11.3

Для сборки:
```
./gradlew build
```
Для запуска:
```
java -jar ./build/libs/file-identifier.jar <файл>
```
Для тестов:
```
./gradlew test
```
Для генерации документации:
```
./gradlew javadoc
```
Включить private методы в документацию:
```
./gradlew javadoc -P fullDocs
```

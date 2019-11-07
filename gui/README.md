## JavaFX + SpringBoot + Maven project template

At the start of each new JavaFX project, you have to create a framework from scratch, or, more often, cut functionality from an existing project.

This solution allows you to immediately begin development. Its key features:

- Integration Maven, JavaFX, Spring Boot
- Configure Liquibase + database
- Java 8
- Template for showing alerts
- Template for opening a window

### Possible questions

- Why Maven? Because in fact for most projects this is the best solution. Gradle, with its rich capabilities, is more difficult to configure.
- Why JavaFX? Because it is a great solution for creating windowed applications, leaving behind legacy solutions: AWT, Swing, and others.
- Why Spring Boot? Because it really simplifies creating a project based on JavaFX. Try without it to make a difference.
- Why Liquibase + H2? That bunch worked in the project from which the current one was assembled. I did not want to remove this solution, the database is used quite often.
- Why Java 8? Just because I now have all the current Java 8 projects. JavaFX 11 is much richer, so if nothing restricts, then go for it.

## Шаблон проекта JavaFX + SpringBoot + Maven

При старте каждого нового проекта на JavaFX приходится с нуля создавать каркас, или, что чаще бывает, вырезать функциональность из существующего проекта.

Данное решение позволяет сразу начать разработку. Его ключевые особенности:

- Интеграция Maven, JavaFX, Spring Boot
- Настройка Liquibase +  база данных
- Java 8 (просто больше пока не потребовалось)
- Заготовка для показа оповещения
- Заготовка для открытия окна

### Возможные вопросы

- Почему Maven? Да потому что по факту для большинства проектов это лучшее решение. Gradle, обладая богатыми возможностями, сложнее в настройке.
- Почему JavaFX? Потому что это отличное решение для создания оконных приложений, оставляющее позади устаревшие решения: AWT, Swing, и другие.
- Почему Spring Boot? Потому что он реально упрощает создания проекта на основе JavaFX. Попробуйте без него, чтобы почувствовать разницу.
- Почему Liquibase + H2? та связка работала в проекте, из которого был собран текущий. Не хотелось убирать это решение, база данных используется достаточно часто.
- Почему Java 8? Просто лишь потому что сейчас у меня все текущие проекты на Java 8. JavaFX 11 куда богаче, так что, если ничего не ограничивает, то переходите на него.

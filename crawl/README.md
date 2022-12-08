## TODO List

Список задач

сохраняет картинки в moby/pages/dos/...
по-идее, надо в moby/cache/dos/...

после reload games list не обновляется количество игр в основной таблице, надо перезайти на страницу

если уже назначена игра, то она не убирается

надо очень внимательно протестировать game binding, такое впечатление, что глючит временами

если второй раз открыть биндинг, то будут данные предыдущих игр

будут проблемы с открытием комбинированных игр, шерлок для СГ1000, игры CD/32x,...
надо открывать по маске sms-.*-binding.json

надо ругаться, если у игр нет ЧПУ (Sega CD)

биндинг - хранить биндинги базы в одном файле (ид платформа ид)

выделять нужны платформы, потом бинд.
при выборке не брать транзитивные платформы

в базу добавить driver. По дефолту это название платформы. Если же указан, то применяется он.
Это поможет с шерлоком для SG/SMS, Phantasy Star для MD, возможно для дисковых игр для SC3000
Так же это должно помочь с аркадными играми


//TODO
генерить список игр, что надо создать, может есть смысл делать сразу SQL


//TODO искать по первым буквам - не приоритетно
//TODO при возврашещении в список платформ чекбоксы сбрасываются - не приоритетно

https://github.com/kwhat/jnativehook


### UI

постараться переписать под https://github.com/AdamBien/afterburner.fx

https://stackoverflow.com/questions/30274267/component-constructor-arguments-fxml-javafx
https://stackoverflow.com/questions/34785417/javafx-fxml-controller-constructor-vs-initialize-method
https://community.oracle.com/tech/developers/discussion/2529134/how-to-have-constructor-with-arguments-for-controller
https://stackoverflow.com/questions/14187963/passing-parameters-javafx-fxml
https://stackoverflow.com/questions/48173320/javafx-fxml-parameter-passing-from-controller-a-to-b-and-back/48217255#48217255
https://github.com/AdamBien/afterburner.fx
https://github.com/AdamBien/followme.fx/tree/master/src/main/java/com/airhacks/followme/dashboard
https://docs.oracle.com/javase/8/javafx/api/javafx/fxml/doc-files/introduction_to_fxml.html

* нужно версионирование, сравнивать с существующими, при перечтении



//TODO
// brokenImages - сохранять причину - 404, либо сломанная, короче причину.


// TODO
// бывают картинки с неверным расширением. JPG могут быть PNG и так далее
// необходимо добавить поле куда сохранять правильное расширение.

// TODO credits - переводить японские имена
// https://stackoverflow.com/questions/8147284/how-to-use-google-translate-api-in-my-java-application

//TODO читать историю
// /stats/recent_entries
// /stats/recent_entries/offset,0/so,2d/
// /stats/recent_modifications
// /stats/recent_reviews

## Алгоритмы сравнения текста

* https://stackoverflow.com/questions/955110/similarity-string-comparison-in-java
* https://softwareengineering.stackexchange.com/questions/330934/what-algorithm-would-you-best-use-for-string-similarity


## Access

* https://jackcess.sourceforge.io/cookbook.html#Reading_a_Table
* https://mvnrepository.com/artifact/com.healthmarketscience.jackcess/jackcess/4.0.1
* https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/similarity/package-summary.html



## Интересные сайты

* http://tv-games.ru/games/index.html
* https://emu-russia.net/ru/romz/
* http://www.emu-land.net/consoles/dendy/roms/rating
* https://retrowith.in/browse.php?cat[35]=1&incldead=1&search=gamebase
* https://www.t2e.pl/t2e-download/gamebase.706
* https://minirevver.weebly.com/
* https://platformadventure.weebly.com/
* https://www.uvlist.net/game-7879-Ikari+Warriors
* https://www.mobygames.com
* https://www.igdb.com/games/ikari-warriors
* https://www.giantbomb.com/ikari-warriors/3030-1619/
* https://www.giantbomb.com/pachiokun/3005-32373/games/
* https://videogamegeek.com/videogameversion/68700/us-atari-7800-edition
* http://www.hardcoregaming101.net/american-dream/
* https://superfamicom.org/famicom/info/american-dream
* https://videogamecritic.com/
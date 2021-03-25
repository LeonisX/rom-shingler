# TODO list

Нужно импортирование семей из других коллекций (той же платформы).

Предупреждать если пытается пережать merged коллекцию. Давать совет - распаковать либо переключиться на plain

Это для альтернативной версии - реализовать как лёгкую замену для текущей.

Не высчитывать среднее отношение, в семье связи только с лидером.
Таким образом добавление и удаление будет мгновенным.

Вот бы ещё что-то с поиском лучшей семьи сделать...

метки у РОМов - автоматически присвояемые, например, уникальные, которые надо обязательно сохранять, или mame
возможность назначать, снимать

Галочка - пропускать существующие архивы. В случае ошибки или прерывания операции упаковки поможет. Главное проверять целостность существующих (галочка), или хотя бы чтобы не 0

Сделать все связи по ID. Это устранит лишний код и проблемы в случае переименования семей как минимум.

Если шингла нет, то не выбрасывается ошибка, она проглатывается и непонятно почему не считается ближайшая семья

Встречаются игры, у которых в конце - или +, после тегов. Это следует игнорировать

Compress - показывает не 100%. Видимо считает семьи

При сжатии игр по одной, в особенности хаков SMB создаётся впечатление что для каждого из них архив распаковывается заново

При разбивке архивов есть смысл узнавать упакованный размер файлов, и делить по этому признаку.

Lunar FMV Test for BSNES (PD).smc: 64 Mb
-Xmx14000m

Понижать важность левых игр по экспоненте. Если много хаков, то они ломают всё.

Подумать про графики.

Разгружать память перед архивированием.

Создавая список уникальных игр, предпочитать T+ над T-
Предпочитать английские переводы над фр, испанскими, итд

Было бы круто ещё и вручную отмечать РОМы, которые 100% уникальные, так как есть исключения, например, не запускаются в эмуляторе, либо 2 одноимённые но разные игры

Если ищутся лучшие семьи и свернуть-развернуть список, то всё теряется, и даже новые вычисления не добавляются


2021-03-23 01:05:44,128 INFO  [md.leonis.shingler.utils.ArchiveUtils:25] Compressing: Unholy Night: The Darkness Hunter [1]|82.01672862453532
Exception in thread "Thread-38" java.nio.file.InvalidPathException: Illegal char <:> at index 12: Unholy Night: The Darkness Hunter.7z


Надо будет потом пересчитать все шинглы, в том числе и для других платформ

При добавлении в семью автоматически предлагать выделенную (либо подбирать по имени игры)

Кнопка аудита - выводить те архивы, в которых основной РОМ по названию не соответствует названию семьи

Кандидат в семью - тут видимо перебор идёт по всем образам, а лучше это делать по лучшему представителю семьи
Для большей точности, потом брать, к примеру, первые 3-5 семей и уже сравнивать всех подряд

Баг - когда нахожу подходящую семью для игры слева, и она одна в своей семье, то она переходит (вроде как), но семья не удаляется

Баг - orphaned games: -8. Вероятно после пересчёта поменяется

Семью должен представлять лучший, то есть b, o, h, t только в крайнем случае

Родных надо считать параллельно, до 20% всего занято процессора

Несколько потоков выполнения разных задач, прерывание предыдущих одинаковых, возможность останавливать любую задачу.
Надо видеть прогресс каждой из них

Кнопка Save Relations горит даже тогда, когда их нет, но она неактивна

// SELECT sid, name, cpu, game, rom FROM `base_nes` ORDER BY n LIMIT 0, 50000
// SELECT sid, name, '' as cpu, '' as game, '' as rom FROM `base_sg1000` ORDER BY n LIMIT 0, 50000

TiVi:

- Don't allow CPU with "_" (validate when generate SQL queries)

- Generate htmls based on excel files

- При повторном проходе необходимо обновлять ссылки. Это очень актуально для больших баз, например NES,
поскольку группируем по 1000, и в зависимости от количества игр игры, хаки, pd могут менять свою группу.
also dont overwrite name, cpu if have games
Ещё лучше отдельный шаг для этого сделать

- Create - act = 'yes'

- All bases - revise regions, names!!!

- Full refactor TIVI stuff, other new code

- Full process for goodmerged (compress, tivi stuff)

- Fix bug SNES - can't find:
2020-01-08 14:38:11,926 WARN  [md.leonis.shingler.utils.TiviUtils:685] Game isn't found: Public Domain+Public Domain (Slide Shows); Public_Domain-Public_Domain_Slide_Shows.7z
2020-01-08 14:41:07,947 WARN  [md.leonis.shingler.utils.TiviUtils:685] Game isn't found: SMW hacks; SMW_hacks.7z
They are separated by few parts
Can't right assign "splitted" games in XLS files 


Bugs:
После создания GoodMerged группы в ней есть семьи, но их приходится генерировать вручную потом

html файл для SNES - SMW hacks (part 1).7z	300299 Kb
                     SMW hacks (part 1).7z	300299 Kb
                     SMW hacks (part 1).7z	300299 Kb
                     SMW hacks (part 1).7z	300299 Kb
                     SMW hacks (part 1).7z	300299 Kb
                     SMW hacks (part 1).7z	300299 Kb
                     SMW hacks (part 1).7z	300299 Kb
                     Public Domain+Public Domain (Slide Shows) (part 1).7z	177157 Kb
                     Public Domain+Public Domain (Slide Shows) (part 1).7z	177157 Kb
                     Public Domain+Public Domain (Slide Shows) (part 1).7z	177157 Kb
                     Public Domain+Public Domain (Slide Shows) (part 1).7z	177157 Kb
 

CODE:

- Change output directory for archives

- Compress in parallel: lzma1 1

- При стандартной упаковке паковать как есть. при продвинутой для плохих жаккартов искать семьи поудачнее.

- Families with sha1 or other id!!!
- When find games - use SHA1 as key if have

- Export families to other precisions

- Process all TODOs in code

- When open platforms - full clean families, relations

- Repair parallel family calculation

- 99,99% when calculate shingles

UI:

- Show correct error when shingle is not found (use Executors)

- Find in families

- Red/black for tribes

- When press "manage families" - run in bg + window (please wait)

- Collections list - update on key pressing

- Right click menu for all

- New family, ... -> show that need calculate relations!!!

- Drag-drop
  
- Images for families


LOGS:

- Disable logging
- Clear logs

- Show remaining time

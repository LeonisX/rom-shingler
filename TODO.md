# TODO list

// SELECT sid, name, cpu, game, rom FROM `base_nes` ORDER BY n LIMIT 0, 50000
// SELECT sid, name, '' as cpu, '' as game, '' as rom FROM `base_sg1000` ORDER BY n LIMIT 0, 50000

TiVi:

- Generate htmls based on excel files

- При повторном проходе необходимо обновлять ссылки. Это очень актуально для больших баз, например NES,
поскольку группируем по 1000, и в зависимости от количества игр игры, хаки, pd могут менять свою группу.
also dont overwrite name, cpu if have games
Ещё лучще отдельный шаг для этого сделать

- Create - act = 'yes'

- All bases - revise regions, names!!!

- Full refactor TIVI stuff, other new code

- Full process for goodmerged (compress, tivi stuff)

- Fix bug SNES - can't find:
2020-01-08 14:38:11,926 WARN  [md.leonis.shingler.utils.TiviUtils:685] Game isn't found: Public Domain+Public Domain (Slide Shows); Public_Domain-Public_Domain_Slide_Shows.7z
2020-01-08 14:41:07,947 WARN  [md.leonis.shingler.utils.TiviUtils:685] Game isn't found: SMW hacks; SMW_hacks.7z
They are separated by few parts
Can't right assign "splitted" games in XLS files 


CODE:

- Change output directory for archives

- Compress in parallel: lzma1 1

- При стандартной упаковке паковать как есть. при продвинутой для плохих жаккартов искать семьи поудачнее.

- Name hashCode collizions. Need separate unique ID

- Families with sha1 or other id!!!
- When find games - use SHA1 as key if have

- Export families to other precisions

- Process all TODOs in code

- When open platforms - full clean families, relations

- Repair parallel family calculation


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

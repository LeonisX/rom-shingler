# TODO list

// SELECT sid, name, cpu, game, rom FROM `base_nes` ORDER BY n LIMIT 0, 50000
// SELECT sid, name, '' as cpu, '' as game, '' as rom FROM `base_sg1000` ORDER BY n LIMIT 0, 50000

TODO group NES games by mapper

TODO generate htmls based on excel files
TODO regenerate html, replace on sites

TODO при повторном проходе необходимо обновлять ссылки. Это очень актуально для больших баз, например NES,
поскольку группируем по 1000, и в зависимости от количества игр игры, хаки, pd могут менять свою группу.
also dont overwrite name, cpu if have games
Ещё лучще отдельный шаг для этого сделать

TODO create - act = 'yes'

TODO all bases - revise regions, names!!!

TODO when open platforms - full clean families, relations

TODO full refactor TIVI stuff, other new code

TODO red/black for tribes

TODO change output directory for archives

TODO compress in parallel: lzma1 1/2

TODO при стандартной упаковке паковать как есть. при продвинутой для плохих жаккартов искать семьи поудачнее.

TODO families with sha1 or other id!!!
TODO when find games - use SHA1 as key if have

Export families to other precisions

TODO full process for goodmerged (compress, tivi stuff)

Find in families

TODO when press "manage families" - run in bg + window (please wait)

TODO collections list - update on key pressing

TODO process all TODOs in code

* Right click menu for all

TODO new family, ... -> show that need calculate relations!!!

  drag-drop
  
  * Images for families

* Disable logging
* Clear logs
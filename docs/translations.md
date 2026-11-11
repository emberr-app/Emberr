# Translations

Emberr is available in English plus 17 languages.

## Files

- App text: `shared/src/commonMain/composeResources/values/strings_<area>.xml`
- Translations: the same file in `values-<lang>/`
- Widgets and Android notifications: `shared/src/androidMain/res/values/strings.xml` (and `values-<lang>/`)
- Emoji search names: `composeResources/files/emoji-names/<lang>.json`

Keys start with their area (`calendar_`, `editor_`). Common words like Save and Cancel are in `strings_common.xml`.

Three special folders:

- `values-pt` is Brazilian Portuguese. `values-pt-rPT` only has the phrases that are different in Portugal.
- `values-in` must be an exact copy of `values-id`. Android still uses the old code `in` for Indonesian.
- `values-zh` is Simplified Chinese.

## Adding a phrase

1. Add it in English to the right `strings_<area>.xml`. Don't put text in Kotlin.
2. Use it with `stringResource(Res.string.key)`, or `pluralStringResource(...)` for counts.
3. Add the translation to every language folder.
4. Run `python3 -I scripts/check-translations.py`. It should say `0 problems`. GitHub runs it on every PR too.

If you change what an English phrase means, update its translations too.

## Rules

- Write full sentences with placeholders (`%1$s`, `%1$d`). Don't build a sentence from pieces.
- Use `<plurals>` for counts. Don't add an "s" in code.
- Apostrophes:
  - In `composeResources` files, write `'` plain. `\'` shows up on screen with the backslash.
  - In Android `res` files, write `\'`. A plain `'` breaks the build.
- Keep brand names as they are (Emberr, TMDB, WebDAV, GitHub, Markdown…).
- If English ends with `...`, the translation must too.
- Keep translations short. Buttons and menus don't have much room.
- Don't write month or day names into phrases. The app gets them from the system.
- Names saved into notes ("Untitled", "Inbox", "Table") stay English in the database. The app translates them on screen.
- Formula function names (`sum()`, `prop()`) stay English.

## Plurals

| Languages | Write these |
|---|---|
| es, pt, fr, it, de, nl, tr, hi | `one`, `other` |
| pl, uk, ru | `one`, `few`, `many`, `other` |
| ja, ko, zh, vi, id | `other` |

In Russian, Ukrainian and Hindi, `one` is also used for other numbers (21, 31… or 0), so always keep the number in it.

## Tone

| Language | Say "you" as | Notes |
|---|---|---|
| Spanish | tú | Latin-American |
| Portuguese (Brazil) | você | celular, tela, arquivo |
| Portuguese (Portugal) | tu | telemóvel, ecrã, ficheiro |
| French | vous | |
| Italian | tu | |
| German | du | |
| Dutch | je | |
| Polish | ty | |
| Ukrainian | ви | |
| Russian | вы | |
| Turkish | polite | |
| Indonesian | Anda | |
| Vietnamese | bạn (or none) | |
| Hindi | आप | |
| Japanese | none | です/ます in sentences |
| Korean | none | 해요 style |
| Chinese | 你 (or none) | |

## Glossary

Use the same word for the same thing everywhere.

### Spanish, Portuguese (Brazil), French, Italian

| English | es | pt (Brazil) | fr | it |
|---|---|---|---|---|
| note | nota | nota | note | nota |
| sub-note | subnota | subnota | sous-note | sottonota |
| space (workspace) | espacio | espaço | espace | spazio |
| folder | carpeta | pasta | dossier | cartella |
| Daily / daily note / daily page | Diario / nota diaria / página diaria | Diário / nota diária / página diária | Quotidien / note du jour / page du jour | Diario / nota del giorno / pagina del giorno |
| block | bloque | bloco | bloc | blocco |
| canvas | lienzo | canvas | canevas | lavagna |
| database (block) | base de datos | banco de dados | base de données | database |
| table (simple) | tabla | tabela | tableau | tabella |
| view (database/watchlist) | vista | visualização | vue | vista |
| Board (view) | Tablero | Quadro | Kanban | Bacheca |
| row / column / cell | fila / columna / celda | linha / coluna / célula | ligne / colonne / cellule | riga / colonna / cella |
| property | propiedad | propriedade | propriété | proprietà |
| template | plantilla | modelo | modèle | modello |
| toggle list | lista desplegable | lista expansível | liste dépliable | elenco a scomparsa |
| callout | destacado | destaque | encadré | riquadro |
| bookmark (web) | marcador | marcador | signet | segnalibro |
| Favorites / Add to Favorites | Favoritos | Favoritos | Favoris | Preferiti |
| Pinned | Fijado(s) | Fixado(s) | Épinglé(s) | Fissato/i |
| Trash / Move to Trash | Papelera / Mover a la papelera | Lixeira / Mover para a lixeira | Corbeille / Placer dans la corbeille | Cestino / Sposta nel cestino |
| Inbox | Bandeja de entrada | Caixa de entrada | Boîte de réception | In arrivo |
| sticky note | nota adhesiva | nota adesiva | note adhésive | nota adesiva |
| slash menu / commands | menú de barra / comandos | menu de barra / comandos | menu barre oblique / commandes | menu barra / comandi |
| mention | mención | menção | mention | menzione |
| reminder | recordatorio | lembrete | rappel | promemoria |
| task / to-do | tarea | tarefa | tâche | attività |
| event | evento | evento | événement | evento |
| all-day | todo el día | dia inteiro | toute la journée | tutto il giorno |
| category (calendar) | categoría | categoria | catégorie | categoria |
| repeat / recurring | repetir / periódico | repetir / recorrente | répéter / récurrent | ripeti / ricorrente |
| People / person / contact | Personas / persona / contacto | Pessoas / pessoa / contato | Personnes / personne / contact | Persone / persona / contatto |
| birthday | cumpleaños | aniversário | anniversaire | compleanno |
| Credentials (login block) | Credenciales | Credenciais | Identifiants | Credenziali |
| password / passphrase | contraseña / frase de contraseña | senha / frase-senha | mot de passe / phrase secrète | password / passphrase |
| Watchlist | Para ver | Para assistir | À regarder | Da guardare |
| movie / show | película / serie | filme / série | film / série | film / serie |
| sync / syncing | sincronizar / sincronizando | sincronizar / sincronizando | synchroniser / synchronisation | sincronizza / sincronizzazione |
| self-host / self-hosted server | servidor propio | servidor próprio | serveur auto-hébergé | server personale |
| LAN sync | sincronización LAN | sincronização LAN | synchro LAN | sincronizzazione LAN |
| device | dispositivo | dispositivo | appareil | dispositivo |
| backup | copia de seguridad | backup | sauvegarde | backup |
| import / export | importar / exportar | importar / exportar | importer / exporter | importa / esporta |
| Settings | Ajustes | Configurações | Réglages | Impostazioni |
| Appearance | Apariencia | Aparência | Apparence | Aspetto |
| theme | tema | tema | thème | tema |
| sidebar | barra lateral | barra lateral | barre latérale | barra laterale |
| split view | vista dividida | tela dividida | vue partagée | vista divisa |
| top bar | barra superior | barra superior | barre supérieure | barra superiore |
| toolbar | barra de herramientas | barra de ferramentas | barre d'outils | barra degli strumenti |
| widget | widget | widget | widget | widget |
| tap / click | tocar / hacer clic | tocar / clicar | appuyer / cliquer | toccare / fare clic |
| Untitled | Sin título | Sem título | Sans titre | Senza titolo |
| Delete | Eliminar | Excluir | Supprimer | Elimina |
| Cancel / Save / Close / Back | Cancelar / Guardar / Cerrar / Atrás | Cancelar / Salvar / Fechar / Voltar | Annuler / Enregistrer / Fermer / Retour | Annulla / Salva / Chiudi / Indietro |
| Done | Listo | Concluído | Terminé | Fine |
| Rename | Renombrar | Renomear | Renommer | Rinomina |
| Search | Buscar | Pesquisar | Rechercher | Cerca |
| App Lock | Bloqueo de la app | Bloqueio do app | Verrouillage de l'app | Blocco app |
| AI chat / embedding model | chat de IA / modelo de embeddings | chat de IA / modelo de embeddings | chat IA / modèle d'embeddings | chat IA / modello di embedding |
| What's New | Novedades | Novidades | Nouveautés | Novità |

### German, Dutch, Polish, Ukrainian, Russian

| English | de | nl | pl | uk | ru |
|---|---|---|---|---|---|
| note | Notiz | notitie | notatka | нотатка | заметка |
| sub-note | Unternotiz | subnotitie | podnotatka | піднотатка | подзаметка |
| space (workspace) | Bereich | ruimte | przestrzeń | простір | пространство |
| folder | Ordner | map | folder | папка | папка |
| Daily (feature) / daily note / daily page | Tagesnotizen / Tagesnotiz / Tagesseite | Dagelijks / dagnotitie / dagpagina | Dziennik / notatka dzienna / strona dnia | Щоденник / щоденна нотатка / сторінка дня | Ежедневник / ежедневная заметка / страница дня |
| block | Block | blok | blok | блок | блок |
| canvas | Leinwand | canvas | płótno | полотно | холст |
| database (block) | Datenbank | database | baza danych | база даних | база данных |
| table (simple) | Tabelle | tabel | tabela | таблиця | таблица |
| view (database/watchlist) | Ansicht | weergave | widok | подання | представление |
| Board (view) | Board | Bord | Tablica | Дошка | Доска |
| row / column / cell | Zeile / Spalte / Zelle | rij / kolom / cel | wiersz / kolumna / komórka | рядок / стовпець / клітинка | строка / столбец / ячейка |
| property | Eigenschaft | eigenschap | właściwość | властивість | свойство |
| template | Vorlage | sjabloon | szablon | шаблон | шаблон |
| toggle list | Aufklappliste | uitklaplijst | lista rozwijana | розгортний список | раскрывающийся список |
| callout | Hinweisbox | opvalblok | wyróżnienie | виноска | выноска |
| bookmark (web) | Lesezeichen | bladwijzer | zakładka | закладка | закладка |
| Favorites | Favoriten | Favorieten | Ulubione | Вибране | Избранное |
| Pinned | Angeheftet | Vastgezet | Przypięte | Закріплені | Закреплённые |
| Trash / Move to Trash | Papierkorb / In den Papierkorb | Prullenbak / Naar prullenbak | Kosz / Przenieś do kosza | Кошик / Перемістити в кошик | Корзина / Переместить в корзину |
| Inbox | Eingang | Inbox | Skrzynka odbiorcza | Вхідні | Входящие |
| sticky note | Haftnotiz | plaknotitie | karteczka | стікер | стикер |
| slash menu / commands | Slash-Menü / Befehle | slash-menu / opdrachten | menu poleceń / polecenia | меню команд / команди | меню команд / команды |
| mention | Erwähnung | vermelding | wzmianka | згадка | упоминание |
| reminder | Erinnerung | herinnering | przypomnienie | нагадування | напоминание |
| task / to-do | Aufgabe | taak | zadanie | завдання | задача |
| event | Termin | afspraak | wydarzenie | подія | событие |
| all-day | ganztägig | hele dag | cały dzień | увесь день | весь день |
| category (calendar) | Kategorie | categorie | kategoria | категорія | категория |
| repeat / recurring | Wiederholen / wiederkehrend | Herhalen / terugkerend | Powtarzaj / cykliczne | Повторювати / повторювана | Повторять / повторяющееся |
| People / person / contact | Personen / Person / Kontakt | Personen / persoon / contact | Osoby / osoba / kontakt | Люди / людина / контакт | Люди / человек / контакт |
| birthday | Geburtstag | verjaardag | urodziny | день народження | день рождения |
| Credentials (login block) | Zugangsdaten | Inloggegevens | Dane logowania | Облікові дані | Учётные данные |
| password / passphrase | Passwort / Passphrase | wachtwoord / wachtwoordzin | hasło / fraza hasła | пароль / парольна фраза | пароль / парольная фраза |
| Watchlist | Merkliste | Kijklijst | Do obejrzenia | Хочу переглянути | Буду смотреть |
| movie / show | Film / Serie | film / serie | film / serial | фільм / серіал | фильм / сериал |
| sync / syncing | synchronisieren / Synchronisierung | synchroniseren / synchronisatie | synchronizuj / synchronizacja | синхронізувати / синхронізація | синхронизировать / синхронизация |
| self-host / self-hosted server | eigener Server | eigen server | własny serwer | власний сервер | собственный сервер |
| LAN sync | LAN-Synchronisierung | LAN-synchronisatie | synchronizacja LAN | синхронізація через LAN | синхронизация по LAN |
| device | Gerät | apparaat | urządzenie | пристрій | устройство |
| backup | Backup | back-up | kopia zapasowa | резервна копія | резервная копия |
| import / export | importieren / exportieren | importeren / exporteren | importuj / eksportuj | імпорт / експорт | импорт / экспорт |
| Settings | Einstellungen | Instellingen | Ustawienia | Налаштування | Настройки |
| Appearance | Darstellung | Uiterlijk | Wygląd | Вигляд | Оформление |
| theme | Design | thema | motyw | тема | тема |
| sidebar | Seitenleiste | zijbalk | pasek boczny | бічна панель | боковая панель |
| split view | geteilte Ansicht | gesplitste weergave | widok podzielony | поділ екрана | разделение экрана |
| top bar | obere Leiste | bovenbalk | górny pasek | верхня панель | верхняя панель |
| toolbar | Symbolleiste | werkbalk | pasek narzędzi | панель інструментів | панель инструментов |
| widget | Widget | widget | widżet | віджет | виджет |
| tap / click | tippen / klicken | tikken / klikken | dotknij / kliknij | торкніться / натисніть | нажмите / нажмите |
| Untitled | Ohne Titel | Naamloos | Bez tytułu | Без назви | Без названия |
| Delete | Löschen | Verwijderen | Usuń | Видалити | Удалить |
| Cancel / Save / Close / Back | Abbrechen / Speichern / Schließen / Zurück | Annuleren / Opslaan / Sluiten / Terug | Anuluj / Zapisz / Zamknij / Wstecz | Скасувати / Зберегти / Закрити / Назад | Отмена / Сохранить / Закрыть / Назад |
| Done | Fertig | Gereed | Gotowe | Готово | Готово |
| Rename | Umbenennen | Naam wijzigen | Zmień nazwę | Перейменувати | Переименовать |
| Search | Suchen | Zoeken | Szukaj | Пошук | Поиск |
| App Lock | App-Sperre | App-vergrendeling | Blokada aplikacji | Блокування застосунку | Блокировка приложения |
| AI chat / embedding model | KI-Chat / Embedding-Modell | AI-chat / embeddingmodel | czat AI / model embeddingów | чат ШІ / модель ембедингів | чат ИИ / модель эмбеддингов |
| What's New | Neuigkeiten | Wat is er nieuw | Co nowego | Що нового | Что нового |

### Turkish, Indonesian, Vietnamese, Hindi

| English | tr | id | vi | hi |
|---|---|---|---|---|
| note | not | catatan | ghi chú | नोट |
| sub-note | alt not | subcatatan | ghi chú con | उप-नोट |
| space (workspace) | alan | ruang | không gian | स्पेस |
| folder | klasör | folder | thư mục | फ़ोल्डर |
| Daily (feature) / daily note / daily page | Günlük / günlük not / gün sayfası | Harian / catatan harian / halaman harian | Hằng ngày / ghi chú hằng ngày / trang hằng ngày | दैनिक / दैनिक नोट / दैनिक पेज |
| block | blok | blok | khối | ब्लॉक |
| canvas | tuval | kanvas | bảng vẽ | कैनवास |
| database (block) | veritabanı | basis data | cơ sở dữ liệu | डेटाबेस |
| table (simple) | tablo | tabel | bảng | तालिका |
| view (database/watchlist) | görünüm | tampilan | chế độ xem | व्यू |
| Board (view) | Pano | Papan | Bảng Kanban | बोर्ड |
| row / column / cell | satır / sütun / hücre | baris / kolom / sel | hàng / cột / ô | पंक्ति / कॉलम / सेल |
| property | özellik | properti | thuộc tính | प्रॉपर्टी |
| template | şablon | templat | mẫu | टेम्पलेट |
| toggle list | açılır liste | daftar lipat | danh sách thu gọn | टॉगल सूची |
| callout | bilgi kutusu | sorotan | khung chú thích | कॉलआउट |
| bookmark (web) | yer işareti | penanda | dấu trang | बुकमार्क |
| Favorites | Favoriler | Favorit | Yêu thích | पसंदीदा |
| Pinned | Sabitlenmiş | Disematkan | Đã ghim | पिन किए गए |
| Trash / Move to Trash | Çöp Kutusu / Çöp Kutusuna taşı | Sampah / Pindahkan ke Sampah | Thùng rác / Chuyển vào thùng rác | ट्रैश / ट्रैश में ले जाएँ |
| Inbox | Gelen Kutusu | Kotak Masuk | Hộp thư đến | इनबॉक्स |
| sticky note | yapışkan not | catatan tempel | ghi chú dán | स्टिकी नोट |
| slash menu / commands | eğik çizgi menüsü / komutlar | menu garis miring / perintah | menu gạch chéo / lệnh | स्लैश मेनू / कमांड |
| mention | bahsetme | sebutan | nhắc đến | उल्लेख |
| reminder | anımsatıcı | pengingat | lời nhắc | रिमाइंडर |
| task / to-do | görev | tugas | việc cần làm | काम |
| event | etkinlik | acara | sự kiện | इवेंट |
| all-day | tüm gün | sepanjang hari | cả ngày | पूरे दिन |
| category (calendar) | kategori | kategori | danh mục | श्रेणी |
| repeat / recurring | tekrarla / yinelenen | ulangi / berulang | lặp lại / định kỳ | दोहराएँ / दोहराने वाला |
| People / person / contact | Kişiler / kişi / kişi | Orang / orang / kontak | Mọi người / người / liên hệ | लोग / व्यक्ति / संपर्क |
| birthday | doğum günü | ulang tahun | sinh nhật | जन्मदिन |
| Credentials (login block) | Giriş bilgileri | Kredensial | Thông tin đăng nhập | लॉगिन जानकारी |
| password / passphrase | şifre / parola ifadesi | kata sandi / frasa sandi | mật khẩu / cụm mật khẩu | पासवर्ड / पासफ़्रेज़ |
| Watchlist | İzleme listesi | Daftar tonton | Danh sách xem | वॉचलिस्ट |
| movie / show | film / dizi | film / serial | phim / chương trình | फ़िल्म / शो |
| sync / syncing | eşitle / eşitleniyor | sinkronkan / menyinkronkan | đồng bộ / đang đồng bộ | सिंक करें / सिंक हो रहा है |
| self-host / self-hosted server | kendi sunucunuz | server sendiri | máy chủ riêng | अपना सर्वर |
| LAN sync | LAN eşitleme | sinkronisasi LAN | đồng bộ LAN | LAN सिंक |
| device | cihaz | perangkat | thiết bị | डिवाइस |
| backup | yedek | cadangan | bản sao lưu | बैकअप |
| import / export | içe aktar / dışa aktar | impor / ekspor | nhập / xuất | इंपोर्ट / एक्सपोर्ट |
| Settings | Ayarlar | Setelan | Cài đặt | सेटिंग |
| Appearance | Görünüm | Tampilan | Giao diện | रूप-रंग |
| theme | tema | tema | chủ đề | थीम |
| sidebar | kenar çubuğu | bilah sisi | thanh bên | साइडबार |
| split view | bölünmüş görünüm | tampilan terpisah | chia đôi màn hình | स्प्लिट व्यू |
| top bar | üst çubuk | bilah atas | thanh trên cùng | टॉप बार |
| toolbar | araç çubuğu | bilah alat | thanh công cụ | टूलबार |
| widget | widget | widget | tiện ích | विजेट |
| tap / click | dokunun / tıklayın | ketuk / klik | nhấn / nhấp | टैप करें / क्लिक करें |
| Untitled | Adsız | Tanpa judul | Không có tiêu đề | बिना शीर्षक |
| Delete | Sil | Hapus | Xóa | मिटाएँ |
| Cancel / Save / Close / Back | İptal / Kaydet / Kapat / Geri | Batal / Simpan / Tutup / Kembali | Hủy / Lưu / Đóng / Quay lại | रद्द करें / सेव करें / बंद करें / वापस |
| Done | Bitti | Selesai | Xong | हो गया |
| Rename | Yeniden adlandır | Ganti nama | Đổi tên | नाम बदलें |
| Search | Ara | Cari | Tìm kiếm | खोजें |
| App Lock | Uygulama Kilidi | Kunci Aplikasi | Khóa ứng dụng | ऐप लॉक |
| AI chat / embedding model | yapay zeka sohbeti / gömme modeli | obrolan AI / model embedding | trò chuyện AI / mô hình nhúng | AI चैट / एम्बेडिंग मॉडल |
| What's New | Yenilikler | Yang Baru | Có gì mới | नया क्या है |

### Japanese, Korean, Chinese (Simplified)

| English | ja | ko | zh |
|---|---|---|---|
| note | メモ | 노트 | 笔记 |
| sub-note | サブメモ | 하위 노트 | 子笔记 |
| space (workspace) | スペース | 스페이스 | 空间 |
| folder | フォルダ | 폴더 | 文件夹 |
| Daily (feature) / daily note / daily page | デイリー / デイリーメモ / デイリーページ | 데일리 / 데일리 노트 / 데일리 페이지 | 每日 / 每日笔记 / 每日页面 |
| block | ブロック | 블록 | 块 |
| canvas | キャンバス | 캔버스 | 画布 |
| database (block) | データベース | 데이터베이스 | 数据库 |
| table (simple) | 表 | 표 | 表格 |
| view (database/watchlist) | ビュー | 보기 | 视图 |
| Board (view) | ボード | 보드 | 看板 |
| row / column / cell | 行 / 列 / セル | 행 / 열 / 셀 | 行 / 列 / 单元格 |
| property | プロパティ | 속성 | 属性 |
| template | テンプレート | 템플릿 | 模板 |
| toggle list | トグルリスト | 토글 목록 | 折叠列表 |
| callout | コールアウト | 콜아웃 | 标注 |
| bookmark (web) | ブックマーク | 북마크 | 书签 |
| Favorites | お気に入り | 즐겨찾기 | 收藏 |
| Pinned | ピン留め | 고정됨 | 已置顶 |
| Trash / Move to Trash | ゴミ箱 / ゴミ箱に移動 | 휴지통 / 휴지통으로 이동 | 废纸篓 / 移到废纸篓 |
| Inbox | 受信トレイ | 받은편지함 | 收件箱 |
| sticky note | 付箋 | 스티커 메모 | 便签 |
| slash menu / commands | スラッシュメニュー / コマンド | 슬래시 메뉴 / 명령어 | 斜杠菜单 / 命令 |
| mention | メンション | 멘션 | 提及 |
| reminder | リマインダー | 알림 | 提醒 |
| task / to-do | タスク | 할 일 | 待办 |
| event | 予定 | 일정 | 日程 |
| all-day | 終日 | 종일 | 全天 |
| category (calendar) | カテゴリ | 카테고리 | 类别 |
| repeat / recurring | 繰り返す / 繰り返し | 반복 / 반복 | 重复 / 重复 |
| People / person / contact | 人物 / 人物 / 連絡先 | 사람 / 사람 / 연락처 | 人物 / 人物 / 联系人 |
| birthday | 誕生日 | 생일 | 生日 |
| Credentials (login block) | ログイン情報 | 로그인 정보 | 登录信息 |
| password / passphrase | パスワード / パスフレーズ | 비밀번호 / 암호 문구 | 密码 / 密码短语 |
| Watchlist | ウォッチリスト | 볼 목록 | 待看清单 |
| movie / show | 映画 / 番組 | 영화 / 시리즈 | 电影 / 剧集 |
| sync / syncing | 同期 / 同期中 | 동기화 / 동기화 중 | 同步 / 正在同步 |
| self-host / self-hosted server | セルフホストサーバー | 자체 서버 | 自托管服务器 |
| LAN sync | LAN同期 | LAN 동기화 | 局域网同步 |
| device | デバイス | 기기 | 设备 |
| backup | バックアップ | 백업 | 备份 |
| import / export | インポート / エクスポート | 가져오기 / 내보내기 | 导入 / 导出 |
| Settings | 設定 | 설정 | 设置 |
| Appearance | 外観 | 모양 | 外观 |
| theme | テーマ | 테마 | 主题 |
| sidebar | サイドバー | 사이드바 | 侧边栏 |
| split view | 分割表示 | 분할 보기 | 分屏 |
| top bar | トップバー | 상단 바 | 顶栏 |
| toolbar | ツールバー | 도구 모음 | 工具栏 |
| widget | ウィジェット | 위젯 | 小组件 |
| tap / click | タップ / クリック | 탭 / 클릭 | 点按 / 点击 |
| Untitled | 無題 | 제목 없음 | 无标题 |
| Delete | 削除 | 삭제 | 删除 |
| Cancel / Save / Close / Back | キャンセル / 保存 / 閉じる / 戻る | 취소 / 저장 / 닫기 / 뒤로 | 取消 / 保存 / 关闭 / 返回 |
| Done | 完了 | 완료 | 完成 |
| Rename | 名前を変更 | 이름 변경 | 重命名 |
| Search | 検索 | 검색 | 搜索 |
| App Lock | アプリロック | 앱 잠금 | 应用锁 |
| AI chat / embedding model | AIチャット / 埋め込みモデル | AI 채팅 / 임베딩 모델 | AI 聊天 / 嵌入模型 |
| What's New | 新機能 | 새로운 기능 | 新功能 |

## Emoji names

Emoji search names come from Unicode CLDR. If you add emojis to `data-by-group.json`, rebuild them:

```
python3 -I scripts/build-emoji-names.py <download folder>
```

## Adding a language

1. Copy all `values/strings_*.xml` files into `values-<lang>/` and translate them. Do the same for the Android `strings.xml`.
2. Add the language to `AppLanguage.kt` and `androidMain/res/xml/locales_config.xml`.
3. If it needs plural forms other than `one`/`other`, add it to `scripts/check-translations.py`.
4. Add it to `scripts/build-emoji-names.py` and rebuild the emoji names.
5. Check that the fonts support it, and that buttons and the Daily week strip still fit.

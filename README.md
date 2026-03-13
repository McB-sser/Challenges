# Challenges

`Challenges` ist ein Minecraft-Plugin fuer Paper/Spigot 1.20, das ein fortlaufendes Herausforderungs-System mit Token-Shop, Modulen und GUI-Steuerung ins Spiel bringt. Spieler erhalten zufaellige Aufgaben in mehreren Gruppen, verdienen durch das Abschliessen Token und schalten damit Komfortfunktionen, Effekte, Kosmetik und spezielle Nutzungs-Module frei.

Der Fokus des Plugins liegt nicht auf einzelnen Quests mit fester Reihenfolge, sondern auf einem wiederholbaren Gameplay-Loop: Aufgaben erledigen, Gruppen abschliessen, Stufen aufbauen, Token sammeln und freigeschaltete Module sinnvoll einsetzen.

## Was das Plugin macht

Das Plugin generiert fuer jeden Spieler zufaellige Herausforderungen aus vordefinierten Vorlagen. Diese Aufgaben sind in drei Gruppen aufgeteilt:

- `Gruppe 1`: kleine bis mittlere Kurzzeit-Ziele
- `Gruppe 2`: groessere Zwischenziele
- `Gruppe 3`: umfangreiche Langzeit-Ziele

Ein Spieler arbeitet immer nur an der aktuell sichtbaren Gruppe. Sobald alle Aufgaben einer Gruppe erledigt oder gueltig uebersprungen wurden, wird die naechste Gruppe freigeschaltet. Nach Abschluss von Gruppe 3 steigt der Spieler eine Stufe auf, alle Gruppen werden neu erzeugt und der Zyklus beginnt erneut auf hoeherem Schwierigkeitsniveau.

Die Schwierigkeit skaliert mit der Spielerstufe. Hoehere Stufen fuehren zu hoeheren Zielwerten und machen den Fortschritt langfristig interessanter. Gleichzeitig steigen die Token-Ertraege pro Aufgabe ebenfalls mit der Gruppenstufe.

## Grundprinzip im Spiel

Das Plugin arbeitet mit einem speziellen Menü-Gegenstand: dem `Herausforderungs-Hub`. Dieser Gegenstand ist eine Sonnenblume mit Verzauberungs-Glow und kann gecraftet werden. Das Rezept ist:

```text
Knochenmehl  Knochenmehl  Knochenmehl
Knochenmehl  Sonnenblume  Knochenmehl
Knochenmehl  Knochenmehl  Knochenmehl
```

Wichtig: Fortschritt fuer Herausforderungen wird nur dann gezaehlt, wenn der Spieler dieses Hub-Item im Inventar oder in der Nebenhand hat. Das Item ist damit bewusst der Schluessel zum System.

Mit Rechtsklick auf die Sonnenblume oeffnet sich das Hauptmenue des Plugins. Von dort aus kann der Spieler:

- die aktuellen Aufgaben ansehen
- den Token-Shop oeffnen
- bereits gekaufte Module verwalten

## Spielablauf Schritt fuer Schritt

1. Der Spieler craftet oder erhaelt den `Herausforderungs-Hub`.
2. Der Spieler behaelt das Item im Inventar.
3. Mit Rechtsklick oeffnet er das Hauptmenue.
4. Im Bereich `Aufgaben` sieht er die aktuell aktive Gruppe mit allen zugehoerigen Challenges.
5. Normale Survival-Aktionen erhoehen den Fortschritt automatisch.
6. Erledigte Aufgaben geben Token.
7. Mit den Token werden im Shop Module, Effekte, Kosmetik oder Spawn-Eier gekauft.
8. Nach Abschluss aller drei Gruppen steigt der Spieler eine Stufe auf und erhaelt einen neuen Durchlauf.

## Challenge-System im Detail

Jeder Spieler besitzt einen eigenen Fortschrittsstand. Gespeichert werden unter anderem:

- aktuelle Token
- aktuelle Stufe
- aktive Challenge-Gruppe
- verbleibende Skips pro Gruppe
- aktive und abgeschlossene Challenges
- freigeschaltete Shop-Module
- Restzeit und Aufladungen gekaufter Module

Die Daten werden in `plugins/challenges/players.yml` gespeichert.

### Die drei Gruppen

Die Anzahl der Aufgaben pro Gruppe ist ueber die Konfiguration steuerbar. Standardmaessig gilt:

- `Gruppe 1`: 5 Aufgaben
- `Gruppe 2`: 7 Aufgaben
- `Gruppe 3`: 10 Aufgaben

Jede Gruppe hat ihr eigenes Skip-Limit:

- `Gruppe 1`: 1 Skip
- `Gruppe 2`: 2 Skips
- `Gruppe 3`: 3 Skips

Im Aufgabenmenue kann eine offene Aufgabe per Rechtsklick uebersprungen werden, solange fuer die aktuelle Gruppe noch Skips verfuegbar sind.

### Zeit- und Fortschrittslogik

Das Plugin arbeitet mit einem Kontingent-System. Beim Abschliessen einer Gruppe wird die verfuegbare Laufzeit verlaengert:

- Abschluss von `Gruppe 1`: `+1 Tag`
- Abschluss von `Gruppe 2`: `+2 Tage`
- Abschluss von `Gruppe 3`: `+5 Tage`

Wenn ein Spieler sein Kontingent ablaufen laesst, wird der Gruppenfortschritt zurueckgesetzt. Je nach Ueberziehung kann ausserdem taeglich Stufe verloren gehen. Dadurch entsteht Druck, den Zyklus aktiv weiterzuspielen, ohne dass das System komplett hart stoppt.

### Dynamische Schwierigkeit

Die Ziele werden nicht immer mit exakt demselben Wert erstellt. Pro Challenge gibt es:

- einen Basiswert
- einen zusaetzlichen Wert pro Stufe
- eine zufaellige Streuung beim Generieren

Dadurch fuehlen sich Wiederholungen nicht komplett identisch an, obwohl sie auf denselben Challenge-Typen basieren.

## Welche Aktionen als Challenge gezaehlt werden

Das Plugin verfolgt verschiedene Survival-Aktivitaeten automatisch und ordnet sie Challenge-Typen zu:

- `BREAK_BLOCK`: beliebige Bloecke abbauen
- `MINE_ORE`: echte Erz-Bloecke abbauen
- `KILL_MOB`: feindliche Mobs toeten
- `FISH`: Fische oder Angelbeute fangen
- `CRAFT`: Items herstellen
- `BREED`: Tiere zuechten
- `SMELT`: Ofen-Ausgabe entnehmen
- `ENCHANT`: Gegenstaende am Zaubertisch verzaubern
- `WALK_DISTANCE`: Distanz zuruecklegen
- `TRADE_VILLAGER`: mit Dorfbewohnern handeln

Die Fortschrittsanzeige wird zusaetzlich ueber eine Bossbar eingeblendet. Spieler sehen dadurch direkt beim Spielen, wie weit eine Aufgabe bereits fortgeschritten ist.

## Beispiele fuer vorhandene Challenges

In der Standard-Konfiguration sind bereits viele Aufgabenvorlagen enthalten, zum Beispiel:

- Stein abbauen
- Erze abbauen
- Monster besiegen
- Fische fangen
- Items craften
- Tiere zuechten
- Gegenstaende schmelzen
- Distanzen laufen
- mit Villagern handeln
- Gegenstaende verzaubern

Die Aufgaben unterscheiden sich je nach Gruppe in Umfang und Belohnung. Fruehe Gruppen sind fuer kurzfristige Ziele gedacht, spaetere Gruppen fuer laengerfristigen Fortschritt.

## Token-System

Fuer jede abgeschlossene Challenge erhaelt der Spieler Token. Die Hoehe der Belohnung haengt von der aktiven Gruppe und indirekt von der aktuellen Stufe ab. Token sind die zentrale Waehrung des Plugins und werden fuer alle Shop-Inhalte verwendet.

Im Hauptmenue wird der aktuelle Token-Bestand angezeigt. Im Shop sieht der Spieler ausserdem, welche Stufe fuer einen Kauf noetig ist und wie viele Token ein Modul kostet.

## Token-Shop und Module

Der Shop ist in mehrere Kategorien aufgeteilt. Jede Kategorie deckt einen anderen Nutzen ab.

### 1. Effekte

Hier kann der Spieler positive Effekte mit Zeitguthaben kaufen. Diese Module lassen sich in `Meine Module` aktivieren oder pausieren. Solange sie aktiv sind, verbrauchen sie pro Sekunde ihre Restzeit.

Beispiele:

- Tempo I und II
- Eile I und II
- Sprungkraft
- Nachtsicht
- Wasseratmung
- Feuerresistenz
- Glueck
- Regeneration
- Staerke
- Resistenz
- Delfins Gnade
- Langsamer Fall
- Meereskraft

### 2. Kosmetik

Kosmetische Module erzeugen Partikel- oder Spezialeffekte. Auch diese laufen ueber Zeitguthaben und koennen an- und ausgeschaltet werden.

Beispiele:

- Herz-Aura
- Gluecks-Partikel
- Magie-Aura
- Teleport-Effekt
- Abbau-Effekt
- Lauf-Effekt
- Bogen-Effekt
- Elytra-Effekt

### 3. Werkbaenke und Utility-Module

Diese Kategorie bietet Module mit Aufladungen statt Laufzeit. Jede Nutzung verbraucht eine Aufladung.

Verfuegbar sind unter anderem:

- mobile Werkbank
- mobiler Steinmetz
- mobiler Webstuhl
- mobiler Kartentisch
- mobiler Schmiedetisch
- mobiler Schleifstein
- mobiler Amboss
- mobile Enderkiste
- mobile Zaubertische mit unterschiedlicher Regalstaerke von 0 bis 15

Damit koennen Spieler viele Arbeitsstationen direkt ueber das Modul verwenden, ohne den Block in der Welt platzieren zu muessen.

### 4. Spawn-Eier

Spieler koennen verschiedene friedliche Spawn-Eier kaufen und spaeter ueber ihre Modulverwaltung einsetzen. Dazu gehoeren zum Beispiel:

- Kuh, Schwein, Schaf, Huhn
- Pferd, Esel, Lama, Maultier
- Katze, Fuchs, Hase, Panda
- Axolotl, Delfin, Schildkroete
- Dorfbewohner und fahrender Haendler
- Allay, Sniffer und weitere friedliche Mobs

Jede Nutzung verbraucht die jeweilige Aufladung und gibt dem Spieler das entsprechende Spawn-Ei.

## Wie man das Plugin benutzt

### Fuer Spieler

1. `Herausforderungs-Hub` craften.
2. Das Item im Inventar behalten.
3. Rechtsklick auf das Item, um das Hauptmenue zu oeffnen.
4. Im Menue `Aufgaben` die aktuelle Gruppe ansehen.
5. Die geforderten Aktionen im normalen Survival-Gameplay ausfuehren.
6. Token verdienen und im `Token-Shop` ausgeben.
7. Unter `Meine Module` gekaufte Inhalte aktivieren, pausieren oder mit Aufladungen verwenden.

### Fuer Serverbetreiber

Nach dem ersten Start wird eine `config.yml` erzeugt. Dort lassen sich unter anderem anpassen:

- Anzahl der Challenges pro Gruppe
- Anzahl der erlaubten Skips pro Gruppe
- Challenge-Vorlagen fuer Gruppe 1 bis 3
- Zielwerte und Bezeichnungen einzelner Aufgaben

Dadurch kann das Plugin leicht an den eigenen Serverstil angepasst werden, etwa mehr Fokus auf Mining, Handel, Exploration oder klassische Survival-Fortschritte.

## Bedienung der Menues

### Hauptmenue

Das Hauptmenue ist die zentrale Einstiegsstelle und enthaelt:

- `Aufgaben`: zeigt die aktuell aktive Challenge-Gruppe
- `Token-Shop`: oeffnet den Shop mit allen Kategorien
- `Meine Module`: zeigt gekaufte Module und ihre Restzeit oder Aufladungen

### Aufgaben-Menue

Hier sieht der Spieler:

- Titel und Beschreibung jeder Aufgabe
- erklaerten Weg zur Erfuellung
- aktuellen Fortschritt
- Token-Belohnung
- verbleibende Skips
- Ablaufzeit des aktuellen Kontingents

Rechtsklick auf eine Aufgabe versucht, sie zu ueberspringen.

### Shop-Menue

Der Shop zeigt pro Eintrag:

- Beschreibung
- erforderliche Mindeststufe
- Preis in Token
- Zeitguthaben oder Aufladungen pro Kauf
- bereits vorhandenes Guthaben

### Meine Module

Hier werden bereits gekaufte Module verwaltet:

- Zeit-Module koennen aktiviert oder pausiert werden
- Aufladungs-Module werden direkt von dort aus benutzt
- pausierte Module verbrauchen keine Restzeit

## Besonderheiten des Plugins

- GUI-basierte Bedienung ohne komplizierte Befehle
- wiederholbarer Progressions-Loop mit steigender Stufe
- zufaellige Aufgaben aus konfigurierbaren Vorlagen
- Bossbar-Fortschritt direkt waehrend des Spielens
- Token als langfristige Motivations- und Belohnungswaehrung
- Shop mit Gameplay-Nutzen und kosmetischen Inhalten
- modulbasierte Freischaltungen mit Zeit- oder Aufladungsprinzip
- persistent gespeicherter Fortschritt pro Spieler

## Technische Hinweise

- Zielplattform: `Paper/Spigot 1.20`
- Hauptklasse: `de.mcbesser.challenges.ChallengesPlugin`
- Konfiguration: `src/main/resources/config.yml`
- Plugin-Beschreibung: `src/main/resources/plugin.yml`
- persistente Spielerdaten: `plugins/challenges/players.yml`

## Kurzfassung

`Challenges` ist ein Survival-Fortschrittsplugin mit zufaelligen Aufgaben, Stufenaufstieg, Token-Waehrung und modularem Shop-System. Spieler behalten einen speziellen Hub-Gegenstand im Inventar, erledigen dadurch automatisch zaehlende Aufgaben und bauen ueber mehrere Gruppen hinweg ihren Fortschritt aus. Das Plugin verbindet Motivation durch wiederkehrende Ziele mit einem klaren Belohnungssystem und vielen freischaltbaren Zusatzfunktionen.

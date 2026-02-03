## Was macht die App Tour Navigator?

Die privatsphäre-freundliche App **Tour Navigator** hilft dir dabei, jede Wanderung vorab perfekt zu planen und unterwegs entspannt zu genießen.  
Die App erstellt aus einer vorhandenen GPX-Datei einen übersichtlichen Zeitplan – mit realistischen Gehzeiten, Zwischenzeiten und einem klar strukturierten Ablauf, genau so wie es sich ein Tourenguide für einen optimalen Wandertag wünscht.

## Was beinhaltet die GPX-Datei?

GPX-Dateien sind der Standard für Touren und geografische Daten. Sie enthalten den genauen Verlauf einer Route, inklusive Höhenangaben und – idealerweise – benannten Wegpunkten.

Du kannst GPX-Dateien bequem über Tourenportale erhalten, z. B.:

- Outdooractive (www.outdooractive.com)
- Schwarzwaldverein (www.schwarzwaldverein-tourenportal.de)

Je besser die Datei vorbereitet ist, desto besser arbeitet Tour Navigator.

**Tipp:** Besonders einfach gelingt es mit dem [Tourenplaner des Schwarzwaldvereins](https://www.schwarzwaldverein.de/schwarzwald/wandern-outdoor/tourenportal).

![](img/Tour_001.png)
Im Tourenportal des Schwarzwaldvereins befinden sich bereits hunderte von Tourenvorschlägen, die direkt übernommen werden können

## Wie funktioniert die Gehzeitberechnung?

Tour Navigator arbeitet mit der bewährten **DIN 33466** – der offiziellen Grundlage für verlässliche Wanderzeitberechnung.  
Die Formel berücksichtigt Geschwindigkeit, Steigung und Gefälle und liefert ein realistisches Zeitmodell.

### Details der Gehzeitberechnung
<details><summary><h3>Details der Gehzeitberechnung</h3></summary>

Sie geht davon aus, dass ein durchschnittlicher Wanderer:

- 4 km ebene Strecke pro Stunde zurücklegt
- 300 Höhenmeter Aufstieg pro Stunde bewältigt
- 500 Höhenmeter Abstieg pro Stunde bewältigt

Bei gemischten Teilstrecken werden horizontale und vertikale Zeiten separat berechnet.  
Als Gehzeit gilt: **längerer Zeitanteil + Hälfte des kürzeren Anteils**.

**Beispiel:**  
1 km eben (15 min) + 300 Hm Aufstieg (60 min)  
→ 60 min + 7,5 min = **67,5 min Gesamtzeit**

Weitere Infos zur [Marschzeitberechnung auf Wikipedia](https://de.wikipedia.org/wiki/Marschzeitberechnung).
</details>

Die drei Parameter der Formel kannst du in den Einstellungen der App an deine individuelle Leistung anpassen. Sie werden dauerhaft gespeichert.

Nach Eingabe der Startzeit berechnet die App anhand der GPX-Daten und deiner Parameter einen tabellarischen Ablaufplan der Tour.  
Ein Höhenprofil zeigt zusätzlich den vertikalen Verlauf der Tour.

## Auch Pausen sind wichtig

Für jeden Zwischenpunkt kannst du einen Kommentar hinzufügen und eine individuelle Pausenzeit festlegen – ideal für Foto-Stopps, Vesper oder Aussichtspunkte.

## Navigation zum Ausgangspunkt der Route

Tour Navigator kann Koordinaten an andere Navigations-Apps übergeben, z. B. Google Maps, um die Anfahrt zum Startpunkt zu erleichtern.  
Die Navigation erfolgt vollständig in der gewählten Ziel-App. *(Datenschutzhinweise beachten!)*

## Echtzeit-Navigation

Während der Tour begleitet dich die App in Echtzeit:

- Anzeige der nächsten Wegpunkte
- Kontrolle des Zeitplans
- Warnung bei Verlassen der Route (optional per Sprachausgabe)

Voraussetzung ist eine aktivierte GPS-Lokalisierung mit genauen Positionsdaten.

## Was macht die App nicht?

Tour Navigator berechnet bewusst **keine Alternativrouten**, sondern führt zuverlässig entlang des geplanten Tracks.

## Was macht die App privatsphäre-freundlich?

Es werden **keine persönlichen Daten** und **keine GPX-Tracks** gespeichert oder weitergegeben.

## Welche Berechtigungen braucht die App?

Erforderlich ist nur:

- Zugriff auf die **genaue GPS-Position** (Navigation & Zeitplanung)

Optional (nur bei Nutzung entsprechender Funktionen):

- Internetzugriff für Wikipedia-Artikel und OpenStreetMap-POIs

</details>

<details><summary><h1>Bedienungsanleitung</h1></summary>
Die Bedienung der App erfolgt über diese Menüs:

<details><summary><h2>Hauptmenü</h2></summary>

| Symbol                                           | Funktion |
|--------------------------------------------------|----------|
| ![](../../app/src/main/assets/img/file_open.jpg) | Öffnet eine GPX-Datei |
| ![](../../app/src/main/assets/img/description.png)                         | Zeigt Titel, Beschreibung und Web-Link der Tour |
| ![](../../app/src/main/assets/img/swap_horiz.png)                          | Kehrt die Richtung der Tour um |
| ![](../../app/src/main/assets/img/signpost.jpg)                            | Wegpunkte aus GPX oder OpenStreetMap hinzufügen |
| ![](../../app/src/main/assets/img/schedule.png)                            | Startzeit festlegen |
| ![](../../app/src/main/assets/img/table_rows.png)                          | Wegzeittabelle anzeigen |
| ![](../../app/src/main/assets/img/file_save.png)                           | Erweiterte GPX-Datei exportieren |
| ![](../../app/src/main/assets/img/html.png)                                | Wegzeittabelle als HTML exportieren |
| ![](../../app/src/main/assets/img/settings.png)                            | Einstellungen |
| ![](../../app/src/main/assets/img/help.png)                                | Hilfe anzeigen |
| ![](../../app/src/main/assets/img/info.png)                                | Copyright-Informationen |

</details>

<details><summary><h2>Kontextmenü</h2></summary>

| Funktion                         | Beschreibung                             |
|----------------------------------|------------------------------------------|
| Pausenzeit                       | Individuelle Pausenzeit je Wegpunkt      |
| Wegpunkt kommentieren            | Kommentar in Wegzeittabelle              |
| Wikipedia-Artikel                | Artikel im Umkreis von 10 km hinzufügen  |
| OSM-POIs                         | OpenStreetMap-POIs im Umkreis von 500 m hinzufügen |
| Zum Wegpunkt navigieren          | Übergabe an externe Karten-Apps          |
| Google-Navigation                | Navigation mit Google Maps               |
| Nicht rückgängig zu machen:      |
| **Starte von hier**              |                                          |
| **Wegpunkt entfernen**           |                                          |
| **Alle Punkte danach entfernen** |                                          |

</details>

<details><summary><h2>Bedien- und Statuselemente in der Fußzeile</h2></summary>
Die Fußzeile stellt die wichtigsten Schaltflächen und Statusinformationen bereit:
<details><summary><h3>Bedienelemente</h3></summary>

| Symbol | Funktion |
|------|----------|
| ![](../../app/src/main/assets/img/follow_the_signs.jpg) | Tracking starten/fortsetzen |
| ![](../../app/src/main/assets/img/accessibility_new.jpg) | Tracking pausieren |
| ![](../../app/src/main/assets/img/expand_more.jpg) | Mehr Informationen |
| ![](../../app/src/main/assets/img/expand_less.jpg) | Weniger Informationen |
| ![](../../app/src/main/assets/img/text_to_speech.jpg) | Sprachausgabe |
| ![](../../app/src/main/assets/img/voice_selection_off.jpg) | Sprachausgabe stoppen |
| ![](../../app/src/main/assets/img/show_chart.jpg) | Höhenprofil anzeigen |
| ![](../../app/src/main/assets/img/open_in_full.jpg) | Höhenprofil ausblenden |

</details>

<details><summary><h3>Statussymbole</h3></summary>

| Symbol | Bedeutung |
|------|-----------|
| ![](../../app/src/main/assets/img/alarm_on.jpg) | Alarm aktiv |
| ![](../../app/src/main/assets/img/alarm_off.jpg) | Alarm deaktiviert |
| ![](../../app/src/main/assets/img/location_disabled.jpg) | Standortberechtigung fehlt |
| ![](../../app/src/main/assets/img/satellite.jpg) | Standortdienste deaktiviert |
| ![](../../app/src/main/assets/img/location_off.png) | GPS aktiv – kein Fix |
| ![](../../app/src/main/assets/img/sync_problem.jpg) | Ungenaue Positionsdaten |
| ![](../../app/src/main/assets/img/location_on.png) | GPS-Fix vorhanden |

</details>
</details>
</details>


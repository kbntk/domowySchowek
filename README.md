# domowySchowek

**Domowy Schowek** to aplikacja na Androida do porządkowania domowych pojemników i schowków. Pozwala zapisać, co jest w każdym pojemniku, i szybko to odnaleźć, bez przeglądania wszystkich szuflad i pudełek.

## Co robi aplikacja

- **Grupy** – tworzysz grupy odpowiadające pojemnikom lub miejscom, np. „Pojemnik 1 duży". Grupy są widoczne na liście, posortowane alfabetycznie.
- **Zdjęcie grupy** – do grupy możesz dodać jedno zdjęcie zrobione aparatem telefonu. Na liście grup zdjęcie wyświetla się pod nazwą grupy.
- **Elementy** – w każdej grupie zapisujesz elementy z nazwą i opcjonalnym zdjęciem. Elementy są wyświetlane pod nazwą grupy w wybranej kolejności sortowania (A–Z domyślnie, Z–A, najnowsze–najstarsze, najstarsze–najnowsze).
- **Edycja** – możesz zmienić nazwę grupy, jej zdjęcie, a także nazwę i zdjęcie elementu. Grupę i elementy można usunąć.
- **Wyszukiwanie** – pole "Szukaj" pod tytułem filtruje listę. Pokazuje grupy, których nazwa pasuje, oraz grupy, w których pasuje jakiś element, a pod grupą wypisuje pasujące elementy. Po wejściu do grupy z wyników pasujące elementy migają przez 2 sekundy, a lista przewija się do pierwszego z nich.
- **Podgląd elementu** – kliknięcie elementu pokazuje jego nazwę, zdjęcie i datę dodania. Przesunięcie palcem po zdjęciu w lewo przechodzi do następnego elementu, w prawo do poprzedniego. Przycisk „Edytuj element" otwiera edycję.
- **Podgląd zdjęcia grupy** – w widoku grupy ikona zdjęcia pokazuje zdjęcie grupy na pełnym ekranie.
- **Ustawienia** – ikona koła zębatego na liście grup. Zawiera wybór szerokości zapisywanych zdjęć, sortowanie elementów oraz eksport i import.
- **Eksport** – wszystkie grupy albo wybrane grupy zapisujesz do wybranego katalogu. Powstaje plik `schowek.json` z listą grup i elementów, zdjęcia grup leżą obok niego, a zdjęcia elementów w podkatalogach nazwanych jak grupa.
- **Import** – wczytujesz wcześniej wyeksportowany katalog. Jeśli grupa o tej samej nazwie już istnieje, aplikacja pyta, czy ją nadpisać, czy zaimportować pod inną nazwą (albo pominąć).

Szerokość nowo robionych zdjęć wybierasz w ustawieniach: 320, 480 lub 640 px (domyślnie 640). Wysokość wynika z proporcji. Zdjęcia węższe niż 640 px są pokazywane w swoim rzeczywistym rozmiarze, bez powiększania.

## Wymagania

- Android 9.0 (API 28) lub nowszy.

## Budowanie

Wymagany jest JDK 17 lub nowszy (np. JBR 21 z Android Studio). Gradle musi widzieć poprawne `JAVA_HOME`, a JDK z samym JRE (bez `javac`) nie wystarczy. Ścieżkę do JDK można też wskazać w lokalnym `gradle.properties`:

```properties
org.gradle.java.home=/sciezka/do/jdk-21
```

Komendy budowania:

```bash
./gradlew :app:assembleDebug        # APK debug
./gradlew :app:assembleRelease      # APK release (niepodpisany bez konfiguracji keystore)
./gradlew :app:bundleRelease        # AAB release
```

Plik `local.properties` z ścieżką do Android SDK (`sdk.dir=...`) tworzy Android Studio.

## Struktura projektu

- `app/src/main/java/eu/jakubzakrzewski/domowyschowek/` – kod aplikacji
  - `data/` – encje i DAO Room, eksport/import (`Transfer.kt`)
  - `ui/` – ekrany Compose i ViewModel
  - `util/` – obróbka zdjęć (skalowanie do wybranej szerokości), ustawienia (`Prefs.kt`)
- `icon.png` – źródło ikony aplikacji, `play-store-icon-512.png` – ikona do Google Play

## Historia zmian

### 1.0.2
- Ustawienia (ikona koła zębatego): eksport i import przeniesione tutaj.
- Wybór szerokości zdjęć: 320, 480 lub 640 px (domyślnie 640).
- Sortowanie elementów w grupach: A–Z, Z–A, najnowsze–najstarsze, najstarsze–najnowsze. Dotyczy wszystkich grup.
- Elementy zapamiętują datę dodania. Elementy z wcześniejszych wersji dostały datę aktualizacji bazy.
- Podgląd elementu z przesuwaniem między elementami i datą dodania.
- Zdjęcia 320 i 480 px nie są powiększane i zachowują ostrość.
- Pole „Nazwa" w oknie dodawania nie wywołuje klawiatury po powrocie z aparatu.

### 1.0.1
- Wyszukiwanie grup i elementów z podświetleniem pasujących elementów.
- Eksport i import grup do katalogu.

## Licencja

Zob. plik [LICENSE](LICENSE).

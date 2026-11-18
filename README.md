# domowySchowek

**Domowy Schowek** to aplikacja na Androida do porządkowania domowych pojemników i schowków. Pozwala zapisać, co jest w każdym pojemniku, i szybko to odnaleźć, bez przeglądania wszystkich szuflad i pudełek.

## Co robi aplikacja

- **Grupy** – tworzysz grupy odpowiadające pojemnikom lub miejscom, np. „Pojemnik 1 duży". Grupy są widoczne na liście, posortowane alfabetycznie.
- **Zdjęcie grupy** – do grupy możesz dodać jedno zdjęcie zrobione aparatem telefonu. Na liście grup zdjęcie wyświetla się pod nazwą grupy.
- **Elementy** – w każdej grupie zapisujesz elementy z nazwą i opcjonalnym zdjęciem. Elementy są wyświetlane alfabetycznie pod nazwą grupy.
- **Edycja** – możesz zmienić nazwę grupy, jej zdjęcie, a także nazwę i zdjęcie elementu. Grupę i elementy można usunąć.
- **Wyszukiwanie** – pole "Szukaj" pod tytułem filtruje listę. Pokazuje grupy, których nazwa pasuje, oraz grupy, w których pasuje jakiś element, a pod grupą wypisuje pasujące elementy. Po wejściu do grupy z wyników pasujące elementy migają przez 2 sekundy, a lista przewija się do pierwszego z nich.
- **Podgląd zdjęcia grupy** – w widoku grupy ikona zdjęcia pokazuje zdjęcie grupy na pełnym ekranie.
- **Eksport** – wszystkie grupy albo wybrane grupy zapisujesz do wybranego katalogu. Powstaje plik `schowek.json` z listą grup i elementów, zdjęcia grup leżą obok niego, a zdjęcia elementów w podkatalogach nazwanych jak grupa.
- **Import** – wczytujesz wcześniej wyeksportowany katalog. Jeśli grupa o tej samej nazwie już istnieje, aplikacja pyta, czy ją nadpisać, czy zaimportować pod inną nazwą (albo pominąć).

Zdjęcia są zapisywane w rozmiarze 640 px szerokości, z zachowaniem proporcji, żeby nie zajmować dużo miejsca.

## Wymagania

- Android 9.0 (API 28) lub nowszy.

## Budowanie

Wymagany jest JDK 17 lub nowszy (np. JBR 21 z Android Studio). Gradle musi widzieć poprawne `JAVA_HOME`, a JDK z samym JRE (bez `javac`) nie wystarczy. Ścieżkę do JDK można też wskazać w lokalnym `gradle.properties`:

```properties
org.gradle.java.home=/sciezka/do/jdk-21
```

Wymagany jest JDK 17 lub nowszy (np. JBR 21 z Android Studio):

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
  - `util/` – obróbka zdjęć (skalowanie do 640 px)
- `icon.png` – źródło ikony aplikacji, `play-store-icon-512.png` – ikona do Google Play

## Licencja

Zob. plik [LICENSE](LICENSE).

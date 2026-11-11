# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Maven project, Spring Boot 2.4.5, Java 8 source/target (JDK 1.8 is installed). There is no `mvnw`, and `mvn` is not on the PATH on this machine, so Maven commands need a local install or an IDE.

```
mvn test                                        # all tests
mvn -Dtest=ScanServiceTest test                 # one test class
mvn -Dtest=SavedPagesTest#bazooka test          # one test method
mvn package                                     # -> target/shopmonitor-0.1.5-SNAPSHOT-spring-boot.jar
```

Lombok is used throughout (`@Slf4j`, getters/setters on the entities), so the IDE needs annotation processing enabled.

Run the app **from the repo root**. Paths are relative to the working directory: `config/application.properties`, the H2 file `./data/smon`, and logs in `./logs/`.

```
java -jar target/shopmonitor-0.1.5-SNAPSHOT-spring-boot.jar scan -ro -nm   # dry run, print new items
java -jar target/shopmonitor-0.1.5-SNAPSHOT-spring-boot.jar shop discover -u <url>
```

Only one process can open `./data/smon` at a time. If an IDE run or another CLI run holds it, the CLI prints "another run is still using ./data/smon" and exits with 1 without saving anything.

Exit codes: 0 ok, 1 failure, 2 usage error. An external scheduler relies on these.

## Architecture

**CLI on Spring Boot.** `smon.Application` is a `CommandLineRunner` that parses args with Airline (`@Cli` lists the commands and groups). Airline creates the command objects, so they are not Spring beans. `Application.run` calls `autowireBean` on every `BaseCommand` before running it. A new command must extend `BaseCommand`, be annotated `@Command`, and be registered in the `@Cli` annotation on `Application`. Top-level commands are `scan` and `search`. `shop` groups list/import/export/discover. `report` contains `failures`.

**Scan pipeline (`smon.scan.ScanServiceImpl`).** `ScanMode` is the single source of truth for what a run does. It has three flags: `filtersKnownItems`, `saves`, and `printsItems`. The mail behaviour is in the `mail()` switch. Flow:
1. Select the shops that are enabled and have a list selector. Search runs use only shops with a search URL.
2. Start one download task per host. Each task runs that host's shops one after another, and there are at most 4 threads. Shops of one host are never requested in parallel.
3. The main thread processes shops **in shop order**, which decides who gets credit for a product listed by several shops. Keep this ordering if you touch the loop.
4. Extraction problems and incomplete items (no description or link) are written to the `ERRORS` table and never stop the run. A failed send to all recipients does throw, and the run exits 1.

**Mail semantics differ by mode.** A normal `scan` saves new items with `SHOULD_NOTIFY=true`, then mails **every** pending row from the DB and clears the flag only after a successful send. The read-only and search modes mail only what this run found. `-nm` saves items as already notified, so they are never mailed later.

**Selector-driven extraction.** Shops are data, not code. Each shop row in H2 has CSS (jsoup) selectors per field (list, description, image, link, price), with priority order. `BaseExtractor` tries them in order and takes the first match. Subclasses are `NewItemsExtractor`, `SearchItemsExtractor`, and `DiscoverExtractor`. Image attributes are tried lazy-load first (`ci-src`, `data-original`, ...) before `src`. Prices go through `util.Price` and `ValueResolver`. "Shop closed / maintenance" is detected by matching phrases from the `MESSAGES` table against the page text. A broken shop is usually fixed by updating its selectors in the DB, not by changing code.

**Shop definitions move through XML.** `shop export` and `shop import` use the JAXB format (`dto.ShopDefinitionList`, `util.MarshallerUtil`). `shop import -t` tests a definition without saving it. `shop discover -u URL -a` picks the best selectors automatically and saves the shop only if list, description, and link were all found. `ShopDefinition.validate()` is the gate for both import and discover.

**Persistence.** Embedded H2 file at `./data/smon`. The schema is owned by `src/main/resources/schema.sql`, which runs on every start, so every statement must be idempotent. `ddl-auto=none` is set, so a schema change means adding SQL to `schema.sql`, not only changing an entity. Repositories live in `smon.*.dao`. `ShopsServiceImpl` initializes the selector collections inside its transaction because download threads use the shops after it ends.

**Downloads.** `smon.downloader.Downloader` uses jsoup with a browser User-Agent, a 20 s timeout, one retry for transient errors (5xx, 429, timeouts), and a 20 MB body cap. **SSL certificate verification is disabled on purpose** (`SSLUtils`); the code does not say why. Don't "fix" this without asking.

**Configuration.** `config/application.properties` is gitignored and holds SMTP and DB credentials. The app refuses to start if a required key is missing. `config/application.properties.example` is the template, and `src/main/resources/application.properties` holds the defaults. Don't print or copy the real file into anything.

**Tests.**
- `SavedPagesTest` runs extraction on real saved HTML in `src/test/resources/pages`, using the selector strings copied from the DB. When a shop changes its layout, add a new saved page next to the old one. Don't overwrite the old one.
- `ScanServiceTest` uses plain Mockito, no Spring.
- `DownloaderTest` runs a local `HttpServer`, not the internet.

## Things that are not part of the app

- `src/main/java/gdziestrzelac/` is a separate helper (`ContactShopUrlExtractor`, `SslUtil`). Nothing in `smon` references it.
- `ml/`, `oldsripts/`, `items/`, `data/`, `data_exports/`, `logs/`, and `target/` are local and gitignored. `items/` holds personal notes and the CLI argument cheat-sheet. Don't commit them.

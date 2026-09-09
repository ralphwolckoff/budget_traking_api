# budget-tracker-api (Spring Boot)

Migration Node/Express + Prisma → Spring Boot + PostgreSQL.

- **Étape 1** : squelette + sécurité JWT + module d'authentification complet
  (register, login, change-password, change-username, delete), en remplacement de
  `routes/auth.js` + `middleware/auth.js`.
- **Étape 2** : entités `MonthSettings`, `Expense`, `ForecastItem`, `CarryOver` +
  `DataController`, en remplacement de `routes/data.js` (y compris `buildAppData()`).

## Démarrage

1. Crée la base PostgreSQL :
   ```sql
   CREATE DATABASE budget_tracker;
   ```

2. Variables d'environnement (à mettre dans ta config IntelliJ "Run/Debug", pas dans un `.env`
   committé) :
   ```
   DATABASE_URL=jdbc:postgresql://localhost:5432/budget_tracker
   DATABASE_USERNAME=postgres
   DATABASE_PASSWORD=<ton mot de passe>
   JWT_SECRET=<génère avec: openssl rand -base64 32>
   PORT=3001
   ALLOWED_ORIGINS=http://localhost:5173,http://localhost:5174
   ```

3. Lance avec `mvn spring-boot:run` ou directement depuis IntelliJ
   (`BudgetTrackerApplication.main()`).

Avec `ddl-auto: update`, Hibernate crée les tables `users` et `settings` tout seul
au premier démarrage — pas besoin de migration manuelle pour l'instant.

## Différences volontaires par rapport au backend Node

| Node (ancien) | Spring Boot (ici) | Pourquoi |
|---|---|---|
| `JWT_SECRET` avec fallback hardcodé | Démarrage refusé si `JWT_SECRET` absent | Le fallback Node est une faille de sécurité réelle (token forgeable si `.env` absent en prod) |
| Colonne `salt` séparée en DB | Supprimée | `BCryptPasswordEncoder` embarque déjà le salt dans le hash |
| Vérifs manuelles dans chaque route | `Bean Validation` (`@NotBlank`, `@Size`) + `ApiException` centralisée | Moins de duplication, cohérent avec les attentes Spring Boot |
| Logique métier dans les routes Express | Extraite dans `AuthService` | Sépare HTTP (controller) et règles métier (service) — pattern standard Spring |

## Points à vérifier avant de brancher le front

- Le front envoie `Authorization: Bearer <token>` exactement comme avec Express — aucun
  changement côté client requis pour l'auth.
- Les réponses JSON gardent la même forme (`{success, username, token}` /
  `{success: false, error}`), donc le contrat d'API reste compatible.
- `DATABASE_URL` change de format (JDBC vs URL Prisma) — ne réutilise pas directement la
  valeur de l'ancien `.env`.

## Endpoints /api/data (étape 2)

Tous nécessitent `Authorization: Bearer <token>` — récupéré via `/api/auth/login` ou
`/api/auth/register`.

| Méthode | Route | Équivalent Node |
|---|---|---|
| GET | `/api/data` | `buildAppData()` (inclut désormais `recurringExpenses`/`investments`) |
| PUT | `/api/data/settings` | salaire/épargne globaux |
| PUT | `/api/data/month-settings` | override mensuel (envoyer `salary`/`savings` à `null` pour supprimer) |
| POST | `/api/data/expenses` | créer une dépense |
| DELETE | `/api/data/expenses/{id}` | supprimer une dépense |
| POST | `/api/data/forecast` | créer (sans `id`) ou modifier (avec `id`) une prévision |
| DELETE | `/api/data/forecast/{id}` | supprimer une prévision |
| PUT | `/api/data/carryover` | définir le report d'un mois |
| PUT | `/api/data/recurring/{id}` | upsert d'une dépense récurrente — id fourni par le **client** |
| DELETE | `/api/data/recurring/{id}` | supprimer une dépense récurrente |
| PUT | `/api/data/investments/{id}` | upsert d'un investissement — id fourni par le **client** |
| DELETE | `/api/data/investments/{id}` | supprimer un investissement |
| PUT | `/api/data/category-budgets` | remplace l'objet complet `{catId: montant, ...}` |
| PUT | `/api/data/goals/{id}` | upsert d'un objectif financier — id fourni par le **client** |
| DELETE | `/api/data/goals/{id}` | supprimer un objectif financier |

Note : `date` dans `POST /expenses` attend un format ISO-8601 complet
(ex. `2026-08-16T00:00:00Z`), pas juste `2026-08-16` — sinon `Instant.parse()` lève une erreur.

`recurring`/`investments` utilisent un id **fourni par le client** (ex. `rec-1234567890`),
pas généré par le serveur — identique à Prisma (`@id` sans `@default`), pour que le front
n'ait jamais besoin de remapper un id après une écriture hors-ligne mise en queue
(voir `storage.ts`). Les sous-collections d'un investissement (`payments`, `gains`, `events`,
`valueHistory`, `documents`) sont stockées en JSON brut (colonnes `TEXT`), sérialisées/
désérialisées via Jackson dans `BudgetDataService` — le client les manipule toujours comme
un objet complet, jamais champ par champ.

## Schéma de base de données

Ce projet part d'une base PostgreSQL vierge (décision prise en cours de migration :
aucune donnée existante du backend Node/Prisma à préserver). Les noms de tables/colonnes
générés par Hibernate (`snake_case`, ex. `users`, `month_settings`) sont donc **différents**
de ceux que Prisma aurait créés (`PascalCase` par défaut, ex. `User`, `MonthSettings`) —
sans conséquence puisqu'on ne réutilise pas l'ancien schéma. Si un jour il fallait migrer
de vraies données Prisma vers ce backend, il faudrait soit exporter/réimporter les données,
soit aligner les `@Table`/`@Column` sur les noms Prisma exacts.

## Rapports exportables (CSV / PDF)

Nouveaux endpoints — nécessitent `Authorization: Bearer <token>`, comme le reste de l'API.
Passer soit `month` (`2026-08`) soit `year` (`2026`), jamais les deux.

| Méthode | Route | Description |
|---|---|---|
| GET | `/api/reports/csv?month=2026-08` | Rapport mensuel CSV (revenu, dépenses détaillées, disponible) |
| GET | `/api/reports/csv?year=2026` | Rapport annuel CSV (résumé mois par mois + détail complet) |
| GET | `/api/reports/pdf?month=2026-08` | Même contenu que le CSV mensuel, mis en page en PDF |
| GET | `/api/reports/pdf?year=2026` | Rapport annuel PDF, un bloc par mois + récapitulatif |

La formule "Disponible" = revenu + report reçu − dépenses (l'épargne visée est affichée à
titre indicatif, pas déduite). Si ta logique budgétaire côté front calcule ça différemment,
ajuste `ReportService.buildMonthReport()` pour rester cohérent entre les deux.

Le CSV est préfixé d'un BOM UTF-8 pour qu'Excel affiche correctement les accents à
l'ouverture. Génération PDF via [OpenPDF](https://github.com/LibrePDF/OpenPDF) (fork
libre d'iText, licence LGPL/MPL).

## Recherche globale

| Méthode | Route | Description |
|---|---|---|
| GET | `/api/data/search?q=...&year=...&month=...&category=...` | Recherche dans description ET catégorie, tous paramètres optionnels |

Exemple : `GET /api/data/search?q=Orange Money&year=2026` — recherche insensible à la
casse à travers tous les mois de 2026. Réponse : `{ success: true, results: [...] }` où
chaque résultat est une dépense avec son `monthKey` (contrairement à `ExpenseDto` dans
`buildAppData()`, où le regroupement par mois rend ce champ redondant).

## Prochaine étape

Migration fonctionnelle terminée (auth + données). Checklist avant une vraie mise en prod :

- [ ] Régénérer un `JWT_SECRET` propre à la prod (celui de dev ne doit plus jamais servir)
- [ ] Mot de passe PostgreSQL de prod différent de celui de dev
- [ ] `ddl-auto: update` → passer à `validate` + un outil de migration versionné
      (Flyway ou Liquibase) plutôt que de laisser Hibernate modifier le schéma tout seul
- [ ] `ALLOWED_ORIGINS` mis à jour avec le vrai domaine du front (retirer les ports Vite de dev)
- [ ] Ajouter un rate limiting sur `/api/auth/login` et `/register` (absent aussi bien ici
      que dans l'original Node — bucket4j ou un reverse-proxy comme Nginx/Caddy sont deux options)
- [ ] Voir la section "Où stocker les secrets en prod" pour le mécanisme adapté à
      l'hébergeur choisi (systemd, Docker, PaaS...)

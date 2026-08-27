# erpnext-java

Front-end web métier au-dessus de l'API REST d'**ERPNext** (Frappe) : fournisseurs, devis et commandes,
factures d'achat et paiement, employés, paie (assignations salariales, fiches de paie, rapport, totaux,
génération sur période, modification groupée), import CSV.

L'application **ne stocke aucune donnée** : ERPNext est la seule source de vérité. Chaque utilisateur se
connecte avec **ses propres identifiants ERPNext** ; toutes les opérations sont exécutées avec ses droits.

## Prérequis

- JDK 17 ou plus récent (le projet compile en `release 17`).
- Une instance ERPNext (Frappe ≥ 13) avec le module **HRMS** (Salary Structure, Salary Structure
  Assignment, Salary Slip, Salary Component).
- Côté ERPNext, deux personnalisations dont l'application dépend :
  - la méthode whitelistée `erpnext.api.salaireImport.import_csv_files` — source dans
    [util/salaireImport.py](util/salaireImport.py), à déployer dans `erpnext/api/` (réservée aux rôles
    *HR Manager* / *System Manager*) ;
  - le rapport **Salary Register** doit accepter les filtres supplémentaires `component`, `signe`,
    `combien` (page « Salaires »).

## Configuration

Toutes les valeurs sont dans [application.properties](src/main/resources/application.properties) et se
surchargent par variables d'environnement (`ERPNEXT_BASE_URL`, `ERP_PAYMENT_PAID_FROM_ACCOUNT`, …).

| Clé | Rôle |
|---|---|
| `erpnext.base-url` | URL de l'instance ERPNext (utiliser `https://` en production) |
| `erpnext.connect-timeout`, `erpnext.read-timeout` | délais des appels HTTP (5 s / 30 s) |
| `erpnext.page-size` | taille de page pour parcourir les listes |
| `erp.payroll.default-company` | société proposée par défaut dans le rapport de salaires |
| `erp.payroll.currency-label` | libellé de devise sur les fiches de paie PDF |
| `erp.payment.paid-from-account`, `erp.payment.paid-from-currency`, `erp.payment.naming-series` | compte débité, devise et série des paiements de factures |
| `server.servlet.session.timeout` | durée de la session utilisateur (porte le cookie ERPNext) |

Les valeurs livrées pour `erp.payroll.*` et `erp.payment.*` sont des **valeurs de test** à remplacer.

## Lancer

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # dev : templates rechargés sans redémarrage
./mvnw clean package && java -jar target/Erp-0.0.1-SNAPSHOT.jar
```

Puis <http://localhost:8080> — connexion avec un compte ERPNext.

## Tests

```bash
./mvnw clean test
```

Sur Windows, utilisez toujours `clean` : le build incrémental est peu fiable avec les dossiers dont la
casse a changé. Les tests n'ont pas besoin d'ERPNext : les appels HTTP sont simulés avec
`MockRestServiceServer`, les règles métier de paie sont testées avec Mockito.

## Architecture

```
hery.itu.erp
├── config/        ErpNextProperties, PaymentProperties, PayrollProperties, SecurityConfig
├── security/      ErpNextUser (principal : identifiant + cookie sid), ErpNextAuthenticationProvider
├── erpnext/       ErpNextClient (seul point d'accès à l'API Frappe), Filters, exceptions typées
├── service/       un service par domaine ; ne parlent à ERPNext que via ErpNextClient
│   └── salary/    SalaryStructAssService (SSA + fiches), PayrollGenerationService (aléa 1),
│                  BulkSalaryAdjustmentService (aléa 2), SalaryTotalService, SalarySlipPdfService…
├── controller/    contrôleurs Spring MVC fins (binding, choix de vue, messages flash)
├── web/           GlobalExceptionHandler (pages d'erreur sans détail interne), Downloads
└── model/         POJOs miroirs partiels des DocTypes ERPNext
```

Règles structurantes :

- **Session par utilisateur** : le cookie `sid` ERPNext vit dans le principal Spring Security de la
  session HTTP ; aucun état partagé dans les singletons.
- **Aucun document soumis n'est supprimé** : un remplacement suit le cycle ERPNext *annulation →
  amendement (`amended_from`) → soumission*, l'historique de paie reste consultable.
- **Aucune URL ni filtre concaténé** : `ErpNextClient` encode doctypes, noms et filtres (Jackson).
- Les listes sont **paginées** (`limit_start` / `limit_page_length`), les tables enfants lues par lots.
- Toute action destructrice est un **POST** protégé par CSRF.

## Règles métier de paie

**Génération sur période (aléa 1)** — pour un employé, une structure et une période : un SSA et une
fiche par mois. Base : celle saisie, sinon la moyenne des bases soumises (case « moyenne »), sinon la
dernière base soumise de l'employé. Un mois déjà couvert est ignoré, sauf « écraser » : le SSA de
*cet employé* pour *ce mois* est annulé puis amendé avec la nouvelle base et la fiche régénérée. Le
résultat liste les fiches créées, les mois ignorés et les mois en échec.

**Modification groupée (aléa 2)** — pour les employés sélectionnés, on cherche les fiches dont le
composant remplit la condition (`<` ou `>` seuil). Les fiches sont regroupées par SSA ; le pourcentage
(ajout ou retrait) est appliqué **une seule fois** par SSA, le SSA est remplacé (annulation +
amendement) et seules les fiches concernées sont régénérées. Les échecs sont rapportés par employé.

## À vérifier sur l'instance ERPNext cible

- Lecture des tables enfants via `GET /api/resource/<Child>?parent=<Parent>` (utilisée pour les
  composants de paie et les items de devis). Si l'instance la refuse (403/417), l'application retombe
  automatiquement sur un appel par document — fonctionnel, plus lent.
- `frappe.only_for` dans `salaireImport.py` après déploiement (un compte sans rôle RH doit recevoir 403).
- Nommage des documents amendés (`<nom>-1`) et absence de blocage à l'annulation d'un SSA lié à des
  fiches soumises.

## Limites connues

- Les traitements de masse (génération, modification groupée) s'exécutent dans la requête HTTP :
  prévoir un traitement asynchrone, ou mieux, déplacer ces workflows dans une méthode Frappe côté
  serveur (une transaction, permissions natives).
- Bootstrap, Font Awesome et Chart.js sont chargés depuis des CDN (versions épinglées, SRI) : à
  vendoriser pour un réseau fermé.
- Les mots de passe transitent vers ERPNext par HTTP si `erpnext.base-url` n'est pas en `https://`.

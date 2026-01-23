# Lancer le `manager` en local (hors Docker) — Guide pour les développeurs

But: ce guide explique comment arrêter uniquement le service `manager` démarré par `docker compose`, exécuter le `manager` localement (IntelliJ ou Maven / Quarkus dev), et configurer la GUI en mode développement pour qu'elle pointe vers ce `manager` local.

Objectifs
- Stopper uniquement le container `manager` de la composition Docker.
- Lancer le `manager` localement sur le port 8080 (compatible avec la GUI en dev).
- Faire en sorte que la GUI (vite) utilise `http://localhost:8080` comme `VITE_API_MANAGER_BASE_URL`.
- Utiliser les IP fixes des containers Docker pour DB, Keycloak et Consul (pas besoin de modifier `/etc/hosts`).

Pré-requis
- Docker et Docker Compose installés et la composition du dépôt démarrée (au moins `traefik`, `consul`, `db`, `keycloak` si vous suivez le `docker-compose.yml` fourni).
- Maven (ou la wrapper `./mvnw`) disponible.
- IntelliJ (ou un IDE capable d'exécuter une configuration Maven).

Contexte réseau utile (depuis `docker-compose.yml`)
- DB (Postgres) : 172.25.0.4
- Consul : 172.25.0.3
- Keycloak (auth) : 172.25.0.5
- Manager (docker) : 172.25.0.6 (mais en local on lance sur localhost:8080)

Résumé des étapes
1. Stopper seulement le container `manager` (Docker).
2. Lancer le `manager` localement (IntelliJ / Maven) en pointant la configuration vers les IP des containers Docker (DB, Keycloak, Consul).
3. Lancer la GUI en dev avec `VITE_API_MANAGER_BASE_URL=http://localhost:8080` et `VITE_OIDC_AUTHORITY` pointant vers Keycloak (container IP).

Étapes détaillées

1) Arrêter uniquement le manager Docker

Depuis la racine du projet (là où se trouve `docker-compose.yml`) :

```bash
# Arrête le service manager sans toucher aux autres services
docker compose stop manager
# Pour supprimer le container afin d'éviter tout redémarrage automatique :
docker compose rm -s -f manager
```

2) Lancer le `manager` en local (IntelliJ / Maven) en utilisant les IP des containers

Le manager local aura besoin d'atteindre :
- la base Postgres sur 172.25.0.4:5432
- Keycloak (OIDC) sur http://172.25.0.5/realms/mbyte
- Consul sur 172.25.0.3:8500 (topology)

Option A — Ligne de commande (Maven Quarkus dev) :

```bash
# Depuis la racine du projet
./mvnw -f manager quarkus:dev \
  -Dquarkus.http.port=8080 \
  -Dquarkus.oidc.auth-server-url=http://172.25.0.5/realms/mbyte \
  -Dquarkus.datasource.jdbc.url=jdbc:postgresql://172.25.0.4:5432/manager \
  -Dquarkus.datasource.username=mbyte \
  -Dquarkus.datasource.password=password \
  -Dmanager.topology.host=172.25.0.3 \
  -Dmanager.topology.port=8500
```

Option B — IntelliJ (Run/Debug configuration)
- Importez le module Maven `manager` dans IntelliJ si nécessaire.
- Créez une nouvelle configuration `Maven` :
  - Working directory : `<repo>/manager`
  - Command line : `quarkus:dev`
  - Env/VM options :
    - `-Dquarkus.http.port=8080`
    - `-Dquarkus.oidc.auth-server-url=http://172.25.0.5/realms/mbyte`
    - `-Dquarkus.datasource.jdbc.url=jdbc:postgresql://172.25.0.4:5432/manager`
    - `-Dquarkus.datasource.username=mbyte`
    - `-Dquarkus.datasource.password=password`
    - `-Dmanager.topology.host=172.25.0.3`
    - `-Dmanager.topology.port=8500`

Remarques :
- Le manager est forcé sur le port 8080 ici pour correspondre à la configuration de la GUI en dev.
- Les IP listées proviennent des adresses statiques définies dans `docker-compose.yml` et sont directement atteignables depuis l'hôte sur une installation Linux standard.

3) Configurer et lancer la GUI (Vite)

La GUI doit connaître :
- l'URL du manager (API) : `VITE_API_MANAGER_BASE_URL` (pointant vers le manager local : `http://localhost:8080`)
- l'autorité OIDC (Keycloak) : `VITE_OIDC_AUTHORITY` (pointant vers Keycloak container IP `http://172.25.0.5/realms/mbyte`)

Exemple de démarrage :

```bash
# Depuis le dossier gui/
VITE_API_MANAGER_BASE_URL=http://localhost:8080 \
VITE_OIDC_AUTHORITY=http://172.25.0.5/realms/mbyte \
VITE_OIDC_CLIENT_ID=mbyte \
VITE_OIDC_SCOPE="openid profile email" \
npm run dev
```

Ou créez un `gui/.env.local` (non commité) :

```
VITE_API_MANAGER_BASE_URL=http://localhost:8080
VITE_OIDC_AUTHORITY=http://172.25.0.5/realms/mbyte
VITE_OIDC_CLIENT_ID=mbyte
VITE_OIDC_SCOPE=openid profile email
```

Puis :

```bash
cd gui
npm run dev
```

4) Vérifications rapides (smoke test)
- Ouvrez la GUI en dev (par défaut http://localhost:5173). Vous devriez pouvoir démarrer la connexion OIDC vers Keycloak (via l'IP 172.25.0.5).
- Vérifiez l'endpoint health du manager local :

```
http://localhost:8080/q/health
```

- Vérifiez l'API métier :

```
http://localhost:8080/api/status
```

5) Résolution des problèmes courants
- Connexion DB refusée :
  - Vérifiez que le container Postgres (`mbyte.db`) est UP et accessible sur 172.25.0.4:5432.
  - Vérifiez que la base `manager` existe (le script `provisioning/init.sql` peut créer les schémas nécessaires).

- Échec OIDC / redirections :
  - Vérifiez que le container Keycloak (`mbyte.auth`) est UP et répond sur 172.25.0.5.
  - Vérifiez la configuration du realm / client dans l'import (`auth/mbyte-realm.json` apprécié dans l'image Keycloak).

- La GUI se plaint que `VITE_API_MANAGER_BASE_URL` est manquant :
  - Lancer vite en exportant la variable d'environnement (voir section 3) ou définir `gui/.env.local`.

6) Restauration du manager dans Docker

Quand vous voulez revenir à la version Docker du manager :

```bash
docker compose up -d manager
```

Notes finales / bonnes pratiques
- Les IP statiques sont pratiques pour le dev, évitez toutefois de les considérer comme une solution portable pour CI ou autres environnements.
- Ne comitez pas les fichiers `.env.local` contenant des secrets.

---

Sur certaines machines non unix, les IPs directes des conteneurs ne sont pas visibles directement sans exposer les ports.
Dans ce cas, il faudra modifier le docker compose pour exposer les ports nécessaires (DB, Keycloak, Consul) sur l'hôte, et ajuster les configurations en conséquence.

# Lancer la `gui` en local (hors Docker) — Guide pour les développeurs

But: ce guide explique comment arrêter uniquement le service `gui` démarré par `docker compose`, exécuter la `gui` localement (Vite dev), et configurer les variables d'environnement pour qu'elle pointe vers les services appropriés (manager, Keycloak, etc.).

Objectifs
- Stopper uniquement le container `gui` de la composition Docker.
- Lancer la `gui` localement sur le port 5173 (par défaut Vite).
- Configurer les variables d'environnement pour pointer vers le manager (local ou Docker) et Keycloak.
- Utiliser les IP fixes des containers Docker pour les services (pas besoin de modifier `/etc/hosts`).

Pré-requis
- Docker et Docker Compose installés et la composition du dépôt démarrée (au moins `traefik`, `consul`, `db`, `keycloak`, `manager` si vous suivez le `docker-compose.yml` fourni).
- Node.js et npm disponibles.
- La `gui` peut être lancée avec `npm run dev`.

Contexte réseau utile (depuis `docker-compose.yml`)
- DB (Postgres) : 172.25.0.4
- Consul : 172.25.0.3
- Keycloak (auth) : 172.25.0.5
- Manager (docker) : 172.25.0.6
- GUI (docker) : 172.25.0.10 (mais en local on lance sur localhost:5173)

Résumé des étapes
1. Stopper seulement le container `gui` (Docker).
2. Lancer la `gui` localement (npm run dev) en configurant les variables d'environnement pour pointer vers les services (manager local ou Docker, Keycloak, etc.).
3. Accéder à la GUI via http://localhost:5173.

Étapes détaillées

1) Arrêter uniquement la gui Docker

Depuis la racine du projet (là où se trouve `docker-compose.yml`) :

```bash
# Arrête le service gui sans toucher aux autres services
docker compose stop gui
# Pour supprimer le container afin d'éviter tout redémarrage automatique :
docker compose rm -f gui
```

2) Lancer la `gui` en local (npm run dev) en utilisant les IP des containers ou localhost

La gui local aura besoin d'atteindre :
- Le manager (API) : soit local sur localhost:8080, soit Docker sur 172.25.0.6:8080
- Keycloak (OIDC) : sur http://172.25.0.5/realms/mbyte

Option A — Manager local (comme dans DEPLOY_LOCAL_MANAGER.md) :

Si vous avez lancé le manager localement (sur localhost:8080), configurez :

```bash
# Depuis le dossier gui/
VITE_API_MANAGER_BASE_URL=http://localhost:8080 \
VITE_OIDC_AUTHORITY=https://auth.mbyte.fr/realms/mbyte \
VITE_OIDC_CLIENT_ID=mbyte-local-gui \
VITE_OIDC_SCOPE="openid profile email" \
npm run dev
```

Option B — Manager Docker :

Si le manager est dans Docker (172.25.0.6:8080), configurez :

```bash
# Depuis le dossier gui/
VITE_API_MANAGER_BASE_URL=http://172.25.0.6:8080 \
VITE_OIDC_AUTHORITY=https://auth.mbyte.fr/realms/mbyte \
VITE_OIDC_CLIENT_ID=mbyte-local-gui \
VITE_OIDC_SCOPE="openid profile email" \
npm run dev
```

Vous pouvez aussi créer un fichier `gui/.env.local` (non commité) pour éviter de répéter les variables :

```
VITE_API_MANAGER_BASE_URL=http://localhost:8080  # ou http://172.25.0.6:8080 si manager Docker
VITE_OIDC_AUTHORITY=http://172.25.0.5/realms/mbyte
VITE_OIDC_CLIENT_ID=mbyte
VITE_OIDC_SCOPE=openid profile email
```

Puis :

```bash
cd gui
npm run dev
```

Remarques :
- La gui est lancée sur le port 5173 par défaut (configurable dans vite.config.ts si nécessaire).
- Les IP listées proviennent des adresses statiques définies dans `docker-compose.yml` et sont directement atteignables depuis l'hôte sur une installation Linux standard.
- Si vous utilisez des stores, ajustez `VITE_API_STORE_BASE_URL`, `VITE_STORES_DOMAIN`, etc. si nécessaire.

3) Vérifications rapides (smoke test)
- Ouvrez la GUI en dev (http://localhost:5173). Vous devriez pouvoir vous connecter via OIDC vers Keycloak (IP 172.25.0.5).
- Vérifiez que les appels API vers le manager fonctionnent (par exemple, via les outils de dev du navigateur).
- Si le manager est local, vérifiez qu'il répond sur http://localhost:8080/q/health.

4) Résolution des problèmes courants
- Erreur de connexion API :
  - Vérifiez que le manager (local ou Docker) est UP et accessible sur l'URL configurée.
  - Vérifiez les variables d'environnement dans le terminal ou .env.local.

- Échec OIDC / redirections :
  - Vérifiez que le container Keycloak (`mbyte.auth`) est UP et répond sur 172.25.0.5.
  - Vérifiez la configuration du realm / client dans l'import (`auth/mbyte-realm.json`).

- Variables manquantes :
  - Assurez-vous que les variables VITE_* sont définies (export ou .env.local).

5) Restauration de la gui dans Docker

Quand vous voulez revenir à la version Docker de la gui :

```bash
docker compose up -d gui
```

Notes finales / bonnes pratiques
- Les IP statiques sont pratiques pour le dev, évitez toutefois de les considérer comme une solution portable pour CI ou autres environnements.
- Ne comitez pas les fichiers `.env.local` contenant des configurations spécifiques.

---

Sur certaines machines non unix, les IPs directes des conteneurs ne sont pas visibles directement sans exposer les ports.
Dans ce cas, il faudra modifier le docker compose pour exposer les ports nécessaires (Manager, Keycloak) sur l'hôte, et ajuster les configurations en conséquence.</content>
<parameter name="filePath">/home/jerome/Miage/mbyte/DEPLOY_LOCAL_GUI.md

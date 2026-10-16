# 🚀 Architecture Microservices Spring Cloud

Ce document présente l'architecture globale, la structure des services ainsi que les principes de fonctionnement du système basé sur Java Spring Cloud. L'application repose sur une architecture distribuée, sécurisée par API Gateway et Redis, tout en intégrant des mécanismes de résilience avancés.

---

## 📸 Structure du Projet

L'architecture backend se compose de plusieurs microservices spécialisés :

```text
server/
├── academic-service    # Microservice métier pour la gestion des données académiques
├── api-gateway         # Point d'entrée unique, filtrage global, sécurité et Rate Limiting
├── auth-service        # Service d'authentification et gestion des tokens JWT / Redis
├── config-service      # Serveur de configuration centralisée (Spring Cloud Config)
└── discovery-service   # Serveur d'enregistrement et de découverte dynamique (Netflix Eureka)


+-----------------------+
                                  |   Config Service      |
                                  | (Spring Cloud Config) |
                                  +-----------+-----------+
                                              | (Configuration centralisée)
                                              v
+------------------+             +-----------------------+             +----------------------+
|   Client App     | ----------> |      API Gateway      | ----------> |  Discovery Service   |
| (Web / Mobile)   |  Requests   |   (Spring Cloud GW)   |  Discovery  |   (Netflix Eureka)   |
+------------------+             +-----------+-----------+             +----------------------+
                                             |
                  +--------------------------+--------------------------+
                  | (Routage + GlobalFilter + RateLimiter)               |
                  v                                                     v
      +-----------------------+                             +-----------------------+
      |     Auth Service      |                             |   Academic Service    |
      | (JWT + Token Rotation)|                             | (Métier + DB Sync)    |
      +-----------+-----------+                             +-----------+-----------+
                  |                                                     |
                  v                                                     v
         +-----------------+                                   +-----------------+
         |   Redis Cache   |                                   |  PostgreSQL/DB  |
         | (Refresh Tokens |                                   | (Resilience4j   |
         |  + Rate Limit)  |                                   |   Bulkhead)     |
         +-----------------+                                   +-----------------+




⚙️ Rôle et Composants de l'Architecture
1. Discovery Service (Netflix Eureka)
Le service de découverte agit comme le registre central du système. Tous les microservices (academic-service, auth-service, api-gateway) s'y enregistrent dynamiquement au démarrage. L'API Gateway utilise Eureka pour équilibrer la charge (lb://) et diriger les requêtes vers les instances actives sans avoir besoin d'adresses IP fixes.

2. Config Service (Spring Cloud Config)
Centralise la gestion des propriétés et des configurations de tous les microservices. Chaque service récupère ses paramètres au démarrage depuis ce serveur unique, ce qui facilite la maintenance et le déploiement.

🔐 Sécurité & Authentification JWT avec Rotation Redis
1. Contrôle d'Accès via GlobalFilter (API Gateway)
Un filtre global (GlobalFilter) configuré sur l'API Gateway intercepte chaque requête entrante pour vérifier les règles de sécurité :

Endpoints Publics (Accès libre sans authentification) :

/auth/login : Authentification de l'utilisateur.

/auth/register : Création de compte.

/auth/refresh : Renouvellement des jetons d'accès.

/eureka/** : Console de découverte.

Endpoints Privés (Authentification obligatoire) :

/academic/** : Nécessite l'envoi d'un jeton JWT valide dans le header HTTP (Authorization: Bearer <Token>).

Tous les autres endpoints métier protégés.

2. Mécanisme de Rotation des Tokens (Access & Refresh Tokens)
Afin de garantir une sécurité maximale tout en conservant une architecture stateless :

Access Token (JWT) : Jeton signé à courte durée de vie, vérifié de manière autonome par l'API Gateway pour autoriser l'accès aux ressources.

Refresh Token : Jeton à plus longue durée de vie, stocké de manière centralisée dans Redis.

Principe de Rotation (Refresh Token Rotation) :

Lors d'une demande de renouvellement sur /auth/refresh, le client présente son Refresh Token.

L'Auth Service valide l'existence du jeton dans Redis.

L'ancien Refresh Token est immédiatement invalidé/supprimé de Redis.

Un nouveau couple (Access Token, Refresh Token) est généré et le nouveau jeton est stocké dans Redis.

En cas de tentative de réutilisation d'un ancien jeton déjà révoqué, le système détecte l'anomalie et révoque l'ensemble des sessions de l'utilisateur concerné.

⚡ Gestion du Trafic et Résilience
1. Rate Limiting & Principe de Bursting (Redis)
Pour protéger l'infrastructure contre le surdébit et les attaques par déni de service (DoS), l'API Gateway s'appuie sur un algorithme de type Token Bucket adossé à Redis :

Replenish Rate : Définit le débit régulier de requêtes autorisées par seconde.

Burst Capacity : Définit la capacité maximale d'absorption lors de pics soudains de trafic (Principe de Bursting), permettant de gérer un afflux temporaire sans bloquer immédiatement l'utilisateur.

2. Isolation du Pool de Connexion DB avec Resilience4j Bulkhead
Afin d'éviter que le microservice métier (academic-service) ne soit saturé par un grand nombre de requêtes simultanées qui épuiseraient le pool de connexions à la base de données :

Le pattern Bulkhead de Resilience4j fixe une limite maximale sur le nombre d'appels simultanés autorisés à interroger la base de données.

Si le seuil est atteint, les requêtes supplémentaires sont mises en attente pendant un temps défini ou dirigées vers un traitement de secours (fallback), évitant ainsi le blocage en cascade et l'effondrement de la base de données.


🚀 Ordre de Démarrage des Services
Pour assurer un démarrage correct de l'environnement, lancez les composants dans l'ordre suivant :

Infrastructures externes (Services Redis et Base de données).

config-service : Serveur de configuration centralisé.

discovery-service : Serveur de découverte Eureka.

auth-service : Service d'authentification et de gestion des sessions Redis.

academic-service : Microservice métier.

api-gateway : Point d'entrée principal.

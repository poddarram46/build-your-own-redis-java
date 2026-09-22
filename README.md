# 🚀 Build Your Own Redis in Java

A lightweight, Redis-inspired in-memory key-value server built from scratch in Java to understand how a real data store works internally — including TCP networking, concurrent clients, TTL, persistence, caching, transactions, Pub/Sub, and multiple data structures.

> **Built from scratch in Java — without using Redis as the backend.**

---

## 🎯 Why I Built This

Instead of only using Redis as a developer tool, I wanted to understand what happens **behind the commands**.

This project explores the core engineering ideas behind a Redis-like server:

* How clients communicate with a server over TCP
* How commands are parsed and executed
* How multiple clients can work concurrently
* How data can expire automatically
* How data survives server restarts
* How caching can improve repeated reads
* How transactions and Pub/Sub work
* How thread safety is handled in a shared in-memory system

---

## ✨ Features

| Feature              | Implementation                             |
| -------------------- | ------------------------------------------ |
| `SET / GET`          | String key-value storage                   |
| `DEL / EXISTS`       | Key management                             |
| `EXPIRE / TTL`       | Time-based key expiration                  |
| `LIST`               | `LPUSH`, `RPUSH`, `LPOP`, `RPOP`, `LRANGE` |
| `HASH`               | `HSET`, `HGET`, `HDEL`, `HGETALL`          |
| `SET`                | `SADD`, `SREM`, `SISMEMBER`, `SMEMBERS`    |
| TCP Networking       | Java `ServerSocket` / `Socket`             |
| Multiple Clients     | Thread pool                                |
| Thread Safety        | Synchronized engine operations             |
| LRU Cache            | `LinkedHashMap` access-order cache         |
| AOF Persistence      | Append-only command log                    |
| Snapshot Persistence | File-based database snapshots              |
| Transactions         | `MULTI`, `EXEC`, `DISCARD`                 |
| Pub/Sub              | `SUBSCRIBE`, `PUBLISH`, `UNSUBSCRIBE`      |
| Command Parser       | Validation + error handling                |
| Recovery             | State restoration after restart            |

---

## 🏗️ Architecture

```text
                    ┌──────────────────────┐
                    │      RedisClient     │
                    │   CLI / TCP Client   │
                    └──────────┬───────────┘
                               │
                               │ TCP
                               ▼
                    ┌──────────────────────┐
                    │     RedisServer      │
                    │    ServerSocket      │
                    └──────────┬───────────┘
                               │
                     Thread Pool / Workers
                               │
                               ▼
                    ┌──────────────────────┐
                    │    ClientHandler     │
                    │  Command Processing  │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │     RedisEngine      │
                    │                      │
                    │ SET / GET / DEL      │
                    │ TTL / EXPIRE         │
                    │ LIST / HASH / SET    │
                    │ Parser / Validation  │
                    └──────┬───────┬───────┘
                           │       │
              ┌────────────┘       └────────────┐
              ▼                                 ▼
       ┌──────────────┐                  ┌──────────────┐
       │   LRU Cache  │                  │ Persistence  │
       │              │                  │              │
       │ Fast Reads   │                  │ AOF          │
       └──────────────┘                  │ Snapshot     │
                                         └──────────────┘

              Additional Components
              ├── TransactionManager
              ├── PubSubManager
              ├── RedisList
              ├── RedisHash
              └── RedisSet
```

---

## 🔄 Request Flow

A typical request follows this path:

```text
Client
  │
  │  SET name Ram
  ▼
TCP Socket
  │
  ▼
ClientHandler
  │
  ▼
Command Parser
  │
  ▼
RedisEngine
  │
  ├── Update in-memory state
  ├── Update LRU cache
  └── Append command to AOF
  │
  ▼
Response
  │
  ▼
Client
```

---

## 🧠 Core Technical Concepts

### 1. TCP Client-Server Communication

The server uses Java networking APIs:

```java
ServerSocket
Socket
```

The server listens on port `6379` and accepts incoming client connections.

Each client is handled independently through the server's thread pool.

---

### 2. Concurrent Client Handling

The server uses:

```java
Executors.newFixedThreadPool(10)
```

This allows multiple clients to communicate concurrently without creating an unlimited number of threads.

```text
Client 1 ──┐
Client 2 ──┤
Client 3 ──┼──> Thread Pool ──> ClientHandler
Client 4 ──┤
Client 5 ──┘
```

---

### 3. Thread Safety

The shared Redis engine contains mutable state accessed by multiple client threads.

Critical operations are synchronized to prevent inconsistent updates.

The project also uses concurrent collections such as:

```java
ConcurrentHashMap
ConcurrentHashMap.newKeySet()
```

---

### 4. TTL / Expiration

Keys can have an expiration time:

```text
SET session Ram
EXPIRE session 10
TTL session
```

The engine stores the expiration timestamp and checks it when accessing the key.

Possible TTL states:

```text
-1  → Key exists but has no expiration
-2  → Key does not exist / already expired
N   → Remaining lifetime in seconds
```

---

## 💾 Persistence

The project implements two persistence mechanisms.

### AOF — Append Only File

Commands are recorded in:

```text
redis.aof
```

Example:

```text
SET name Ram
EXPIREAT session 1760000000000
DEL oldKey
```

When the server starts again, the AOF is replayed to reconstruct state.

---

### Snapshot Persistence

The project also supports:

```text
SAVE
```

which writes the current string database state to:

```text
redis.snapshot
```

The snapshot also stores expiration timestamps where applicable.

On restart:

```text
Snapshot
   ↓
Load database
   ↓
Restore expiration
   ↓
Replay AOF
   ↓
Server ready
```

---

## ⚡ LRU Cache

A small LRU cache is implemented using:

```java
LinkedHashMap
```

with access-order enabled.

```text
GET key
   │
   ▼
Check LRU Cache
   │
   ├── Hit  → return cached value
   │
   └── Miss
         │
         ▼
      Database
         │
         ▼
      Cache value
```

The cache has a fixed capacity of `100` entries.

---

## 🗃️ Supported Data Structures

### Strings

```text
SET name Ram
GET name
DEL name
EXISTS name
```

### Lists

```text
LPUSH fruits apple
RPUSH fruits mango
LRANGE fruits 0 -1
LPOP fruits
RPOP fruits
```

### Hashes

```text
HSET user name Ram
HSET user age 21
HGET user name
HGETALL user
HDEL user age
```

### Sets

```text
SADD skills Java
SADD skills Python
SISMEMBER skills Java
SMEMBERS skills
SREM skills Java
```

---

## 🔄 Transactions

The project supports a basic transaction workflow:

```text
MULTI
SET name Ram
SET age 21
EXEC
```

Commands issued between `MULTI` and `EXEC` are queued and executed when `EXEC` is received.

Transactions can also be cancelled:

```text
MULTI
SET name Ram
DISCARD
```

---

## 📡 Pub/Sub

Clients can subscribe to channels:

```text
SUBSCRIBE news
```

Another client can publish:

```text
PUBLISH news Hello
```

Subscribed clients receive:

```text
Message [news]: Hello
```

The project was tested with separate publisher and subscriber clients.

---

## 🧪 Testing

The following scenarios were manually verified:

### Basic Operations

```text
SET → GET → EXISTS → DEL
```

### Expiration

```text
SET
EXPIRE
TTL
GET after expiration
```

Expired keys correctly returned:

```text
(nil)
```

### Multiple Clients

A value written by one client was successfully read and updated by another connected client.

### Persistence

Both AOF and snapshot recovery were tested across server restarts.

### TTL Persistence

Expiration timestamps were persisted and restored so that remaining TTL continued after restart.

### Pub/Sub

Publisher and subscriber clients were tested independently.

### Parser Validation

Invalid commands and invalid numeric arguments return readable errors.

---

## 🛠️ Tech Stack

**Language**

* Java

**Networking**

* TCP
* `ServerSocket`
* `Socket`

**Concurrency**

* `ExecutorService`
* Thread Pool
* `synchronized`
* `ConcurrentHashMap`

**Data Structures**

* HashMap / ConcurrentHashMap
* LinkedList
* HashSet
* LinkedHashMap

**Persistence**

* File I/O
* AOF
* Snapshot

**Development**

* Java Compiler
* PowerShell
* Git / GitHub

---

## 📁 Project Structure

```text
Build-Your-Own-Redis-in-Java/
│
├── src/
│   ├── Main.java
│   ├── RedisServer.java
│   ├── RedisClient.java
│   ├── ClientHandler.java
│   ├── RedisEngine.java
│   ├── RedisList.java
│   ├── RedisHash.java
│   ├── RedisSet.java
│   ├── LRUCache.java
│   ├── AOFManager.java
│   ├── SnapshotManager.java
│   ├── TransactionManager.java
│   ├── PubSubManager.java
│   ├── RedisCommand.java
│   └── SetCommand.java
│
├── README.md
├── .gitignore
└── ...
```

---

## ▶️ How to Run

### 1. Clone the repository

```bash
git clone <YOUR_REPOSITORY_URL>
cd Build-Your-Own-Redis-in-Java
```

### 2. Compile

```powershell
javac src\*.java
```

### 3. Start the server

```powershell
java -cp src Main
```

Expected:

```text
Mini Redis Server Started
Port: 6379
Waiting for clients...
```

### 4. Start the client

Open another terminal:

```powershell
java -cp src RedisClient
```

You should see:

```text
Welcome to Mini Redis!
redis>
```

---

## 💻 Example Session

```text
redis> SET name Ram
OK

redis> GET name
Ram

redis> EXISTS name
1

redis> EXPIRE name 10
1

redis> TTL name
9

redis> GET name
(nil)
```

---

## ⚠️ Current Limitations

This project focuses on learning Redis-like internals rather than being a production replacement for Redis.

Current limitations include:

* Simplified command protocol compared with the real Redis RESP protocol
* Basic transaction implementation
* Limited data-type persistence compared with native Redis
* No authentication
* No replication
* No clustering
* No production-grade configuration system
* No benchmark suite comparable to Redis `redis-benchmark`

These limitations define possible future improvements rather than hidden shortcomings.

---

## 🔮 Future Improvements

Potential extensions:

* RESP protocol support
* More Redis commands
* Full persistence for all data structures
* Better transaction semantics
* Authentication
* Replication
* Connection pooling
* Non-blocking I/O
* Configurable thread pool
* Automated unit/integration tests
* JMH-based benchmarking
* Metrics and monitoring

---

## 📌 What I Learned

Building this project helped me understand practical concepts beyond syntax:

* Object-oriented design
* TCP networking
* Client-server architecture
* Concurrency and thread safety
* Thread pools
* In-memory data stores
* Caching strategies
* TTL and expiration
* File-based persistence
* Recovery mechanisms
* Transactions
* Publish/Subscribe architecture
* Command parsing
* Data-structure design

---

## 👨‍💻 Author

**Ram Kumar**

B.Tech Computer Science Engineering

**Focus Areas**

* Software Development
* Data Structures & Algorithms
* Computer Networks
* Database Systems
* Problem Solving

---

## ⭐ Project Goal

The goal of this project was not to reproduce the entire Redis codebase.

The goal was to **build a working Redis-inspired server from first principles and understand the engineering concepts behind an in-memory data store.**

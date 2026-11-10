# Build Your Own Redis in Java

A lightweight, Redis-inspired in-memory key-value server built from scratch in Java to implement core distributed systems concepts—including TCP networking, concurrent client handling, TTL expiration, persistence, LRU caching, transactions, and Pub/Sub.

---

## Terminal Demo

*(Tip: GitHub editor mein apne terminal ka 30-second video ya screenshot yahan drag-and-drop kar dein jisme server aur 2 clients ek saath chal rahe hon)*

---

## Core Engineering Highlights

- **TCP Client-Server Protocol:** Built custom socket communication over port `6379` using Java `ServerSocket` and `Socket` APIs.
- **Concurrent Request Handling:** Managed multiple simultaneous client connections using a fixed thread pool (`Executors.newFixedThreadPool(10)`).
- **Thread-Safe Shared State:** Prevented race conditions across threads using `ConcurrentHashMap`, `ConcurrentHashMap.newKeySet()`, and synchronized critical sections.
- **Dual Persistence & Recovery:** Implemented Append-Only File (`redis.aof`) command logging and point-in-time `SAVE` snapshots (`redis.snapshot`) with TTL restoration across server restarts.
- **LRU Read Caching:** Added a bounded 100-entry Least Recently Used (LRU) cache using access-ordered `LinkedHashMap` to accelerate repeated reads.
- **ACID-Inspired Transactions & Pub/Sub:** Built command queuing (`MULTI`, `EXEC`, `DISCARD`) and real-time message broadcasting (`SUBSCRIBE`, `PUBLISH`, `UNSUBSCRIBE`).

---

## Supported Commands & Features

| Category | Commands / Mechanism | Description |
| :--- | :--- | :--- |
| **Strings** | `SET`, `GET`, `DEL`, `EXISTS` | Basic key-value storage, lookup, and deletion |
| **Expiration** | `EXPIRE`, `TTL` | Time-based key expiration with lazy/active timestamp checks |
| **Lists** | `LPUSH`, `RPUSH`, `LPOP`, `RPOP`, `LRANGE` | Doubly-linked list operations for queues and stacks |
| **Hashes** | `HSET`, `HGET`, `HDEL`, `HGETALL` | Field-value map storage for structured objects |
| **Sets** | `SADD`, `SREM`, `SISMEMBER`, `SMEMBERS` | Unordered unique string collections |
| **Transactions** | `MULTI`, `EXEC`, `DISCARD` | Atomic batch execution and rollback of queued commands |
| **Pub/Sub** | `SUBSCRIBE`, `PUBLISH`, `UNSUBSCRIBE` | Channel-based real-time messaging across clients |
| **Persistence** | `SAVE`, AOF Replay | Crash recovery via snapshot loading and AOF log replay |

---

## System Architecture

```text
                    ┌──────────────────────┐
                    │      RedisClient     │
                    │   CLI / TCP Client   │
                    └──────────┬───────────┘
                               │
                               │ TCP (Port 6379)
                               ▼
                    ┌──────────────────────┐
                    │     RedisServer      │
                    │    ServerSocket      │
                    └──────────┬───────────┘
                               │
                     Thread Pool (10 Workers)
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
       │ (Cap: 100)   │                  │ Snapshot     │
       └──────────────┘                  └──────────────┘

              Additional Components
              ├── TransactionManager
              ├── PubSubManager
              ├── RedisList
              ├── RedisHash
              └── RedisSet
```

---

## Request Execution Flow

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
Command Parser (Validation & Error Handling)
  │
  ▼
RedisEngine (Thread-Safe Execution)
  ├── 1. Update in-memory data store
  ├── 2. Update LRU cache
  └── 3. Append mutation command to redis.aof
  │
  ▼
Response ("OK")
  │
  ▼
Client
```

---

## Core Technical Concepts

### 1. Concurrency & Thread Safety
- Uses `Executors.newFixedThreadPool(10)` so incoming TCP connections are handled concurrently without unbounded thread creation.
- Shared mutable state inside `RedisEngine` is protected via thread-safe collections (`ConcurrentHashMap`) and synchronized blocks to ensure data consistency under multi-client load.

### 2. TTL & Expiration States
- Keys store exact expiration epoch timestamps and validate expiration on access.
- `TTL <key>` returns standard Redis status codes:
  - `-1`: Key exists with no expiration set.
  - `-2`: Key does not exist or has already expired.
  - `N`: Remaining time-to-live in seconds.

### 3. Persistence & Crash Recovery
- **AOF (`redis.aof`):** Logs every write/mutation command sequentially.
- **Snapshot (`redis.snapshot`):** Triggered via `SAVE` to serialize current database state and expiration timestamps.
- **Startup Recovery Sequence:**
  `Load Snapshot` -> `Restore Expiration Timestamps` -> `Replay AOF Logs` -> `Server Ready`

### 4. LRU Read Cache
- Implemented using Java's `LinkedHashMap` with `accessOrder = true` and a fixed capacity of 100 entries.
- `GET` requests check the LRU cache first (`Cache Hit`); on a `Cache Miss`, data is fetched from the main store and cached for subsequent fast reads.

---

## Command Examples

### Strings & TTL
```text
SET session Ram
EXPIRE session 10
TTL session
GET session
```

### Lists, Hashes & Sets
```text
# Lists
LPUSH fruits apple
RPUSH fruits mango
LRANGE fruits 0 -1

# Hashes
HSET user name Ram
HSET user role SDE
HGETALL user

# Sets
SADD skills Java
SADD skills DSA
SMEMBERS skills
```

### Transactions
```text
MULTI
SET account_a 500
SET account_b 1000
EXEC
```

### Pub/Sub
```text
# Client 1 (Subscriber)
SUBSCRIBE notifications

# Client 2 (Publisher)
PUBLISH notifications "Deployment Complete"
```

---

## Testing & Verification

- **Multi-Client Concurrency:** Verified simultaneous reads and writes across multiple connected TCP clients without data corruption.
- **TTL & Restart Recovery:** Verified keys expire accurately and remaining TTL values survive server restarts via Snapshot + AOF replay.
- **Transaction Atomicity:** Tested command queuing (`MULTI`/`EXEC`) and rollback (`DISCARD`).
- **Pub/Sub Isolation:** Tested independent publisher and subscriber clients communicating over named channels.
- **Input Validation:** Verified malformed commands and invalid numeric arguments return descriptive error messages without crashing the server.

---

## Tech Stack

- **Language:** Java (Core Java, OOP)
- **Networking:** TCP/IP, `ServerSocket`, `Socket`
- **Concurrency:** `ExecutorService`, Thread Pool, `synchronized`, `ConcurrentHashMap`
- **Data Structures:** `HashMap`, `LinkedHashMap` (LRU), `LinkedList`, `HashSet`
- **Storage & I/O:** Java File I/O (AOF & Snapshot Persistence)

---

## Project Structure

```text
build-your-own-redis-java/
├── src/
│   ├── Main.java                 # Application entry point
│   ├── RedisServer.java          # TCP ServerSocket & thread pool manager
│   ├── RedisClient.java          # Interactive CLI TCP client
│   ├── ClientHandler.java        # Per-client socket I/O handler
│   ├── RedisEngine.java          # Core thread-safe command execution engine
│   ├── RedisList.java            # List data structure implementation
│   ├── RedisHash.java            # Hash data structure implementation
│   ├── RedisSet.java             # Set data structure implementation
│   ├── LRUCache.java             # Bounded access-order LRU cache
│   ├── AOFManager.java           # Append-only file logger & replayer
│   ├── SnapshotManager.java      # Snapshot serialization & recovery
│   ├── TransactionManager.java   # MULTI/EXEC/DISCARD queue handler
│   ├── PubSubManager.java        # Channel subscription & broadcast manager
│   ├── RedisCommand.java         # Command parser & validator
│   └── SetCommand.java           # Set operation helper
├── .gitignore
└── README.md
```

---

## How to Run

### Prerequisites
- Java Development Kit (JDK 11 or higher)

### 1. Clone the Repository
```bash
git clone https://github.com
cd build-your-own-redis-java
```

### 2. Compile the Source Code
```bash
javac -d out src/*.java
```

### 3. Start the Redis Server
```bash
java -cp out Main
```
**Expected Output:**
```text
Mini Redis Server started on port 6379...
```

### 4. Connect Clients (Open a New Terminal)
Open one or more separate terminals to test concurrent clients and Pub/Sub:
```bash
java -cp out RedisClient
```
**Sample Session:**
```text

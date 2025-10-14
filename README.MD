# Accounting Ledger — Java CLI + Web UI

A simple, auditable personal accounting ledger built in Java 17.

It includes a CLI for entering and reporting on transactions and a lightweight web UI (served by Javalin) for viewing
All/Deposits/Payments and date-range results in a polished table.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Data Format](#data-format)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Run: CLI](#run-cli)
    - [Run: Web UI](#run-web-ui)
- [Web API Endpoints](#web-api-endpoints)
- [Frontend (HTML/CSS/JS)](#frontend-htmlcssjs)
- [License](#license)

## Features

- **Transaction model** with date, time, description, vendor, amount, and computed type (debit for deposits > 0, credit
  for payments < 0).
- **Robust CSV/pipe ingestion** that tolerates header lines anywhere and skips malformed rows.
    - Prevents duplicate transactions to be added to ledger which is very important for financial institutions.

- **Reports**:
    - Month-to-Date, Previous Month
    - Year-to-Date, Previous Year
    - Custom search by date range, description, vendor, exact amount
- **Multi-user permissions**
    - Admin can view all users’ transactions (Super User permissions)
    - Regular users see only their transactions and their transactions are associated with only them
    - New transactions are saved with the current user’s userId
- **Sorted output** (latest first) everywhere.
- **Web UI**:
    - Three quick views (All, Deposits, Payments)
    - Date-range query
    - Clean, dark, responsive design with summary KPIs
- **Web API (JSON)** served by Javalin.

## Tech Stack

- **Language**: Java 17
- **Build**: Maven
- **Server**: Javalin 5
- **JSON**: Jackson (basic databind; date/time serialized as strings via DTO)
- **Frontend**: Vanilla HTML/CSS/JS (no framework)

## Project Structure

```text
src/
 └─ main/
     ├─ java/com/pluralsight/
     │   ├─ AccountingLedgerApplication.java  # CLI App entrypoint
     │   ├─ Utilities.java                    # Shared helpers (I/O, filters, permissions)
     │   ├─ Transaction.java                  # Transaction Class
     │   ├─ User.java                         # User Class (id, name, pin, adminAccess)
     │   ├─ DataStore.java                    # In-memory ledger
     │   └─ WebServer.java                    # Javalin server + API routes
     └─ resources/
         └─ public/
             ├─ index.html                    # Web UI
             ├─ styles.css                    # Theme styles
             └─ app.js                        # UI logic (fetch + render)
```

## Data Format

Transactions are stored as pipe-delimited lines in 'transactions.cvs':

```
date|time|description|vendor|amount
2025-10-12|12:21:46|Taxes|IRS|-500.00
2025-10-12|12:21:24|Tax Refund|IRS|345.21
2025-10-09|00:18:45|invoice 2043 paid|Client A|950.00
```

Date: YYYY-MM-DD

Time: HH:mm:ss (always 2-digit padding; seconds included)

Amount: deposits positive, payments negative

Even if header lines appear anywhere; the importer auto-skips them.

User Profiles are stored as pipe-delimited lines in 'profiles.cvs':

```
userid|name|pin|access
1234|Ahsan Baseer|0000|true
4211|Billy Bob|1212|false
```

User ID: 4 digit int

Name: full name string

PIN: String

## Getting Started

### Prerequisites

* Java 17+
* JDK 17 Coretto
* IntelliJ IDE Community Edition (recommended)

### Run: CLI

Run the CLI main class from your IDE by running the AccountingLedgerApplication.java file

### Run: Web UI

Run the Web UI by running the WebServer.java from your IDE. This will start the server at:

```bash

http://localhost:8080
```

The UI loads and immediately fetches ***/api/transactions***.

## Web API Endpoints

```
GET /api/health → "ok"

GET /api/transactions → All transactions (newest first)

GET /api/transactions/deposits → Only amount > 0

GET /api/transactions/payments → Only amount < 0

GET /api/transactions/user/{userID} → All transactions by user id

GET /api/transactions/range?start=YYYY-MM-DD&end=YYYY-MM-DD → Date-range results (inclusive)
```

## Frontend (HTML/CSS/JS)

* Loads All/Deposits/Payments via the chips.
* Date-range picker calls /api/transactions/range.
* Renders summary KPIs (balance, totals, count).

## Backend

### Architecture

Entry (web): WebServer.java
Starts Javalin, serves static assets, exposes /api/* routes.

Business logic: Utilities.java
CSV I/O, visibility filtering (canView), search/report helpers, user/session state for CLI mode.

Models: Transaction, User

Transaction implements equals/hashCode for de-dup at load time.

User holds id, name, pin (String), adminAccess (boolean).

In-memory store: DataStore.java

## License

MIT License. Feel free to use, modify, and share.
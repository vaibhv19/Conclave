# DB_Schema.md — Conclave Persistence Model

This document defines the relational database schema for **Conclave**. The schema is designed to support a unified conversation history across multiple AI providers, dynamic role-to-model mapping, and the persistence of the summarized `WorkflowState`.

---

## 1. Entity Relationship Diagram (ASCII)

```text
                     +------------------+
                     |      users       |
                     +------------------+
                                | 1
                                |
                                | M
                     +------------------+         +-----------------------+
                     |      rooms       +---------+    workflow_state     |
                     +--------┬---------+ 1     1 +-----------------------+
                              |
             +----------------+----------------+
             | 1              | 1              | 1
    +--------▼--------+  +----▼-----------+  +-▼------------------+
    |role_assignments |  | conversation_  |  |  token_usage_log   |
    +-----------------+  |    history     |  +----------┬---------+
                         +--------┬-------+            |
                                  | 1                  |
                                  +--------------------+ 1
```

---

## 2. Table Dictionary

1.  **`users`**: Identity and credential storage for room owners and participants.
2.  **`rooms`**: Core meeting room instances, including the project objective and current operational status (Active/Paused).
3.  **`role_assignments`**: The dynamic registry mapping specific Roles (e.g., "Lead Writer") to specific Models (e.g., "GEMINI_PRO") per room.
4.  **`conversation_history`**: Canonical-format storage of all user and AI turns, independent of provider-specific API shapes.
5.  **`workflow_state`**: The current "source of truth" context (Drafts, Tasks, Comments) passed to adapters to minimize context-window usage.
6.  **`token_usage_log`**: Detailed metrics tracking of prompt and completion tokens per turn for both real and mocked providers.

---

## 3. Table Specifications

### 3.1 `users`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Unique user identifier. |
| `email` | `VARCHAR(255)` | UNIQUE, NOT NULL | User login identifier. |
| `password_hash` | `VARCHAR(255)` | NOT NULL | BCrypt hashed credentials. |
| `name` | `VARCHAR(100)` | NOT NULL | Display name for the UI. |

### 3.2 `rooms`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Unique room identifier. |
| `owner_id` | `UUID` | FK (users.id) | The user who created the room. |
| `name` | `VARCHAR(255)` | NOT NULL | Room display name. |
| `objective` | `TEXT` | NOT NULL | The primary task/goal for the session. |
| `status` | `VARCHAR(50)` | NOT NULL | `ACTIVE`, `PAUSED`, or `ARCHIVED`. |

### 3.3 `role_assignments`
*This table implements the dynamic registry requested in the feature list.*
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Unique assignment identifier. |
| `room_id` | `UUID` | FK (rooms.id) | Associated meeting room. |
| `role_name` | `VARCHAR(100)` | NOT NULL | Custom role (e.g., "Code Reviewer"). |
| `model_id` | `VARCHAR(100)` | NOT NULL | Provider ID (e.g., `MOCK_CLAUDE`). |
| `ui_color_hex` | `CHAR(7)` | NOT NULL | Color used for chat bubbles in the UI. |

### 3.4 `conversation_history`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Unique message identifier. |
| `room_id` | `UUID` | FK (rooms.id) | Associated meeting room. |
| `sender_type` | `VARCHAR(20)` | NOT NULL | `USER`, `AI`, or `SYSTEM`. |
| `role_name` | `VARCHAR(100)` | NULLABLE | Maps to role_assignments for AI messages. |
| `content` | `TEXT` | NOT NULL | The message body in Markdown. |
| `created_at` | `TIMESTAMP` | NOT NULL | Audit timestamp for chronological sorting. |

### 3.5 `workflow_state`
*Holds the summarized context passed to each adapter.*
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Unique state identifier. |
| `room_id` | `UUID` | FK (rooms.id) | Unique 1:1 relationship with room. |
| `current_draft` | `TEXT` | NULLABLE | The latest version of the generated work. |
| `review_comments` | `TEXT` | NULLABLE | Cumulative list of model/user feedback. |
| `last_updated_at` | `TIMESTAMP` | NOT NULL | Tracks when the state was last re-summarized. |

### 3.6 `token_usage_log`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Unique log identifier. |
| `message_id` | `UUID` | FK (conv_history.id)| Associated turn. |
| `room_id` | `UUID` | FK (rooms.id) | For aggregation by room. |
| `model_id` | `VARCHAR(100)` | NOT NULL | Which model incurred the cost. |
| `prompt_tokens` | `INTEGER` | NOT NULL | Input token count. |
| `completion_tokens` | `INTEGER` | NOT NULL | Output token count. |
| `is_mocked` | `BOOLEAN` | NOT NULL | Tracks if cost is real (Gemini) or heuristic. |

---

## 4. Key Relationships & Constraints

1.  **Room Deletion**: On deleting a `room`, all `role_assignments`, `conversation_history`, and `token_usage_log` entries must be deleted (`ON DELETE CASCADE`).
2.  **Turn Order**: `conversation_history` relies on a `created_at` index to ensure the backend preserves the exact sequence for the `ProviderAdapter` translation.
3.  **Role Uniqueness**: A `UNIQUE` constraint exists on `(room_id, role_name)` within the `role_assignments` table to prevent duplicate @-mention targets in the same room.
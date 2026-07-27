# Context Compression & The Janitor Service Architecture

This document describes the architectural design, summarization prompts, token conservation math, and operation details of the **Janitor Service** responsible for room conversation history pruning and context compression.

---

## 1. The Context Compression (Janitor) Pattern

### 1.1 The Challenge of LLM Context Inflation
In collaborative multi-agent environments, conversation history grows linearly with each turn. This poses several challenges:
1. **Context Window Limitations:** Older models or large room histories can exceed prompt token limits.
2. **Context Dilution:** LLMs pay less attention to details in the middle of extremely long prompts ("Lost in the Middle" phenomenon).
3. **Exploding Costs:** Every message adds to the prompt token count, leading to quadratic increases in token consumption and API billing.

### 1.2 The Janitor Solution
Conclave introduces a context compression mechanism called the **Janitor Service** (implemented via `WorkflowStateService` and qualified Gemini LLM execution).
When a room's active conversation history exceeds **10 messages**, the Janitor is triggered.
It summarizes the history, updates the document draft, and deletes intermediate history entries to optimize the context window.

---

## 2. Operation and Prompt Strategy

### 2.1 Summarization Prompts
When triggered, the Janitor resolves a dedicated Gemini client and provides a structured system summarization template:

```
You are Conclave Janitor, a context compression assistant.
Your task is to review the active conversation history and update the current draft and review comments.

Current Draft:
[Active Draft from DB]

Review Comments:
[Active Review Comments from DB]

New Message History:
[Chronological User + AI messages]

Instructions:
1. Incorporate any agreed-upon changes from the conversation history into the document draft.
2. Extract any unresolved critique points or feedback as review comments.
3. Output the result strictly in JSON format with exactly two keys: 'currentDraft' and 'reviewComments'. Do not include any other conversational filler text.
```

### 2.2 Response Parsing and Resiliency
To handle potential variations in LLM response generation, the parsing engine:
1. Strips markdown block markers (e.g. ````json ... ````).
2. Attempts parsing via Jackson `ObjectMapper`.
3. Falls back gracefully by updating the `currentDraft` with the raw text response and logging a warning to prevent database exceptions or transaction rollbacks.

---

## 3. History Purging Boundaries

Once the draft has been updated, the Janitor purges middle messages in the database (`conversation_history` table). It maintains critical boundaries:
1. **First Message (Context Foundation):** Always preserved (contains the initial user prompt, objective, or base context).
2. **Last 2 Messages (Short-Term Memory):** Always preserved (retains the immediate preceding discussion to maintain conversational continuity).
3. **Middle Messages:** Permanently deleted.

### History State Transition Diagram

```mermaid
graph TD
    subgraph Pre-Cleanup [Pre-Cleanup History (Count > 10)]
        M0[Message 0: Initial Objective]
        M1[Message 1: User Turn]
        M2[Message 2: AI Response]
        M3[Message 3: User Turn]
        M4[Message 4: AI Response]
        M5[Message 5: User Turn]
        M6[Message 6: AI Response]
        M7[Message 7: User Turn]
        M8[Message 8: AI Response]
        M9[Message 9: User Turn]
        M10[Message 10: AI Response]
        
        M0 --> M1 --> M2 --> M3 --> M4 --> M5 --> M6 --> M7 --> M8 --> M9 --> M10
    end

    subgraph Janitor [Janitor Execution]
        J[Gemini Summarizer Runs]
        J --> |Updates| WS[(WorkflowState Table)]
    end

    subgraph Post-Cleanup [Post-Cleanup History (Count = 3)]
        P0[Message 0: Initial Objective]
        P9[Message 9: User Turn]
        P10[Message 10: AI Response]
        
        P0 --> P9 --> P10
        style P0 fill:#4CAF50,stroke:#388E3C,stroke-width:2px,color:#fff
        style P9 fill:#2196F3,stroke:#1976D2,stroke-width:2px,color:#fff
        style P10 fill:#2196F3,stroke:#1976D2,stroke-width:2px,color:#fff
    end

    M1 -.-> |Deleted| J
    M2 -.-> |Deleted| J
    M3 -.-> |Deleted| J
    M4 -.-> |Deleted| J
    M5 -.-> |Deleted| J
    M6 -.-> |Deleted| J
    M7 -.-> |Deleted| J
    M8 -.-> |Deleted| J
    M0 --> |Preserved| P0
    M9 --> |Preserved| P9
    M10 --> |Preserved| P10
```

---

## 4. Token Conservation Math

To demonstrate the efficiency of this pattern, consider a room where the average message length is **400 characters (approx. 100 tokens)**:

### 4.1 Without Context Compression
For turn $N$, the total prompt size is:
$$\text{Tokens}_N = N \times 100$$
For turn 12:
$$\text{Tokens}_{12} = 12 \times 100 = 1200 \text{ tokens}$$
Accumulated token cost for 12 turns:
$$\text{Cost} = \sum_{i=1}^{12} (i \times 100) = 7,800 \text{ tokens}$$

### 4.2 With Context Compression (Janitor triggered at 11)
At Turn 11:
- The history contains **11 messages (1100 tokens)**.
- Janitor summarizes history into a single compact draft summary of **approx. 200 tokens**.
- Middle 8 messages are deleted.
- History size resets to **3 messages (300 tokens)**.

For turn 12 (post-cleanup):
$$\text{Tokens}_{12} = 300 \text{ (history)} + 200 \text{ (compressed draft)} = 500 \text{ tokens}$$
This reduces the prompt size for turn 12 by **over 58%**, saving tokens and dramatically improving model performance and attention focus.

# notification-service

In-app notifications and their delivery preferences.

|           |                                                                    |
|-----------|--------------------------------------------------------------------|
| Port      | 8086                                                               |
| Database  | 5437                                                               |
| Publishes | `mail-requested` (contract owned by mail-service)                  |
| Consumes  | project and task events, `user-registered`, `user-profile-updated` |

It owns no integration events. It is a consumer that turns other contexts' events into
notifications; the only thing it emits is a command asking for an e-mail.

## Messages are keys, not sentences

A notification stores a type and a parameter map, and the client renders the sentence. Storing
text would freeze the language at write time — a notification written while the recipient used
Polish would still be Polish after they switched — and would make this service responsible for
plural rules in languages it knows nothing about.

## Delivery rules

| Rule                                                | Why                                                                         |
|-----------------------------------------------------|-----------------------------------------------------------------------------|
| A muted type produces nothing on any channel        | Silencing in-app and still getting e-mail reads as a bug                    |
| E-mail defaults to invitations and assignments only | The rest is discoverable by opening the app; comment traffic is inbox noise |
| The actor is excluded from their own event          | Nobody needs telling what they just did                                     |
| Missing settings mean defaults, not silence         | A user who never opened settings still expects to hear about assigned work  |
| An archived task retires its notifications          | Otherwise the list offers a link into nothing                               |

## Real-time transport

STOMP over WebSocket at `/ws/notifications`, user destinations `/user/queue/notifications` and
`/user/queue/notifications.unread`.

- **The simple in-memory broker is deliberate.** Fan-out already happens through Kafka, so every
  replica sees every event and pushes only to the sessions it holds. An external relay would add
  a second fan-out mechanism beside the one that works.
- **The handshake authenticates from the `Authorization` header**, like every other request: it
  is an ordinary HTTP upgrade, and the gateway swaps the session cookie for a token before
  routing. The usual `?token=` workaround is unnecessary — the browser holds no token — and
  would be worse, since a token in a URL lands in access logs.
- **Pushes are the best effort.** A broker failure is logged and swallowed: the record is already
  committed and the recipient sees it on their next load. Propagating would roll back a
  notification that was correctly raised.

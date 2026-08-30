# mail-service

Delivers e-mail on request and reports the outcome.

| Property  | Value                      |
|-----------|----------------------------|
| Port      | 8083                       |
| Database  | 5433                       |
| Publishes | `mail-sent`, `mail-failed` |
| Consumes  | `mail-requested`           |

## It owns the `mail-requested` contract

Although it never produces it. The consumer defines what it accepts, and there are two producers
— iam-service for account mail and notification-service for notification mail. Leaving the
schema under the first producer would make the second look like an intruder in somebody else's
namespace.

## Failure is an event, not an exception

A message that cannot be delivered produces `mail-failed`, which is what lets the requesting
saga compensate. Retrying forever inside this service would leave the caller waiting on
something it cannot see.

## Rendering

Templates and the recipient's language live here, because delivery is asynchronous: there is no
request and therefore no language header to read.

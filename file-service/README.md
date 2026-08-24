# file-service

File metadata and signed URLs. The bytes never pass through it.

|                |                                  |
|----------------|----------------------------------|
| Port           | 8088                             |
| Database       | 5439                             |
| Object storage | Garage (S3 API), bucket private  |
| Publishes      | `file-confirmed`, `file-deleted` |
| Consumes       | nothing                          |

## Why the bytes stay out

Streaming a 25 MB attachment through Spring occupies a request thread and heap for the whole
transfer, and this service has nothing to say about the contents. The only thing it can usefully
decide is *whether* the transfer may happen — which is what a pre-signed URL encodes.

The exchange is three steps by necessity: the record is created before the transfer, so nothing
can be written that was not allowed, and confirmed after it against storage, because a client
saying "done" is a claim while a reported object size is evidence.

## Limits live with the scope

`FileScope` carries its own cap and type list, so "how big may this be" has one answer rather
than one per calling service. Attachments accept any type deliberately — a project may need a
format nobody anticipated — while avatars and icons do not. The declared size only produces an
early error; the real limit is signed in to the URL.

## Deletion is two steps

A database transaction and an object store do not commit together. A delete marks the record and
a sweep removes the object. Ordered that way on purpose: a crash leaks an object, which the
sweep collects, instead of losing the record that names the key, which nothing could.

The second sweep follows from the same gap — a record left `PENDING` past its window is an
abandoned upload.

## What it does not decide

Whether a project member may read another member's attachment is a project question, answered in
project-service. This service enforces ownership only; duplicating the rule would mean two
copies of an access rule that must never disagree.

See [Files](../docs/files.md).

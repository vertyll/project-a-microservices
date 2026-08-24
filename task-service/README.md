# task-service

Tasks, comments and the board.

|           |                                                                                               |
|-----------|-----------------------------------------------------------------------------------------------|
| Port      | 8085                                                                                          |
| Database  | 5436                                                                                          |
| Publishes | `task-created`, `task-assigned`, `task-status-changed`, `task-archived`, `task-comment-added` |
| Consumes  | project events, `user-registered`, `user-profile-updated`, `file-deleted`                     |

## It owns no projects, categories, statuses or users

All four are referenced by id and mirrored locally from the events that own them. That is what
lets a board render without a call to project-service per row, and keeps task listings working
when that service is down.

The trade is stated rather than hidden: **authorization reads the local membership projection**,
so a member removed a second ago may still pass one check. The alternative is a synchronous call
on every task read, coupling the availability of the two services.

`TaskPermission` is this context's own enum, deliberately not project-service's
`ProjectPermission`. Importing another context's enum would make a change over there a compile
break here.

## Two rules that keep the board quiet

- **A no-op status move returns the same instance.** Dragging a card back into its own column
  emits nothing, so it does not reach every watcher as a notification.
- **A removed category or status repairs the tasks that referenced it**, in the same
  transaction. Left behind, the reference renders as a label nobody can display.

The same applies to `file-deleted`: the attachment id is dropped, because a task keeping a
broken download surfaces days later as a bug report.

## The board query

`TaskQueryAdapter` is why CQRS is applied here. One row needs the task, its status label, its
category labels, its assignees and its comment count — five sources. The page is one statement
and the labels are resolved with three more **for the whole page**, not per row.

A label missing in the requested language falls back to whatever the author wrote, reported in
`nameLanguage`. Dropping the row instead would make the chip vanish and look like a deletion.

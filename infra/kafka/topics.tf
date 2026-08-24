locals {
  business_topics = [
    "mail-requested",
    "mail-sent",
    "mail-failed",
    "user-registered",
    "user-profile-updated",
    "saga-compensation-iam",
    "saga-compensation-mail",

    # project bounded context
    "project-created",
    "project-updated",
    "project-archived",
    "project-member-invited",
    "project-member-joined",
    "project-member-removed",
    "project-category-changed",
    "project-status-changed",
    "saga-compensation-project",

    # task bounded context
    "task-created",
    "task-assigned",
    "task-status-changed",
    "task-archived",
    "task-comment-added",
    "saga-compensation-task",

    # notification bounded context
    #
    # No integration events of its own: notification-service is a pure consumer
    # that turns other contexts' events into notifications. The only thing it
    # publishes is `mail-requested`, whose contract belongs to mail-service.
    "saga-compensation-notification",

    # file bounded context — no saga: it takes part in no distributed flow
    "file-confirmed",
    "file-deleted",
  ]
}

resource "kafka_topic" "business" {
  for_each           = toset(local.business_topics)
  name               = each.value
  partitions         = var.partitions
  replication_factor = var.replication_factor
}

resource "kafka_topic" "dlt" {
  for_each           = toset(local.business_topics)
  name               = "${each.value}-dlt"
  partitions         = var.partitions
  replication_factor = var.replication_factor
}

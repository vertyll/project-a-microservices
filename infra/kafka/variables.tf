variable "bootstrap_servers" {
  type        = list(string)
  description = "Kafka bootstrap servers, e.g., [\"localhost:29092\"]"
  default     = ["localhost:29092"]
}

variable "tls_enabled" {
  type        = bool
  description = "Connect to the broker over TLS (SSL listener :9094)"
  default     = false
}

variable "ca_cert_file" {
  type        = string
  description = "Path to the PEM of the CA that signed the broker cert (empty = plain/system trust)"
  default     = ""
}

variable "partitions" {
  type        = number
  description = "Default partition count for topics"
  default     = 1
}

variable "replication_factor" {
  type        = number
  description = "Default replication factor for topics"
  default     = 1
}

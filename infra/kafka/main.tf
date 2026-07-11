terraform {
  required_version = ">= 1.6.0"

  backend "local" {
    path = "/state/terraform.tfstate"
  }

  required_providers {
    kafka = {
      source  = "Mongey/kafka"
      version = ">= 0.6.0"
    }
  }
}

provider "kafka" {
  bootstrap_servers = var.bootstrap_servers
  # TLS toward the broker's SSL listener (:9094). ca_cert holds the PEM of
  # the cluster's internal CA (k8s: mounted internal-ca-cert ConfigMap).
  tls_enabled = var.tls_enabled
  ca_cert     = var.ca_cert_file != "" ? file(var.ca_cert_file) : null
}

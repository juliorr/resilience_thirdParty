variable "name" {
  type        = string
  description = "Name prefix for observability resources"
}

variable "log_retention_days" {
  type        = number
  description = "CloudWatch log group retention"
  default     = 30
}

variable "enable_managed_prometheus" {
  type        = bool
  description = "Create an Amazon Managed Prometheus workspace"
  default     = true
}

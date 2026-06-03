variable "table_name" {
  type        = string
  description = "DynamoDB table name"
  default     = "verification"
}

variable "ttl_enabled" {
  type        = bool
  description = "Enable TTL on expiresAt (audit history keeps it disabled)"
  default     = false
}

variable "name" {
  type        = string
  description = "Name prefix for SSM parameters"
}

variable "table_name" {
  type        = string
  description = "DynamoDB table name to publish"
}

variable "region" {
  type        = string
  description = "AWS region to publish"
}

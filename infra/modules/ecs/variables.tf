variable "name" {
  type        = string
  description = "Name prefix for ECS resources"
}

variable "image" {
  type        = string
  description = "Container image URI (ECR repo:tag)"
}

variable "app_port" {
  type        = number
  description = "Container port"
  default     = 8080
}

variable "cpu" {
  type        = number
  description = "Fargate task CPU units"
  default     = 512
}

variable "memory" {
  type        = number
  description = "Fargate task memory (MiB)"
  default     = 1024
}

variable "desired_count" {
  type        = number
  description = "Baseline task count"
  default     = 2
}

variable "max_count" {
  type        = number
  description = "Maximum task count for autoscaling"
  default     = 6
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_ids" {
  type = list(string)
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "alb_security_group_id" {
  type = string
}

variable "tasks_security_group_id" {
  type = string
}

variable "table_name" {
  type = string
}

variable "dynamodb_access_policy_arn" {
  type = string
}

variable "log_group_name" {
  type = string
}

variable "oauth2_issuer_uri" {
  type        = string
  description = "OIDC issuer URI for JWT validation in the aws profile"
}

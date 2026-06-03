variable "name" {
  type        = string
  description = "Name prefix for network resources"
}

variable "vpc_cidr" {
  type        = string
  description = "CIDR block for the VPC"
  default     = "10.0.0.0/16"
}

variable "app_port" {
  type        = number
  description = "Container port the app listens on"
  default     = 8080
}

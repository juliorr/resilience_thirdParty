variable "region" {
  type        = string
  description = "AWS region"
  default     = "us-east-1"
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "app_port" {
  type    = number
  default = 8080
}

variable "table_name" {
  type    = string
  default = "verification"
}

variable "image" {
  type        = string
  description = "Container image URI; defaults to the ECR repo :latest when empty"
  default     = ""
}

variable "oauth2_issuer_uri" {
  type        = string
  description = "OIDC issuer URI used by the aws Spring profile for JWT validation"
}

variable "third_party_base_url" {
  type        = string
  description = "Base URL of the external third-party provider API consumed by verification-service"
}

variable "github_owner" {
  type        = string
  description = "GitHub org/user that owns the deploy repository"
  default     = "juliorr"
}

variable "github_repo" {
  type        = string
  description = "GitHub repository name (without owner)"
  default     = "resilience_thirdParty"
}

variable "create_github_oidc_provider" {
  type        = bool
  description = "Create the GitHub Actions OIDC provider. Set false if one already exists in the account."
  default     = true
}

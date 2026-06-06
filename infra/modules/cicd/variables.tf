variable "name" {
  type        = string
  description = "Name prefix for CI/CD resources (e.g. verification)"
}

variable "github_owner" {
  type        = string
  description = "GitHub org/user that owns the repository"
}

variable "github_repo" {
  type        = string
  description = "GitHub repository name (without owner)"
}

variable "create_oidc_provider" {
  type        = bool
  description = "Create the account-wide GitHub Actions OIDC provider. Set false to reference an existing one."
  default     = true
}

variable "allowed_ref" {
  type        = string
  description = "Git ref allowed to assume the deploy role (sub claim), e.g. refs/heads/main"
  default     = "refs/heads/main"
}

variable "ecr_repository_arn" {
  type        = string
  description = "ARN of the ECR repository the deploy role may push to"
}

variable "ecs_execution_role_arn" {
  type        = string
  description = "ARN of the ECS task execution role (for iam:PassRole)"
}

variable "ecs_task_role_arn" {
  type        = string
  description = "ARN of the ECS task role (for iam:PassRole)"
}

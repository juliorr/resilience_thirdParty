terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    github = {
      source  = "integrations/github"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.region
}

# Auth via GITHUB_TOKEN env var (e.g. export GITHUB_TOKEN=$(gh auth token)).
# Token needs `repo` scope to write Actions secrets.
provider "github" {
  owner = var.github_owner
}

locals {
  name = "verification"
}

module "network" {
  source   = "../../modules/network"
  name     = local.name
  vpc_cidr = var.vpc_cidr
  app_port = var.app_port
}

module "ecr" {
  source = "../../modules/ecr"
  name   = local.name
}

module "dynamodb" {
  source     = "../../modules/dynamodb"
  table_name = var.table_name
}

module "config" {
  source     = "../../modules/config"
  name       = local.name
  table_name = module.dynamodb.table_name
  region     = var.region
}

module "observability" {
  source = "../../modules/observability"
  name   = local.name
}

module "ecs" {
  source                     = "../../modules/ecs"
  name                       = local.name
  image                      = var.image != "" ? var.image : "${module.ecr.repository_url}:latest"
  app_port                   = var.app_port
  vpc_id                     = module.network.vpc_id
  public_subnet_ids          = module.network.public_subnet_ids
  private_subnet_ids         = module.network.private_subnet_ids
  alb_security_group_id      = module.network.alb_security_group_id
  tasks_security_group_id    = module.network.tasks_security_group_id
  table_name                 = module.dynamodb.table_name
  dynamodb_access_policy_arn = module.dynamodb.access_policy_arn
  log_group_name             = module.observability.log_group_name
  oauth2_issuer_uri          = var.oauth2_issuer_uri
  third_party_base_url       = var.third_party_base_url
}

module "cicd" {
  source                 = "../../modules/cicd"
  name                   = local.name
  github_owner           = var.github_owner
  github_repo            = var.github_repo
  create_oidc_provider   = var.create_github_oidc_provider
  ecr_repository_arn     = module.ecr.repository_arn
  ecs_execution_role_arn = module.ecs.execution_role_arn
  ecs_task_role_arn      = module.ecs.task_role_arn
}

# Set AWS_DEPLOY_ROLE_ARN in the repo so the deploy workflow can assume the role.
resource "github_actions_secret" "deploy_role_arn" {
  repository      = var.github_repo
  secret_name     = "AWS_DEPLOY_ROLE_ARN"
  plaintext_value = module.cicd.deploy_role_arn
}

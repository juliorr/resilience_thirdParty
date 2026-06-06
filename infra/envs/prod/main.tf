terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
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

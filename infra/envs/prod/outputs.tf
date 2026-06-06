output "alb_dns_name" {
  value = module.ecs.alb_dns_name
}

output "ecr_repository_url" {
  value = module.ecr.repository_url
}

output "dynamodb_table_name" {
  value = module.dynamodb.table_name
}

output "ecs_cluster_name" {
  value = module.ecs.cluster_name
}

output "ecs_service_name" {
  value = module.ecs.service_name
}

output "deploy_role_arn" {
  value = module.cicd.deploy_role_arn
}

output "log_group_name" {
  value = aws_cloudwatch_log_group.app.name
}

output "prometheus_workspace_id" {
  value = var.enable_managed_prometheus ? aws_prometheus_workspace.this[0].id : null
}

output "prometheus_remote_write_endpoint" {
  value = var.enable_managed_prometheus ? "${aws_prometheus_workspace.this[0].prometheus_endpoint}api/v1/remote_write" : null
}

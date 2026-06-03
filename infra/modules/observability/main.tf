resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.name}"
  retention_in_days = var.log_retention_days
}

resource "aws_prometheus_workspace" "this" {
  count = var.enable_managed_prometheus ? 1 : 0
  alias = "${var.name}-amp"
}
